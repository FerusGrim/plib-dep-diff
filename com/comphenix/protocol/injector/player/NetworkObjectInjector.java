package com.comphenix.protocol.injector.player;

import com.comphenix.protocol.injector.PlayerInjectHooks;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.protocol.utility.EnhancerFactory;
import com.comphenix.net.sf.cglib.proxy.LazyLoader;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketListener;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.injector.server.SocketInjector;
import org.bukkit.Server;
import com.comphenix.protocol.injector.ListenerInvoker;
import org.bukkit.entity.Player;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import com.comphenix.net.sf.cglib.proxy.CallbackFilter;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.concurrency.IntegerSet;

public class NetworkObjectInjector extends PlayerInjector
{
    private IntegerSet sendingFilters;
    private MinecraftVersion safeVersion;
    private static volatile CallbackFilter callbackFilter;
    private static volatile TemporaryPlayerFactory tempPlayerFactory;
    
    public NetworkObjectInjector(final ErrorReporter reporter, final Player player, final ListenerInvoker invoker, final IntegerSet sendingFilters) throws IllegalAccessException {
        super(reporter, player, invoker);
        this.safeVersion = new MinecraftVersion("1.4.4");
        this.sendingFilters = sendingFilters;
    }
    
    @Override
    protected boolean hasListener(final int packetID) {
        return this.sendingFilters.contains(packetID);
    }
    
    public Player createTemporaryPlayer(final Server server) {
        if (NetworkObjectInjector.tempPlayerFactory == null) {
            NetworkObjectInjector.tempPlayerFactory = new TemporaryPlayerFactory();
        }
        return NetworkObjectInjector.tempPlayerFactory.createTemporaryPlayer(server, this);
    }
    
    @Override
    public void sendServerPacket(final Object packet, final NetworkMarker marker, final boolean filtered) throws InvocationTargetException {
        final Object networkDelegate = filtered ? this.networkManagerRef.getValue() : this.networkManagerRef.getOldValue();
        if (networkDelegate != null) {
            try {
                if (marker != null) {
                    this.queuedMarkers.put(packet, marker);
                }
                NetworkObjectInjector.queueMethod.invoke(networkDelegate, packet);
                return;
            }
            catch (IllegalArgumentException e) {
                throw e;
            }
            catch (InvocationTargetException e2) {
                throw e2;
            }
            catch (IllegalAccessException e3) {
                throw new IllegalStateException("Unable to access queue method.", e3);
            }
            throw new IllegalStateException("Unable to load network mananager. Cannot send packet.");
        }
        throw new IllegalStateException("Unable to load network mananager. Cannot send packet.");
    }
    
    @Override
    public UnsupportedListener checkListener(final MinecraftVersion version, final PacketListener listener) {
        if (version != null && version.compareTo(this.safeVersion) > 0) {
            return null;
        }
        final int[] unsupported = { 51, 56 };
        if (ListeningWhitelist.containsAny(listener.getSendingWhitelist(), unsupported)) {
            return new UnsupportedListener("The NETWORK_OBJECT_INJECTOR hook doesn't support map chunk listeners.", unsupported);
        }
        return null;
    }
    
    @Override
    public void injectManager() {
        if (this.networkManager != null) {
            final Class<?> networkInterface = this.networkManagerRef.getField().getType();
            final Object networkDelegate = this.networkManagerRef.getOldValue();
            if (!networkInterface.isInterface()) {
                throw new UnsupportedOperationException("Must use CraftBukkit 1.3.0 or later to inject into into NetworkMananger.");
            }
            final Callback queueFilter = new MethodInterceptor() {
                @Override
                public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
                    Object packet = args[0];
                    if (packet != null) {
                        packet = NetworkObjectInjector.this.handlePacketSending(packet);
                        if (packet == null) {
                            return null;
                        }
                        args[0] = packet;
                    }
                    return proxy.invokeSuper(networkDelegate, args);
                }
            };
            final Callback dispatch = new LazyLoader() {
                @Override
                public Object loadObject() throws Exception {
                    return networkDelegate;
                }
            };
            if (NetworkObjectInjector.callbackFilter == null) {
                NetworkObjectInjector.callbackFilter = new CallbackFilter() {
                    @Override
                    public int accept(final Method method) {
                        if (method.equals(PlayerInjector.queueMethod)) {
                            return 0;
                        }
                        return 1;
                    }
                };
            }
            final Enhancer ex = EnhancerFactory.getInstance().createEnhancer();
            ex.setSuperclass(networkInterface);
            ex.setCallbacks(new Callback[] { queueFilter, dispatch });
            ex.setCallbackFilter(NetworkObjectInjector.callbackFilter);
            this.networkManagerRef.setValue(ex.create());
        }
    }
    
    @Override
    protected void cleanHook() {
        if (this.networkManagerRef != null && this.networkManagerRef.isCurrentSet()) {
            this.networkManagerRef.revertValue();
        }
    }
    
    @Override
    public void handleDisconnect() {
    }
    
    @Override
    public boolean canInject(final GamePhase phase) {
        return true;
    }
    
    @Override
    public PlayerInjectHooks getHookType() {
        return PlayerInjectHooks.NETWORK_MANAGER_OBJECT;
    }
}
