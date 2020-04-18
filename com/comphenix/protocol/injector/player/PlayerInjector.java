package com.comphenix.protocol.injector.player;

import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.injector.PlayerInjectHooks;
import com.comphenix.protocol.injector.GamePhase;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.google.common.collect.MapMaker;
import com.comphenix.protocol.injector.packet.InterceptWritePacket;
import com.comphenix.protocol.events.NetworkMarker;
import java.util.Map;
import com.comphenix.protocol.error.ErrorReporter;
import java.io.DataInputStream;
import com.comphenix.protocol.injector.ListenerInvoker;
import java.net.SocketAddress;
import java.net.Socket;
import com.comphenix.protocol.reflect.VolatileField;
import org.bukkit.entity.Player;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import com.comphenix.protocol.error.ReportType;
import com.comphenix.protocol.injector.server.SocketInjector;

public abstract class PlayerInjector implements SocketInjector
{
    public static final ReportType REPORT_ASSUME_DISCONNECT_METHOD;
    public static final ReportType REPORT_INVALID_ARGUMENT_DISCONNECT;
    public static final ReportType REPORT_CANNOT_ACCESS_DISCONNECT;
    public static final ReportType REPORT_CANNOT_CLOSE_SOCKET;
    public static final ReportType REPORT_ACCESS_DENIED_CLOSE_SOCKET;
    public static final ReportType REPORT_DETECTED_CUSTOM_SERVER_HANDLER;
    public static final ReportType REPORT_CANNOT_PROXY_SERVER_HANDLER;
    public static final ReportType REPORT_CANNOT_UPDATE_PLAYER;
    public static final ReportType REPORT_CANNOT_HANDLE_PACKET;
    public static final ReportType REPORT_INVALID_NETWORK_MANAGER;
    private static Field netLoginNetworkField;
    private static Method loginDisconnect;
    private static Method serverDisconnect;
    protected static Field serverHandlerField;
    protected static Field proxyServerField;
    protected static Field networkManagerField;
    protected static Field netHandlerField;
    protected static Field socketField;
    protected static Field socketAddressField;
    private static Field inputField;
    private static Field entityPlayerField;
    private static boolean hasProxyType;
    protected static StructureModifier<Object> networkModifier;
    protected static Method queueMethod;
    protected static Method processMethod;
    protected volatile Player player;
    protected boolean hasInitialized;
    protected VolatileField networkManagerRef;
    protected VolatileField serverHandlerRef;
    protected Object networkManager;
    protected Object loginHandler;
    protected Object serverHandler;
    protected Object netHandler;
    protected Socket socket;
    protected SocketAddress socketAddress;
    protected ListenerInvoker invoker;
    protected DataInputStream cachedInput;
    protected ErrorReporter reporter;
    protected Map<Object, NetworkMarker> queuedMarkers;
    protected InterceptWritePacket writePacketInterceptor;
    private boolean clean;
    boolean updateOnLogin;
    volatile Player updatedPlayer;
    
    public PlayerInjector(final ErrorReporter reporter, final Player player, final ListenerInvoker invoker) {
        this.queuedMarkers = (Map<Object, NetworkMarker>)new MapMaker().weakKeys().makeMap();
        this.reporter = reporter;
        this.player = player;
        this.invoker = invoker;
        this.writePacketInterceptor = invoker.getInterceptWritePacket();
    }
    
    protected Object getEntityPlayer(final Player player) {
        final BukkitUnwrapper unwrapper = new BukkitUnwrapper();
        return unwrapper.unwrapItem(player);
    }
    
    public void initialize(final Object injectionSource) throws IllegalAccessException {
        if (injectionSource == null) {
            throw new IllegalArgumentException("injectionSource cannot be NULL");
        }
        if (injectionSource instanceof Player) {
            this.initializePlayer((Player)injectionSource);
        }
        else {
            if (!MinecraftReflection.isLoginHandler(injectionSource)) {
                throw new IllegalArgumentException("Cannot initialize a player hook using a " + injectionSource.getClass().getName());
            }
            this.initializeLogin(injectionSource);
        }
    }
    
    public void initializePlayer(final Player player) {
        final Object notchEntity = this.getEntityPlayer(player);
        this.player = player;
        if (!this.hasInitialized) {
            this.hasInitialized = true;
            if (PlayerInjector.serverHandlerField == null) {
                PlayerInjector.serverHandlerField = FuzzyReflection.fromObject(notchEntity).getFieldByType("NetServerHandler", MinecraftReflection.getPlayerConnectionClass());
                PlayerInjector.proxyServerField = this.getProxyField(notchEntity, PlayerInjector.serverHandlerField);
            }
            this.serverHandlerRef = new VolatileField(PlayerInjector.serverHandlerField, notchEntity);
            this.serverHandler = this.serverHandlerRef.getValue();
            if (PlayerInjector.networkManagerField == null) {
                PlayerInjector.networkManagerField = FuzzyReflection.fromObject(this.serverHandler).getFieldByType("networkManager", MinecraftReflection.getNetworkManagerClass());
            }
            this.initializeNetworkManager(PlayerInjector.networkManagerField, this.serverHandler);
        }
    }
    
    public void initializeLogin(final Object netLoginHandler) {
        if (!this.hasInitialized) {
            if (!MinecraftReflection.isLoginHandler(netLoginHandler)) {
                throw new IllegalArgumentException("netLoginHandler (" + netLoginHandler + ") is not a " + MinecraftReflection.getNetLoginHandlerName());
            }
            this.hasInitialized = true;
            this.loginHandler = netLoginHandler;
            if (PlayerInjector.netLoginNetworkField == null) {
                PlayerInjector.netLoginNetworkField = FuzzyReflection.fromObject(netLoginHandler).getFieldByType("networkManager", MinecraftReflection.getNetworkManagerClass());
            }
            this.initializeNetworkManager(PlayerInjector.netLoginNetworkField, netLoginHandler);
        }
    }
    
    private void initializeNetworkManager(final Field reference, final Object container) {
        this.networkManagerRef = new VolatileField(reference, container);
        this.networkManager = this.networkManagerRef.getValue();
        if (this.networkManager instanceof Factory) {
            return;
        }
        if (this.networkManager != null && PlayerInjector.networkModifier == null) {
            PlayerInjector.networkModifier = new StructureModifier<Object>(this.networkManager.getClass(), null, false);
        }
        if (PlayerInjector.queueMethod == null) {
            PlayerInjector.queueMethod = FuzzyReflection.fromClass(reference.getType()).getMethodByParameters("queue", MinecraftReflection.getPacketClass());
        }
    }
    
    protected boolean hasProxyServerHandler() {
        return PlayerInjector.hasProxyType;
    }
    
    public Object getNetworkManager() {
        return this.networkManagerRef.getValue();
    }
    
    public Object getServerHandler() {
        return this.serverHandlerRef.getValue();
    }
    
    public void setNetworkManager(final Object value, final boolean force) {
        this.networkManagerRef.setValue(value);
        if (force) {
            this.networkManagerRef.saveValue();
        }
        this.initializeNetworkManager(PlayerInjector.networkManagerField, this.serverHandler);
    }
    
    @Override
    public Socket getSocket() throws IllegalAccessException {
        try {
            if (PlayerInjector.socketField == null) {
                PlayerInjector.socketField = FuzzyReflection.fromObject(this.networkManager, true).getFieldListByType(Socket.class).get(0);
            }
            if (this.socket == null) {
                this.socket = (Socket)FieldUtils.readField(PlayerInjector.socketField, this.networkManager, true);
            }
            return this.socket;
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalAccessException("Unable to read the socket field.");
        }
    }
    
    @Override
    public SocketAddress getAddress() throws IllegalAccessException {
        try {
            if (PlayerInjector.socketAddressField == null) {
                PlayerInjector.socketAddressField = FuzzyReflection.fromObject(this.networkManager, true).getFieldListByType(SocketAddress.class).get(0);
            }
            if (this.socketAddress == null) {
                this.socketAddress = (SocketAddress)FieldUtils.readField(PlayerInjector.socketAddressField, this.networkManager, true);
            }
            return this.socketAddress;
        }
        catch (IndexOutOfBoundsException e) {
            this.reporter.reportWarning(this, Report.newBuilder(PlayerInjector.REPORT_INVALID_NETWORK_MANAGER).callerParam(this.networkManager).build());
            throw new IllegalAccessException("Unable to read the socket address field.");
        }
    }
    
    @Override
    public void disconnect(final String message) throws InvocationTargetException {
        final boolean usingNetServer = this.serverHandler != null;
        final Object handler = usingNetServer ? this.serverHandler : this.loginHandler;
        Method disconnect = usingNetServer ? PlayerInjector.serverDisconnect : PlayerInjector.loginDisconnect;
        if (handler != null) {
            if (disconnect == null) {
                try {
                    disconnect = FuzzyReflection.fromObject(handler).getMethodByName("disconnect.*");
                }
                catch (IllegalArgumentException e) {
                    disconnect = FuzzyReflection.fromObject(handler).getMethodByParameters("disconnect", String.class);
                    this.reporter.reportWarning(this, Report.newBuilder(PlayerInjector.REPORT_ASSUME_DISCONNECT_METHOD).messageParam(disconnect));
                }
                if (usingNetServer) {
                    PlayerInjector.serverDisconnect = disconnect;
                }
                else {
                    PlayerInjector.loginDisconnect = disconnect;
                }
            }
            try {
                disconnect.invoke(handler, message);
                return;
            }
            catch (IllegalArgumentException e) {
                this.reporter.reportDetailed(this, Report.newBuilder(PlayerInjector.REPORT_INVALID_ARGUMENT_DISCONNECT).error(e).messageParam(message).callerParam(handler));
            }
            catch (IllegalAccessException e2) {
                this.reporter.reportWarning(this, Report.newBuilder(PlayerInjector.REPORT_CANNOT_ACCESS_DISCONNECT).error(e2));
            }
        }
        try {
            final Socket socket = this.getSocket();
            try {
                socket.close();
            }
            catch (IOException e3) {
                this.reporter.reportDetailed(this, Report.newBuilder(PlayerInjector.REPORT_CANNOT_CLOSE_SOCKET).error(e3).callerParam(socket));
            }
        }
        catch (IllegalAccessException e2) {
            this.reporter.reportWarning(this, Report.newBuilder(PlayerInjector.REPORT_ACCESS_DENIED_CLOSE_SOCKET).error(e2));
        }
    }
    
    private Field getProxyField(final Object notchEntity, final Field serverField) {
        try {
            final Object currentHandler = FieldUtils.readField(PlayerInjector.serverHandlerField, notchEntity, true);
            if (currentHandler == null) {
                throw new ServerHandlerNull();
            }
            if (!this.isStandardMinecraftNetHandler(currentHandler)) {
                if (currentHandler instanceof Factory) {
                    return null;
                }
                PlayerInjector.hasProxyType = true;
                this.reporter.reportWarning(this, Report.newBuilder(PlayerInjector.REPORT_DETECTED_CUSTOM_SERVER_HANDLER).callerParam(serverField));
                try {
                    final FuzzyReflection reflection = FuzzyReflection.fromObject(currentHandler, true);
                    return reflection.getFieldByType("NetServerHandler", MinecraftReflection.getPlayerConnectionClass());
                }
                catch (RuntimeException ex) {}
            }
        }
        catch (IllegalAccessException e) {
            this.reporter.reportWarning(this, Report.newBuilder(PlayerInjector.REPORT_CANNOT_PROXY_SERVER_HANDLER).error(e).callerParam(notchEntity, serverField));
        }
        return null;
    }
    
    private boolean isStandardMinecraftNetHandler(final Object obj) {
        if (obj == null) {
            return false;
        }
        final Class<?> clazz = obj.getClass();
        return MinecraftReflection.getNetLoginHandlerClass().equals(clazz) || MinecraftReflection.getPlayerConnectionClass().equals(clazz);
    }
    
    protected Object getNetHandler() throws IllegalAccessException {
        return this.getNetHandler(false);
    }
    
    protected Object getNetHandler(final boolean refresh) throws IllegalAccessException {
        try {
            if (PlayerInjector.netHandlerField == null) {
                PlayerInjector.netHandlerField = FuzzyReflection.fromClass(this.networkManager.getClass(), true).getFieldByType("NetHandler", MinecraftReflection.getNetHandlerClass());
            }
        }
        catch (RuntimeException ex) {}
        if (PlayerInjector.netHandlerField == null) {
            try {
                PlayerInjector.netHandlerField = FuzzyReflection.fromClass(this.networkManager.getClass(), true).getFieldByType(MinecraftReflection.getMinecraftObjectRegex());
            }
            catch (RuntimeException e2) {
                throw new IllegalAccessException("Cannot locate net handler. " + e2.getMessage());
            }
        }
        if (this.netHandler == null || refresh) {
            this.netHandler = FieldUtils.readField(PlayerInjector.netHandlerField, this.networkManager, true);
        }
        return this.netHandler;
    }
    
    private Object getEntityPlayer(final Object netHandler) throws IllegalAccessException {
        if (PlayerInjector.entityPlayerField == null) {
            PlayerInjector.entityPlayerField = FuzzyReflection.fromObject(netHandler).getFieldByType("EntityPlayer", MinecraftReflection.getEntityPlayerClass());
        }
        return FieldUtils.readField(PlayerInjector.entityPlayerField, netHandler);
    }
    
    public void processPacket(final Object packet) throws IllegalAccessException, InvocationTargetException {
        final Object netHandler = this.getNetHandler();
        if (PlayerInjector.processMethod == null) {
            try {
                PlayerInjector.processMethod = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethodByParameters("processPacket", PlayerInjector.netHandlerField.getType());
            }
            catch (RuntimeException e) {
                throw new IllegalArgumentException("Cannot locate process packet method: " + e.getMessage());
            }
        }
        try {
            PlayerInjector.processMethod.invoke(packet, netHandler);
        }
        catch (IllegalArgumentException e3) {
            throw new IllegalArgumentException("Method " + PlayerInjector.processMethod.getName() + " is not compatible.");
        }
        catch (InvocationTargetException e2) {
            throw e2;
        }
    }
    
    @Override
    public abstract void sendServerPacket(final Object p0, final NetworkMarker p1, final boolean p2) throws InvocationTargetException;
    
    public abstract void injectManager();
    
    public final void cleanupAll() {
        if (!this.clean) {
            this.cleanHook();
            this.writePacketInterceptor.cleanup();
        }
        this.clean = true;
    }
    
    public abstract void handleDisconnect();
    
    protected abstract void cleanHook();
    
    public boolean isClean() {
        return this.clean;
    }
    
    public abstract boolean canInject(final GamePhase p0);
    
    public abstract PlayerInjectHooks getHookType();
    
    public abstract UnsupportedListener checkListener(final MinecraftVersion p0, final PacketListener p1);
    
    public Object handlePacketSending(final Object packet) {
        try {
            final Integer id = this.invoker.getPacketID(packet);
            Player currentPlayer = this.player;
            if (this.updateOnLogin) {
                if (this.updatedPlayer == null) {
                    try {
                        final Object handler = this.getNetHandler(true);
                        if (MinecraftReflection.getPlayerConnectionClass().isAssignableFrom(handler.getClass())) {
                            this.setUpdatedPlayer((Player)MinecraftReflection.getBukkitEntity(this.getEntityPlayer(handler)));
                        }
                    }
                    catch (IllegalAccessException e) {
                        this.reporter.reportDetailed(this, Report.newBuilder(PlayerInjector.REPORT_CANNOT_UPDATE_PLAYER).error(e).callerParam(packet));
                    }
                }
                if (this.updatedPlayer != null) {
                    currentPlayer = this.updatedPlayer;
                    this.updateOnLogin = false;
                }
            }
            if (id != null && this.hasListener(id)) {
                NetworkMarker marker = this.queuedMarkers.remove(packet);
                final PacketType type = PacketType.findLegacy(id, PacketType.Sender.SERVER);
                final PacketContainer container = new PacketContainer(type, packet);
                final PacketEvent event = PacketEvent.fromServer(this.invoker, container, marker, currentPlayer);
                this.invoker.invokePacketSending(event);
                if (event.isCancelled()) {
                    return null;
                }
                Object result = event.getPacket().getHandle();
                marker = NetworkMarker.getNetworkMarker(event);
                if (result != null && (NetworkMarker.hasOutputHandlers(marker) || NetworkMarker.hasPostListeners(marker))) {
                    result = this.writePacketInterceptor.constructProxy(result, event, marker);
                }
                return result;
            }
        }
        catch (OutOfMemoryError e2) {
            throw e2;
        }
        catch (ThreadDeath e3) {
            throw e3;
        }
        catch (Throwable e4) {
            this.reporter.reportDetailed(this, Report.newBuilder(PlayerInjector.REPORT_CANNOT_HANDLE_PACKET).error(e4).callerParam(packet));
        }
        return packet;
    }
    
    protected abstract boolean hasListener(final int p0);
    
    public DataInputStream getInputStream(final boolean cache) {
        if (this.networkManager == null) {
            throw new IllegalStateException("Network manager is NULL.");
        }
        if (PlayerInjector.inputField == null) {
            PlayerInjector.inputField = FuzzyReflection.fromObject(this.networkManager, true).getFieldByType("java\\.io\\.DataInputStream");
        }
        try {
            if (cache && this.cachedInput != null) {
                return this.cachedInput;
            }
            return this.cachedInput = (DataInputStream)FieldUtils.readField(PlayerInjector.inputField, this.networkManager, true);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to read input stream.", e);
        }
    }
    
    @Override
    public Player getPlayer() {
        return this.player;
    }
    
    public void setPlayer(final Player player) {
        this.player = player;
    }
    
    public ListenerInvoker getInvoker() {
        return this.invoker;
    }
    
    @Override
    public Player getUpdatedPlayer() {
        if (this.updatedPlayer != null) {
            return this.updatedPlayer;
        }
        return this.player;
    }
    
    @Override
    public void transferState(final SocketInjector delegate) {
    }
    
    @Override
    public void setUpdatedPlayer(final Player updatedPlayer) {
        this.updatedPlayer = updatedPlayer;
    }
    
    static {
        REPORT_ASSUME_DISCONNECT_METHOD = new ReportType("Cannot find disconnect method by name. Assuming %s.");
        REPORT_INVALID_ARGUMENT_DISCONNECT = new ReportType("Invalid argument passed to disconnect method: %s");
        REPORT_CANNOT_ACCESS_DISCONNECT = new ReportType("Unable to access disconnect method.");
        REPORT_CANNOT_CLOSE_SOCKET = new ReportType("Unable to close socket.");
        REPORT_ACCESS_DENIED_CLOSE_SOCKET = new ReportType("Insufficient permissions. Cannot close socket.");
        REPORT_DETECTED_CUSTOM_SERVER_HANDLER = new ReportType("Detected server handler proxy type by another plugin. Conflict may occur!");
        REPORT_CANNOT_PROXY_SERVER_HANDLER = new ReportType("Unable to load server handler from proxy type.");
        REPORT_CANNOT_UPDATE_PLAYER = new ReportType("Cannot update player in PlayerEvent.");
        REPORT_CANNOT_HANDLE_PACKET = new ReportType("Cannot handle server packet.");
        REPORT_INVALID_NETWORK_MANAGER = new ReportType("NetworkManager doesn't appear to be valid.");
    }
    
    public static class ServerHandlerNull extends IllegalAccessError
    {
        private static final long serialVersionUID = 1L;
        
        public ServerHandlerNull() {
            super("Unable to fetch server handler: was NUll.");
        }
        
        public ServerHandlerNull(final String s) {
            super(s);
        }
    }
}
