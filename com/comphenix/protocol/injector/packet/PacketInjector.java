package com.comphenix.protocol.injector.packet;

import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.ListenerOptions;
import java.util.Set;
import com.comphenix.protocol.PacketType;

public interface PacketInjector
{
    boolean isCancelled(final Object p0);
    
    void setCancelled(final Object p0, final boolean p1);
    
    boolean addPacketHandler(final PacketType p0, final Set<ListenerOptions> p1);
    
    boolean removePacketHandler(final PacketType p0);
    
    boolean hasPacketHandler(final PacketType p0);
    
    void inputBuffersChanged(final Set<PacketType> p0);
    
    Set<PacketType> getPacketHandlers();
    
    PacketEvent packetRecieved(final PacketContainer p0, final Player p1, final byte[] p2);
    
    void cleanupAll();
}
