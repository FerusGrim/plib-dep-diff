package com.comphenix.protocol.injector.packet;

import com.comphenix.protocol.error.Report;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.MapMaker;
import com.comphenix.protocol.injector.NetworkProcessor;
import com.comphenix.protocol.error.ErrorReporter;
import java.util.Map;
import com.comphenix.protocol.error.ReportType;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;

public class WritePacketModifier implements MethodInterceptor
{
    public static final ReportType REPORT_CANNOT_WRITE_SERVER_PACKET;
    private Map<Object, ProxyInformation> proxyLookup;
    private final ErrorReporter reporter;
    private final NetworkProcessor processor;
    private boolean isWriteMethod;
    
    public WritePacketModifier(final ErrorReporter reporter, final boolean isWriteMethod) {
        this.proxyLookup = (Map<Object, ProxyInformation>)new MapMaker().weakKeys().makeMap();
        this.reporter = reporter;
        this.processor = new NetworkProcessor(reporter);
        this.isWriteMethod = isWriteMethod;
    }
    
    public void register(final Object generatedClass, final Object proxyObject, final PacketEvent event, final NetworkMarker marker) {
        this.proxyLookup.put(generatedClass, new ProxyInformation(proxyObject, event, marker));
    }
    
    @Override
    public Object intercept(final Object thisObj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
        final ProxyInformation information = this.proxyLookup.get(thisObj);
        if (information == null) {
            throw new RuntimeException("Cannot find proxy information for " + thisObj);
        }
        if (this.isWriteMethod) {
            if (!information.marker.getOutputHandlers().isEmpty()) {
                try {
                    final DataOutput output = (DataOutput)args[0];
                    final ByteArrayOutputStream outputBufferStream = new ByteArrayOutputStream();
                    proxy.invoke(information.proxyObject, new Object[] { new DataOutputStream(outputBufferStream) });
                    final byte[] outputBuffer = this.processor.processOutput(information.event, information.marker, outputBufferStream.toByteArray());
                    output.write(outputBuffer);
                    this.processor.invokePostEvent(information.event, information.marker);
                    return null;
                }
                catch (OutOfMemoryError e) {
                    throw e;
                }
                catch (ThreadDeath e2) {
                    throw e2;
                }
                catch (Throwable e3) {
                    this.reporter.reportDetailed(this, Report.newBuilder(WritePacketModifier.REPORT_CANNOT_WRITE_SERVER_PACKET).callerParam(args[0]).error(e3));
                }
            }
            proxy.invoke(information.proxyObject, args);
            this.processor.invokePostEvent(information.event, information.marker);
            return null;
        }
        return proxy.invoke(information.proxyObject, args);
    }
    
    static {
        REPORT_CANNOT_WRITE_SERVER_PACKET = new ReportType("Cannot write server packet.");
    }
    
    private static class ProxyInformation
    {
        public final Object proxyObject;
        public final PacketEvent event;
        public final NetworkMarker marker;
        
        public ProxyInformation(final Object proxyObject, final PacketEvent event, final NetworkMarker marker) {
            this.proxyObject = proxyObject;
            this.event = event;
            this.marker = marker;
        }
    }
}
