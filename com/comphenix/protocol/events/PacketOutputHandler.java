package com.comphenix.protocol.events;

import org.bukkit.plugin.Plugin;

public interface PacketOutputHandler
{
    ListenerPriority getPriority();
    
    Plugin getPlugin();
    
    byte[] handle(final PacketEvent p0, final byte[] p1);
}
