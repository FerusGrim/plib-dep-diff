package com.comphenix.protocol.injector;

import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.ListeningWhitelist;
import org.bukkit.World;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.google.common.collect.Sets;
import java.util.Set;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.Collection;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import com.comphenix.protocol.injector.netty.WirePacket;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.ConnectionSide;
import java.util.Iterator;
import com.comphenix.protocol.error.Report;
import com.google.common.base.Objects;
import org.bukkit.entity.Player;
import com.google.common.base.Preconditions;
import java.util.Collections;
import com.google.common.collect.Lists;
import javax.annotation.Nonnull;
import com.comphenix.protocol.utility.MinecraftVersion;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.events.PacketListener;
import java.util.List;
import com.comphenix.protocol.error.ReportType;

public class DelayedPacketManager implements InternalManager
{
    public static final ReportType REPORT_CANNOT_SEND_QUEUED_PACKET;
    public static final ReportType REPORT_CANNOT_SEND_QUEUED_WIRE_PACKET;
    public static final ReportType REPORT_CANNOT_REGISTER_QUEUED_LISTENER;
    private volatile InternalManager delegate;
    private final List<Runnable> queuedActions;
    private final List<PacketListener> queuedListeners;
    private AsynchronousManager asyncManager;
    private ErrorReporter reporter;
    private PlayerInjectHooks hook;
    private boolean closed;
    private boolean debug;
    private PluginManager queuedManager;
    private Plugin queuedPlugin;
    private MinecraftVersion version;
    
    public DelayedPacketManager(@Nonnull final ErrorReporter reporter, @Nonnull final MinecraftVersion version) {
        this.queuedActions = Collections.synchronizedList((List<Runnable>)Lists.newArrayList());
        this.queuedListeners = Collections.synchronizedList((List<PacketListener>)Lists.newArrayList());
        this.hook = PlayerInjectHooks.NETWORK_SERVER_OBJECT;
        Preconditions.checkNotNull((Object)reporter, (Object)"reporter cannot be NULL.");
        Preconditions.checkNotNull((Object)version, (Object)"version cannot be NULL.");
        this.reporter = reporter;
        this.version = version;
    }
    
    public InternalManager getDelegate() {
        return this.delegate;
    }
    
    @Override
    public int getProtocolVersion(final Player player) {
        if (this.delegate != null) {
            return this.delegate.getProtocolVersion(player);
        }
        return Integer.MIN_VALUE;
    }
    
    @Override
    public MinecraftVersion getMinecraftVersion() {
        if (this.delegate != null) {
            return this.delegate.getMinecraftVersion();
        }
        return this.version;
    }
    
    protected void setDelegate(final InternalManager delegate) {
        this.delegate = delegate;
        if (delegate != null) {
            if (!Objects.equal((Object)delegate.getPlayerHook(), (Object)this.hook)) {
                delegate.setPlayerHook(this.hook);
            }
            if (this.queuedManager != null && this.queuedPlugin != null) {
                delegate.registerEvents(this.queuedManager, this.queuedPlugin);
            }
            delegate.setDebug(this.debug);
            synchronized (this.queuedListeners) {
                for (final PacketListener listener : this.queuedListeners) {
                    try {
                        delegate.addPacketListener(listener);
                    }
                    catch (IllegalArgumentException e) {
                        this.reporter.reportWarning(this, Report.newBuilder(DelayedPacketManager.REPORT_CANNOT_REGISTER_QUEUED_LISTENER).callerParam(delegate).messageParam(listener).error(e));
                    }
                }
            }
            synchronized (this.queuedActions) {
                for (final Runnable action : this.queuedActions) {
                    action.run();
                }
            }
            this.queuedListeners.clear();
            this.queuedActions.clear();
        }
    }
    
    private Runnable queuedAddPacket(final ConnectionSide side, final Player player, final PacketContainer packet, final NetworkMarker marker, final boolean filtered) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    switch (side) {
                        case CLIENT_SIDE: {
                            DelayedPacketManager.this.delegate.recieveClientPacket(player, packet, marker, filtered);
                            break;
                        }
                        case SERVER_SIDE: {
                            DelayedPacketManager.this.delegate.sendServerPacket(player, packet, marker, filtered);
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException("side cannot be " + side);
                        }
                    }
                }
                catch (Exception e) {
                    DelayedPacketManager.this.reporter.reportWarning(this, Report.newBuilder(DelayedPacketManager.REPORT_CANNOT_SEND_QUEUED_PACKET).callerParam(DelayedPacketManager.this.delegate).messageParam(packet).error(e));
                }
            }
        };
    }
    
    @Override
    public void setPlayerHook(final PlayerInjectHooks playerHook) {
        this.hook = playerHook;
    }
    
    @Override
    public PlayerInjectHooks getPlayerHook() {
        return this.hook;
    }
    
    @Override
    public void sendServerPacket(final Player receiver, final PacketContainer packet) throws InvocationTargetException {
        this.sendServerPacket(receiver, packet, null, true);
    }
    
    @Override
    public void sendServerPacket(final Player receiver, final PacketContainer packet, final boolean filters) throws InvocationTargetException {
        this.sendServerPacket(receiver, packet, null, filters);
    }
    
    @Override
    public void sendServerPacket(final Player receiver, final PacketContainer packet, final NetworkMarker marker, final boolean filters) throws InvocationTargetException {
        if (this.delegate != null) {
            this.delegate.sendServerPacket(receiver, packet, marker, filters);
        }
        else {
            this.queuedActions.add(this.queuedAddPacket(ConnectionSide.SERVER_SIDE, receiver, packet, marker, filters));
        }
    }
    
    @Override
    public void sendWirePacket(final Player receiver, final int id, final byte[] bytes) throws InvocationTargetException {
        final WirePacket packet = new WirePacket(id, bytes);
        this.sendWirePacket(receiver, packet);
    }
    
    @Override
    public void sendWirePacket(final Player receiver, final WirePacket packet) throws InvocationTargetException {
        if (this.delegate != null) {
            this.delegate.sendWirePacket(receiver, packet);
        }
        else {
            this.queuedActions.add(new Runnable() {
                @Override
                public void run() {
                    try {
                        DelayedPacketManager.this.delegate.sendWirePacket(receiver, packet);
                    }
                    catch (Throwable ex) {
                        DelayedPacketManager.this.reporter.reportWarning(this, Report.newBuilder(DelayedPacketManager.REPORT_CANNOT_SEND_QUEUED_WIRE_PACKET).callerParam(DelayedPacketManager.this.delegate).messageParam(packet).error(ex));
                    }
                }
            });
        }
    }
    
    @Override
    public void recieveClientPacket(final Player sender, final PacketContainer packet) throws IllegalAccessException, InvocationTargetException {
        this.recieveClientPacket(sender, packet, null, true);
    }
    
    @Override
    public void recieveClientPacket(final Player sender, final PacketContainer packet, final boolean filters) throws IllegalAccessException, InvocationTargetException {
        this.recieveClientPacket(sender, packet, null, filters);
    }
    
    @Override
    public void recieveClientPacket(final Player sender, final PacketContainer packet, final NetworkMarker marker, final boolean filters) throws IllegalAccessException, InvocationTargetException {
        if (this.delegate != null) {
            this.delegate.recieveClientPacket(sender, packet, marker, filters);
        }
        else {
            this.queuedActions.add(this.queuedAddPacket(ConnectionSide.CLIENT_SIDE, sender, packet, marker, filters));
        }
    }
    
    @Override
    public void broadcastServerPacket(final PacketContainer packet, final Entity entity, final boolean includeTracker) {
        if (this.delegate != null) {
            this.delegate.broadcastServerPacket(packet, entity, includeTracker);
        }
        else {
            this.queuedActions.add(new Runnable() {
                @Override
                public void run() {
                    DelayedPacketManager.this.delegate.broadcastServerPacket(packet, entity, includeTracker);
                }
            });
        }
    }
    
    @Override
    public void broadcastServerPacket(final PacketContainer packet, final Location origin, final int maxObserverDistance) {
        if (this.delegate != null) {
            this.delegate.broadcastServerPacket(packet, origin, maxObserverDistance);
        }
        else {
            this.queuedActions.add(new Runnable() {
                @Override
                public void run() {
                    DelayedPacketManager.this.delegate.broadcastServerPacket(packet, origin, maxObserverDistance);
                }
            });
        }
    }
    
    @Override
    public void broadcastServerPacket(final PacketContainer packet) {
        if (this.delegate != null) {
            this.delegate.broadcastServerPacket(packet);
        }
        else {
            this.queuedActions.add(new Runnable() {
                @Override
                public void run() {
                    DelayedPacketManager.this.delegate.broadcastServerPacket(packet);
                }
            });
        }
    }
    
    @Override
    public ImmutableSet<PacketListener> getPacketListeners() {
        if (this.delegate != null) {
            return this.delegate.getPacketListeners();
        }
        return (ImmutableSet<PacketListener>)ImmutableSet.copyOf((Collection)this.queuedListeners);
    }
    
    @Override
    public void addPacketListener(final PacketListener listener) {
        if (this.delegate != null) {
            this.delegate.addPacketListener(listener);
        }
        else {
            this.queuedListeners.add(listener);
        }
    }
    
    @Override
    public void removePacketListener(final PacketListener listener) {
        if (this.delegate != null) {
            this.delegate.removePacketListener(listener);
        }
        else {
            this.queuedListeners.remove(listener);
        }
    }
    
    @Override
    public void removePacketListeners(final Plugin plugin) {
        if (this.delegate != null) {
            this.delegate.removePacketListeners(plugin);
        }
        else {
            final Iterator<PacketListener> it = this.queuedListeners.iterator();
            while (it.hasNext()) {
                if (Objects.equal((Object)it.next().getPlugin(), (Object)plugin)) {
                    it.remove();
                }
            }
        }
    }
    
    @Deprecated
    @Override
    public PacketContainer createPacket(final int id) {
        if (this.delegate != null) {
            return this.delegate.createPacket(id);
        }
        return this.createPacket(id, true);
    }
    
    @Deprecated
    @Override
    public PacketContainer createPacket(final int id, final boolean forceDefaults) {
        if (this.delegate != null) {
            return this.delegate.createPacket(id);
        }
        final PacketContainer packet = new PacketContainer(id);
        if (forceDefaults) {
            try {
                packet.getModifier().writeDefaults();
            }
            catch (FieldAccessException e) {
                throw new RuntimeException("Security exception.", e);
            }
        }
        return packet;
    }
    
    @Override
    public PacketConstructor createPacketConstructor(final int id, final Object... arguments) {
        if (this.delegate != null) {
            return this.delegate.createPacketConstructor(id, arguments);
        }
        return PacketConstructor.DEFAULT.withPacket(id, arguments);
    }
    
    @Override
    public PacketConstructor createPacketConstructor(final PacketType type, final Object... arguments) {
        if (this.delegate != null) {
            return this.delegate.createPacketConstructor(type, arguments);
        }
        return PacketConstructor.DEFAULT.withPacket(type, arguments);
    }
    
    @Deprecated
    @Override
    public Set<Integer> getSendingFilters() {
        if (this.delegate != null) {
            return this.delegate.getSendingFilters();
        }
        final Set<Integer> sending = (Set<Integer>)Sets.newHashSet();
        for (final PacketListener listener : this.queuedListeners) {
            sending.addAll(listener.getSendingWhitelist().getWhitelist());
        }
        return sending;
    }
    
    @Deprecated
    @Override
    public Set<Integer> getReceivingFilters() {
        if (this.delegate != null) {
            return this.delegate.getReceivingFilters();
        }
        final Set<Integer> recieving = (Set<Integer>)Sets.newHashSet();
        for (final PacketListener listener : this.queuedListeners) {
            recieving.addAll(listener.getReceivingWhitelist().getWhitelist());
        }
        return recieving;
    }
    
    @Override
    public PacketContainer createPacket(final PacketType type) {
        return this.createPacket(type.getLegacyId());
    }
    
    @Override
    public PacketContainer createPacket(final PacketType type, final boolean forceDefaults) {
        return this.createPacket(type.getLegacyId(), forceDefaults);
    }
    
    @Override
    public Set<PacketType> getSendingFilterTypes() {
        return PacketRegistry.toPacketTypes(this.getSendingFilters(), PacketType.Sender.SERVER);
    }
    
    @Override
    public Set<PacketType> getReceivingFilterTypes() {
        return PacketRegistry.toPacketTypes(this.getReceivingFilters(), PacketType.Sender.CLIENT);
    }
    
    @Override
    public void updateEntity(final Entity entity, final List<Player> observers) throws FieldAccessException {
        if (this.delegate != null) {
            this.delegate.updateEntity(entity, observers);
        }
        else {
            EntityUtilities.updateEntity(entity, observers);
        }
    }
    
    @Override
    public Entity getEntityFromID(final World container, final int id) throws FieldAccessException {
        if (this.delegate != null) {
            return this.delegate.getEntityFromID(container, id);
        }
        return EntityUtilities.getEntityFromID(container, id);
    }
    
    @Override
    public List<Player> getEntityTrackers(final Entity entity) throws FieldAccessException {
        if (this.delegate != null) {
            return this.delegate.getEntityTrackers(entity);
        }
        return EntityUtilities.getEntityTrackers(entity);
    }
    
    @Override
    public boolean isClosed() {
        return this.closed || (this.delegate != null && this.delegate.isClosed());
    }
    
    @Override
    public AsynchronousManager getAsynchronousManager() {
        if (this.delegate != null) {
            return this.delegate.getAsynchronousManager();
        }
        return this.asyncManager;
    }
    
    @Override
    public boolean isDebug() {
        return this.debug;
    }
    
    @Override
    public void setDebug(final boolean debug) {
        this.debug = debug;
        if (this.delegate != null) {
            this.delegate.setDebug(debug);
        }
    }
    
    public void setAsynchronousManager(final AsynchronousManager asyncManager) {
        this.asyncManager = asyncManager;
    }
    
    @Override
    public void registerEvents(final PluginManager manager, final Plugin plugin) {
        if (this.delegate != null) {
            this.delegate.registerEvents(manager, plugin);
        }
        else {
            this.queuedManager = manager;
            this.queuedPlugin = plugin;
        }
    }
    
    @Override
    public void close() {
        if (this.delegate != null) {
            this.delegate.close();
        }
        this.closed = true;
    }
    
    @Override
    public void verifyWhitelist(final PacketListener listener, final ListeningWhitelist whitelist) {
        for (final PacketType type : whitelist.getTypes()) {
            if (type == null) {
                throw new IllegalArgumentException(String.format("Packet type in in listener %s was NULL.", PacketAdapter.getPluginName(listener)));
            }
        }
    }
    
    static {
        REPORT_CANNOT_SEND_QUEUED_PACKET = new ReportType("Cannot send queued packet %s.");
        REPORT_CANNOT_SEND_QUEUED_WIRE_PACKET = new ReportType("Cannot send queued wire packet %s.");
        REPORT_CANNOT_REGISTER_QUEUED_LISTENER = new ReportType("Cannot register queued listener %s.");
    }
}
