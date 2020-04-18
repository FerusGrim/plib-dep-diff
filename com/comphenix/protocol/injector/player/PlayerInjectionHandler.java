package com.comphenix.protocol.injector.player;

import io.netty.channel.Channel;
import com.comphenix.protocol.events.PacketEvent;
import java.io.InputStream;
import com.comphenix.protocol.events.PacketListener;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketContainer;
import java.net.InetSocketAddress;
import java.io.DataInputStream;
import com.comphenix.protocol.events.ListenerOptions;
import java.util.Set;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.injector.PlayerInjectHooks;
import org.bukkit.entity.Player;

public interface PlayerInjectionHandler
{
    int getProtocolVersion(final Player p0);
    
    PlayerInjectHooks getPlayerHook();
    
    PlayerInjectHooks getPlayerHook(final GamePhase p0);
    
    void setPlayerHook(final PlayerInjectHooks p0);
    
    void setPlayerHook(final GamePhase p0, final PlayerInjectHooks p1);
    
    void addPacketHandler(final PacketType p0, final Set<ListenerOptions> p1);
    
    void removePacketHandler(final PacketType p0);
    
    Player getPlayerByConnection(final DataInputStream p0) throws InterruptedException;
    
    void injectPlayer(final Player p0, final ConflictStrategy p1);
    
    void handleDisconnect(final Player p0);
    
    boolean uninjectPlayer(final Player p0);
    
    boolean uninjectPlayer(final InetSocketAddress p0);
    
    void sendServerPacket(final Player p0, final PacketContainer p1, final NetworkMarker p2, final boolean p3) throws InvocationTargetException;
    
    void recieveClientPacket(final Player p0, final Object p1) throws IllegalAccessException, InvocationTargetException;
    
    void updatePlayer(final Player p0);
    
    void checkListener(final Set<PacketListener> p0);
    
    void checkListener(final PacketListener p0);
    
    Set<PacketType> getSendingFilters();
    
    boolean canRecievePackets();
    
    PacketEvent handlePacketRecieved(final PacketContainer p0, final InputStream p1, final byte[] p2);
    
    void close();
    
    boolean hasMainThreadListener(final PacketType p0);
    
    Channel getChannel(final Player p0);
    
    public enum ConflictStrategy
    {
        OVERRIDE, 
        BAIL_OUT;
    }
}
