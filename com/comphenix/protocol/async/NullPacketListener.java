package com.comphenix.protocol.async;

import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ListenerPriority;
import org.bukkit.plugin.Plugin;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketListener;

class NullPacketListener implements PacketListener
{
    private ListeningWhitelist sendingWhitelist;
    private ListeningWhitelist receivingWhitelist;
    private Plugin plugin;
    
    public NullPacketListener(final PacketListener original) {
        this.sendingWhitelist = this.cloneWhitelist(ListenerPriority.LOW, original.getSendingWhitelist());
        this.receivingWhitelist = this.cloneWhitelist(ListenerPriority.LOW, original.getReceivingWhitelist());
        this.plugin = original.getPlugin();
    }
    
    @Override
    public void onPacketSending(final PacketEvent event) {
    }
    
    @Override
    public void onPacketReceiving(final PacketEvent event) {
    }
    
    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return this.sendingWhitelist;
    }
    
    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return this.receivingWhitelist;
    }
    
    private ListeningWhitelist cloneWhitelist(final ListenerPriority priority, final ListeningWhitelist whitelist) {
        if (whitelist != null) {
            return ListeningWhitelist.newBuilder(whitelist).priority(priority).mergeOptions(ListenerOptions.ASYNC).build();
        }
        return null;
    }
    
    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }
}
