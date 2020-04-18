package com.comphenix.protocol.injector.packet;

import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.injector.PacketFilterManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import com.comphenix.protocol.injector.player.PlayerInjectionHandler;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.injector.ListenerInvoker;

public class PacketInjectorBuilder
{
    protected ListenerInvoker invoker;
    protected ErrorReporter reporter;
    protected PlayerInjectionHandler playerInjection;
    
    protected PacketInjectorBuilder() {
    }
    
    public static PacketInjectorBuilder newBuilder() {
        return new PacketInjectorBuilder();
    }
    
    public PacketInjectorBuilder reporter(@Nonnull final ErrorReporter reporter) {
        Preconditions.checkNotNull((Object)reporter, (Object)"reporter cannot be NULL");
        this.reporter = reporter;
        return this;
    }
    
    public PacketInjectorBuilder invoker(@Nonnull final ListenerInvoker invoker) {
        Preconditions.checkNotNull((Object)invoker, (Object)"invoker cannot be NULL");
        this.invoker = invoker;
        return this;
    }
    
    @Nonnull
    public PacketInjectorBuilder playerInjection(@Nonnull final PlayerInjectionHandler playerInjection) {
        Preconditions.checkNotNull((Object)playerInjection, (Object)"playerInjection cannot be NULL");
        this.playerInjection = playerInjection;
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
        if (this.playerInjection == null) {
            throw new IllegalStateException("Player injection parameter must be initialized.");
        }
    }
    
    public PacketInjector buildInjector() throws FieldAccessException {
        this.initializeDefaults();
        return new ProxyPacketInjector(this.invoker, this.playerInjection, this.reporter);
    }
}
