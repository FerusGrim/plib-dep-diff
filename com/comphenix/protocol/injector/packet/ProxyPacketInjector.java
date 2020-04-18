package com.comphenix.protocol.injector.packet;

import java.util.Iterator;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedIntHashMap;
import java.io.DataInput;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.ConnectionSide;
import org.bukkit.entity.Player;
import java.io.DataInputStream;
import com.comphenix.protocol.events.PacketEvent;
import java.io.InputStream;
import com.comphenix.protocol.events.PacketContainer;
import java.util.Map;
import com.comphenix.protocol.error.Report;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.net.sf.cglib.proxy.NoOp;
import com.comphenix.protocol.reflect.MethodInfo;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.utility.EnhancerFactory;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.PacketType;
import java.util.Set;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.net.sf.cglib.proxy.CallbackFilter;
import com.comphenix.protocol.injector.player.PlayerInjectionHandler;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.injector.ListenerInvoker;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.error.ReportType;

class ProxyPacketInjector implements PacketInjector
{
    public static final ReportType REPORT_CANNOT_FIND_READ_PACKET_METHOD;
    public static final ReportType REPORT_UNKNOWN_ORIGIN_FOR_PACKET;
    private static FuzzyMethodContract READ_PACKET;
    private static PacketClassLookup lookup;
    private ListenerInvoker manager;
    private ErrorReporter reporter;
    private PlayerInjectionHandler playerInjection;
    private CallbackFilter filter;
    private boolean readPacketIntercepted;
    
    public ProxyPacketInjector(final ListenerInvoker manager, final PlayerInjectionHandler playerInjection, final ErrorReporter reporter) throws FieldAccessException {
        this.readPacketIntercepted = false;
        this.manager = manager;
        this.playerInjection = playerInjection;
        this.reporter = reporter;
        this.initialize();
    }
    
    @Override
    public boolean isCancelled(final Object packet) {
        return ReadPacketModifier.isCancelled(packet);
    }
    
    @Override
    public void setCancelled(final Object packet, final boolean cancelled) {
        if (cancelled) {
            ReadPacketModifier.setOverride(packet, null);
        }
        else {
            ReadPacketModifier.removeOverride(packet);
        }
    }
    
    private void initialize() throws FieldAccessException {
        if (ProxyPacketInjector.lookup == null) {
            try {
                ProxyPacketInjector.lookup = new IntHashMapLookup();
            }
            catch (Exception e1) {
                try {
                    ProxyPacketInjector.lookup = new ArrayLookup();
                }
                catch (Exception e2) {
                    throw new FieldAccessException(e1.getMessage() + ". Workaround failed too.", e2);
                }
            }
        }
    }
    
    @Override
    public void inputBuffersChanged(final Set<PacketType> set) {
    }
    
    @Override
    public boolean addPacketHandler(final PacketType type, final Set<ListenerOptions> options) {
        final int packetID = type.getLegacyId();
        if (this.hasPacketHandler(type)) {
            return false;
        }
        final Enhancer ex = EnhancerFactory.getInstance().createEnhancer();
        final Map<Integer, Class> overwritten = PacketRegistry.getOverwrittenPackets();
        final Map<Integer, Class> previous = PacketRegistry.getPreviousPackets();
        final Map<Class, Integer> registry = PacketRegistry.getPacketToID();
        final Class old = PacketRegistry.getPacketClassFromType(type);
        if (old == null) {
            throw new IllegalStateException("Packet ID " + type + " is not a valid packet type in this version.");
        }
        if (Factory.class.isAssignableFrom(old)) {
            throw new IllegalStateException("Packet " + type + " has already been injected.");
        }
        if (this.filter == null) {
            this.readPacketIntercepted = false;
            this.filter = new CallbackFilter() {
                @Override
                public int accept(final Method method) {
                    if (method.getDeclaringClass().equals(Object.class)) {
                        return 0;
                    }
                    if (ProxyPacketInjector.READ_PACKET.isMatch(MethodInfo.fromMethod(method), null)) {
                        ProxyPacketInjector.this.readPacketIntercepted = true;
                        return 1;
                    }
                    return 2;
                }
            };
        }
        ex.setSuperclass(old);
        ex.setCallbackFilter(this.filter);
        ex.setCallbackTypes(new Class[] { NoOp.class, ReadPacketModifier.class, ReadPacketModifier.class });
        final Class proxy = ex.createClass();
        final ReadPacketModifier modifierReadPacket = new ReadPacketModifier(packetID, this, this.reporter, true);
        final ReadPacketModifier modifierRest = new ReadPacketModifier(packetID, this, this.reporter, false);
        Enhancer.registerStaticCallbacks(proxy, new Callback[] { NoOp.INSTANCE, modifierReadPacket, modifierRest });
        if (!this.readPacketIntercepted) {
            this.reporter.reportWarning(this, Report.newBuilder(ProxyPacketInjector.REPORT_CANNOT_FIND_READ_PACKET_METHOD).messageParam(packetID));
        }
        previous.put(packetID, old);
        registry.put(proxy, packetID);
        overwritten.put(packetID, proxy);
        ProxyPacketInjector.lookup.setLookup(packetID, proxy);
        return true;
    }
    
    @Override
    public boolean removePacketHandler(final PacketType type) {
        final int packetID = type.getLegacyId();
        if (!this.hasPacketHandler(type)) {
            return false;
        }
        final Map<Class, Integer> registry = PacketRegistry.getPacketToID();
        final Map<Integer, Class> previous = PacketRegistry.getPreviousPackets();
        final Map<Integer, Class> overwritten = PacketRegistry.getOverwrittenPackets();
        final Class old = previous.get(packetID);
        final Class proxy = PacketRegistry.getPacketClassFromType(type);
        ProxyPacketInjector.lookup.setLookup(packetID, old);
        previous.remove(packetID);
        registry.remove(proxy);
        overwritten.remove(packetID);
        return true;
    }
    
    @Deprecated
    public boolean requireInputBuffers(final int packetId) {
        return this.manager.requireInputBuffer(packetId);
    }
    
    @Override
    public boolean hasPacketHandler(final PacketType type) {
        return PacketRegistry.getPreviousPackets().containsKey(type.getLegacyId());
    }
    
    @Override
    public Set<PacketType> getPacketHandlers() {
        return PacketRegistry.toPacketTypes(PacketRegistry.getPreviousPackets().keySet(), PacketType.Sender.CLIENT);
    }
    
    public PacketEvent packetRecieved(final PacketContainer packet, final InputStream input, final byte[] buffered) {
        if (this.playerInjection.canRecievePackets()) {
            return this.playerInjection.handlePacketRecieved(packet, input, buffered);
        }
        try {
            final Player client = this.playerInjection.getPlayerByConnection((DataInputStream)input);
            if (client != null) {
                return this.packetRecieved(packet, client, buffered);
            }
            this.reporter.reportDetailed(this, Report.newBuilder(ProxyPacketInjector.REPORT_UNKNOWN_ORIGIN_FOR_PACKET).messageParam(input, packet.getType()));
            return null;
        }
        catch (InterruptedException e) {
            return null;
        }
    }
    
    @Override
    public PacketEvent packetRecieved(final PacketContainer packet, final Player client, final byte[] buffered) {
        final NetworkMarker marker = (buffered != null) ? new LegacyNetworkMarker(ConnectionSide.CLIENT_SIDE, buffered, packet.getType()) : null;
        final PacketEvent event = PacketEvent.fromClient(this.manager, packet, marker, client);
        this.manager.invokePacketRecieving(event);
        return event;
    }
    
    @Override
    public synchronized void cleanupAll() {
        final Map<Integer, Class> overwritten = PacketRegistry.getOverwrittenPackets();
        final Map<Integer, Class> previous = PacketRegistry.getPreviousPackets();
        for (final Integer id : previous.keySet().toArray(new Integer[0])) {
            this.removePacketHandler(PacketType.findLegacy(id, PacketType.Sender.CLIENT));
            this.removePacketHandler(PacketType.findLegacy(id, PacketType.Sender.SERVER));
        }
        overwritten.clear();
        previous.clear();
    }
    
    static {
        REPORT_CANNOT_FIND_READ_PACKET_METHOD = new ReportType("Cannot find read packet method for ID %s.");
        REPORT_UNKNOWN_ORIGIN_FOR_PACKET = new ReportType("Timeout: Unknown origin %s for packet %s. Are you using GamePhase.LOGIN?");
        ProxyPacketInjector.READ_PACKET = FuzzyMethodContract.newBuilder().returnTypeVoid().parameterDerivedOf(DataInput.class).parameterCount(1).build();
    }
    
    private static class IntHashMapLookup implements PacketClassLookup
    {
        private WrappedIntHashMap intHashMap;
        
        public IntHashMapLookup() throws IllegalAccessException {
            this.initialize();
        }
        
        @Override
        public void setLookup(final int packetID, final Class<?> clazz) {
            this.intHashMap.put(packetID, clazz);
        }
        
        private void initialize() throws IllegalAccessException {
            if (this.intHashMap == null) {
                final Field intHashMapField = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass(), true).getFieldByType("packetIdMap", MinecraftReflection.getIntHashMapClass());
                try {
                    this.intHashMap = WrappedIntHashMap.fromHandle(FieldUtils.readField(intHashMapField, (Object)null, true));
                }
                catch (IllegalArgumentException e) {
                    throw new RuntimeException("Minecraft is incompatible.", e);
                }
            }
        }
    }
    
    private static class ArrayLookup implements PacketClassLookup
    {
        private Class<?>[] array;
        
        public ArrayLookup() throws IllegalAccessException {
            this.initialize();
        }
        
        @Override
        public void setLookup(final int packetID, final Class<?> clazz) {
            this.array[packetID] = clazz;
        }
        
        private void initialize() throws IllegalAccessException {
            final FuzzyReflection reflection = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass());
            for (final Field field : reflection.getFieldListByType(Class[].class)) {
                final Class<?>[] test = (Class<?>[])FieldUtils.readField(field, (Object)null);
                if (test.length == 256) {
                    this.array = test;
                    return;
                }
            }
            throw new IllegalArgumentException("Unable to find an array with the type " + Class[].class + " in " + MinecraftReflection.getPacketClass());
        }
    }
    
    private interface PacketClassLookup
    {
        void setLookup(final int p0, final Class<?> p1);
    }
}
