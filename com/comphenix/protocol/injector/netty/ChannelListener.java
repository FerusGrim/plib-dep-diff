package com.comphenix.protocol.injector.netty;

import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.NetworkMarker;

public interface ChannelListener
{
    PacketEvent onPacketSending(final Injector p0, final Object p1, final NetworkMarker p2);
    
    PacketEvent onPacketReceiving(final Injector p0, final Object p1, final NetworkMarker p2);
    
    boolean hasListener(final Class<?> p0);
    
    boolean hasMainThreadListener(final Class<?> p0);
    
    boolean includeBuffer(final Class<?> p0);
    
    ErrorReporter getReporter();
    
    boolean isDebug();
}
