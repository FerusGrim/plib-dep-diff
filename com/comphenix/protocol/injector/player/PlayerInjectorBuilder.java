package com.comphenix.protocol.injector.player;

import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import com.comphenix.protocol.injector.PacketFilterManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import com.comphenix.protocol.utility.MinecraftVersion;
import org.bukkit.Server;
import com.comphenix.protocol.events.PacketListener;
import java.util.Set;
import com.comphenix.protocol.injector.ListenerInvoker;
import com.comphenix.protocol.injector.GamePhase;
import com.google.common.base.Predicate;
import com.comphenix.protocol.error.ErrorReporter;

public class PlayerInjectorBuilder
{
    protected ErrorReporter reporter;
    protected Predicate<GamePhase> injectionFilter;
    protected ListenerInvoker invoker;
    protected Set<PacketListener> packetListeners;
    protected Server server;
    protected MinecraftVersion version;
    
    public static PlayerInjectorBuilder newBuilder() {
        return new PlayerInjectorBuilder();
    }
    
    protected PlayerInjectorBuilder() {
    }
    
    public PlayerInjectorBuilder reporter(@Nonnull final ErrorReporter reporter) {
        Preconditions.checkNotNull((Object)reporter, (Object)"reporter cannot be NULL");
        this.reporter = reporter;
        return this;
    }
    
    @Nonnull
    public PlayerInjectorBuilder injectionFilter(@Nonnull final Predicate<GamePhase> injectionFilter) {
        Preconditions.checkNotNull((Object)injectionFilter, (Object)"injectionFilter cannot be NULL");
        this.injectionFilter = injectionFilter;
        return this;
    }
    
    public PlayerInjectorBuilder invoker(@Nonnull final ListenerInvoker invoker) {
        Preconditions.checkNotNull((Object)invoker, (Object)"invoker cannot be NULL");
        this.invoker = invoker;
        return this;
    }
    
    @Nonnull
    public PlayerInjectorBuilder packetListeners(@Nonnull final Set<PacketListener> packetListeners) {
        Preconditions.checkNotNull((Object)packetListeners, (Object)"packetListeners cannot be NULL");
        this.packetListeners = packetListeners;
        return this;
    }
    
    public PlayerInjectorBuilder server(@Nonnull final Server server) {
        Preconditions.checkNotNull((Object)server, (Object)"server cannot be NULL");
        this.server = server;
        return this;
    }
    
    public PlayerInjectorBuilder version(final MinecraftVersion version) {
        this.version = version;
        return this;
    }
    
    private void initializeDefaults() {
        final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        if (this.reporter == null) {
            this.reporter = ProtocolLibrary.getErrorReporter();
        }
        if (this.invoker == null) {
            this.invoker = (PacketFilterManager)manager;
        }
        if (this.server == null) {
            this.server = Bukkit.getServer();
        }
        if (this.injectionFilter == null) {
            throw new IllegalStateException("injectionFilter must be initialized.");
        }
        if (this.packetListeners == null) {
            throw new IllegalStateException("packetListeners must be initialized.");
        }
    }
    
    public PlayerInjectionHandler buildHandler() {
        this.initializeDefaults();
        return new ProxyPlayerInjectionHandler(this.reporter, this.injectionFilter, this.invoker, this.packetListeners, this.server, this.version);
    }
}
