package com.comphenix.protocol.injector;

import com.comphenix.protocol.injector.player.PlayerInjector;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import io.netty.channel.Channel;
import com.comphenix.protocol.injector.netty.WirePacket;
import com.comphenix.protocol.events.ListenerPriority;
import org.bukkit.Bukkit;
import com.comphenix.protocol.events.NetworkMarker;
import org.bukkit.World;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.async.AsyncMarker;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Objects;
import java.util.List;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.utility.Util;
import java.util.Iterator;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.ListenerOptions;
import java.util.Collection;
import com.google.common.collect.ImmutableSet;
import com.comphenix.protocol.AsynchronousManager;
import org.bukkit.entity.Player;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.comphenix.protocol.injector.player.PlayerInjectorBuilder;
import com.comphenix.protocol.injector.packet.PacketInjectorBuilder;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.error.Report;
import javax.annotation.Nullable;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.injector.netty.ProtocolInjector;
import com.comphenix.protocol.injector.spigot.SpigotPacketInjector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.comphenix.protocol.async.AsyncFilterManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.Server;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.injector.packet.InterceptWritePacket;
import com.comphenix.protocol.injector.player.PlayerInjectionHandler;
import com.comphenix.protocol.injector.packet.PacketInjector;
import com.comphenix.protocol.events.PacketListener;
import java.util.Set;
import com.comphenix.protocol.error.ReportType;

public final class PacketFilterManager implements ListenerInvoker, InternalManager
{
    public static final ReportType REPORT_CANNOT_LOAD_PACKET_LIST;
    public static final ReportType REPORT_CANNOT_INITIALIZE_PACKET_INJECTOR;
    public static final ReportType REPORT_PLUGIN_DEPEND_MISSING;
    public static final ReportType REPORT_UNSUPPORTED_SERVER_PACKET;
    public static final ReportType REPORT_UNSUPPORTED_CLIENT_PACKET;
    public static final ReportType REPORT_CANNOT_UNINJECT_PLAYER;
    public static final ReportType REPORT_CANNOT_UNINJECT_OFFLINE_PLAYER;
    public static final ReportType REPORT_CANNOT_INJECT_PLAYER;
    public static final ReportType REPORT_CANNOT_UNREGISTER_PLUGIN;
    public static final ReportType REPORT_PLUGIN_VERIFIER_ERROR;
    public static final int TICKS_PER_SECOND = 20;
    private static final int UNHOOK_DELAY = 100;
    private DelayedSingleTask unhookTask;
    private Set<PacketListener> packetListeners;
    private PacketInjector packetInjector;
    private PlayerInjectionHandler playerInjection;
    private InterceptWritePacket interceptWritePacket;
    private volatile Set<PacketType> inputBufferedPackets;
    private SortedPacketListenerList recievedListeners;
    private SortedPacketListenerList sendingListeners;
    private volatile boolean hasClosed;
    private ClassLoader classLoader;
    private ErrorReporter reporter;
    private Server server;
    private Plugin library;
    private AsyncFilterManager asyncFilterManager;
    private boolean knowsServerPackets;
    private boolean knowsClientPackets;
    private AtomicInteger phaseLoginCount;
    private AtomicInteger phasePlayingCount;
    private AtomicBoolean packetCreation;
    private SpigotPacketInjector spigotInjector;
    private ProtocolInjector nettyInjector;
    private PluginVerifier pluginVerifier;
    private boolean hasRecycleDistance;
    private MinecraftVersion minecraftVersion;
    private LoginPackets loginPackets;
    private boolean debug;
    
    public PacketFilterManager(final PacketFilterBuilder builder) {
        this.packetListeners = Collections.newSetFromMap(new ConcurrentHashMap<PacketListener, Boolean>());
        this.inputBufferedPackets = (Set<PacketType>)Sets.newHashSet();
        this.phaseLoginCount = new AtomicInteger(0);
        this.phasePlayingCount = new AtomicInteger(0);
        this.packetCreation = new AtomicBoolean();
        this.hasRecycleDistance = true;
        final Predicate<GamePhase> isInjectionNecessary = (Predicate<GamePhase>)new Predicate<GamePhase>() {
            public boolean apply(@Nullable final GamePhase phase) {
                boolean result = true;
                if (phase.hasLogin()) {
                    result &= (PacketFilterManager.this.getPhaseLoginCount() > 0);
                }
                if (phase.hasPlaying()) {
                    result &= (PacketFilterManager.this.getPhasePlayingCount() > 0 || PacketFilterManager.this.unhookTask.isRunning());
                }
                return result;
            }
        };
        this.recievedListeners = new SortedPacketListenerList();
        this.sendingListeners = new SortedPacketListenerList();
        this.unhookTask = builder.getUnhookTask();
        this.server = builder.getServer();
        this.classLoader = builder.getClassLoader();
        this.reporter = builder.getReporter();
        try {
            this.pluginVerifier = new PluginVerifier(builder.getLibrary());
        }
        catch (OutOfMemoryError e) {
            throw e;
        }
        catch (Throwable e2) {
            this.reporter.reportWarning(this, Report.newBuilder(PacketFilterManager.REPORT_PLUGIN_VERIFIER_ERROR).messageParam(e2.getMessage()).error(e2));
        }
        this.minecraftVersion = builder.getMinecraftVersion();
        this.loginPackets = new LoginPackets(this.minecraftVersion);
        this.interceptWritePacket = new InterceptWritePacket(this.reporter);
        if (MinecraftReflection.isUsingNetty()) {
            this.nettyInjector = new ProtocolInjector(builder.getLibrary(), this, this.reporter);
            this.playerInjection = this.nettyInjector.getPlayerInjector();
            this.packetInjector = this.nettyInjector.getPacketInjector();
        }
        else if (builder.isNettyEnabled()) {
            this.spigotInjector = new SpigotPacketInjector(this.reporter, this, this.server);
            this.playerInjection = this.spigotInjector.getPlayerHandler();
            this.packetInjector = this.spigotInjector.getPacketInjector();
            this.spigotInjector.setProxyPacketInjector(PacketInjectorBuilder.newBuilder().invoker(this).reporter(this.reporter).playerInjection(this.playerInjection).buildInjector());
        }
        else {
            this.playerInjection = PlayerInjectorBuilder.newBuilder().invoker(this).server(this.server).reporter(this.reporter).packetListeners(this.packetListeners).injectionFilter(isInjectionNecessary).version(builder.getMinecraftVersion()).buildHandler();
            this.packetInjector = PacketInjectorBuilder.newBuilder().invoker(this).reporter(this.reporter).playerInjection(this.playerInjection).buildInjector();
        }
        this.asyncFilterManager = builder.getAsyncManager();
        this.library = builder.getLibrary();
        try {
            this.knowsServerPackets = (PacketRegistry.getClientPacketTypes() != null);
            this.knowsClientPackets = (PacketRegistry.getServerPacketTypes() != null);
        }
        catch (FieldAccessException e3) {
            this.reporter.reportWarning(this, Report.newBuilder(PacketFilterManager.REPORT_CANNOT_LOAD_PACKET_LIST).error(e3));
        }
    }
    
    public static PacketFilterBuilder newBuilder() {
        return new PacketFilterBuilder();
    }
    
    @Override
    public int getProtocolVersion(final Player player) {
        return this.playerInjection.getProtocolVersion(player);
    }
    
    @Override
    public MinecraftVersion getMinecraftVersion() {
        return this.minecraftVersion;
    }
    
    @Override
    public AsynchronousManager getAsynchronousManager() {
        return this.asyncFilterManager;
    }
    
    @Override
    public boolean isDebug() {
        return this.debug;
    }
    
    @Override
    public void setDebug(final boolean debug) {
        this.debug = debug;
        if (this.nettyInjector != null) {
            this.nettyInjector.setDebug(debug);
        }
    }
    
    @Override
    public PlayerInjectHooks getPlayerHook() {
        return this.playerInjection.getPlayerHook();
    }
    
    @Override
    public void setPlayerHook(final PlayerInjectHooks playerHook) {
        this.playerInjection.setPlayerHook(playerHook);
    }
    
    @Override
    public ImmutableSet<PacketListener> getPacketListeners() {
        return (ImmutableSet<PacketListener>)ImmutableSet.copyOf((Collection)this.packetListeners);
    }
    
    @Override
    public InterceptWritePacket getInterceptWritePacket() {
        return this.interceptWritePacket;
    }
    
    private void printPluginWarnings(final Plugin plugin) {
        if (this.pluginVerifier == null) {
            return;
        }
        try {
            switch (this.pluginVerifier.verify(plugin)) {
                case NO_DEPEND: {
                    this.reporter.reportWarning(this, Report.newBuilder(PacketFilterManager.REPORT_PLUGIN_DEPEND_MISSING).messageParam(plugin.getName()));
                    break;
                }
            }
        }
        catch (Exception e) {
            this.reporter.reportWarning(this, Report.newBuilder(PacketFilterManager.REPORT_PLUGIN_VERIFIER_ERROR).messageParam(e.getMessage()));
        }
    }
    
    @Override
    public void addPacketListener(final PacketListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be NULL.");
        }
        if (this.packetListeners.contains(listener)) {
            return;
        }
        final ListeningWhitelist sending = listener.getSendingWhitelist();
        final ListeningWhitelist receiving = listener.getReceivingWhitelist();
        final boolean hasSending = sending != null && sending.isEnabled();
        final boolean hasReceiving = receiving != null && receiving.isEnabled();
        if ((!hasSending || !sending.getOptions().contains(ListenerOptions.SKIP_PLUGIN_VERIFIER)) && (!hasReceiving || !receiving.getOptions().contains(ListenerOptions.SKIP_PLUGIN_VERIFIER))) {
            this.printPluginWarnings(listener.getPlugin());
        }
        if (hasSending || hasReceiving) {
            if (hasSending) {
                if (sending.getOptions().contains(ListenerOptions.INTERCEPT_INPUT_BUFFER)) {
                    throw new IllegalArgumentException("Sending whitelist cannot require input bufferes to be intercepted.");
                }
                this.verifyWhitelist(listener, sending);
                this.sendingListeners.addListener(listener, sending);
                this.enablePacketFilters(listener, sending.getTypes());
                this.playerInjection.checkListener(listener);
            }
            if (hasSending) {
                this.incrementPhases(this.processPhase(sending));
            }
            if (hasReceiving) {
                this.verifyWhitelist(listener, receiving);
                this.recievedListeners.addListener(listener, receiving);
                this.enablePacketFilters(listener, receiving.getTypes());
            }
            if (hasReceiving) {
                this.incrementPhases(this.processPhase(receiving));
            }
            this.packetListeners.add(listener);
            this.updateRequireInputBuffers();
        }
    }
    
    private GamePhase processPhase(final ListeningWhitelist whitelist) {
        if (!whitelist.getGamePhase().hasLogin() && !whitelist.getOptions().contains(ListenerOptions.DISABLE_GAMEPHASE_DETECTION)) {
            for (final PacketType type : whitelist.getTypes()) {
                if (this.loginPackets.isLoginPacket(type)) {
                    return GamePhase.BOTH;
                }
            }
        }
        return whitelist.getGamePhase();
    }
    
    private void updateRequireInputBuffers() {
        final Set<PacketType> updated = (Set<PacketType>)Sets.newHashSet();
        for (final PacketListener listener : this.packetListeners) {
            final ListeningWhitelist whitelist = listener.getReceivingWhitelist();
            if (whitelist.getOptions().contains(ListenerOptions.INTERCEPT_INPUT_BUFFER)) {
                updated.addAll(whitelist.getTypes());
            }
        }
        this.inputBufferedPackets = updated;
        this.packetInjector.inputBuffersChanged(updated);
    }
    
    private void incrementPhases(final GamePhase phase) {
        if (phase.hasLogin()) {
            this.phaseLoginCount.incrementAndGet();
        }
        if (phase.hasPlaying() && this.phasePlayingCount.incrementAndGet() == 1) {
            if (this.unhookTask.isRunning()) {
                this.unhookTask.cancel();
            }
            else {
                this.initializePlayers(Util.getOnlinePlayers());
            }
        }
    }
    
    private void decrementPhases(final GamePhase phase) {
        if (phase.hasLogin()) {
            this.phaseLoginCount.decrementAndGet();
        }
        if (phase.hasPlaying() && this.phasePlayingCount.decrementAndGet() == 0) {
            this.unhookTask.schedule(100L, new Runnable() {
                @Override
                public void run() {
                    PacketFilterManager.this.uninitializePlayers(Util.getOnlinePlayers());
                }
            });
        }
    }
    
    @Override
    public void verifyWhitelist(final PacketListener listener, final ListeningWhitelist whitelist) {
        for (final PacketType type : whitelist.getTypes()) {
            if (type == null) {
                throw new IllegalArgumentException(String.format("Packet type in in listener %s was NULL.", PacketAdapter.getPluginName(listener)));
            }
        }
    }
    
    @Override
    public void removePacketListener(final PacketListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be NULL");
        }
        List<PacketType> sendingRemoved = null;
        List<PacketType> receivingRemoved = null;
        final ListeningWhitelist sending = listener.getSendingWhitelist();
        final ListeningWhitelist receiving = listener.getReceivingWhitelist();
        if (!this.packetListeners.remove(listener)) {
            return;
        }
        if (sending != null && sending.isEnabled()) {
            sendingRemoved = this.sendingListeners.removeListener(listener, sending);
            this.decrementPhases(this.processPhase(sending));
        }
        if (receiving != null && receiving.isEnabled()) {
            receivingRemoved = this.recievedListeners.removeListener(listener, receiving);
            this.decrementPhases(this.processPhase(receiving));
        }
        if (sendingRemoved != null && sendingRemoved.size() > 0) {
            this.disablePacketFilters(ConnectionSide.SERVER_SIDE, sendingRemoved);
        }
        if (receivingRemoved != null && receivingRemoved.size() > 0) {
            this.disablePacketFilters(ConnectionSide.CLIENT_SIDE, receivingRemoved);
        }
        this.updateRequireInputBuffers();
    }
    
    @Override
    public void removePacketListeners(final Plugin plugin) {
        for (final PacketListener listener : this.packetListeners) {
            if (Objects.equal((Object)listener.getPlugin(), (Object)plugin)) {
                this.removePacketListener(listener);
            }
        }
        this.asyncFilterManager.unregisterAsyncHandlers(plugin);
    }
    
    @Override
    public void invokePacketRecieving(final PacketEvent event) {
        if (!this.hasClosed) {
            this.handlePacket(this.recievedListeners, event, false);
        }
    }
    
    @Override
    public void invokePacketSending(final PacketEvent event) {
        if (!this.hasClosed) {
            this.handlePacket(this.sendingListeners, event, true);
        }
    }
    
    @Override
    public boolean requireInputBuffer(final int packetId) {
        return this.inputBufferedPackets.contains(PacketType.findLegacy(packetId, PacketType.Sender.CLIENT));
    }
    
    private void handlePacket(final SortedPacketListenerList packetListeners, final PacketEvent event, final boolean sending) {
        if (this.asyncFilterManager.hasAsynchronousListeners(event)) {
            event.setAsyncMarker(this.asyncFilterManager.createAsyncMarker());
        }
        if (sending) {
            packetListeners.invokePacketSending(this.reporter, event);
        }
        else {
            packetListeners.invokePacketRecieving(this.reporter, event);
        }
        if (!event.isCancelled() && !this.hasAsyncCancelled(event.getAsyncMarker())) {
            this.asyncFilterManager.enqueueSyncPacket(event, event.getAsyncMarker());
            event.setReadOnly(false);
            event.setCancelled(true);
        }
    }
    
    private boolean hasAsyncCancelled(final AsyncMarker marker) {
        return marker == null || marker.isAsyncCancelled();
    }
    
    private void enablePacketFilters(final PacketListener listener, final Iterable<PacketType> packets) {
        for (final PacketType type : packets) {
            if (type.getSender() == PacketType.Sender.SERVER) {
                if (!this.knowsServerPackets || PacketRegistry.getServerPacketTypes().contains(type)) {
                    this.playerInjection.addPacketHandler(type, listener.getSendingWhitelist().getOptions());
                }
                else {
                    this.reporter.reportWarning(this, Report.newBuilder(PacketFilterManager.REPORT_UNSUPPORTED_SERVER_PACKET).messageParam(PacketAdapter.getPluginName(listener), type));
                }
            }
            if (type.getSender() == PacketType.Sender.CLIENT && this.packetInjector != null) {
                if (!this.knowsClientPackets || PacketRegistry.getClientPacketTypes().contains(type)) {
                    this.packetInjector.addPacketHandler(type, listener.getReceivingWhitelist().getOptions());
                }
                else {
                    this.reporter.reportWarning(this, Report.newBuilder(PacketFilterManager.REPORT_UNSUPPORTED_CLIENT_PACKET).messageParam(PacketAdapter.getPluginName(listener), type));
                }
            }
        }
    }
    
    private void disablePacketFilters(final ConnectionSide side, final Iterable<PacketType> packets) {
        if (side == null) {
            throw new IllegalArgumentException("side cannot be NULL.");
        }
        for (final PacketType type : packets) {
            if (side.isForServer()) {
                this.playerInjection.removePacketHandler(type);
            }
            if (side.isForClient() && this.packetInjector != null) {
                this.packetInjector.removePacketHandler(type);
            }
        }
    }
    
    @Override
    public void broadcastServerPacket(final PacketContainer packet) {
        Preconditions.checkNotNull((Object)packet, (Object)"packet cannot be NULL.");
        this.broadcastServerPacket(packet, Util.getOnlinePlayers());
    }
    
    @Override
    public void broadcastServerPacket(final PacketContainer packet, final Entity entity, final boolean includeTracker) {
        Preconditions.checkNotNull((Object)packet, (Object)"packet cannot be NULL.");
        Preconditions.checkNotNull((Object)entity, (Object)"entity cannot be NULL.");
        final List<Player> trackers = this.getEntityTrackers(entity);
        if (includeTracker && entity instanceof Player) {
            trackers.add((Player)entity);
        }
        this.broadcastServerPacket(packet, trackers);
    }
    
    @Override
    public void broadcastServerPacket(final PacketContainer packet, final Location origin, final int maxObserverDistance) {
        try {
            final int maxDistance = maxObserverDistance * maxObserverDistance;
            final World world = origin.getWorld();
            final Location recycle = origin.clone();
            for (final Player player : Util.getOnlinePlayers()) {
                if (world.equals(player.getWorld()) && this.getDistanceSquared(origin, recycle, player) <= maxDistance) {
                    this.sendServerPacket(player, packet);
                }
            }
        }
        catch (InvocationTargetException e) {
            throw new FieldAccessException("Unable to send server packet.", e);
        }
    }
    
    private double getDistanceSquared(final Location origin, final Location recycle, final Player player) {
        if (this.hasRecycleDistance) {
            try {
                return player.getLocation(recycle).distanceSquared(origin);
            }
            catch (Error e) {
                this.hasRecycleDistance = false;
            }
        }
        return player.getLocation().distanceSquared(origin);
    }
    
    private void broadcastServerPacket(final PacketContainer packet, final Iterable<Player> players) {
        try {
            for (final Player player : players) {
                this.sendServerPacket(player, packet);
            }
        }
        catch (InvocationTargetException e) {
            throw new FieldAccessException("Unable to send server packet.", e);
        }
    }
    
    @Override
    public void sendServerPacket(final Player reciever, final PacketContainer packet) throws InvocationTargetException {
        this.sendServerPacket(reciever, packet, null, true);
    }
    
    @Override
    public void sendServerPacket(final Player reciever, final PacketContainer packet, final boolean filters) throws InvocationTargetException {
        this.sendServerPacket(reciever, packet, null, filters);
    }
    
    @Override
    public void sendServerPacket(final Player receiver, final PacketContainer packet, NetworkMarker marker, final boolean filters) throws InvocationTargetException {
        if (receiver == null) {
            throw new IllegalArgumentException("receiver cannot be NULL.");
        }
        if (packet == null) {
            throw new IllegalArgumentException("packet cannot be NULL.");
        }
        if (packet.getType().getSender() == PacketType.Sender.CLIENT) {
            throw new IllegalArgumentException("Packet of sender CLIENT cannot be sent to a client.");
        }
        if (this.packetCreation.compareAndSet(false, true)) {
            this.incrementPhases(GamePhase.PLAYING);
        }
        if (!filters) {
            if (!Bukkit.isPrimaryThread() && this.playerInjection.hasMainThreadListener(packet.getType())) {
                final NetworkMarker copy = marker;
                this.server.getScheduler().scheduleSyncDelayedTask(this.library, (Runnable)new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!Bukkit.isPrimaryThread()) {
                                throw new IllegalStateException("Scheduled task was not executed on the main thread!");
                            }
                            PacketFilterManager.this.sendServerPacket(receiver, packet, copy, filters);
                        }
                        catch (Exception e) {
                            PacketFilterManager.this.reporter.reportMinimal(PacketFilterManager.this.library, "sendServerPacket-run()", e);
                        }
                    }
                });
                return;
            }
            final PacketEvent event = PacketEvent.fromServer(this, packet, marker, receiver, false);
            this.sendingListeners.invokePacketSending(this.reporter, event, ListenerPriority.MONITOR);
            marker = NetworkMarker.getNetworkMarker(event);
        }
        this.playerInjection.sendServerPacket(receiver, packet, marker, filters);
    }
    
    @Override
    public void sendWirePacket(final Player receiver, final int id, final byte[] bytes) throws InvocationTargetException {
        final WirePacket packet = new WirePacket(id, bytes);
        this.sendWirePacket(receiver, packet);
    }
    
    @Override
    public void sendWirePacket(final Player receiver, final WirePacket packet) throws InvocationTargetException {
        final Channel channel = this.playerInjection.getChannel(receiver);
        if (channel == null) {
            throw new InvocationTargetException(new NullPointerException(), "Failed to obtain channel for " + receiver.getName());
        }
        channel.writeAndFlush((Object)packet);
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
        if (sender == null) {
            throw new IllegalArgumentException("sender cannot be NULL.");
        }
        if (packet == null) {
            throw new IllegalArgumentException("packet cannot be NULL.");
        }
        if (packet.getType().getSender() == PacketType.Sender.SERVER) {
            throw new IllegalArgumentException("Packet of sender SERVER cannot be sent to the server.");
        }
        if (this.packetCreation.compareAndSet(false, true)) {
            this.incrementPhases(GamePhase.PLAYING);
        }
        Object mcPacket = packet.getHandle();
        final boolean cancelled = this.packetInjector.isCancelled(mcPacket);
        if (cancelled) {
            this.packetInjector.setCancelled(mcPacket, false);
        }
        if (filters) {
            final byte[] data = NetworkMarker.getByteBuffer(marker);
            final PacketEvent event = this.packetInjector.packetRecieved(packet, sender, data);
            if (event.isCancelled()) {
                return;
            }
            mcPacket = event.getPacket().getHandle();
        }
        else {
            this.recievedListeners.invokePacketSending(this.reporter, PacketEvent.fromClient(this, packet, marker, sender, false), ListenerPriority.MONITOR);
        }
        this.playerInjection.recieveClientPacket(sender, mcPacket);
        if (cancelled) {
            this.packetInjector.setCancelled(mcPacket, true);
        }
    }
    
    @Deprecated
    @Override
    public PacketContainer createPacket(final int id) {
        return this.createPacket(PacketType.findLegacy(id), true);
    }
    
    @Override
    public PacketContainer createPacket(final PacketType type) {
        return this.createPacket(type, true);
    }
    
    @Deprecated
    @Override
    public PacketContainer createPacket(final int id, final boolean forceDefaults) {
        return this.createPacket(PacketType.findLegacy(id), forceDefaults);
    }
    
    @Override
    public PacketContainer createPacket(final PacketType type, final boolean forceDefaults) {
        final PacketContainer packet = new PacketContainer(type);
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
    
    @Deprecated
    @Override
    public PacketConstructor createPacketConstructor(final int id, final Object... arguments) {
        return PacketConstructor.DEFAULT.withPacket(id, arguments);
    }
    
    @Override
    public PacketConstructor createPacketConstructor(final PacketType type, final Object... arguments) {
        return PacketConstructor.DEFAULT.withPacket(type, arguments);
    }
    
    @Deprecated
    @Override
    public Set<Integer> getSendingFilters() {
        return PacketRegistry.toLegacy(this.playerInjection.getSendingFilters());
    }
    
    @Override
    public Set<Integer> getReceivingFilters() {
        return PacketRegistry.toLegacy(this.packetInjector.getPacketHandlers());
    }
    
    @Override
    public Set<PacketType> getSendingFilterTypes() {
        return Collections.unmodifiableSet((Set<? extends PacketType>)this.playerInjection.getSendingFilters());
    }
    
    @Override
    public Set<PacketType> getReceivingFilterTypes() {
        return Collections.unmodifiableSet((Set<? extends PacketType>)this.packetInjector.getPacketHandlers());
    }
    
    @Override
    public void updateEntity(final Entity entity, final List<Player> observers) throws FieldAccessException {
        EntityUtilities.updateEntity(entity, observers);
    }
    
    @Override
    public Entity getEntityFromID(final World container, final int id) throws FieldAccessException {
        return EntityUtilities.getEntityFromID(container, id);
    }
    
    @Override
    public List<Player> getEntityTrackers(final Entity entity) throws FieldAccessException {
        return EntityUtilities.getEntityTrackers(entity);
    }
    
    public void initializePlayers(final List<Player> players) {
        for (final Player player : players) {
            this.playerInjection.injectPlayer(player, PlayerInjectionHandler.ConflictStrategy.OVERRIDE);
        }
    }
    
    public void uninitializePlayers(final List<Player> players) {
        for (final Player player : players) {
            this.playerInjection.uninjectPlayer(player);
        }
    }
    
    @Override
    public void registerEvents(final PluginManager manager, final Plugin plugin) {
        if (this.spigotInjector != null && !this.spigotInjector.register(plugin)) {
            throw new IllegalArgumentException("Spigot has already been registered.");
        }
        if (this.nettyInjector != null) {
            this.nettyInjector.inject();
        }
        manager.registerEvents((Listener)new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerLogin(final PlayerLoginEvent event) {
                PacketFilterManager.this.onPlayerLogin(event);
            }
            
            @EventHandler(priority = EventPriority.LOWEST)
            public void onPrePlayerJoin(final PlayerJoinEvent event) {
                PacketFilterManager.this.onPrePlayerJoin(event);
            }
            
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerJoin(final PlayerJoinEvent event) {
                PacketFilterManager.this.onPlayerJoin(event);
            }
            
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerQuit(final PlayerQuitEvent event) {
                PacketFilterManager.this.onPlayerQuit(event);
            }
            
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPluginDisabled(final PluginDisableEvent event) {
                PacketFilterManager.this.onPluginDisabled(event, plugin);
            }
        }, plugin);
    }
    
    private void onPlayerLogin(final PlayerLoginEvent event) {
        this.playerInjection.updatePlayer(event.getPlayer());
    }
    
    private void onPrePlayerJoin(final PlayerJoinEvent event) {
        this.playerInjection.updatePlayer(event.getPlayer());
    }
    
    private void onPlayerJoin(final PlayerJoinEvent event) {
        try {
            this.playerInjection.uninjectPlayer(event.getPlayer().getAddress());
            this.playerInjection.injectPlayer(event.getPlayer(), PlayerInjectionHandler.ConflictStrategy.OVERRIDE);
        }
        catch (PlayerInjector.ServerHandlerNull serverHandlerNull) {}
        catch (Exception e) {
            this.reporter.reportDetailed(this, Report.newBuilder(PacketFilterManager.REPORT_CANNOT_INJECT_PLAYER).callerParam(event).error(e));
        }
    }
    
    private void onPlayerQuit(final PlayerQuitEvent event) {
        try {
            final Player player = event.getPlayer();
            this.asyncFilterManager.removePlayer(player);
            this.playerInjection.handleDisconnect(player);
            this.playerInjection.uninjectPlayer(player);
        }
        catch (Exception e) {
            this.reporter.reportDetailed(this, Report.newBuilder(PacketFilterManager.REPORT_CANNOT_UNINJECT_OFFLINE_PLAYER).callerParam(event).error(e));
        }
    }
    
    private void onPluginDisabled(final PluginDisableEvent event, final Plugin protocolLibrary) {
        try {
            if (event.getPlugin() != protocolLibrary) {
                this.removePacketListeners(event.getPlugin());
            }
        }
        catch (Exception e) {
            this.reporter.reportDetailed(this, Report.newBuilder(PacketFilterManager.REPORT_CANNOT_UNREGISTER_PLUGIN).callerParam(event).error(e));
        }
    }
    
    private int getPhasePlayingCount() {
        return this.phasePlayingCount.get();
    }
    
    private int getPhaseLoginCount() {
        return this.phaseLoginCount.get();
    }
    
    @Deprecated
    @Override
    public int getPacketID(final Object packet) {
        return PacketRegistry.getPacketID(packet.getClass());
    }
    
    @Override
    public PacketType getPacketType(final Object packet) {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be NULL.");
        }
        if (!MinecraftReflection.isPacketClass(packet)) {
            throw new IllegalArgumentException("The given object " + packet + " is not a packet.");
        }
        final PacketType type = PacketRegistry.getPacketType(packet.getClass());
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("Unable to find associated packet of " + packet + ": Lookup returned NULL.");
    }
    
    @Deprecated
    @Override
    public void registerPacketClass(final Class<?> clazz, final int packetID) {
        PacketRegistry.getPacketToID().put(clazz, packetID);
    }
    
    @Deprecated
    @Override
    public void unregisterPacketClass(final Class<?> clazz) {
        PacketRegistry.getPacketToID().remove(clazz);
    }
    
    @Deprecated
    @Override
    public Class<?> getPacketClassFromID(final int packetID, final boolean forceVanilla) {
        return (Class<?>)PacketRegistry.getPacketClassFromID(packetID, forceVanilla);
    }
    
    @Deprecated
    public static Set<Integer> getServerPackets() throws FieldAccessException {
        return PacketRegistry.getServerPackets();
    }
    
    @Deprecated
    public static Set<Integer> getClientPackets() throws FieldAccessException {
        return PacketRegistry.getClientPackets();
    }
    
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }
    
    @Override
    public boolean isClosed() {
        return this.hasClosed;
    }
    
    @Override
    public void close() {
        if (this.hasClosed) {
            return;
        }
        if (this.packetInjector != null) {
            this.packetInjector.cleanupAll();
        }
        if (this.spigotInjector != null) {
            this.spigotInjector.cleanupAll();
        }
        if (this.nettyInjector != null) {
            this.nettyInjector.close();
        }
        this.playerInjection.close();
        this.hasClosed = true;
        this.packetListeners.clear();
        this.recievedListeners = null;
        this.sendingListeners = null;
        this.interceptWritePacket.cleanup();
        this.asyncFilterManager.cleanupAll();
    }
    
    @Override
    protected void finalize() throws Throwable {
        this.close();
    }
    
    static {
        REPORT_CANNOT_LOAD_PACKET_LIST = new ReportType("Cannot load server and client packet list.");
        REPORT_CANNOT_INITIALIZE_PACKET_INJECTOR = new ReportType("Unable to initialize packet injector");
        REPORT_PLUGIN_DEPEND_MISSING = new ReportType("%s doesn't depend on ProtocolLib. Check that its plugin.yml has a 'depend' directive.");
        REPORT_UNSUPPORTED_SERVER_PACKET = new ReportType("[%s] Unsupported server packet in current Minecraft version: %s");
        REPORT_UNSUPPORTED_CLIENT_PACKET = new ReportType("[%s] Unsupported client packet in current Minecraft version: %s");
        REPORT_CANNOT_UNINJECT_PLAYER = new ReportType("Unable to uninject net handler for player.");
        REPORT_CANNOT_UNINJECT_OFFLINE_PLAYER = new ReportType("Unable to uninject logged off player.");
        REPORT_CANNOT_INJECT_PLAYER = new ReportType("Unable to inject player.");
        REPORT_CANNOT_UNREGISTER_PLUGIN = new ReportType("Unable to handle disabled plugin.");
        REPORT_PLUGIN_VERIFIER_ERROR = new ReportType("Verifier error: %s");
    }
}
