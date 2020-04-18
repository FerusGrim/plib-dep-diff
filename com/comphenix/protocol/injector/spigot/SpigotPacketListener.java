package com.comphenix.protocol.injector.spigot;

interface SpigotPacketListener
{
    Object packetReceived(final Object p0, final Object p1, final Object p2);
    
    Object packetQueued(final Object p0, final Object p1, final Object p2);
}
