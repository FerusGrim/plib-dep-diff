package com.comphenix.protocol.events;

import com.google.common.base.Preconditions;
import org.bukkit.plugin.Plugin;

public abstract class PacketPostAdapter implements PacketPostListener
{
    private Plugin plugin;
    
    public PacketPostAdapter(final Plugin plugin) {
        this.plugin = (Plugin)Preconditions.checkNotNull((Object)plugin, (Object)"plugin cannot be NULL");
    }
    
    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }
}
