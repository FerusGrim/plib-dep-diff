package com.comphenix.protocol.injector.spigot;

import java.util.List;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.comphenix.protocol.reflect.FieldUtils;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.injector.packet.LegacyNetworkMarker;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.error.DelegatedErrorReporter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.injector.PlayerLoggedOutException;
import com.comphenix.protocol.concurrency.IntegerSet;
import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.injector.player.PlayerInjectionHandler;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import java.util.Iterator;
import org.bukkit.Bukkit;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import com.comphenix.net.sf.cglib.proxy.CallbackFilter;
import com.comphenix.protocol.utility.EnhancerFactory;
import com.comphenix.net.sf.cglib.proxy.NoOp;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;
import com.comphenix.net.sf.cglib.proxy.Callback;
import java.lang.reflect.Method;
import com.google.common.collect.Maps;
import java.util.Collections;
import com.google.common.collect.MapMaker;
import com.comphenix.protocol.injector.packet.PacketInjector;
import org.bukkit.Server;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.injector.ListenerInvoker;
import java.util.Map;
import org.bukkit.entity.Player;
import com.comphenix.protocol.injector.player.NetworkObjectInjector;
import java.util.concurrent.ConcurrentMap;
import com.comphenix.protocol.concurrency.PacketTypeSet;
import org.bukkit.plugin.Plugin;
import java.util.Set;
import java.lang.reflect.Field;
import com.comphenix.protocol.error.ReportType;

public class SpigotPacketInjector implements SpigotPacketListener
{
    public static final ReportType REPORT_CANNOT_CLEANUP_SPIGOT;
    private static volatile Class<?> spigotListenerClass;
    private static volatile boolean classChecked;
    private static volatile Field playerConnectionPlayer;
    private Set<Object> ignoredPackets;
    private static final int CLEANUP_DELAY = 100;
    private Object dynamicListener;
    private Plugin plugin;
    private PacketTypeSet queuedFilters;
    private PacketTypeSet reveivedFilters;
    private ConcurrentMap<Object, NetworkObjectInjector> networkManagerInjector;
    private ConcurrentMap<Player, NetworkObjectInjector> playerInjector;
    private Map<Object, byte[]> readBufferedPackets;
    private ListenerInvoker invoker;
    private ErrorReporter reporter;
    private Server server;
    private PacketInjector proxyPacketInjector;
    private static final int BACKGROUND_DELAY = 600;
    private int backgroundId;
    
    public SpigotPacketInjector(final ErrorReporter reporter, final ListenerInvoker invoker, final Server server) {
        this.ignoredPackets = Collections.newSetFromMap((Map<Object, Boolean>)new MapMaker().weakKeys().makeMap());
        this.networkManagerInjector = (ConcurrentMap<Object, NetworkObjectInjector>)Maps.newConcurrentMap();
        this.playerInjector = (ConcurrentMap<Player, NetworkObjectInjector>)Maps.newConcurrentMap();
        this.readBufferedPackets = (Map<Object, byte[]>)new MapMaker().weakKeys().makeMap();
        this.reporter = reporter;
        this.invoker = invoker;
        this.server = server;
        this.queuedFilters = new PacketTypeSet();
        this.reveivedFilters = new PacketTypeSet();
    }
    
    public ListenerInvoker getInvoker() {
        return this.invoker;
    }
    
    public void setProxyPacketInjector(final PacketInjector proxyPacketInjector) {
        this.proxyPacketInjector = proxyPacketInjector;
    }
    
    public PacketInjector getProxyPacketInjector() {
        return this.proxyPacketInjector;
    }
    
    private static Class<?> getSpigotListenerClass() {
        if (!SpigotPacketInjector.classChecked) {
            try {
                SpigotPacketInjector.spigotListenerClass = SpigotPacketInjector.class.getClassLoader().loadClass("org.spigotmc.netty.PacketListener");
            }
            catch (ClassNotFoundException e) {
                return null;
            }
            finally {
                SpigotPacketInjector.classChecked = true;
            }
        }
        return SpigotPacketInjector.spigotListenerClass;
    }
    
    private static Method getRegisterMethod() {
        final Class<?> clazz = getSpigotListenerClass();
        if (clazz != null) {
            try {
                return clazz.getMethod("register", clazz, Plugin.class);
            }
            catch (SecurityException e) {
                throw new RuntimeException("Reflection is not allowed.", e);
            }
            catch (NoSuchMethodException e2) {
                throw new IllegalStateException("Cannot find register() method in " + clazz, e2);
            }
        }
        throw new IllegalStateException("Spigot could not be found!");
    }
    
    public static boolean canUseSpigotListener() {
        return getSpigotListenerClass() != null;
    }
    
    public boolean register(final Plugin plugin) {
        if (this.hasRegistered()) {
            return false;
        }
        this.plugin = plugin;
        final Callback[] callbacks = new Callback[3];
        final boolean[] found = new boolean[3];
        callbacks[0] = new MethodInterceptor() {
            @Override
            public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
                return SpigotPacketInjector.this.packetReceived(args[0], args[1], args[2]);
            }
        };
        callbacks[1] = new MethodInterceptor() {
            @Override
            public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
                return SpigotPacketInjector.this.packetQueued(args[0], args[1], args[2]);
            }
        };
        callbacks[2] = NoOp.INSTANCE;
        final Enhancer enhancer = EnhancerFactory.getInstance().createEnhancer();
        enhancer.setSuperclass(getSpigotListenerClass());
        enhancer.setCallbacks(callbacks);
        enhancer.setCallbackFilter(new CallbackFilter() {
            @Override
            public int accept(final Method method) {
                if (SpigotPacketInjector.this.matchMethod("packetReceived", method)) {
                    found[0] = true;
                    return 0;
                }
                if (SpigotPacketInjector.this.matchMethod("packetQueued", method)) {
                    found[1] = true;
                    return 1;
                }
                found[2] = true;
                return 2;
            }
        });
        this.dynamicListener = enhancer.create();
        if (!found[0]) {
            throw new IllegalStateException("Unable to find a valid packet receiver in Spigot.");
        }
        if (!found[1]) {
            throw new IllegalStateException("Unable to find a valid packet queue in Spigot.");
        }
        try {
            getRegisterMethod().invoke(null, this.dynamicListener, plugin);
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot register Spigot packet listener.", e);
        }
        this.backgroundId = this.createBackgroundTask();
        return true;
    }
    
    private int createBackgroundTask() {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, (Runnable)new Runnable() {
            @Override
            public void run() {
                SpigotPacketInjector.this.cleanupInjectors();
            }
        }, 600L, 600L);
    }
    
    private void cleanupInjectors() {
        for (final NetworkObjectInjector injector : this.networkManagerInjector.values()) {
            try {
                if (injector.getSocket() == null || !injector.getSocket().isClosed()) {
                    continue;
                }
                this.cleanupInjector(injector);
            }
            catch (Exception e) {
                this.reporter.reportMinimal(this.plugin, "cleanupInjectors", e);
                this.cleanupInjector(injector);
            }
        }
    }
    
    private void cleanupInjector(final NetworkObjectInjector injector) {
        this.playerInjector.remove(injector.getPlayer());
        this.playerInjector.remove(injector.getUpdatedPlayer());
        this.networkManagerInjector.remove(injector.getNetworkManager());
    }
    
    private boolean matchMethod(final String methodName, final Method method) {
        return FuzzyMethodContract.newBuilder().nameExact(methodName).parameterCount(3).parameterSuperOf(MinecraftReflection.getNetHandlerClass(), 1).parameterSuperOf(MinecraftReflection.getPacketClass(), 2).returnTypeExact(MinecraftReflection.getPacketClass()).build().isMatch(MethodInfo.fromMethod(method), null);
    }
    
    public boolean hasRegistered() {
        return this.dynamicListener != null;
    }
    
    public PlayerInjectionHandler getPlayerHandler() {
        return new DummyPlayerHandler(this, this.queuedFilters);
    }
    
    public PacketInjector getPacketInjector() {
        return new DummyPacketInjector(this, this.reveivedFilters);
    }
    
    NetworkObjectInjector getInjector(final Player player, final boolean createNew) {
        NetworkObjectInjector injector = this.playerInjector.get(player);
        if (injector == null && createNew) {
            if (player instanceof Factory) {
                throw new IllegalArgumentException("Cannot inject tempoary player " + player);
            }
            try {
                final NetworkObjectInjector created = new NetworkObjectInjector(this.filterImpossibleWarnings(this.reporter), null, this.invoker, null);
                created.initializePlayer(player);
                if (created.getNetworkManager() == null) {
                    throw new PlayerLoggedOutException("Player " + player + " has logged out.");
                }
                injector = this.saveInjector(created.getNetworkManager(), created);
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot create dummy injector.", e);
            }
        }
        return injector;
    }
    
    NetworkObjectInjector getInjector(final Object networkManager, final Object connection) {
        NetworkObjectInjector dummyInjector = this.networkManagerInjector.get(networkManager);
        if (dummyInjector == null) {
            try {
                final NetworkObjectInjector created = new NetworkObjectInjector(this.filterImpossibleWarnings(this.reporter), null, this.invoker, null);
                if (MinecraftReflection.isLoginHandler(connection)) {
                    created.initialize(connection);
                    created.setPlayer(created.createTemporaryPlayer(this.server));
                }
                else {
                    if (!MinecraftReflection.isServerHandler(connection)) {
                        throw new IllegalArgumentException("Unregonized connection in NetworkManager.");
                    }
                    if (SpigotPacketInjector.playerConnectionPlayer == null) {
                        SpigotPacketInjector.playerConnectionPlayer = FuzzyReflection.fromObject(connection).getFieldByType("player", MinecraftReflection.getEntityPlayerClass());
                    }
                    final Object entityPlayer = SpigotPacketInjector.playerConnectionPlayer.get(connection);
                    created.initialize(MinecraftReflection.getBukkitEntity(entityPlayer));
                }
                dummyInjector = this.saveInjector(networkManager, created);
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot create dummy injector.", e);
            }
        }
        return dummyInjector;
    }
    
    private ErrorReporter filterImpossibleWarnings(final ErrorReporter reporter) {
        return new DelegatedErrorReporter(reporter) {
            @Override
            protected Report filterReport(final Object sender, final Report report, final boolean detailed) {
                if (report.getType() == NetworkObjectInjector.REPORT_DETECTED_CUSTOM_SERVER_HANDLER) {
                    return null;
                }
                return report;
            }
        };
    }
    
    private NetworkObjectInjector saveInjector(final Object networkManager, final NetworkObjectInjector created) {
        NetworkObjectInjector result = this.networkManagerInjector.putIfAbsent(networkManager, created);
        if (result == null) {
            result = created;
        }
        this.playerInjector.put(created.getPlayer(), created);
        return result;
    }
    
    public void saveBuffered(final Object handle, final byte[] buffered) {
        this.readBufferedPackets.put(handle, buffered);
    }
    
    @Override
    public Object packetReceived(final Object networkManager, final Object connection, final Object packet) {
        if (!this.reveivedFilters.contains(packet.getClass())) {
            return packet;
        }
        final Integer id = this.invoker.getPacketID(packet);
        if (this.ignoredPackets.remove(packet)) {
            return packet;
        }
        final Player sender = this.getInjector(networkManager, connection).getUpdatedPlayer();
        final PacketType type = PacketType.findLegacy(id, PacketType.Sender.CLIENT);
        final PacketContainer container = new PacketContainer(type, packet);
        final PacketEvent event = this.packetReceived(container, sender, this.readBufferedPackets.get(packet));
        if (!event.isCancelled()) {
            return event.getPacket().getHandle();
        }
        return null;
    }
    
    @Override
    public Object packetQueued(final Object networkManager, final Object connection, final Object packet) {
        if (!this.queuedFilters.contains(packet.getClass())) {
            return packet;
        }
        final Integer id = this.invoker.getPacketID(packet);
        if (this.ignoredPackets.remove(packet)) {
            return packet;
        }
        final Player reciever = this.getInjector(networkManager, connection).getUpdatedPlayer();
        final PacketType type = PacketType.findLegacy(id, PacketType.Sender.SERVER);
        final PacketContainer container = new PacketContainer(type, packet);
        final PacketEvent event = this.packetQueued(container, reciever);
        if (!event.isCancelled()) {
            return event.getPacket().getHandle();
        }
        return null;
    }
    
    PacketEvent packetQueued(final PacketContainer packet, final Player receiver) {
        final PacketEvent event = PacketEvent.fromServer(this, packet, receiver);
        this.invoker.invokePacketSending(event);
        return event;
    }
    
    PacketEvent packetReceived(final PacketContainer packet, final Player sender, final byte[] buffered) {
        final NetworkMarker marker = (buffered != null) ? new LegacyNetworkMarker(ConnectionSide.CLIENT_SIDE, buffered, packet.getType()) : null;
        final PacketEvent event = PacketEvent.fromClient(this, packet, marker, sender);
        this.invoker.invokePacketRecieving(event);
        return event;
    }
    
    void injectPlayer(final Player player) {
        try {
            final NetworkObjectInjector dummy = new NetworkObjectInjector(this.filterImpossibleWarnings(this.reporter), player, this.invoker, null);
            dummy.initializePlayer(player);
            final NetworkObjectInjector realInjector = this.networkManagerInjector.get(dummy.getNetworkManager());
            if (realInjector != null) {
                realInjector.setUpdatedPlayer(player);
                this.playerInjector.put(player, realInjector);
            }
            else {
                this.saveInjector(dummy.getNetworkManager(), dummy);
            }
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot inject " + player);
        }
    }
    
    void uninjectPlayer(final Player player) {
        final NetworkObjectInjector injector = this.getInjector(player, false);
        if (player != null && injector != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, (Runnable)new Runnable() {
                @Override
                public void run() {
                    SpigotPacketInjector.this.cleanupInjector(injector);
                }
            }, 100L);
        }
    }
    
    void sendServerPacket(final Player receiver, final PacketContainer packet, final NetworkMarker marker, final boolean filters) throws InvocationTargetException {
        final NetworkObjectInjector networkObject = this.getInjector(receiver, true);
        if (filters) {
            this.ignoredPackets.remove(packet.getHandle());
        }
        else {
            this.ignoredPackets.add(packet.getHandle());
        }
        networkObject.sendServerPacket(packet.getHandle(), marker, filters);
    }
    
    void processPacket(final Player player, final Object mcPacket) throws IllegalAccessException, InvocationTargetException {
        final NetworkObjectInjector networkObject = this.getInjector(player, true);
        this.ignoredPackets.add(mcPacket);
        networkObject.processPacket(mcPacket);
    }
    
    private void cleanupListener() {
        final Class<?> listenerClass = getSpigotListenerClass();
        synchronized (listenerClass) {
            try {
                final Field listenersField = FieldUtils.getField(listenerClass, "listeners", true);
                final Field bakedField = FieldUtils.getField(listenerClass, "baked", true);
                final Map<Object, Plugin> listenerMap = (Map<Object, Plugin>)listenersField.get(null);
                final List<Object> listenerArray = (List<Object>)Lists.newArrayList((Object[])bakedField.get(null));
                listenerMap.remove(this.dynamicListener);
                listenerArray.remove(this.dynamicListener);
                bakedField.set(null, Iterables.toArray((Iterable)listenerArray, (Class)listenerClass));
                this.dynamicListener = null;
            }
            catch (Exception e) {
                this.reporter.reportWarning(this, Report.newBuilder(SpigotPacketInjector.REPORT_CANNOT_CLEANUP_SPIGOT).callerParam(this.dynamicListener).error(e));
            }
        }
    }
    
    public void cleanupAll() {
        if (this.dynamicListener != null) {
            this.cleanupListener();
        }
        if (this.backgroundId >= 0) {
            Bukkit.getScheduler().cancelTask(this.backgroundId);
            this.backgroundId = -1;
        }
        if (this.proxyPacketInjector != null) {
            this.proxyPacketInjector.cleanupAll();
        }
    }
    
    static {
        REPORT_CANNOT_CLEANUP_SPIGOT = new ReportType("Cannot cleanup Spigot listener.");
    }
}
