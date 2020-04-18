package com.comphenix.protocol.injector.player;

import io.netty.channel.Channel;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.Iterator;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketContainer;
import java.net.InetSocketAddress;
import com.comphenix.protocol.injector.server.BukkitSocketInjector;
import com.google.common.base.Objects;
import java.net.Socket;
import java.net.SocketAddress;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.injector.PlayerLoggedOutException;
import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.injector.server.SocketInjector;
import java.io.InputStream;
import java.io.DataInputStream;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.utility.MinecraftProtocolVersion;
import com.comphenix.protocol.injector.server.InputStreamLookupBuilder;
import com.google.common.collect.Maps;
import com.comphenix.protocol.concurrency.BlockingHashMap;
import java.util.concurrent.TimeUnit;
import com.comphenix.protocol.utility.SafeCacheBuilder;
import org.bukkit.Server;
import com.comphenix.protocol.injector.GamePhase;
import com.google.common.base.Predicate;
import com.comphenix.protocol.events.PacketListener;
import java.util.Set;
import com.comphenix.protocol.concurrency.IntegerSet;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.injector.ListenerInvoker;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.injector.PlayerInjectHooks;
import java.util.Map;
import org.bukkit.entity.Player;
import java.util.concurrent.ConcurrentMap;
import java.lang.ref.WeakReference;
import com.comphenix.protocol.injector.server.AbstractInputStreamLookup;
import com.comphenix.protocol.error.ReportType;

class ProxyPlayerInjectionHandler implements PlayerInjectionHandler
{
    public static final ReportType REPORT_UNSUPPPORTED_LISTENER;
    public static final ReportType REPORT_PLAYER_HOOK_FAILED;
    public static final ReportType REPORT_SWITCHED_PLAYER_HOOK;
    public static final ReportType REPORT_HOOK_CLEANUP_FAILED;
    public static final ReportType REPORT_CANNOT_REVERT_HOOK;
    private InjectedServerConnection serverInjection;
    private AbstractInputStreamLookup inputStreamLookup;
    private NetLoginInjector netLoginInjector;
    private WeakReference<PlayerInjector> lastSuccessfulHook;
    private ConcurrentMap<Player, PlayerInjector> dummyInjectors;
    private Map<Player, PlayerInjector> playerInjection;
    private volatile PlayerInjectHooks loginPlayerHook;
    private volatile PlayerInjectHooks playingPlayerHook;
    private ErrorReporter reporter;
    private boolean hasClosed;
    private ListenerInvoker invoker;
    private MinecraftVersion version;
    private IntegerSet sendingFilters;
    private Set<PacketListener> packetListeners;
    private Predicate<GamePhase> injectionFilter;
    
    public ProxyPlayerInjectionHandler(final ErrorReporter reporter, final Predicate<GamePhase> injectionFilter, final ListenerInvoker invoker, final Set<PacketListener> packetListeners, final Server server, final MinecraftVersion version) {
        this.dummyInjectors = SafeCacheBuilder.newBuilder().expireAfterWrite(30L, TimeUnit.SECONDS).build(BlockingHashMap.newInvalidCacheLoader());
        this.playerInjection = (Map<Player, PlayerInjector>)Maps.newConcurrentMap();
        this.loginPlayerHook = PlayerInjectHooks.NETWORK_SERVER_OBJECT;
        this.playingPlayerHook = PlayerInjectHooks.NETWORK_SERVER_OBJECT;
        this.sendingFilters = new IntegerSet(256);
        this.reporter = reporter;
        this.invoker = invoker;
        this.injectionFilter = injectionFilter;
        this.packetListeners = packetListeners;
        this.version = version;
        this.inputStreamLookup = InputStreamLookupBuilder.newBuilder().server(server).reporter(reporter).build();
        this.netLoginInjector = new NetLoginInjector(reporter, server, this);
        (this.serverInjection = new InjectedServerConnection(reporter, this.inputStreamLookup, server, this.netLoginInjector)).injectList();
    }
    
    @Override
    public int getProtocolVersion(final Player player) {
        return MinecraftProtocolVersion.getCurrentVersion();
    }
    
    @Override
    public PlayerInjectHooks getPlayerHook() {
        return this.getPlayerHook(GamePhase.PLAYING);
    }
    
    @Override
    public PlayerInjectHooks getPlayerHook(final GamePhase phase) {
        switch (phase) {
            case LOGIN: {
                return this.loginPlayerHook;
            }
            case PLAYING: {
                return this.playingPlayerHook;
            }
            default: {
                throw new IllegalArgumentException("Cannot retrieve injection hook for both phases at the same time.");
            }
        }
    }
    
    @Override
    public boolean hasMainThreadListener(final PacketType type) {
        return this.sendingFilters.contains(type.getLegacyId());
    }
    
    @Override
    public void setPlayerHook(final PlayerInjectHooks playerHook) {
        this.setPlayerHook(GamePhase.PLAYING, playerHook);
    }
    
    @Override
    public void setPlayerHook(final GamePhase phase, final PlayerInjectHooks playerHook) {
        if (phase.hasLogin()) {
            this.loginPlayerHook = playerHook;
        }
        if (phase.hasPlaying()) {
            this.playingPlayerHook = playerHook;
        }
        this.checkListener(this.packetListeners);
    }
    
    @Override
    public void addPacketHandler(final PacketType type, final Set<ListenerOptions> options) {
        this.sendingFilters.add(type.getLegacyId());
    }
    
    @Override
    public void removePacketHandler(final PacketType type) {
        this.sendingFilters.remove(type.getLegacyId());
    }
    
    private PlayerInjector getHookInstance(final Player player, final PlayerInjectHooks hook) throws IllegalAccessException {
        switch (hook) {
            case NETWORK_HANDLER_FIELDS: {
                return new NetworkFieldInjector(this.reporter, player, this.invoker, this.sendingFilters);
            }
            case NETWORK_MANAGER_OBJECT: {
                return new NetworkObjectInjector(this.reporter, player, this.invoker, this.sendingFilters);
            }
            case NETWORK_SERVER_OBJECT: {
                return new NetworkServerInjector(this.reporter, player, this.invoker, this.sendingFilters, this.serverInjection);
            }
            default: {
                throw new IllegalArgumentException("Cannot construct a player injector.");
            }
        }
    }
    
    @Override
    public Player getPlayerByConnection(final DataInputStream inputStream) {
        final SocketInjector injector = this.inputStreamLookup.waitSocketInjector(inputStream);
        if (injector != null) {
            return injector.getPlayer();
        }
        return null;
    }
    
    private PlayerInjectHooks getInjectorType(final PlayerInjector injector) {
        return (injector != null) ? injector.getHookType() : PlayerInjectHooks.NONE;
    }
    
    @Override
    public void injectPlayer(final Player player, final ConflictStrategy strategy) {
        if (this.isInjectionNecessary(GamePhase.PLAYING)) {
            this.injectPlayer(player, player, strategy, GamePhase.PLAYING);
        }
    }
    
    public boolean isInjectionNecessary(final GamePhase phase) {
        return this.injectionFilter.apply((Object)phase);
    }
    
    PlayerInjector injectPlayer(final Player player, final Object injectionPoint, final ConflictStrategy stategy, final GamePhase phase) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be NULL.");
        }
        if (injectionPoint == null) {
            throw new IllegalArgumentException("injectionPoint cannot be NULL.");
        }
        if (phase == null) {
            throw new IllegalArgumentException("phase cannot be NULL.");
        }
        synchronized (player) {
            return this.injectPlayerInternal(player, injectionPoint, stategy, phase);
        }
    }
    
    private PlayerInjector injectPlayerInternal(final Player player, final Object injectionPoint, final ConflictStrategy stategy, final GamePhase phase) {
        PlayerInjector injector = this.playerInjection.get(player);
        PlayerInjectHooks permanentHook;
        PlayerInjectHooks tempHook = permanentHook = this.getPlayerHook(phase);
        final boolean invalidInjector = injector == null || !injector.canInject(phase);
        if (!this.hasClosed && (tempHook != this.getInjectorType(injector) || invalidInjector)) {
            while (tempHook != PlayerInjectHooks.NONE) {
                boolean hookFailed = false;
                this.cleanupHook(injector);
                try {
                    injector = this.getHookInstance(player, tempHook);
                    if (injector.canInject(phase)) {
                        injector.initialize(injectionPoint);
                        final SocketAddress address = injector.getAddress();
                        if (address == null) {
                            return null;
                        }
                        final SocketInjector previous = this.inputStreamLookup.peekSocketInjector(address);
                        final Socket socket = injector.getSocket();
                        if (previous != null && !(player instanceof Factory)) {
                            switch (stategy) {
                                case OVERRIDE: {
                                    this.uninjectPlayer(previous.getPlayer(), true);
                                    break;
                                }
                                case BAIL_OUT: {
                                    return null;
                                }
                            }
                        }
                        injector.injectManager();
                        this.saveAddressLookup(address, socket, injector);
                        break;
                    }
                }
                catch (PlayerLoggedOutException e) {
                    throw e;
                }
                catch (Exception e2) {
                    this.reporter.reportDetailed(this, Report.newBuilder(ProxyPlayerInjectionHandler.REPORT_PLAYER_HOOK_FAILED).messageParam(tempHook).callerParam(player, injectionPoint, phase).error(e2));
                    hookFailed = true;
                }
                tempHook = PlayerInjectHooks.values()[tempHook.ordinal() - 1];
                if (hookFailed) {
                    this.reporter.reportWarning(this, Report.newBuilder(ProxyPlayerInjectionHandler.REPORT_SWITCHED_PLAYER_HOOK).messageParam(tempHook));
                }
                if (tempHook == PlayerInjectHooks.NONE) {
                    this.cleanupHook(injector);
                    injector = null;
                    hookFailed = true;
                }
                if (hookFailed) {
                    permanentHook = tempHook;
                }
            }
            if (injector != null) {
                this.lastSuccessfulHook = new WeakReference<PlayerInjector>(injector);
            }
            if (permanentHook != this.getPlayerHook(phase)) {
                this.setPlayerHook(phase, tempHook);
            }
            if (injector != null) {
                this.playerInjection.put(player, injector);
            }
        }
        return injector;
    }
    
    private void saveAddressLookup(final SocketAddress address, final Socket socket, final SocketInjector injector) {
        final SocketAddress socketAddress = (socket != null) ? socket.getRemoteSocketAddress() : null;
        if (socketAddress != null && !Objects.equal((Object)socketAddress, (Object)address)) {
            this.inputStreamLookup.setSocketInjector(socketAddress, injector);
        }
        this.inputStreamLookup.setSocketInjector(address, injector);
    }
    
    private void cleanupHook(final PlayerInjector injector) {
        try {
            if (injector != null) {
                injector.cleanupAll();
            }
        }
        catch (Exception ex) {
            this.reporter.reportDetailed(this, Report.newBuilder(ProxyPlayerInjectionHandler.REPORT_HOOK_CLEANUP_FAILED).callerParam(injector).error(ex));
        }
    }
    
    @Override
    public void handleDisconnect(final Player player) {
        final PlayerInjector injector = this.getInjector(player);
        if (injector != null) {
            injector.handleDisconnect();
        }
    }
    
    @Override
    public void updatePlayer(final Player player) {
        final SocketAddress address = player.getAddress();
        if (address != null) {
            final SocketInjector injector = this.inputStreamLookup.peekSocketInjector(address);
            if (injector != null) {
                injector.setUpdatedPlayer(player);
            }
            else {
                this.inputStreamLookup.setSocketInjector(player.getAddress(), new BukkitSocketInjector(player));
            }
        }
    }
    
    @Override
    public boolean uninjectPlayer(final Player player) {
        return this.uninjectPlayer(player, false);
    }
    
    private boolean uninjectPlayer(final Player player, final boolean prepareNextHook) {
        if (!this.hasClosed && player != null) {
            final PlayerInjector injector = this.playerInjection.remove(player);
            if (injector != null) {
                injector.cleanupAll();
                if (prepareNextHook && injector instanceof NetworkObjectInjector) {
                    try {
                        final PlayerInjector dummyInjector = this.getHookInstance(player, PlayerInjectHooks.NETWORK_SERVER_OBJECT);
                        dummyInjector.initializePlayer(player);
                        dummyInjector.setNetworkManager(injector.getNetworkManager(), true);
                    }
                    catch (IllegalAccessException e) {
                        this.reporter.reportWarning(this, Report.newBuilder(ProxyPlayerInjectionHandler.REPORT_CANNOT_REVERT_HOOK).error(e));
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean uninjectPlayer(final InetSocketAddress address) {
        if (!this.hasClosed && address != null) {
            final SocketInjector injector = this.inputStreamLookup.peekSocketInjector(address);
            if (injector != null) {
                this.uninjectPlayer(injector.getPlayer(), true);
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void sendServerPacket(final Player receiver, final PacketContainer packet, final NetworkMarker marker, final boolean filters) throws InvocationTargetException {
        final SocketInjector injector = this.getInjector(receiver);
        if (injector != null) {
            injector.sendServerPacket(packet.getHandle(), marker, filters);
            return;
        }
        throw new PlayerLoggedOutException(String.format("Unable to send packet %s (%s): Player %s has logged out.", packet.getType(), packet, receiver));
    }
    
    @Override
    public void recieveClientPacket(final Player player, final Object mcPacket) throws IllegalAccessException, InvocationTargetException {
        final PlayerInjector injector = this.getInjector(player);
        if (injector != null) {
            injector.processPacket(mcPacket);
            return;
        }
        throw new PlayerLoggedOutException(String.format("Unable to receieve packet %s. Player %s has logged out.", mcPacket, player));
    }
    
    private PlayerInjector getInjector(final Player player) {
        final PlayerInjector injector = this.playerInjection.get(player);
        if (injector != null) {
            return injector;
        }
        final SocketAddress address = player.getAddress();
        if (address == null) {
            return null;
        }
        final SocketInjector result = this.inputStreamLookup.peekSocketInjector(address);
        if (result instanceof PlayerInjector) {
            return (PlayerInjector)result;
        }
        return this.createDummyInjector(player);
    }
    
    private PlayerInjector createDummyInjector(final Player player) {
        if (!MinecraftReflection.getCraftPlayerClass().isAssignableFrom(player.getClass())) {
            return null;
        }
        try {
            final PlayerInjector dummyInjector = this.getHookInstance(player, PlayerInjectHooks.NETWORK_SERVER_OBJECT);
            dummyInjector.initializePlayer(player);
            if (dummyInjector.getSocket() == null) {
                return null;
            }
            this.inputStreamLookup.setSocketInjector(dummyInjector.getAddress(), dummyInjector);
            this.dummyInjectors.put(player, dummyInjector);
            return dummyInjector;
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access fields.", e);
        }
    }
    
    PlayerInjector getInjectorByNetworkHandler(final Object networkManager) {
        if (networkManager == null) {
            return null;
        }
        for (final PlayerInjector injector : this.playerInjection.values()) {
            if (injector.getNetworkManager() == networkManager) {
                return injector;
            }
        }
        return null;
    }
    
    @Override
    public boolean canRecievePackets() {
        return false;
    }
    
    @Override
    public PacketEvent handlePacketRecieved(final PacketContainer packet, final InputStream input, final byte[] buffered) {
        throw new UnsupportedOperationException("Proxy injection cannot handle received packets.");
    }
    
    @Override
    public void checkListener(final Set<PacketListener> listeners) {
        if (this.getLastSuccessfulHook() != null) {
            for (final PacketListener listener : listeners) {
                this.checkListener(listener);
            }
        }
    }
    
    private PlayerInjector getLastSuccessfulHook() {
        return (this.lastSuccessfulHook != null) ? this.lastSuccessfulHook.get() : null;
    }
    
    @Override
    public void checkListener(final PacketListener listener) {
        final PlayerInjector last = this.getLastSuccessfulHook();
        if (last != null) {
            final UnsupportedListener result = last.checkListener(this.version, listener);
            if (result != null) {
                this.reporter.reportWarning(this, Report.newBuilder(ProxyPlayerInjectionHandler.REPORT_UNSUPPPORTED_LISTENER).messageParam(PacketAdapter.getPluginName(listener), result));
                for (final int packetID : result.getPackets()) {
                    this.removePacketHandler(PacketType.findLegacy(packetID, PacketType.Sender.CLIENT));
                    this.removePacketHandler(PacketType.findLegacy(packetID, PacketType.Sender.SERVER));
                }
            }
        }
    }
    
    @Override
    public Set<PacketType> getSendingFilters() {
        return PacketRegistry.toPacketTypes(this.sendingFilters.toSet(), PacketType.Sender.SERVER);
    }
    
    @Override
    public void close() {
        if (this.hasClosed || this.playerInjection == null) {
            return;
        }
        for (final PlayerInjector injection : this.playerInjection.values()) {
            if (injection != null) {
                injection.cleanupAll();
            }
        }
        if (this.inputStreamLookup != null) {
            this.inputStreamLookup.cleanupAll();
        }
        if (this.serverInjection != null) {
            this.serverInjection.cleanupAll();
        }
        if (this.netLoginInjector != null) {
            this.netLoginInjector.cleanupAll();
        }
        this.inputStreamLookup = null;
        this.serverInjection = null;
        this.netLoginInjector = null;
        this.hasClosed = true;
        this.playerInjection.clear();
        this.invoker = null;
    }
    
    @Override
    public Channel getChannel(final Player player) {
        throw new UnsupportedOperationException();
    }
    
    static {
        REPORT_UNSUPPPORTED_LISTENER = new ReportType("Cannot fully register listener for %s: %s");
        REPORT_PLAYER_HOOK_FAILED = new ReportType("Player hook %s failed.");
        REPORT_SWITCHED_PLAYER_HOOK = new ReportType("Switching to %s instead.");
        REPORT_HOOK_CLEANUP_FAILED = new ReportType("Cleaing up after player hook failed.");
        REPORT_CANNOT_REVERT_HOOK = new ReportType("Unable to fully revert old injector. May cause conflicts.");
    }
}
