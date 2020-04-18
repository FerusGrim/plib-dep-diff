package com.comphenix.protocol.injector;

import com.comphenix.protocol.injector.packet.InterceptWritePacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;

public interface ListenerInvoker
{
    void invokePacketRecieving(final PacketEvent p0);
    
    void invokePacketSending(final PacketEvent p0);
    
    @Deprecated
    int getPacketID(final Object p0);
    
    PacketType getPacketType(final Object p0);
    
    InterceptWritePacket getInterceptWritePacket();
    
    @Deprecated
    boolean requireInputBuffer(final int p0);
    
    void unregisterPacketClass(final Class<?> p0);
    
    @Deprecated
    void registerPacketClass(final Class<?> p0, final int p1);
    
    @Deprecated
    Class<?> getPacketClassFromID(final int p0, final boolean p1);
}
