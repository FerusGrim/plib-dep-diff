package com.comphenix.protocol.injector;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;
import com.comphenix.protocol.error.Report;
import com.google.common.util.concurrent.FutureCallback;
import com.comphenix.executors.BukkitFutures;
import org.bukkit.event.world.WorldInitEvent;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.injector.player.InjectedServerConnection;
import com.comphenix.protocol.injector.spigot.SpigotPacketInjector;
import javax.annotation.Nonnull;
import com.comphenix.protocol.async.AsyncFilterManager;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.utility.MinecraftVersion;
import org.bukkit.plugin.Plugin;
import org.bukkit.Server;
import com.comphenix.protocol.error.ReportType;

public class PacketFilterBuilder
{
    public static final ReportType REPORT_TEMPORARY_EVENT_ERROR;
    public static final ReportType REPORT_SPIGOT_IS_DELAYING_INJECTOR;
    private ClassLoader classLoader;
    private Server server;
    private Plugin library;
    private MinecraftVersion mcVersion;
    private DelayedSingleTask unhookTask;
    private ErrorReporter reporter;
    private AsyncFilterManager asyncManager;
    private boolean nettyEnabled;
    
    public PacketFilterBuilder classLoader(@Nonnull final ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be NULL.");
        }
        this.classLoader = classLoader;
        return this;
    }
    
    public PacketFilterBuilder server(@Nonnull final Server server) {
        if (server == null) {
            throw new IllegalArgumentException("server cannot be NULL.");
        }
        this.server = server;
        return this;
    }
    
    public PacketFilterBuilder library(@Nonnull final Plugin library) {
        if (library == null) {
            throw new IllegalArgumentException("library cannot be NULL.");
        }
        this.library = library;
        return this;
    }
    
    public PacketFilterBuilder minecraftVersion(@Nonnull final MinecraftVersion mcVersion) {
        if (mcVersion == null) {
            throw new IllegalArgumentException("minecraftVersion cannot be NULL.");
        }
        this.mcVersion = mcVersion;
        return this;
    }
    
    public PacketFilterBuilder unhookTask(@Nonnull final DelayedSingleTask unhookTask) {
        if (unhookTask == null) {
            throw new IllegalArgumentException("unhookTask cannot be NULL.");
        }
        this.unhookTask = unhookTask;
        return this;
    }
    
    public PacketFilterBuilder reporter(@Nonnull final ErrorReporter reporter) {
        if (reporter == null) {
            throw new IllegalArgumentException("reporter cannot be NULL.");
        }
        this.reporter = reporter;
        return this;
    }
    
    public boolean isNettyEnabled() {
        return this.nettyEnabled;
    }
    
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }
    
    public Server getServer() {
        return this.server;
    }
    
    public Plugin getLibrary() {
        return this.library;
    }
    
    public MinecraftVersion getMinecraftVersion() {
        return this.mcVersion;
    }
    
    public DelayedSingleTask getUnhookTask() {
        return this.unhookTask;
    }
    
    public ErrorReporter getReporter() {
        return this.reporter;
    }
    
    public AsyncFilterManager getAsyncManager() {
        return this.asyncManager;
    }
    
    public InternalManager build() {
        if (this.reporter == null) {
            throw new IllegalArgumentException("reporter cannot be NULL.");
        }
        if (this.classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be NULL.");
        }
        this.asyncManager = new AsyncFilterManager(this.reporter, this.server.getScheduler());
        this.nettyEnabled = false;
        if (SpigotPacketInjector.canUseSpigotListener()) {
            if (InjectedServerConnection.getServerConnection(this.reporter, this.server) == null) {
                final DelayedPacketManager delayed = new DelayedPacketManager(this.reporter, this.mcVersion);
                delayed.setAsynchronousManager(this.asyncManager);
                this.asyncManager.setManager(delayed);
                Futures.addCallback((ListenableFuture)BukkitFutures.nextEvent(this.library, WorldInitEvent.class), (FutureCallback)new FutureCallback<WorldInitEvent>() {
                    public void onSuccess(final WorldInitEvent event) {
                        if (delayed.isClosed()) {
                            return;
                        }
                        try {
                            PacketFilterBuilder.this.registerSpigot(delayed);
                        }
                        catch (Exception e) {
                            this.onFailure(e);
                        }
                    }
                    
                    public void onFailure(final Throwable error) {
                        PacketFilterBuilder.this.reporter.reportWarning(PacketFilterBuilder.this, Report.newBuilder(PacketFilterBuilder.REPORT_TEMPORARY_EVENT_ERROR).error(error));
                    }
                });
                this.reporter.reportWarning(this, Report.newBuilder(PacketFilterBuilder.REPORT_SPIGOT_IS_DELAYING_INJECTOR));
                return delayed;
            }
            this.nettyEnabled = !MinecraftReflection.isMinecraftObject(InjectedServerConnection.getServerConnection(this.reporter, this.server));
        }
        return this.buildInternal();
    }
    
    private void registerSpigot(final DelayedPacketManager delayed) {
        this.nettyEnabled = !MinecraftReflection.isMinecraftObject(InjectedServerConnection.getServerConnection(this.reporter, this.server));
        delayed.setDelegate(this.buildInternal());
    }
    
    private PacketFilterManager buildInternal() {
        final PacketFilterManager manager = new PacketFilterManager(this);
        this.asyncManager.setManager(manager);
        return manager;
    }
    
    static {
        REPORT_TEMPORARY_EVENT_ERROR = new ReportType("Unable to register or handle temporary event.");
        REPORT_SPIGOT_IS_DELAYING_INJECTOR = new ReportType("Delaying due to Spigot.");
    }
}
