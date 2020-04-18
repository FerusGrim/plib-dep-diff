package com.comphenix.protocol;

import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.Set;
import org.bukkit.plugin.Plugin;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketListener;

public interface AsynchronousManager
{
    AsyncListenerHandler registerAsyncHandler(final PacketListener p0);
    
    void unregisterAsyncHandler(final AsyncListenerHandler p0);
    
    void unregisterAsyncHandler(final PacketListener p0);
    
    void unregisterAsyncHandlers(final Plugin p0);
    
    @Deprecated
    Set<Integer> getSendingFilters();
    
    Set<PacketType> getSendingTypes();
    
    @Deprecated
    Set<Integer> getReceivingFilters();
    
    Set<PacketType> getReceivingTypes();
    
    boolean hasAsynchronousListeners(final PacketEvent p0);
    
    PacketStream getPacketStream();
    
    ErrorReporter getErrorReporter();
    
    void cleanupAll();
    
    void signalPacketTransmission(final PacketEvent p0);
    
    void registerTimeoutHandler(final PacketListener p0);
    
    void unregisterTimeoutHandler(final PacketListener p0);
    
    Set<PacketListener> getTimeoutHandlers();
    
    Set<PacketListener> getAsyncHandlers();
}
