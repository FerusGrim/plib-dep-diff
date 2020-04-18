package com.comphenix.protocol.injector.netty;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.NetworkMarker;
import org.bukkit.entity.Player;

public class ClosedInjector implements Injector
{
    private Player player;
    
    public ClosedInjector(final Player player) {
        this.player = player;
    }
    
    @Override
    public boolean inject() {
        return false;
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public void sendServerPacket(final Object packet, final NetworkMarker marker, final boolean filtered) {
    }
    
    @Override
    public void recieveClientPacket(final Object packet) {
    }
    
    @Override
    public PacketType.Protocol getCurrentProtocol() {
        return PacketType.Protocol.HANDSHAKING;
    }
    
    @Override
    public NetworkMarker getMarker(final Object packet) {
        return null;
    }
    
    @Override
    public void saveMarker(final Object packet, final NetworkMarker marker) {
    }
    
    @Override
    public void setUpdatedPlayer(final Player player) {
    }
    
    @Override
    public Player getPlayer() {
        return this.player;
    }
    
    @Override
    public void setPlayer(final Player player) {
        this.player = player;
    }
    
    @Override
    public boolean isInjected() {
        return false;
    }
    
    @Override
    public boolean isClosed() {
        return true;
    }
    
    @Override
    public int getProtocolVersion() {
        return Integer.MIN_VALUE;
    }
}
