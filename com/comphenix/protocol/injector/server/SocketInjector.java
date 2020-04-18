package com.comphenix.protocol.injector.server;

import org.bukkit.entity.Player;
import com.comphenix.protocol.events.NetworkMarker;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.net.Socket;

public interface SocketInjector
{
    Socket getSocket() throws IllegalAccessException;
    
    SocketAddress getAddress() throws IllegalAccessException;
    
    void disconnect(final String p0) throws InvocationTargetException;
    
    void sendServerPacket(final Object p0, final NetworkMarker p1, final boolean p2) throws InvocationTargetException;
    
    Player getPlayer();
    
    Player getUpdatedPlayer();
    
    void transferState(final SocketInjector p0);
    
    void setUpdatedPlayer(final Player p0);
}
