package com.comphenix.protocol;

import com.comphenix.protocol.injector.netty.WirePacket;
import com.comphenix.protocol.events.NetworkMarker;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

public interface PacketStream
{
    void sendServerPacket(final Player p0, final PacketContainer p1) throws InvocationTargetException;
    
    void sendServerPacket(final Player p0, final PacketContainer p1, final boolean p2) throws InvocationTargetException;
    
    void sendServerPacket(final Player p0, final PacketContainer p1, final NetworkMarker p2, final boolean p3) throws InvocationTargetException;
    
    void sendWirePacket(final Player p0, final int p1, final byte[] p2) throws InvocationTargetException;
    
    void sendWirePacket(final Player p0, final WirePacket p1) throws InvocationTargetException;
    
    void recieveClientPacket(final Player p0, final PacketContainer p1) throws IllegalAccessException, InvocationTargetException;
    
    void recieveClientPacket(final Player p0, final PacketContainer p1, final boolean p2) throws IllegalAccessException, InvocationTargetException;
    
    void recieveClientPacket(final Player p0, final PacketContainer p1, final NetworkMarker p2, final boolean p3) throws IllegalAccessException, InvocationTargetException;
}
