package com.comphenix.protocol.async;

import java.util.ArrayList;
import com.comphenix.protocol.PacketType;
import java.util.List;
import java.util.Iterator;
import com.comphenix.protocol.events.PacketEvent;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.comphenix.protocol.concurrency.ConcurrentPlayerMap;
import java.util.concurrent.Executor;
import com.comphenix.protocol.injector.SortedPacketListenerList;
import org.bukkit.entity.Player;
import java.util.concurrent.ConcurrentMap;
import com.comphenix.protocol.error.ErrorReporter;

class PlayerSendingHandler
{
    private ErrorReporter reporter;
    private ConcurrentMap<Player, QueueContainer> playerSendingQueues;
    private SortedPacketListenerList serverTimeoutListeners;
    private SortedPacketListenerList clientTimeoutListeners;
    private Executor asynchronousSender;
    private volatile boolean cleaningUp;
    
    public PlayerSendingHandler(final ErrorReporter reporter, final SortedPacketListenerList serverTimeoutListeners, final SortedPacketListenerList clientTimeoutListeners) {
        this.reporter = reporter;
        this.serverTimeoutListeners = serverTimeoutListeners;
        this.clientTimeoutListeners = clientTimeoutListeners;
        this.playerSendingQueues = (ConcurrentMap<Player, QueueContainer>)ConcurrentPlayerMap.usingAddress();
    }
    
    public synchronized void initializeScheduler() {
        if (this.asynchronousSender == null) {
            final ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ProtocolLib-AsyncSender %s").build();
            this.asynchronousSender = Executors.newSingleThreadExecutor(factory);
        }
    }
    
    public PacketSendingQueue getSendingQueue(final PacketEvent packet) {
        return this.getSendingQueue(packet, true);
    }
    
    public PacketSendingQueue getSendingQueue(final PacketEvent packet, final boolean createNew) {
        QueueContainer queues = this.playerSendingQueues.get(packet.getPlayer());
        if (queues == null && createNew) {
            final QueueContainer newContainer = new QueueContainer();
            queues = this.playerSendingQueues.putIfAbsent(packet.getPlayer(), newContainer);
            if (queues == null) {
                queues = newContainer;
            }
        }
        if (queues != null) {
            return packet.isServerPacket() ? queues.getServerQueue() : queues.getClientQueue();
        }
        return null;
    }
    
    public void sendAllPackets() {
        if (!this.cleaningUp) {
            for (final QueueContainer queues : this.playerSendingQueues.values()) {
                queues.getClientQueue().cleanupAll();
                queues.getServerQueue().cleanupAll();
            }
        }
    }
    
    public void sendServerPackets(final List<PacketType> types, final boolean synchronusOK) {
        if (!this.cleaningUp) {
            for (final QueueContainer queue : this.playerSendingQueues.values()) {
                queue.getServerQueue().signalPacketUpdate(types, synchronusOK);
            }
        }
    }
    
    public void sendClientPackets(final List<PacketType> types, final boolean synchronusOK) {
        if (!this.cleaningUp) {
            for (final QueueContainer queue : this.playerSendingQueues.values()) {
                queue.getClientQueue().signalPacketUpdate(types, synchronusOK);
            }
        }
    }
    
    public void trySendServerPackets(final boolean onMainThread) {
        for (final QueueContainer queue : this.playerSendingQueues.values()) {
            queue.getServerQueue().trySendPackets(onMainThread);
        }
    }
    
    public void trySendClientPackets(final boolean onMainThread) {
        for (final QueueContainer queue : this.playerSendingQueues.values()) {
            queue.getClientQueue().trySendPackets(onMainThread);
        }
    }
    
    public List<PacketSendingQueue> getServerQueues() {
        final List<PacketSendingQueue> result = new ArrayList<PacketSendingQueue>();
        for (final QueueContainer queue : this.playerSendingQueues.values()) {
            result.add(queue.getServerQueue());
        }
        return result;
    }
    
    public List<PacketSendingQueue> getClientQueues() {
        final List<PacketSendingQueue> result = new ArrayList<PacketSendingQueue>();
        for (final QueueContainer queue : this.playerSendingQueues.values()) {
            result.add(queue.getClientQueue());
        }
        return result;
    }
    
    public void cleanupAll() {
        if (!this.cleaningUp) {
            this.cleaningUp = true;
            this.sendAllPackets();
            this.playerSendingQueues.clear();
        }
    }
    
    public void removePlayer(final Player player) {
        this.playerSendingQueues.remove(player);
    }
    
    private class QueueContainer
    {
        private PacketSendingQueue serverQueue;
        private PacketSendingQueue clientQueue;
        
        public QueueContainer() {
            this.serverQueue = new PacketSendingQueue(false, PlayerSendingHandler.this.asynchronousSender) {
                @Override
                protected void onPacketTimeout(final PacketEvent event) {
                    if (!PlayerSendingHandler.this.cleaningUp) {
                        PlayerSendingHandler.this.serverTimeoutListeners.invokePacketSending(PlayerSendingHandler.this.reporter, event);
                    }
                }
            };
            this.clientQueue = new PacketSendingQueue(true, PlayerSendingHandler.this.asynchronousSender) {
                @Override
                protected void onPacketTimeout(final PacketEvent event) {
                    if (!PlayerSendingHandler.this.cleaningUp) {
                        PlayerSendingHandler.this.clientTimeoutListeners.invokePacketSending(PlayerSendingHandler.this.reporter, event);
                    }
                }
            };
        }
        
        public PacketSendingQueue getServerQueue() {
            return this.serverQueue;
        }
        
        public PacketSendingQueue getClientQueue() {
            return this.clientQueue;
        }
    }
}
