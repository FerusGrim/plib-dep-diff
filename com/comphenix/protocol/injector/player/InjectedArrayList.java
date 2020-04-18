package com.comphenix.protocol.injector.player;

import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;
import com.google.common.collect.MapMaker;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.injector.ListenerInvoker;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.EnhancerFactory;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.NetworkMarker;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import com.comphenix.protocol.error.ReportType;
import java.util.ArrayList;

class InjectedArrayList extends ArrayList<Object>
{
    public static final ReportType REPORT_CANNOT_REVERT_CANCELLED_PACKET;
    private static final long serialVersionUID = -1173865905404280990L;
    private static ConcurrentMap<Object, Object> delegateLookup;
    private transient PlayerInjector injector;
    private transient Set<Object> ignoredPackets;
    private transient InvertedIntegerCallback callback;
    
    public InjectedArrayList(final PlayerInjector injector, final Set<Object> ignoredPackets) {
        this.injector = injector;
        this.ignoredPackets = ignoredPackets;
        this.callback = new InvertedIntegerCallback();
    }
    
    @Override
    public boolean add(final Object packet) {
        Object result = null;
        if (packet instanceof NetworkFieldInjector.FakePacket) {
            return true;
        }
        if (this.ignoredPackets.contains(packet)) {
            result = this.ignoredPackets.remove(packet);
        }
        else {
            result = this.injector.handlePacketSending(packet);
        }
        try {
            if (result != null) {
                super.add(result);
            }
            else {
                this.injector.sendServerPacket(this.createNegativePacket(packet), null, true);
            }
            return true;
        }
        catch (InvocationTargetException e) {
            ProtocolLibrary.getErrorReporter().reportDetailed(this, Report.newBuilder(InjectedArrayList.REPORT_CANNOT_REVERT_CANCELLED_PACKET).error(e).callerParam(packet));
            return false;
        }
    }
    
    Object createNegativePacket(final Object source) {
        final ListenerInvoker invoker = this.injector.getInvoker();
        final PacketType type = invoker.getPacketType(source);
        final Enhancer ex = EnhancerFactory.getInstance().createEnhancer();
        ex.setSuperclass(MinecraftReflection.getPacketClass());
        ex.setInterfaces(new Class[] { NetworkFieldInjector.FakePacket.class });
        ex.setUseCache(true);
        ex.setCallbackType(InvertedIntegerCallback.class);
        final Class<?> proxyClass = (Class<?>)ex.createClass();
        Enhancer.registerCallbacks(proxyClass, new Callback[] { this.callback });
        try {
            invoker.registerPacketClass(proxyClass, type.getLegacyId());
            final Object proxy = proxyClass.newInstance();
            registerDelegate(proxy, source);
            return proxy;
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot create fake class.", e);
        }
        finally {
            invoker.unregisterPacketClass(proxyClass);
        }
    }
    
    private static void registerDelegate(final Object proxy, final Object source) {
        InjectedArrayList.delegateLookup.put(proxy, source);
    }
    
    static {
        REPORT_CANNOT_REVERT_CANCELLED_PACKET = new ReportType("Reverting cancelled packet failed.");
        InjectedArrayList.delegateLookup = (ConcurrentMap<Object, Object>)new MapMaker().weakKeys().makeMap();
    }
    
    private class InvertedIntegerCallback implements MethodInterceptor
    {
        @Override
        public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
            final Object delegate = InjectedArrayList.delegateLookup.get(obj);
            if (delegate == null) {
                throw new IllegalStateException("Unable to find delegate source for " + obj);
            }
            if (method.getReturnType().equals(Integer.TYPE) && args.length == 0) {
                final Integer result = (Integer)proxy.invoke(delegate, args);
                return -result;
            }
            return proxy.invoke(delegate, args);
        }
    }
}
