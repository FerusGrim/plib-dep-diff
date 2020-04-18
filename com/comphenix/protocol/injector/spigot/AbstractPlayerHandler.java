package com.comphenix.protocol.injector.spigot;

import com.comphenix.protocol.events.PacketListener;
import org.bukkit.entity.Player;
import java.io.DataInputStream;
import com.comphenix.protocol.events.ListenerOptions;
import java.util.Set;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.injector.PlayerInjectHooks;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.concurrency.PacketTypeSet;
import com.comphenix.protocol.injector.player.PlayerInjectionHandler;

public abstract class AbstractPlayerHandler implements PlayerInjectionHandler
{
    protected PacketTypeSet sendingFilters;
    
    public AbstractPlayerHandler(final PacketTypeSet sendingFilters) {
        this.sendingFilters = sendingFilters;
    }
    
    @Override
    public void setPlayerHook(final GamePhase phase, final PlayerInjectHooks playerHook) {
        throw new UnsupportedOperationException("This is not needed in Spigot.");
    }
    
    @Override
    public void setPlayerHook(final PlayerInjectHooks playerHook) {
        throw new UnsupportedOperationException("This is not needed in Spigot.");
    }
    
    @Override
    public void addPacketHandler(final PacketType type, final Set<ListenerOptions> options) {
        this.sendingFilters.addType(type);
    }
    
    @Override
    public void removePacketHandler(final PacketType type) {
        this.sendingFilters.removeType(type);
    }
    
    @Override
    public Set<PacketType> getSendingFilters() {
        return this.sendingFilters.values();
    }
    
    @Override
    public void close() {
        this.sendingFilters.clear();
    }
    
    @Override
    public PlayerInjectHooks getPlayerHook(final GamePhase phase) {
        return PlayerInjectHooks.NETWORK_SERVER_OBJECT;
    }
    
    @Override
    public boolean canRecievePackets() {
        return true;
    }
    
    @Override
    public PlayerInjectHooks getPlayerHook() {
        return PlayerInjectHooks.NETWORK_SERVER_OBJECT;
    }
    
    @Override
    public Player getPlayerByConnection(final DataInputStream inputStream) throws InterruptedException {
        throw new UnsupportedOperationException("This is not needed in Spigot.");
    }
    
    @Override
    public void checkListener(final PacketListener listener) {
    }
    
    @Override
    public void checkListener(final Set<PacketListener> listeners) {
    }
}
