package com.comphenix.protocol.injector.spigot;

import com.comphenix.protocol.events.ListenerOptions;
import java.util.Set;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.concurrency.PacketTypeSet;
import com.comphenix.protocol.injector.packet.PacketInjector;

public abstract class AbstractPacketInjector implements PacketInjector
{
    private PacketTypeSet reveivedFilters;
    
    public AbstractPacketInjector(final PacketTypeSet reveivedFilters) {
        this.reveivedFilters = reveivedFilters;
    }
    
    @Override
    public boolean isCancelled(final Object packet) {
        return false;
    }
    
    @Override
    public void setCancelled(final Object packet, final boolean cancelled) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean addPacketHandler(final PacketType type, final Set<ListenerOptions> options) {
        this.reveivedFilters.addType(type);
        return true;
    }
    
    @Override
    public boolean removePacketHandler(final PacketType type) {
        this.reveivedFilters.removeType(type);
        return true;
    }
    
    @Override
    public boolean hasPacketHandler(final PacketType type) {
        return this.reveivedFilters.contains(type);
    }
    
    @Override
    public Set<PacketType> getPacketHandlers() {
        return this.reveivedFilters.values();
    }
    
    @Override
    public void cleanupAll() {
        this.reveivedFilters.clear();
    }
}
