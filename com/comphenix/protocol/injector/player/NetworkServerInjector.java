package com.comphenix.protocol.injector.player;

import java.util.Arrays;
import com.comphenix.protocol.injector.PlayerInjectHooks;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import com.comphenix.protocol.reflect.instances.DefaultInstances;
import com.comphenix.protocol.reflect.instances.InstanceProvider;
import com.comphenix.protocol.reflect.instances.ExistingGenerator;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.net.sf.cglib.proxy.NoOp;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;
import com.comphenix.protocol.utility.EnhancerFactory;
import com.comphenix.protocol.reflect.VolatileField;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.net.sf.cglib.proxy.Factory;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.utility.MinecraftMethods;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.injector.ListenerInvoker;
import org.bukkit.entity.Player;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.reflect.ObjectWriter;
import com.comphenix.protocol.concurrency.IntegerSet;
import java.lang.reflect.Field;
import com.comphenix.net.sf.cglib.proxy.CallbackFilter;
import com.comphenix.protocol.error.ReportType;

class NetworkServerInjector extends PlayerInjector
{
    public static final ReportType REPORT_ASSUMING_DISCONNECT_FIELD;
    public static final ReportType REPORT_DISCONNECT_FIELD_MISSING;
    public static final ReportType REPORT_DISCONNECT_FIELD_FAILURE;
    private static volatile CallbackFilter callbackFilter;
    private static volatile boolean foundSendPacket;
    private static volatile Field disconnectField;
    private InjectedServerConnection serverInjection;
    private IntegerSet sendingFilters;
    private boolean hasDisconnected;
    private final ObjectWriter writer;
    
    public NetworkServerInjector(final ErrorReporter reporter, final Player player, final ListenerInvoker invoker, final IntegerSet sendingFilters, final InjectedServerConnection serverInjection) {
        super(reporter, player, invoker);
        this.writer = new ObjectWriter();
        this.sendingFilters = sendingFilters;
        this.serverInjection = serverInjection;
    }
    
    @Override
    protected boolean hasListener(final int packetID) {
        return this.sendingFilters.contains(packetID);
    }
    
    @Override
    public void sendServerPacket(final Object packet, final NetworkMarker marker, final boolean filtered) throws InvocationTargetException {
        final Object serverDelegate = filtered ? this.serverHandlerRef.getValue() : this.serverHandlerRef.getOldValue();
        if (serverDelegate != null) {
            try {
                if (marker != null) {
                    this.queuedMarkers.put(packet, marker);
                }
                MinecraftMethods.getSendPacketMethod().invoke(serverDelegate, packet);
                return;
            }
            catch (IllegalArgumentException e) {
                throw e;
            }
            catch (InvocationTargetException e2) {
                throw e2;
            }
            catch (IllegalAccessException e3) {
                throw new IllegalStateException("Unable to access send packet method.", e3);
            }
            throw new IllegalStateException("Unable to load server handler. Cannot send packet.");
        }
        throw new IllegalStateException("Unable to load server handler. Cannot send packet.");
    }
    
    @Override
    public void injectManager() {
        if (this.serverHandlerRef == null) {
            throw new IllegalStateException("Cannot find server handler.");
        }
        if (this.serverHandlerRef.getValue() instanceof Factory) {
            return;
        }
        if (!this.tryInjectManager()) {
            Class<?> serverHandlerClass = MinecraftReflection.getPlayerConnectionClass();
            if (NetworkServerInjector.proxyServerField != null) {
                this.serverHandlerRef = new VolatileField(NetworkServerInjector.proxyServerField, this.serverHandler, true);
                this.serverHandler = this.serverHandlerRef.getValue();
                if (this.serverHandler == null) {
                    throw new RuntimeException("Cannot hook player: Inner proxy object is NULL.");
                }
                serverHandlerClass = this.serverHandler.getClass();
                if (this.tryInjectManager()) {
                    return;
                }
            }
            throw new RuntimeException("Cannot hook player: Unable to find a valid constructor for the " + serverHandlerClass.getName() + " object.");
        }
    }
    
    private boolean tryInjectManager() {
        final Class<?> serverClass = this.serverHandler.getClass();
        final Enhancer ex = EnhancerFactory.getInstance().createEnhancer();
        final Callback sendPacketCallback = new MethodInterceptor() {
            @Override
            public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
                Object packet = args[0];
                if (packet != null) {
                    packet = NetworkServerInjector.this.handlePacketSending(packet);
                    if (packet == null) {
                        return null;
                    }
                    args[0] = packet;
                }
                return proxy.invokeSuper(obj, args);
            }
        };
        final Callback noOpCallback = NoOp.INSTANCE;
        if (NetworkServerInjector.callbackFilter == null) {
            NetworkServerInjector.callbackFilter = new SendMethodFilter();
        }
        ex.setSuperclass(serverClass);
        ex.setCallbacks(new Callback[] { sendPacketCallback, noOpCallback });
        ex.setCallbackFilter(NetworkServerInjector.callbackFilter);
        final Class<?> minecraftSuperClass = this.getFirstMinecraftSuperClass(this.serverHandler.getClass());
        final ExistingGenerator generator = ExistingGenerator.fromObjectFields(this.serverHandler, minecraftSuperClass);
        DefaultInstances serverInstances = null;
        final Object proxyInstance = this.getProxyServerHandler();
        if (proxyInstance != null && proxyInstance != this.serverHandler) {
            serverInstances = DefaultInstances.fromArray(generator, ExistingGenerator.fromObjectArray(new Object[] { proxyInstance }));
        }
        else {
            serverInstances = DefaultInstances.fromArray(generator);
        }
        serverInstances.setNonNull(true);
        serverInstances.setMaximumRecursion(1);
        final Object proxyObject = serverInstances.forEnhancer(ex).getDefault(serverClass);
        if (proxyObject == null) {
            return false;
        }
        if (!NetworkServerInjector.foundSendPacket) {
            throw new IllegalArgumentException("Unable to find a sendPacket method in " + serverClass);
        }
        this.serverInjection.replaceServerHandler(this.serverHandler, proxyObject);
        this.serverHandlerRef.setValue(proxyObject);
        return true;
    }
    
    private Object getProxyServerHandler() {
        if (NetworkServerInjector.proxyServerField != null && !NetworkServerInjector.proxyServerField.equals(this.serverHandlerRef.getField())) {
            try {
                return FieldUtils.readField(NetworkServerInjector.proxyServerField, this.serverHandler, true);
            }
            catch (OutOfMemoryError e) {
                throw e;
            }
            catch (ThreadDeath e2) {
                throw e2;
            }
            catch (Throwable t) {}
        }
        return null;
    }
    
    private Class<?> getFirstMinecraftSuperClass(final Class<?> clazz) {
        if (MinecraftReflection.isMinecraftClass(clazz)) {
            return clazz;
        }
        if (clazz.equals(Object.class)) {
            return clazz;
        }
        return this.getFirstMinecraftSuperClass(clazz.getSuperclass());
    }
    
    @Override
    protected void cleanHook() {
        if (this.serverHandlerRef != null && this.serverHandlerRef.isCurrentSet()) {
            this.writer.copyTo(this.serverHandlerRef.getValue(), this.serverHandlerRef.getOldValue(), this.serverHandler.getClass());
            this.serverHandlerRef.revertValue();
            try {
                if (this.getNetHandler() != null) {
                    try {
                        FieldUtils.writeField(NetworkServerInjector.netHandlerField, this.networkManager, this.serverHandlerRef.getOldValue(), true);
                    }
                    catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (this.hasDisconnected) {
                this.setDisconnect(this.serverHandlerRef.getValue(), true);
            }
        }
        this.serverInjection.revertServerHandler(this.serverHandler);
    }
    
    @Override
    public void handleDisconnect() {
        this.hasDisconnected = true;
    }
    
    private void setDisconnect(final Object handler, final boolean value) {
        try {
            if (NetworkServerInjector.disconnectField == null) {
                NetworkServerInjector.disconnectField = FuzzyReflection.fromObject(handler).getFieldByName("disconnected.*");
            }
            FieldUtils.writeField(NetworkServerInjector.disconnectField, handler, value);
        }
        catch (IllegalArgumentException e) {
            if (NetworkServerInjector.disconnectField == null) {
                NetworkServerInjector.disconnectField = FuzzyReflection.fromObject(handler).getFieldByType("disconnected", Boolean.TYPE);
                this.reporter.reportWarning(this, Report.newBuilder(NetworkServerInjector.REPORT_ASSUMING_DISCONNECT_FIELD).messageParam(NetworkServerInjector.disconnectField));
                if (NetworkServerInjector.disconnectField != null) {
                    this.setDisconnect(handler, value);
                    return;
                }
            }
            this.reporter.reportDetailed(this, Report.newBuilder(NetworkServerInjector.REPORT_DISCONNECT_FIELD_MISSING).error(e));
        }
        catch (IllegalAccessException e2) {
            this.reporter.reportWarning(this, Report.newBuilder(NetworkServerInjector.REPORT_DISCONNECT_FIELD_FAILURE).error(e2));
        }
    }
    
    @Override
    public UnsupportedListener checkListener(final MinecraftVersion version, final PacketListener listener) {
        return null;
    }
    
    @Override
    public boolean canInject(final GamePhase phase) {
        return phase == GamePhase.PLAYING;
    }
    
    @Override
    public PlayerInjectHooks getHookType() {
        return PlayerInjectHooks.NETWORK_SERVER_OBJECT;
    }
    
    static {
        REPORT_ASSUMING_DISCONNECT_FIELD = new ReportType("Unable to find 'disconnected' field. Assuming %s.");
        REPORT_DISCONNECT_FIELD_MISSING = new ReportType("Cannot find disconnected field. Is ProtocolLib up to date?");
        REPORT_DISCONNECT_FIELD_FAILURE = new ReportType("Unable to update disconnected field. Player quit event may be sent twice.");
    }
    
    private static class SendMethodFilter implements CallbackFilter
    {
        private Method sendPacket;
        
        private SendMethodFilter() {
            this.sendPacket = MinecraftMethods.getSendPacketMethod();
        }
        
        @Override
        public int accept(final Method method) {
            if (this.isCallableEqual(this.sendPacket, method)) {
                NetworkServerInjector.foundSendPacket = true;
                return 0;
            }
            return 1;
        }
        
        private boolean isCallableEqual(final Method first, final Method second) {
            return first.getName().equals(second.getName()) && first.getReturnType().equals(second.getReturnType()) && Arrays.equals(first.getParameterTypes(), second.getParameterTypes());
        }
    }
}
