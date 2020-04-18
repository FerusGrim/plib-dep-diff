package com.comphenix.protocol.injector.server;

import java.util.Iterator;
import com.comphenix.protocol.events.NetworkMarker;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

public class BukkitSocketInjector implements SocketInjector
{
    private Player player;
    private List<QueuedSendPacket> syncronizedQueue;
    
    public BukkitSocketInjector(final Player player) {
        this.syncronizedQueue = Collections.synchronizedList(new ArrayList<QueuedSendPacket>());
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be NULL.");
        }
        this.player = player;
    }
    
    @Override
    public Socket getSocket() throws IllegalAccessException {
        throw new UnsupportedOperationException("Cannot get socket from Bukkit player.");
    }
    
    @Override
    public SocketAddress getAddress() throws IllegalAccessException {
        return this.player.getAddress();
    }
    
    @Override
    public void disconnect(final String message) throws InvocationTargetException {
        this.player.kickPlayer(message);
    }
    
    @Override
    public void sendServerPacket(final Object packet, final NetworkMarker marker, final boolean filtered) throws InvocationTargetException {
        final QueuedSendPacket command = new QueuedSendPacket(packet, marker, filtered);
        this.syncronizedQueue.add(command);
    }
    
    @Override
    public Player getPlayer() {
        return this.player;
    }
    
    @Override
    public Player getUpdatedPlayer() {
        return this.player;
    }
    
    @Override
    public void transferState(final SocketInjector delegate) {
        try {
            synchronized (this.syncronizedQueue) {
                for (final QueuedSendPacket command : this.syncronizedQueue) {
                    delegate.sendServerPacket(command.getPacket(), command.getMarker(), command.isFiltered());
                }
                this.syncronizedQueue.clear();
            }
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to transmit packets to " + delegate + " from old injector.", e);
        }
    }
    
    @Override
    public void setUpdatedPlayer(final Player updatedPlayer) {
        this.player = updatedPlayer;
    }
}
