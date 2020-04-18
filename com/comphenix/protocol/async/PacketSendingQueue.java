package com.comphenix.protocol.async;

import java.io.IOException;
import com.comphenix.protocol.injector.PlayerLoggedOutException;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.entity.Player;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import com.comphenix.protocol.PacketType;
import java.util.List;
import com.comphenix.protocol.events.PacketEvent;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import com.comphenix.protocol.error.ReportType;

abstract class PacketSendingQueue
{
    public static final ReportType REPORT_DROPPED_PACKET;
    public static final int INITIAL_CAPACITY = 10;
    private PriorityBlockingQueue<PacketEventHolder> sendingQueue;
    private Executor asynchronousSender;
    private final boolean notThreadSafe;
    private boolean cleanedUp;
    
    public PacketSendingQueue(final boolean notThreadSafe, final Executor asynchronousSender) {
        this.cleanedUp = false;
        this.sendingQueue = new PriorityBlockingQueue<PacketEventHolder>(10);
        this.notThreadSafe = notThreadSafe;
        this.asynchronousSender = asynchronousSender;
    }
    
    public int size() {
        return this.sendingQueue.size();
    }
    
    public void enqueue(final PacketEvent packet) {
        this.sendingQueue.add(new PacketEventHolder(packet));
    }
    
    public synchronized void signalPacketUpdate(final PacketEvent packetUpdated, final boolean onMainThread) {
        final AsyncMarker marker = packetUpdated.getAsyncMarker();
        if (marker.getQueuedSendingIndex() != marker.getNewSendingIndex() && !marker.hasExpired()) {
            final PacketEvent copy = PacketEvent.fromSynchronous(packetUpdated, marker);
            packetUpdated.setReadOnly(false);
            packetUpdated.setCancelled(true);
            this.enqueue(copy);
        }
        marker.setProcessed(true);
        this.trySendPackets(onMainThread);
    }
    
    public synchronized void signalPacketUpdate(final List<PacketType> packetsRemoved, final boolean onMainThread) {
        final Set<PacketType> lookup = new HashSet<PacketType>(packetsRemoved);
        for (final PacketEventHolder holder : this.sendingQueue) {
            final PacketEvent event = holder.getEvent();
            if (lookup.contains(event.getPacketType())) {
                event.getAsyncMarker().setProcessed(true);
            }
        }
        this.trySendPackets(onMainThread);
    }
    
    public void trySendPackets(final boolean onMainThread) {
        boolean sending = true;
        while (sending) {
            final PacketEventHolder holder = this.sendingQueue.poll();
            if (holder != null) {
                sending = this.processPacketHolder(onMainThread, holder);
                if (sending) {
                    continue;
                }
                this.sendingQueue.add(holder);
            }
            else {
                sending = false;
            }
        }
    }
    
    private boolean processPacketHolder(final boolean onMainThread, final PacketEventHolder holder) {
        final PacketEvent current = holder.getEvent();
        AsyncMarker marker = current.getAsyncMarker();
        boolean hasExpired = marker.hasExpired();
        if (this.cleanedUp) {
            return true;
        }
        if (marker.isProcessed() || hasExpired) {
            if (hasExpired) {
                this.onPacketTimeout(current);
                marker = current.getAsyncMarker();
                hasExpired = marker.hasExpired();
                if (!marker.isProcessed() && !hasExpired) {
                    return false;
                }
            }
            if (!current.isCancelled() && !hasExpired) {
                if (this.notThreadSafe) {
                    try {
                        final boolean wantAsync = marker.isMinecraftAsync(current);
                        final boolean wantSync = !wantAsync;
                        if (!onMainThread && wantSync) {
                            return false;
                        }
                        if (onMainThread && wantAsync) {
                            this.asynchronousSender.execute(new Runnable() {
                                @Override
                                public void run() {
                                    PacketSendingQueue.this.processPacketHolder(false, holder);
                                }
                            });
                            return true;
                        }
                    }
                    catch (FieldAccessException e) {
                        e.printStackTrace();
                        return true;
                    }
                }
                if (this.isOnline(current.getPlayer())) {
                    this.sendPacket(current);
                }
            }
            return true;
        }
        return false;
    }
    
    protected abstract void onPacketTimeout(final PacketEvent p0);
    
    private boolean isOnline(final Player player) {
        return player != null && player.isOnline();
    }
    
    private void forceSend() {
        while (true) {
            final PacketEventHolder holder = this.sendingQueue.poll();
            if (holder == null) {
                break;
            }
            this.sendPacket(holder.getEvent());
        }
    }
    
    public boolean isSynchronizeMain() {
        return this.notThreadSafe;
    }
    
    private void sendPacket(final PacketEvent event) {
        final AsyncMarker marker = event.getAsyncMarker();
        try {
            if (marker != null && !marker.isTransmitted()) {
                marker.sendPacket(event);
            }
        }
        catch (PlayerLoggedOutException e2) {
            ProtocolLibrary.getErrorReporter().reportDebug(this, Report.newBuilder(PacketSendingQueue.REPORT_DROPPED_PACKET).messageParam(marker.getOriginalSendingIndex(), event.getPacketType()).callerParam(event));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void cleanupAll() {
        if (!this.cleanedUp) {
            this.forceSend();
            this.cleanedUp = true;
        }
    }
    
    static {
        REPORT_DROPPED_PACKET = new ReportType("Warning: Dropped packet index %s of type %s.");
    }
}
