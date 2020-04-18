package com.comphenix.protocol.async;

import com.comphenix.protocol.PacketStream;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import org.bukkit.entity.Player;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Objects;
import org.bukkit.plugin.Plugin;
import java.util.List;
import com.comphenix.protocol.PacketType;
import java.util.Iterator;
import com.comphenix.protocol.injector.PrioritizedListener;
import com.google.common.collect.Iterables;
import java.util.Collection;
import com.google.common.collect.ImmutableSet;
import com.comphenix.protocol.events.ListeningWhitelist;
import java.util.Map;
import com.google.common.collect.Sets;
import java.util.concurrent.ConcurrentHashMap;
import com.comphenix.protocol.ProtocolManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.scheduler.BukkitScheduler;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.events.PacketListener;
import java.util.Set;
import com.comphenix.protocol.injector.SortedPacketListenerList;
import com.comphenix.protocol.AsynchronousManager;

public class AsyncFilterManager implements AsynchronousManager
{
    private SortedPacketListenerList serverTimeoutListeners;
    private SortedPacketListenerList clientTimeoutListeners;
    private Set<PacketListener> timeoutListeners;
    private PacketProcessingQueue serverProcessingQueue;
    private PacketProcessingQueue clientProcessingQueue;
    private final PlayerSendingHandler playerSendingHandler;
    private final ErrorReporter reporter;
    private final Thread mainThread;
    private final BukkitScheduler scheduler;
    private final AtomicInteger currentSendingIndex;
    private ProtocolManager manager;
    
    public AsyncFilterManager(final ErrorReporter reporter, final BukkitScheduler scheduler) {
        this.currentSendingIndex = new AtomicInteger();
        this.serverTimeoutListeners = new SortedPacketListenerList();
        this.clientTimeoutListeners = new SortedPacketListenerList();
        this.timeoutListeners = (Set<PacketListener>)Sets.newSetFromMap((Map)new ConcurrentHashMap());
        this.playerSendingHandler = new PlayerSendingHandler(reporter, this.serverTimeoutListeners, this.clientTimeoutListeners);
        this.serverProcessingQueue = new PacketProcessingQueue(this.playerSendingHandler);
        this.clientProcessingQueue = new PacketProcessingQueue(this.playerSendingHandler);
        this.playerSendingHandler.initializeScheduler();
        this.scheduler = scheduler;
        this.reporter = reporter;
        this.mainThread = Thread.currentThread();
    }
    
    public ProtocolManager getManager() {
        return this.manager;
    }
    
    public void setManager(final ProtocolManager manager) {
        this.manager = manager;
    }
    
    @Override
    public AsyncListenerHandler registerAsyncHandler(final PacketListener listener) {
        return this.registerAsyncHandler(listener, true);
    }
    
    @Override
    public void registerTimeoutHandler(final PacketListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be NULL.");
        }
        if (!this.timeoutListeners.add(listener)) {
            return;
        }
        final ListeningWhitelist sending = listener.getSendingWhitelist();
        final ListeningWhitelist receiving = listener.getReceivingWhitelist();
        if (!ListeningWhitelist.isEmpty(sending)) {
            this.serverTimeoutListeners.addListener(listener, sending);
        }
        if (!ListeningWhitelist.isEmpty(receiving)) {
            this.serverTimeoutListeners.addListener(listener, receiving);
        }
    }
    
    @Override
    public Set<PacketListener> getTimeoutHandlers() {
        return (Set<PacketListener>)ImmutableSet.copyOf((Collection)this.timeoutListeners);
    }
    
    @Override
    public Set<PacketListener> getAsyncHandlers() {
        final ImmutableSet.Builder<PacketListener> builder = (ImmutableSet.Builder<PacketListener>)ImmutableSet.builder();
        for (final PrioritizedListener<AsyncListenerHandler> handler : Iterables.concat((Iterable)this.serverProcessingQueue.values(), (Iterable)this.clientProcessingQueue.values())) {
            builder.add((Object)handler.getListener().getAsyncListener());
        }
        return (Set<PacketListener>)builder.build();
    }
    
    public AsyncListenerHandler registerAsyncHandler(final PacketListener listener, final boolean autoInject) {
        final AsyncListenerHandler handler = new AsyncListenerHandler(this.mainThread, this, listener);
        final ListeningWhitelist sendingWhitelist = listener.getSendingWhitelist();
        final ListeningWhitelist receivingWhitelist = listener.getReceivingWhitelist();
        if (!this.hasValidWhitelist(sendingWhitelist) && !this.hasValidWhitelist(receivingWhitelist)) {
            throw new IllegalArgumentException("Listener has an empty sending and receiving whitelist.");
        }
        if (this.hasValidWhitelist(sendingWhitelist)) {
            this.manager.verifyWhitelist(listener, sendingWhitelist);
            this.serverProcessingQueue.addListener(handler, sendingWhitelist);
        }
        if (this.hasValidWhitelist(receivingWhitelist)) {
            this.manager.verifyWhitelist(listener, receivingWhitelist);
            this.clientProcessingQueue.addListener(handler, receivingWhitelist);
        }
        if (autoInject) {
            handler.setNullPacketListener(new NullPacketListener(listener));
            this.manager.addPacketListener(handler.getNullPacketListener());
        }
        return handler;
    }
    
    private boolean hasValidWhitelist(final ListeningWhitelist whitelist) {
        return whitelist != null && whitelist.getTypes().size() > 0;
    }
    
    @Override
    public void unregisterTimeoutHandler(final PacketListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be NULL.");
        }
        final ListeningWhitelist sending = listener.getSendingWhitelist();
        final ListeningWhitelist receiving = listener.getReceivingWhitelist();
        if (this.serverTimeoutListeners.removeListener(listener, sending).size() > 0 || this.clientTimeoutListeners.removeListener(listener, receiving).size() > 0) {
            this.timeoutListeners.remove(listener);
        }
    }
    
    @Override
    public void unregisterAsyncHandler(final PacketListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be NULL.");
        }
        AsyncListenerHandler handler = this.findHandler(this.serverProcessingQueue, listener.getSendingWhitelist(), listener);
        if (handler == null) {
            handler = this.findHandler(this.clientProcessingQueue, listener.getReceivingWhitelist(), listener);
        }
        this.unregisterAsyncHandler(handler);
    }
    
    private AsyncListenerHandler findHandler(final PacketProcessingQueue queue, final ListeningWhitelist search, final PacketListener target) {
        if (ListeningWhitelist.isEmpty(search)) {
            return null;
        }
        for (final PacketType type : search.getTypes()) {
            for (final PrioritizedListener<AsyncListenerHandler> element : queue.getListener(type)) {
                if (element.getListener().getAsyncListener() == target) {
                    return element.getListener();
                }
            }
        }
        return null;
    }
    
    @Override
    public void unregisterAsyncHandler(final AsyncListenerHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("listenerToken cannot be NULL");
        }
        handler.cancel();
    }
    
    void unregisterAsyncHandlerInternal(final AsyncListenerHandler handler) {
        final PacketListener listener = handler.getAsyncListener();
        final boolean synchronusOK = this.onMainThread();
        if (handler.getNullPacketListener() != null) {
            this.manager.removePacketListener(handler.getNullPacketListener());
        }
        if (this.hasValidWhitelist(listener.getSendingWhitelist())) {
            final List<PacketType> removed = this.serverProcessingQueue.removeListener(handler, listener.getSendingWhitelist());
            this.playerSendingHandler.sendServerPackets(removed, synchronusOK);
        }
        if (this.hasValidWhitelist(listener.getReceivingWhitelist())) {
            final List<PacketType> removed = this.clientProcessingQueue.removeListener(handler, listener.getReceivingWhitelist());
            this.playerSendingHandler.sendClientPackets(removed, synchronusOK);
        }
    }
    
    private boolean onMainThread() {
        return Thread.currentThread().getId() == this.mainThread.getId();
    }
    
    @Override
    public void unregisterAsyncHandlers(final Plugin plugin) {
        this.unregisterAsyncHandlers(this.serverProcessingQueue, plugin);
        this.unregisterAsyncHandlers(this.clientProcessingQueue, plugin);
    }
    
    private void unregisterAsyncHandlers(final PacketProcessingQueue processingQueue, final Plugin plugin) {
        for (final PrioritizedListener<AsyncListenerHandler> listener : processingQueue.values()) {
            if (Objects.equal((Object)listener.getListener().getPlugin(), (Object)plugin)) {
                this.unregisterAsyncHandler(listener.getListener());
            }
        }
    }
    
    public synchronized void enqueueSyncPacket(final PacketEvent syncPacket, final AsyncMarker asyncMarker) {
        final PacketEvent newEvent = PacketEvent.fromSynchronous(syncPacket, asyncMarker);
        if (asyncMarker.isQueued() || asyncMarker.isTransmitted()) {
            throw new IllegalArgumentException("Cannot queue a packet that has already been queued.");
        }
        asyncMarker.setQueuedSendingIndex(asyncMarker.getNewSendingIndex());
        final Player player = syncPacket.getPlayer();
        if (player != null) {
            this.getSendingQueue(syncPacket).enqueue(newEvent);
            this.getProcessingQueue(syncPacket).enqueue(newEvent, true);
        }
    }
    
    @Override
    public Set<Integer> getSendingFilters() {
        return PacketRegistry.toLegacy(this.serverProcessingQueue.keySet());
    }
    
    @Override
    public Set<PacketType> getReceivingTypes() {
        return this.serverProcessingQueue.keySet();
    }
    
    @Override
    public Set<Integer> getReceivingFilters() {
        return PacketRegistry.toLegacy(this.clientProcessingQueue.keySet());
    }
    
    @Override
    public Set<PacketType> getSendingTypes() {
        return this.clientProcessingQueue.keySet();
    }
    
    public BukkitScheduler getScheduler() {
        return this.scheduler;
    }
    
    @Override
    public boolean hasAsynchronousListeners(final PacketEvent packet) {
        final Collection<?> list = this.getProcessingQueue(packet).getListener(packet.getPacketType());
        return list != null && list.size() > 0;
    }
    
    public AsyncMarker createAsyncMarker() {
        return this.createAsyncMarker(1800000L);
    }
    
    public AsyncMarker createAsyncMarker(final long timeoutDelta) {
        return this.createAsyncMarker(timeoutDelta, this.currentSendingIndex.incrementAndGet());
    }
    
    private AsyncMarker createAsyncMarker(final long timeoutDelta, final long sendingIndex) {
        return new AsyncMarker(this.manager, sendingIndex, System.currentTimeMillis(), timeoutDelta);
    }
    
    @Override
    public PacketStream getPacketStream() {
        return this.manager;
    }
    
    @Override
    public ErrorReporter getErrorReporter() {
        return this.reporter;
    }
    
    @Override
    public void cleanupAll() {
        this.serverProcessingQueue.cleanupAll();
        this.playerSendingHandler.cleanupAll();
        this.timeoutListeners.clear();
        this.serverTimeoutListeners = null;
        this.clientTimeoutListeners = null;
    }
    
    @Override
    public void signalPacketTransmission(final PacketEvent packet) {
        this.signalPacketTransmission(packet, this.onMainThread());
    }
    
    private void signalPacketTransmission(final PacketEvent packet, final boolean onMainThread) {
        final AsyncMarker marker = packet.getAsyncMarker();
        if (marker == null) {
            throw new IllegalArgumentException("A sync packet cannot be transmitted by the asynchronous manager.");
        }
        if (!marker.isQueued()) {
            throw new IllegalArgumentException("A packet must have been queued before it can be transmitted.");
        }
        if (marker.decrementProcessingDelay() == 0) {
            final PacketSendingQueue queue = this.getSendingQueue(packet, false);
            if (queue != null) {
                queue.signalPacketUpdate(packet, onMainThread);
            }
        }
    }
    
    public PacketSendingQueue getSendingQueue(final PacketEvent packet) {
        return this.playerSendingHandler.getSendingQueue(packet);
    }
    
    public PacketSendingQueue getSendingQueue(final PacketEvent packet, final boolean createNew) {
        return this.playerSendingHandler.getSendingQueue(packet, createNew);
    }
    
    public PacketProcessingQueue getProcessingQueue(final PacketEvent packet) {
        return packet.isServerPacket() ? this.serverProcessingQueue : this.clientProcessingQueue;
    }
    
    public void signalFreeProcessingSlot(final PacketEvent packet) {
        this.getProcessingQueue(packet).signalProcessingDone();
    }
    
    public void sendProcessedPackets(final int tickCounter, final boolean onMainThread) {
        if (tickCounter % 10 == 0) {
            this.playerSendingHandler.trySendServerPackets(onMainThread);
        }
        this.playerSendingHandler.trySendClientPackets(onMainThread);
    }
    
    public void removePlayer(final Player player) {
        this.playerSendingHandler.removePlayer(player);
    }
}
