package com.comphenix.protocol.injector.spigot;

import io.netty.channel.Channel;
import com.comphenix.protocol.events.PacketEvent;
import java.io.InputStream;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.injector.player.PlayerInjectionHandler;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketContainer;
import java.net.InetSocketAddress;
import com.comphenix.protocol.concurrency.PacketTypeSet;
import com.comphenix.protocol.utility.MinecraftProtocolVersion;
import org.bukkit.entity.Player;

class DummyPlayerHandler extends AbstractPlayerHandler
{
    private SpigotPacketInjector injector;
    
    @Override
    public int getProtocolVersion(final Player player) {
        return MinecraftProtocolVersion.getCurrentVersion();
    }
    
    public DummyPlayerHandler(final SpigotPacketInjector injector, final PacketTypeSet sendingFilters) {
        super(sendingFilters);
        this.injector = injector;
    }
    
    @Override
    public boolean uninjectPlayer(final InetSocketAddress address) {
        return true;
    }
    
    @Override
    public boolean uninjectPlayer(final Player player) {
        this.injector.uninjectPlayer(player);
        return true;
    }
    
    @Override
    public void sendServerPacket(final Player receiver, final PacketContainer packet, final NetworkMarker marker, final boolean filters) throws InvocationTargetException {
        this.injector.sendServerPacket(receiver, packet, marker, filters);
    }
    
    @Override
    public void recieveClientPacket(final Player player, final Object mcPacket) throws IllegalAccessException, InvocationTargetException {
        this.injector.processPacket(player, mcPacket);
    }
    
    @Override
    public void injectPlayer(final Player player, final PlayerInjectionHandler.ConflictStrategy strategy) {
        this.injector.injectPlayer(player);
    }
    
    @Override
    public boolean hasMainThreadListener(final PacketType type) {
        return this.sendingFilters.contains(type);
    }
    
    @Override
    public void handleDisconnect(final Player player) {
    }
    
    @Override
    public PacketEvent handlePacketRecieved(final PacketContainer packet, final InputStream input, final byte[] buffered) {
        if (buffered != null) {
            this.injector.saveBuffered(packet.getHandle(), buffered);
        }
        return null;
    }
    
    @Override
    public void updatePlayer(final Player player) {
    }
    
    @Override
    public Channel getChannel(final Player player) {
        throw new UnsupportedOperationException();
    }
}
