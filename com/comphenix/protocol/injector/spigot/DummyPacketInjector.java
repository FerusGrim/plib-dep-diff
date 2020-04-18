package com.comphenix.protocol.injector.spigot;

import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import com.comphenix.protocol.events.PacketContainer;
import java.util.Iterator;
import com.comphenix.protocol.events.ListenerOptions;
import com.google.common.collect.Sets;
import com.comphenix.protocol.PacketType;
import java.util.Set;
import com.comphenix.protocol.concurrency.PacketTypeSet;

class DummyPacketInjector extends AbstractPacketInjector
{
    private SpigotPacketInjector injector;
    private PacketTypeSet lastBufferedPackets;
    
    public DummyPacketInjector(final SpigotPacketInjector injector, final PacketTypeSet reveivedFilters) {
        super(reveivedFilters);
        this.lastBufferedPackets = new PacketTypeSet();
        this.injector = injector;
    }
    
    @Override
    public void inputBuffersChanged(final Set<PacketType> set) {
        final Set<PacketType> removed = (Set<PacketType>)Sets.difference((Set)this.lastBufferedPackets.values(), (Set)set);
        final Set<PacketType> added = (Set<PacketType>)Sets.difference((Set)set, (Set)this.lastBufferedPackets.values());
        for (final PacketType packet : removed) {
            this.injector.getProxyPacketInjector().removePacketHandler(packet);
        }
        for (final PacketType packet : added) {
            this.injector.getProxyPacketInjector().addPacketHandler(packet, null);
        }
    }
    
    @Override
    public PacketEvent packetRecieved(final PacketContainer packet, final Player client, final byte[] buffered) {
        return this.injector.packetReceived(packet, client, buffered);
    }
}
