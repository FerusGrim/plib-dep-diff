package com.comphenix.protocol.events;

import org.bukkit.plugin.Plugin;

public abstract class PacketOutputAdapter implements PacketOutputHandler
{
    private final Plugin plugin;
    private final ListenerPriority priority;
    
    public PacketOutputAdapter(final Plugin plugin, final ListenerPriority priority) {
        this.priority = priority;
        this.plugin = plugin;
    }
    
    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }
    
    @Override
    public ListenerPriority getPriority() {
        return this.priority;
    }
}
