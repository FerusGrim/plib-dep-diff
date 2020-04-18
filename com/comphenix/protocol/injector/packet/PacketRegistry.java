package com.comphenix.protocol.injector.packet;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.collect.Sets;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.Iterator;
import java.util.Collections;
import com.google.common.collect.Maps;
import com.google.common.base.Function;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.injector.netty.NettyProtocolRegistry;
import java.util.Map;
import java.util.Set;
import com.comphenix.protocol.injector.netty.ProtocolRegistry;
import com.comphenix.protocol.error.ReportType;

public class PacketRegistry
{
    public static final ReportType REPORT_CANNOT_CORRECT_TROVE_MAP;
    public static final ReportType REPORT_INSUFFICIENT_SERVER_PACKETS;
    public static final ReportType REPORT_INSUFFICIENT_CLIENT_PACKETS;
    private static volatile ProtocolRegistry NETTY;
    private static volatile Set<Integer> LEGACY_SERVER_PACKETS;
    private static volatile Set<Integer> LEGACY_CLIENT_PACKETS;
    private static volatile Map<Integer, Class> LEGACY_PREVIOUS_PACKETS;
    private static volatile boolean INITIALIZED;
    
    private static void initialize() {
        if (!PacketRegistry.INITIALIZED) {
            PacketRegistry.NETTY = new NettyProtocolRegistry();
            PacketRegistry.INITIALIZED = true;
            return;
        }
        if (PacketRegistry.NETTY == null) {
            throw new IllegalStateException("Failed to initialize packet registry.");
        }
    }
    
    public static boolean isSupported(final PacketType type) {
        initialize();
        return PacketRegistry.NETTY.getPacketTypeLookup().containsKey(type);
    }
    
    @Deprecated
    public static Map<Class, Integer> getPacketToID() {
        initialize();
        final Map<Class, Integer> result = (Map<Class, Integer>)Maps.transformValues((Map)PacketRegistry.NETTY.getPacketClassLookup(), (Function)new Function<PacketType, Integer>() {
            public Integer apply(final PacketType type) {
                return type.getLegacyId();
            }
        });
        return result;
    }
    
    public static Map<Class, PacketType> getPacketToType() {
        initialize();
        final Map<Class, PacketType> result = (Map<Class, PacketType>)PacketRegistry.NETTY.getPacketClassLookup();
        return result;
    }
    
    @Deprecated
    public static Map<Integer, Class> getOverwrittenPackets() {
        initialize();
        throw new IllegalStateException("Not supported on Netty.");
    }
    
    @Deprecated
    public static Map<Integer, Class> getPreviousPackets() {
        initialize();
        if (PacketRegistry.LEGACY_PREVIOUS_PACKETS == null) {
            final Map<Integer, Class> map = (Map<Integer, Class>)Maps.newHashMap();
            for (final Map.Entry<PacketType, Class<?>> entry : PacketRegistry.NETTY.getPacketTypeLookup().entrySet()) {
                map.put(entry.getKey().getLegacyId(), entry.getValue());
            }
            PacketRegistry.LEGACY_PREVIOUS_PACKETS = (Map<Integer, Class>)Collections.unmodifiableMap((Map<? extends Integer, ? extends Class>)map);
        }
        return PacketRegistry.LEGACY_PREVIOUS_PACKETS;
    }
    
    @Deprecated
    public static Set<Integer> getServerPackets() throws FieldAccessException {
        if (PacketRegistry.LEGACY_SERVER_PACKETS == null) {
            PacketRegistry.LEGACY_SERVER_PACKETS = toLegacy(getServerPacketTypes());
        }
        return PacketRegistry.LEGACY_SERVER_PACKETS;
    }
    
    public static Set<PacketType> getServerPacketTypes() {
        initialize();
        PacketRegistry.NETTY.synchronize();
        return PacketRegistry.NETTY.getServerPackets();
    }
    
    @Deprecated
    public static Set<Integer> getClientPackets() throws FieldAccessException {
        if (PacketRegistry.LEGACY_CLIENT_PACKETS == null) {
            PacketRegistry.LEGACY_CLIENT_PACKETS = toLegacy(getClientPacketTypes());
        }
        return PacketRegistry.LEGACY_CLIENT_PACKETS;
    }
    
    public static Set<PacketType> getClientPacketTypes() {
        initialize();
        PacketRegistry.NETTY.synchronize();
        return PacketRegistry.NETTY.getClientPackets();
    }
    
    public static Set<Integer> toLegacy(final Set<PacketType> types) {
        final Set<Integer> result = (Set<Integer>)Sets.newHashSet();
        for (final PacketType type : types) {
            result.add(type.getLegacyId());
        }
        return Collections.unmodifiableSet((Set<? extends Integer>)result);
    }
    
    public static Set<PacketType> toPacketTypes(final Set<Integer> ids) {
        return toPacketTypes(ids, null);
    }
    
    public static Set<PacketType> toPacketTypes(final Set<Integer> ids, final PacketType.Sender preference) {
        final Set<PacketType> result = (Set<PacketType>)Sets.newHashSet();
        for (final int id : ids) {
            result.add(PacketType.fromLegacy(id, preference));
        }
        return Collections.unmodifiableSet((Set<? extends PacketType>)result);
    }
    
    @Deprecated
    public static Class getPacketClassFromID(final int packetID) {
        initialize();
        return PacketRegistry.NETTY.getPacketTypeLookup().get(PacketType.findLegacy(packetID));
    }
    
    public static Class getPacketClassFromType(final PacketType type) {
        return getPacketClassFromType(type, false);
    }
    
    public static Class getPacketClassFromType(final PacketType type, final boolean forceVanilla) {
        initialize();
        Class<?> clazz = PacketRegistry.NETTY.getPacketTypeLookup().get(type);
        if (clazz != null) {
            return clazz;
        }
        final String[] classNames = type.getClassNames();
        final int length = classNames.length;
        int i = 0;
        while (i < length) {
            final String name = classNames[i];
            try {
                clazz = MinecraftReflection.getMinecraftClass(name);
            }
            catch (Exception ex) {
                ++i;
                continue;
            }
            break;
        }
        return clazz;
    }
    
    @Deprecated
    public static Class getPacketClassFromID(final int packetID, final boolean forceVanilla) {
        initialize();
        return getPacketClassFromID(packetID);
    }
    
    @Deprecated
    public static int getPacketID(final Class<?> packet) {
        initialize();
        return PacketRegistry.NETTY.getPacketClassLookup().get(packet).getLegacyId();
    }
    
    public static PacketType getPacketType(final Class<?> packet) {
        return getPacketType(packet, null);
    }
    
    public static PacketType getPacketType(final Class<?> packet, final PacketType.Sender sender) {
        initialize();
        return PacketRegistry.NETTY.getPacketClassLookup().get(packet);
    }
    
    static {
        REPORT_CANNOT_CORRECT_TROVE_MAP = new ReportType("Unable to correct no entry value.");
        REPORT_INSUFFICIENT_SERVER_PACKETS = new ReportType("Too few server packets detected: %s");
        REPORT_INSUFFICIENT_CLIENT_PACKETS = new ReportType("Too few client packets detected: %s");
        PacketRegistry.INITIALIZED = false;
    }
}
