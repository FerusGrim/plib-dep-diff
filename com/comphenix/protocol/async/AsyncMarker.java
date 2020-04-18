package com.comphenix.protocol.async;

import com.google.common.primitives.Longs;
import java.util.List;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.ProtocolLogger;
import java.util.logging.Level;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketEvent;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import com.comphenix.protocol.injector.PrioritizedListener;
import java.util.Iterator;
import com.comphenix.protocol.PacketStream;
import java.io.Serializable;

public class AsyncMarker implements Serializable, Comparable<AsyncMarker>
{
    private static final long serialVersionUID = -2621498096616187384L;
    public static final int DEFAULT_TIMEOUT_DELTA = 1800000;
    public static final int DEFAULT_SENDING_DELTA = 0;
    private transient PacketStream packetStream;
    private transient Iterator<PrioritizedListener<AsyncListenerHandler>> listenerTraversal;
    private long initialTime;
    private long timeout;
    private long originalSendingIndex;
    private long newSendingIndex;
    private Long queuedSendingIndex;
    private volatile boolean processed;
    private volatile boolean transmitted;
    private volatile boolean asyncCancelled;
    private AtomicInteger processingDelay;
    private Object processingLock;
    private transient AsyncListenerHandler listenerHandler;
    private transient int workerID;
    private static volatile Method isMinecraftAsync;
    private static volatile boolean alwaysSync;
    
    AsyncMarker(final PacketStream packetStream, final long sendingIndex, final long initialTime, final long timeoutDelta) {
        this.processingDelay = new AtomicInteger();
        this.processingLock = new Object();
        if (packetStream == null) {
            throw new IllegalArgumentException("packetStream cannot be NULL");
        }
        this.packetStream = packetStream;
        this.initialTime = initialTime;
        this.timeout = initialTime + timeoutDelta;
        this.originalSendingIndex = sendingIndex;
        this.newSendingIndex = sendingIndex;
    }
    
    public long getInitialTime() {
        return this.initialTime;
    }
    
    public long getTimeout() {
        return this.timeout;
    }
    
    public void setTimeout(final long timeout) {
        this.timeout = timeout;
    }
    
    public long getOriginalSendingIndex() {
        return this.originalSendingIndex;
    }
    
    public long getNewSendingIndex() {
        return this.newSendingIndex;
    }
    
    public void setNewSendingIndex(final long newSendingIndex) {
        this.newSendingIndex = newSendingIndex;
    }
    
    public PacketStream getPacketStream() {
        return this.packetStream;
    }
    
    public void setPacketStream(final PacketStream packetStream) {
        this.packetStream = packetStream;
    }
    
    public boolean isProcessed() {
        return this.processed;
    }
    
    void setProcessed(final boolean processed) {
        this.processed = processed;
    }
    
    public int incrementProcessingDelay() {
        return this.processingDelay.incrementAndGet();
    }
    
    int decrementProcessingDelay() {
        return this.processingDelay.decrementAndGet();
    }
    
    public int getProcessingDelay() {
        return this.processingDelay.get();
    }
    
    public boolean isQueued() {
        return this.queuedSendingIndex != null;
    }
    
    public long getQueuedSendingIndex() {
        return (this.queuedSendingIndex != null) ? this.queuedSendingIndex : 0L;
    }
    
    void setQueuedSendingIndex(final Long queuedSendingIndex) {
        this.queuedSendingIndex = queuedSendingIndex;
    }
    
    public Object getProcessingLock() {
        return this.processingLock;
    }
    
    public void setProcessingLock(final Object processingLock) {
        this.processingLock = processingLock;
    }
    
    public boolean isTransmitted() {
        return this.transmitted;
    }
    
    public boolean hasExpired() {
        return this.hasExpired(System.currentTimeMillis());
    }
    
    public boolean hasExpired(final long currentTime) {
        return this.timeout < currentTime;
    }
    
    public boolean isAsyncCancelled() {
        return this.asyncCancelled;
    }
    
    public void setAsyncCancelled(final boolean asyncCancelled) {
        this.asyncCancelled = asyncCancelled;
    }
    
    public AsyncListenerHandler getListenerHandler() {
        return this.listenerHandler;
    }
    
    void setListenerHandler(final AsyncListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }
    
    public int getWorkerID() {
        return this.workerID;
    }
    
    void setWorkerID(final int workerID) {
        this.workerID = workerID;
    }
    
    Iterator<PrioritizedListener<AsyncListenerHandler>> getListenerTraversal() {
        return this.listenerTraversal;
    }
    
    void setListenerTraversal(final Iterator<PrioritizedListener<AsyncListenerHandler>> listenerTraversal) {
        this.listenerTraversal = listenerTraversal;
    }
    
    void sendPacket(final PacketEvent event) throws IOException {
        try {
            if (event.isServerPacket()) {
                this.packetStream.sendServerPacket(event.getPlayer(), event.getPacket(), NetworkMarker.getNetworkMarker(event), false);
            }
            else {
                this.packetStream.recieveClientPacket(event.getPlayer(), event.getPacket(), NetworkMarker.getNetworkMarker(event), false);
            }
            this.transmitted = true;
        }
        catch (InvocationTargetException e) {
            throw new IOException("Cannot send packet", e);
        }
        catch (IllegalAccessException e2) {
            throw new IOException("Cannot send packet", e2);
        }
    }
    
    public boolean isMinecraftAsync(final PacketEvent event) throws FieldAccessException {
        if (AsyncMarker.isMinecraftAsync == null && !AsyncMarker.alwaysSync) {
            try {
                AsyncMarker.isMinecraftAsync = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethodByName("a_.*");
            }
            catch (RuntimeException e4) {
                final List<Method> methods = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethodListByParameters(Boolean.TYPE, new Class[0]);
                if (methods.size() == 2) {
                    AsyncMarker.isMinecraftAsync = methods.get(1);
                }
                else if (methods.size() == 1) {
                    AsyncMarker.alwaysSync = true;
                }
                else if (MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.BOUNTIFUL_UPDATE)) {
                    if (event.getPacketType() != PacketType.Play.Client.CHAT) {
                        return event.getPacketType() == PacketType.Status.Server.SERVER_INFO;
                    }
                    final String content = event.getPacket().getStrings().readSafely(0);
                    if (content != null) {
                        return !content.startsWith("/");
                    }
                    ProtocolLogger.log(Level.WARNING, "Failed to determine contents of incoming chat packet!", new Object[0]);
                    AsyncMarker.alwaysSync = true;
                }
                else {
                    ProtocolLogger.log(Level.INFO, "Could not determine asynchronous state of packets (this can probably be ignored)", new Object[0]);
                    AsyncMarker.alwaysSync = true;
                }
            }
        }
        if (AsyncMarker.alwaysSync) {
            return false;
        }
        try {
            return (boolean)AsyncMarker.isMinecraftAsync.invoke(event.getPacket().getHandle(), new Object[0]);
        }
        catch (IllegalArgumentException e) {
            throw new FieldAccessException("Illegal argument", e);
        }
        catch (IllegalAccessException e2) {
            throw new FieldAccessException("Unable to reflect method call 'a_', or: isAsyncPacket.", e2);
        }
        catch (InvocationTargetException e3) {
            throw new FieldAccessException("Minecraft error", e3);
        }
    }
    
    @Override
    public int compareTo(final AsyncMarker o) {
        if (o == null) {
            return 1;
        }
        return Longs.compare(this.getNewSendingIndex(), o.getNewSendingIndex());
    }
    
    @Override
    public boolean equals(final Object other) {
        return other == this || (other instanceof AsyncMarker && this.getNewSendingIndex() == ((AsyncMarker)other).getNewSendingIndex());
    }
    
    @Override
    public int hashCode() {
        return Longs.hashCode(this.getNewSendingIndex());
    }
}
