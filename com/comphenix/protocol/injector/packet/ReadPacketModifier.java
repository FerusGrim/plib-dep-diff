package com.comphenix.protocol.injector.packet;

import com.google.common.collect.MapMaker;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.PacketType;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import java.util.Map;
import com.comphenix.protocol.injector.NetworkProcessor;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.error.ReportType;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;

class ReadPacketModifier implements MethodInterceptor
{
    public static final ReportType REPORT_CANNOT_HANDLE_CLIENT_PACKET;
    private static final Object CANCEL_MARKER;
    private ProxyPacketInjector packetInjector;
    private int packetID;
    private ErrorReporter reporter;
    private NetworkProcessor processor;
    private boolean isReadPacketDataMethod;
    private static Map<Object, Object> override;
    
    public ReadPacketModifier(final int packetID, final ProxyPacketInjector packetInjector, final ErrorReporter reporter, final boolean isReadPacketDataMethod) {
        this.packetID = packetID;
        this.packetInjector = packetInjector;
        this.reporter = reporter;
        this.processor = new NetworkProcessor(reporter);
        this.isReadPacketDataMethod = isReadPacketDataMethod;
    }
    
    public static void removeOverride(final Object packet) {
        ReadPacketModifier.override.remove(packet);
    }
    
    public static Object getOverride(final Object packet) {
        return ReadPacketModifier.override.get(packet);
    }
    
    public static void setOverride(final Object packet, final Object overridePacket) {
        ReadPacketModifier.override.put(packet, (overridePacket != null) ? overridePacket : ReadPacketModifier.CANCEL_MARKER);
    }
    
    public static boolean isCancelled(final Object packet) {
        return getOverride(packet) == ReadPacketModifier.CANCEL_MARKER;
    }
    
    @Override
    public Object intercept(final Object thisObj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
        Object overridenObject = ReadPacketModifier.override.get(thisObj);
        Object returnValue = null;
        final InputStream input = this.isReadPacketDataMethod ? ((InputStream)args[0]) : null;
        ByteArrayOutputStream bufferStream = null;
        if (this.isReadPacketDataMethod && this.packetInjector.requireInputBuffers(this.packetID)) {
            final CaptureInputStream captured = new CaptureInputStream(input, bufferStream = new ByteArrayOutputStream());
            args[0] = new DataInputStream(captured);
        }
        if (overridenObject != null) {
            if (overridenObject == ReadPacketModifier.CANCEL_MARKER) {
                if (method.getReturnType().equals(Void.TYPE)) {
                    return null;
                }
                overridenObject = thisObj;
            }
            returnValue = proxy.invokeSuper(overridenObject, args);
        }
        else {
            returnValue = proxy.invokeSuper(thisObj, args);
        }
        if (this.isReadPacketDataMethod) {
            args[0] = input;
            try {
                final byte[] buffer = (byte[])((bufferStream != null) ? bufferStream.toByteArray() : null);
                final PacketType type = PacketType.findLegacy(this.packetID, PacketType.Sender.CLIENT);
                final PacketContainer container = new PacketContainer(type, thisObj);
                final PacketEvent event = this.packetInjector.packetRecieved(container, input, buffer);
                if (event != null) {
                    final Object result = event.getPacket().getHandle();
                    if (event.isCancelled()) {
                        ReadPacketModifier.override.put(thisObj, ReadPacketModifier.CANCEL_MARKER);
                        return returnValue;
                    }
                    if (!this.objectEquals(thisObj, result)) {
                        ReadPacketModifier.override.put(thisObj, result);
                    }
                    final NetworkMarker marker = NetworkMarker.getNetworkMarker(event);
                    this.processor.invokePostEvent(event, marker);
                }
            }
            catch (OutOfMemoryError e) {
                throw e;
            }
            catch (ThreadDeath e2) {
                throw e2;
            }
            catch (Throwable e3) {
                this.reporter.reportDetailed(this, Report.newBuilder(ReadPacketModifier.REPORT_CANNOT_HANDLE_CLIENT_PACKET).callerParam(args[0]).error(e3));
            }
        }
        return returnValue;
    }
    
    private boolean objectEquals(final Object a, final Object b) {
        return System.identityHashCode(a) != System.identityHashCode(b);
    }
    
    static {
        REPORT_CANNOT_HANDLE_CLIENT_PACKET = new ReportType("Cannot handle client packet.");
        CANCEL_MARKER = new Object();
        ReadPacketModifier.override = (Map<Object, Object>)new MapMaker().weakKeys().makeMap();
    }
}
