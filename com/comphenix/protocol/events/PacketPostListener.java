package com.comphenix.protocol.events;

import org.bukkit.plugin.Plugin;

public interface PacketPostListener
{
    Plugin getPlugin();
    
    void onPostEvent(final PacketEvent p0);
}
