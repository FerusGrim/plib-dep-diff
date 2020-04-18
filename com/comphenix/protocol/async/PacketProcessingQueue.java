package com.comphenix.protocol.async;

import java.util.Iterator;
import java.util.Collection;
import com.comphenix.protocol.injector.PrioritizedListener;
import com.comphenix.protocol.events.PacketEvent;
import java.util.PriorityQueue;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.collect.MinMaxPriorityQueue;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import com.comphenix.protocol.error.ReportType;
import com.comphenix.protocol.concurrency.AbstractConcurrentListenerMultimap;

class PacketProcessingQueue extends AbstractConcurrentListenerMultimap<AsyncListenerHandler>
{
    public static final ReportType REPORT_GUAVA_CORRUPT_MISSING;
    public static final int INITIAL_CAPACITY = 64;
    public static final int DEFAULT_MAXIMUM_CONCURRENCY = 32;
    public static final int DEFAULT_QUEUE_LIMIT = 61440;
    private final int maximumConcurrency;
    private Semaphore concurrentProcessing;
    private Queue<PacketEventHolder> processingQueue;
    private PlayerSendingHandler sendingHandler;
    
    public PacketProcessingQueue(final PlayerSendingHandler sendingHandler) {
        this(sendingHandler, 64, 61440, 32);
    }
    
    public PacketProcessingQueue(final PlayerSendingHandler sendingHandler, final int initialSize, final int maximumSize, final int maximumConcurrency) {
        try {
            this.processingQueue = Synchronization.queue((Queue<PacketEventHolder>)MinMaxPriorityQueue.expectedSize(initialSize).maximumSize(maximumSize).create(), null);
        }
        catch (IncompatibleClassChangeError e) {
            ProtocolLibrary.getErrorReporter().reportWarning(this, Report.newBuilder(PacketProcessingQueue.REPORT_GUAVA_CORRUPT_MISSING).error(e));
            this.processingQueue = Synchronization.queue(new PriorityQueue<PacketEventHolder>(), null);
        }
        this.maximumConcurrency = maximumConcurrency;
        this.concurrentProcessing = new Semaphore(maximumConcurrency);
        this.sendingHandler = sendingHandler;
    }
    
    public boolean enqueue(final PacketEvent packet, final boolean onMainThread) {
        try {
            this.processingQueue.add(new PacketEventHolder(packet));
            this.signalBeginProcessing(onMainThread);
            return true;
        }
        catch (IllegalStateException e) {
            return false;
        }
    }
    
    public int size() {
        return this.processingQueue.size();
    }
    
    public void signalBeginProcessing(final boolean onMainThread) {
        while (this.concurrentProcessing.tryAcquire()) {
            final PacketEventHolder holder = this.processingQueue.poll();
            if (holder == null) {
                this.signalProcessingDone();
                return;
            }
            final PacketEvent packet = holder.getEvent();
            final AsyncMarker marker = packet.getAsyncMarker();
            final Collection<PrioritizedListener<AsyncListenerHandler>> list = this.getListener(packet.getPacketType());
            marker.incrementProcessingDelay();
            if (list != null) {
                final Iterator<PrioritizedListener<AsyncListenerHandler>> iterator = list.iterator();
                if (iterator.hasNext()) {
                    marker.setListenerTraversal(iterator);
                    iterator.next().getListener().enqueuePacket(packet);
                    continue;
                }
            }
            if (marker.decrementProcessingDelay() == 0) {
                final PacketSendingQueue sendingQueue = this.sendingHandler.getSendingQueue(packet, false);
                if (sendingQueue != null) {
                    sendingQueue.signalPacketUpdate(packet, onMainThread);
                }
            }
            this.signalProcessingDone();
        }
    }
    
    public void signalProcessingDone() {
        this.concurrentProcessing.release();
    }
    
    public int getMaximumConcurrency() {
        return this.maximumConcurrency;
    }
    
    public void cleanupAll() {
        for (final PrioritizedListener<AsyncListenerHandler> handler : this.values()) {
            if (handler != null) {
                handler.getListener().cancel();
            }
        }
        this.clearListeners();
        this.processingQueue.clear();
    }
    
    static {
        REPORT_GUAVA_CORRUPT_MISSING = new ReportType("Guava is either missing or corrupt. Reverting to PriorityQueue.");
    }
}
