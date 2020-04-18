package com.comphenix.protocol.injector.netty;

import java.util.Iterator;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.injector.server.SocketInjector;
import org.bukkit.Bukkit;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import javax.annotation.Nonnull;
import com.comphenix.protocol.reflect.FuzzyReflection;
import io.netty.channel.Channel;
import com.comphenix.protocol.utility.MinecraftFields;
import com.google.common.collect.MapMaker;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import java.util.concurrent.ConcurrentMap;

public class InjectionFactory
{
    private final ConcurrentMap<Player, Injector> playerLookup;
    private final ConcurrentMap<String, Injector> nameLookup;
    private volatile boolean closed;
    private final Plugin plugin;
    
    public InjectionFactory(final Plugin plugin) {
        this.playerLookup = (ConcurrentMap<Player, Injector>)new MapMaker().weakKeys().weakValues().makeMap();
        this.nameLookup = (ConcurrentMap<String, Injector>)new MapMaker().weakValues().makeMap();
        this.plugin = plugin;
    }
    
    public Plugin getPlugin() {
        return this.plugin;
    }
    
    @Nonnull
    public Injector fromPlayer(final Player player, final ChannelListener listener) {
        if (this.closed) {
            return new ClosedInjector(player);
        }
        Injector injector = this.playerLookup.get(player);
        if (injector == null) {
            injector = this.getTemporaryInjector(player);
        }
        if (injector != null && !injector.isClosed()) {
            return injector;
        }
        final Object networkManager = MinecraftFields.getNetworkManager(player);
        if (networkManager == null) {
            return this.fromName(player.getName(), player);
        }
        final Channel channel = FuzzyReflection.getFieldValue(networkManager, Channel.class, true);
        injector = (ChannelInjector)ChannelInjector.findChannelHandler(channel, ChannelInjector.class);
        if (injector != null) {
            this.playerLookup.remove(injector.getPlayer());
            injector.setPlayer(player);
        }
        else {
            injector = new ChannelInjector(player, networkManager, channel, listener, this);
        }
        this.cacheInjector(player, injector);
        return injector;
    }
    
    public Injector fromName(final String name, final Player player) {
        if (!this.closed) {
            final Injector injector = this.nameLookup.get(name);
            if (injector != null) {
                injector.setUpdatedPlayer(player);
                return injector;
            }
        }
        return new ClosedInjector(player);
    }
    
    @Nonnull
    public Injector fromChannel(final Channel channel, final ChannelListener listener, final TemporaryPlayerFactory playerFactory) {
        if (this.closed) {
            return new ClosedInjector(null);
        }
        final Object networkManager = this.findNetworkManager(channel);
        final Player temporaryPlayer = playerFactory.createTemporaryPlayer(Bukkit.getServer());
        final ChannelInjector injector = new ChannelInjector(temporaryPlayer, networkManager, channel, listener, this);
        TemporaryPlayerFactory.setInjectorInPlayer(temporaryPlayer, new ChannelInjector.ChannelSocketInjector(injector));
        return injector;
    }
    
    public Injector invalidate(final Player player) {
        final Injector injector = this.playerLookup.remove(player);
        this.nameLookup.remove(player.getName());
        return injector;
    }
    
    public Injector cacheInjector(final Player player, final Injector injector) {
        this.nameLookup.put(player.getName(), injector);
        return this.playerLookup.put(player, injector);
    }
    
    public Injector cacheInjector(final String name, final Injector injector) {
        return this.nameLookup.put(name, injector);
    }
    
    private ChannelInjector getTemporaryInjector(final Player player) {
        final SocketInjector injector = TemporaryPlayerFactory.getInjectorFromPlayer(player);
        if (injector != null) {
            return ((ChannelInjector.ChannelSocketInjector)injector).getChannelInjector();
        }
        return null;
    }
    
    private Object findNetworkManager(final Channel channel) {
        final Object networkManager = ChannelInjector.findChannelHandler(channel, MinecraftReflection.getNetworkManagerClass());
        if (networkManager != null) {
            return networkManager;
        }
        throw new IllegalArgumentException("Unable to find NetworkManager in " + channel);
    }
    
    public boolean isClosed() {
        return this.closed;
    }
    
    public synchronized void close() {
        if (!this.closed) {
            this.closed = true;
            for (final Injector injector : this.playerLookup.values()) {
                injector.close();
            }
            for (final Injector injector : this.nameLookup.values()) {
                injector.close();
            }
        }
    }
}
