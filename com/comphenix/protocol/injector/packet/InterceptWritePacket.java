package com.comphenix.protocol.injector.packet;

import java.io.DataOutput;
import java.util.Iterator;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.error.Report;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.MethodInfo;
import java.lang.reflect.Method;
import com.comphenix.protocol.utility.EnhancerFactory;
import com.google.common.collect.Maps;
import com.comphenix.protocol.error.ErrorReporter;
import java.util.concurrent.ConcurrentMap;
import com.comphenix.net.sf.cglib.proxy.CallbackFilter;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.error.ReportType;

public class InterceptWritePacket
{
    public static final ReportType REPORT_CANNOT_FIND_WRITE_PACKET_METHOD;
    public static final ReportType REPORT_CANNOT_CONSTRUCT_WRITE_PROXY;
    private static FuzzyMethodContract WRITE_PACKET;
    private CallbackFilter filter;
    private boolean writePacketIntercepted;
    private ConcurrentMap<Integer, Class<?>> proxyClasses;
    private ErrorReporter reporter;
    private WritePacketModifier modifierWrite;
    private WritePacketModifier modifierRest;
    
    public InterceptWritePacket(final ErrorReporter reporter) {
        this.proxyClasses = (ConcurrentMap<Integer, Class<?>>)Maps.newConcurrentMap();
        this.reporter = reporter;
        this.modifierWrite = new WritePacketModifier(reporter, true);
        this.modifierRest = new WritePacketModifier(reporter, false);
    }
    
    private Class<?> createProxyClass() {
        final Enhancer ex = EnhancerFactory.getInstance().createEnhancer();
        if (this.filter == null) {
            this.filter = new CallbackFilter() {
                @Override
                public int accept(final Method method) {
                    if (InterceptWritePacket.WRITE_PACKET.isMatch(MethodInfo.fromMethod(method), null)) {
                        InterceptWritePacket.this.writePacketIntercepted = true;
                        return 0;
                    }
                    return 1;
                }
            };
        }
        ex.setSuperclass(MinecraftReflection.getPacketClass());
        ex.setCallbackFilter(this.filter);
        ex.setUseCache(false);
        ex.setCallbackTypes(new Class[] { WritePacketModifier.class, WritePacketModifier.class });
        final Class<?> proxyClass = (Class<?>)ex.createClass();
        Enhancer.registerStaticCallbacks(proxyClass, new Callback[] { this.modifierWrite, this.modifierRest });
        if (proxyClass != null && !this.writePacketIntercepted) {
            this.reporter.reportWarning(this, Report.newBuilder(InterceptWritePacket.REPORT_CANNOT_FIND_WRITE_PACKET_METHOD).messageParam(MinecraftReflection.getPacketClass()));
        }
        return proxyClass;
    }
    
    private Class<?> getProxyClass(final int packetId) {
        Class<?> stored = this.proxyClasses.get(packetId);
        if (stored == null) {
            final Class<?> created = this.createProxyClass();
            stored = this.proxyClasses.putIfAbsent(packetId, created);
            if (stored == null) {
                stored = created;
                PacketRegistry.getPacketToID().put(stored, packetId);
            }
        }
        return stored;
    }
    
    public Object constructProxy(final Object proxyObject, final PacketEvent event, final NetworkMarker marker) {
        Class<?> proxyClass = null;
        try {
            proxyClass = this.getProxyClass(event.getPacketID());
            final Object generated = proxyClass.newInstance();
            this.modifierWrite.register(generated, proxyObject, event, marker);
            this.modifierRest.register(generated, proxyObject, event, marker);
            return generated;
        }
        catch (Exception e) {
            this.reporter.reportWarning(this, Report.newBuilder(InterceptWritePacket.REPORT_CANNOT_CONSTRUCT_WRITE_PROXY).messageParam(proxyClass));
            return null;
        }
    }
    
    public void cleanup() {
        for (final Class<?> stored : this.proxyClasses.values()) {
            PacketRegistry.getPacketToID().remove(stored);
        }
    }
    
    static {
        REPORT_CANNOT_FIND_WRITE_PACKET_METHOD = new ReportType("Cannot find write packet method in %s.");
        REPORT_CANNOT_CONSTRUCT_WRITE_PROXY = new ReportType("Cannot construct write proxy packet %s.");
        InterceptWritePacket.WRITE_PACKET = FuzzyMethodContract.newBuilder().returnTypeVoid().parameterDerivedOf(DataOutput.class).parameterCount(1).build();
    }
}
