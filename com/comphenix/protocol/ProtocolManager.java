package com.comphenix.protocol;

import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.utility.MinecraftVersion;
import java.util.Set;
import org.bukkit.World;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.List;
import com.comphenix.protocol.injector.PacketConstructor;
import org.bukkit.plugin.Plugin;
import com.comphenix.protocol.events.PacketListener;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

public interface ProtocolManager extends PacketStream
{
    int getProtocolVersion(final Player p0);
    
    void sendServerPacket(final Player p0, final PacketContainer p1, final boolean p2) throws InvocationTargetException;
    
    void recieveClientPacket(final Player p0, final PacketContainer p1, final boolean p2) throws IllegalAccessException, InvocationTargetException;
    
    void broadcastServerPacket(final PacketContainer p0);
    
    void broadcastServerPacket(final PacketContainer p0, final Entity p1, final boolean p2);
    
    void broadcastServerPacket(final PacketContainer p0, final Location p1, final int p2);
    
    ImmutableSet<PacketListener> getPacketListeners();
    
    void addPacketListener(final PacketListener p0);
    
    void removePacketListener(final PacketListener p0);
    
    void removePacketListeners(final Plugin p0);
    
    @Deprecated
    PacketContainer createPacket(final int p0);
    
    PacketContainer createPacket(final PacketType p0);
    
    @Deprecated
    PacketContainer createPacket(final int p0, final boolean p1);
    
    PacketContainer createPacket(final PacketType p0, final boolean p1);
    
    @Deprecated
    PacketConstructor createPacketConstructor(final int p0, final Object... p1);
    
    PacketConstructor createPacketConstructor(final PacketType p0, final Object... p1);
    
    void updateEntity(final Entity p0, final List<Player> p1) throws FieldAccessException;
    
    Entity getEntityFromID(final World p0, final int p1) throws FieldAccessException;
    
    List<Player> getEntityTrackers(final Entity p0) throws FieldAccessException;
    
    @Deprecated
    Set<Integer> getSendingFilters();
    
    Set<PacketType> getSendingFilterTypes();
    
    @Deprecated
    Set<Integer> getReceivingFilters();
    
    Set<PacketType> getReceivingFilterTypes();
    
    MinecraftVersion getMinecraftVersion();
    
    boolean isClosed();
    
    AsynchronousManager getAsynchronousManager();
    
    void verifyWhitelist(final PacketListener p0, final ListeningWhitelist p1);
}
