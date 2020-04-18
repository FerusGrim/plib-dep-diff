package com.comphenix.protocol.injector.server;

import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.utility.ChatExtensions;
import com.comphenix.net.sf.cglib.proxy.NoOp;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import com.comphenix.net.sf.cglib.proxy.CallbackFilter;

public class TemporaryPlayerFactory
{
    private static CallbackFilter callbackFilter;
    
    public static SocketInjector getInjectorFromPlayer(final Player player) {
        if (player instanceof TemporaryPlayer) {
            return ((TemporaryPlayer)player).getInjector();
        }
        return null;
    }
    
    public static void setInjectorInPlayer(final Player player, final SocketInjector injector) {
        ((TemporaryPlayer)player).setInjector(injector);
    }
    
    public Player createTemporaryPlayer(final Server server) {
        final Callback implementation = new MethodInterceptor() {
            @Override
            public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
                final String methodName = method.getName();
                final SocketInjector injector = ((TemporaryPlayer)obj).getInjector();
                if (injector == null) {
                    throw new IllegalStateException("Unable to find injector.");
                }
                if (methodName.equals("getPlayer")) {
                    return injector.getUpdatedPlayer();
                }
                if (methodName.equals("getAddress")) {
                    return injector.getAddress();
                }
                if (methodName.equals("getServer")) {
                    return server;
                }
                Label_0200: {
                    if (!methodName.equals("chat")) {
                        if (!methodName.equals("sendMessage")) {
                            break Label_0200;
                        }
                    }
                    try {
                        final Object argument = args[0];
                        if (argument instanceof String) {
                            return TemporaryPlayerFactory.this.sendMessage(injector, (String)argument);
                        }
                        if (argument instanceof String[]) {
                            for (final String message : (String[])argument) {
                                TemporaryPlayerFactory.this.sendMessage(injector, message);
                            }
                            return null;
                        }
                    }
                    catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
                if (methodName.equals("kickPlayer")) {
                    injector.disconnect((String)args[0]);
                    return null;
                }
                final Player updated = injector.getUpdatedPlayer();
                if (updated != obj && updated != null) {
                    return proxy.invoke(updated, args);
                }
                if (methodName.equals("isOnline")) {
                    return injector.getSocket() != null && injector.getSocket().isConnected();
                }
                if (methodName.equals("getName")) {
                    return "UNKNOWN[" + injector.getSocket().getRemoteSocketAddress() + "]";
                }
                throw new UnsupportedOperationException("The method " + method.getName() + " is not supported for temporary players.");
            }
        };
        if (TemporaryPlayerFactory.callbackFilter == null) {
            TemporaryPlayerFactory.callbackFilter = new CallbackFilter() {
                @Override
                public int accept(final Method method) {
                    if (method.getDeclaringClass().equals(Object.class) || method.getDeclaringClass().equals(TemporaryPlayer.class)) {
                        return 0;
                    }
                    return 1;
                }
            };
        }
        final Enhancer ex = new Enhancer();
        ex.setSuperclass(TemporaryPlayer.class);
        ex.setInterfaces(new Class[] { Player.class });
        ex.setCallbacks(new Callback[] { NoOp.INSTANCE, implementation });
        ex.setCallbackFilter(TemporaryPlayerFactory.callbackFilter);
        return (Player)ex.create();
    }
    
    public Player createTemporaryPlayer(final Server server, final SocketInjector injector) {
        final Player temporary = this.createTemporaryPlayer(server);
        ((TemporaryPlayer)temporary).setInjector(injector);
        return temporary;
    }
    
    private Object sendMessage(final SocketInjector injector, final String message) throws InvocationTargetException, FieldAccessException {
        for (final PacketContainer packet : ChatExtensions.createChatPackets(message)) {
            injector.sendServerPacket(packet.getHandle(), null, false);
        }
        return null;
    }
}
