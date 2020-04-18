package com.comphenix.protocol.injector.netty;

import org.bukkit.entity.Player;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.NetworkMarker;

public interface Injector
{
    int getProtocolVersion();
    
    boolean inject();
    
    void close();
    
    void sendServerPacket(final Object p0, final NetworkMarker p1, final boolean p2);
    
    void recieveClientPacket(final Object p0);
    
    PacketType.Protocol getCurrentProtocol();
    
    NetworkMarker getMarker(final Object p0);
    
    void saveMarker(final Object p0, final NetworkMarker p1);
    
    Player getPlayer();
    
    void setPlayer(final Player p0);
    
    boolean isInjected();
    
    boolean isClosed();
    
    void setUpdatedPlayer(final Player p0);
}
