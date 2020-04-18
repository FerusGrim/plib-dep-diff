package com.comphenix.protocol.injector.netty;

import com.comphenix.protocol.ProtocolLogger;
import java.util.HashMap;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.util.Iterator;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.injector.packet.MapContainer;
import java.util.Map;
import com.comphenix.protocol.reflect.StructureModifier;
import com.google.common.collect.Maps;
import com.comphenix.protocol.utility.MinecraftVersion;

public class NettyProtocolRegistry extends ProtocolRegistry
{
    @Override
    protected synchronized void initialize() {
        if (MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.BEE_UPDATE)) {
            this.initializeNew();
            return;
        }
        final Object[] protocols = (Object[])this.enumProtocol.getEnumConstants();
        final Map<Object, Map<Integer, Class<?>>> serverMaps = (Map<Object, Map<Integer, Class<?>>>)Maps.newLinkedHashMap();
        final Map<Object, Map<Integer, Class<?>>> clientMaps = (Map<Object, Map<Integer, Class<?>>>)Maps.newLinkedHashMap();
        final Register result = new Register();
        StructureModifier<Object> modifier = null;
        for (final Object protocol : protocols) {
            if (modifier == null) {
                modifier = new StructureModifier<Object>(protocol.getClass().getSuperclass(), false);
            }
            final StructureModifier<Map<Object, Map<Integer, Class<?>>>> maps = modifier.withTarget(protocol).withType(Map.class);
            for (final Map.Entry<Object, Map<Integer, Class<?>>> entry : maps.read(0).entrySet()) {
                final String direction = entry.getKey().toString();
                if (direction.contains("CLIENTBOUND")) {
                    serverMaps.put(protocol, entry.getValue());
                }
                else {
                    if (!direction.contains("SERVERBOUND")) {
                        continue;
                    }
                    clientMaps.put(protocol, entry.getValue());
                }
            }
        }
        for (final Object map : serverMaps.values()) {
            result.containers.add(new MapContainer(map));
        }
        for (final Object map : clientMaps.values()) {
            result.containers.add(new MapContainer(map));
        }
        for (final Object protocol : protocols) {
            final Enum<?> enumProtocol = (Enum<?>)protocol;
            final PacketType.Protocol equivalent = PacketType.Protocol.fromVanilla(enumProtocol);
            if (serverMaps.containsKey(protocol)) {
                this.associatePackets(result, serverMaps.get(protocol), equivalent, PacketType.Sender.SERVER);
            }
            if (clientMaps.containsKey(protocol)) {
                this.associatePackets(result, clientMaps.get(protocol), equivalent, PacketType.Sender.CLIENT);
            }
        }
        this.register = result;
    }
    
    private synchronized void initializeNew() {
        final Object[] protocols = (Object[])this.enumProtocol.getEnumConstants();
        final Map<Object, Map<Class<?>, Integer>> serverMaps = (Map<Object, Map<Class<?>, Integer>>)Maps.newLinkedHashMap();
        final Map<Object, Map<Class<?>, Integer>> clientMaps = (Map<Object, Map<Class<?>, Integer>>)Maps.newLinkedHashMap();
        final Register result = new Register();
        Field mainMapField = null;
        Field packetMapField = null;
        for (final Object protocol : protocols) {
            if (mainMapField == null) {
                final FuzzyReflection fuzzy = FuzzyReflection.fromClass(protocol.getClass(), true);
                mainMapField = fuzzy.getField(FuzzyFieldContract.newBuilder().banModifier(8).requireModifier(16).typeDerivedOf(Map.class).build());
                mainMapField.setAccessible(true);
            }
            Map<Object, Object> directionMap;
            try {
                directionMap = (Map<Object, Object>)mainMapField.get(protocol);
            }
            catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to access packet map", ex);
            }
            for (final Map.Entry<Object, Object> entry : directionMap.entrySet()) {
                final Object holder = entry.getValue();
                if (packetMapField == null) {
                    final FuzzyReflection fuzzy2 = FuzzyReflection.fromClass(holder.getClass(), true);
                    packetMapField = fuzzy2.getField(FuzzyFieldContract.newBuilder().banModifier(8).requireModifier(16).typeDerivedOf(Map.class).build());
                    packetMapField.setAccessible(true);
                }
                Map<Class<?>, Integer> packetMap;
                try {
                    packetMap = (Map<Class<?>, Integer>)packetMapField.get(holder);
                }
                catch (ReflectiveOperationException ex2) {
                    throw new RuntimeException("Failed to access packet map", ex2);
                }
                final String direction = entry.getKey().toString();
                if (direction.contains("CLIENTBOUND")) {
                    serverMaps.put(protocol, packetMap);
                }
                else {
                    if (!direction.contains("SERVERBOUND")) {
                        continue;
                    }
                    clientMaps.put(protocol, packetMap);
                }
            }
        }
        for (final Object protocol : protocols) {
            final Enum<?> enumProtocol = (Enum<?>)protocol;
            final PacketType.Protocol equivalent = PacketType.Protocol.fromVanilla(enumProtocol);
            if (serverMaps.containsKey(protocol)) {
                this.associatePackets(result, this.reverse(serverMaps.get(protocol)), equivalent, PacketType.Sender.SERVER);
            }
            if (clientMaps.containsKey(protocol)) {
                this.associatePackets(result, this.reverse(clientMaps.get(protocol)), equivalent, PacketType.Sender.CLIENT);
            }
        }
        this.register = result;
    }
    
    private <K, V> Map<V, K> reverse(final Map<K, V> map) {
        final Map<V, K> newMap = new HashMap<V, K>(map.size());
        for (final Map.Entry<K, V> entry : map.entrySet()) {
            newMap.put(entry.getValue(), entry.getKey());
        }
        return newMap;
    }
    
    @Override
    protected void associatePackets(final Register register, final Map<Integer, Class<?>> lookup, final PacketType.Protocol protocol, final PacketType.Sender sender) {
        for (final Map.Entry<Integer, Class<?>> entry : lookup.entrySet()) {
            final PacketType type = PacketType.fromCurrent(protocol, sender, entry.getKey(), entry.getValue());
            try {
                register.typeToClass.put((Object)type, (Object)entry.getValue());
                if (sender == PacketType.Sender.SERVER) {
                    register.serverPackets.add(type);
                }
                if (sender != PacketType.Sender.CLIENT) {
                    continue;
                }
                register.clientPackets.add(type);
            }
            catch (Exception ex) {
                ProtocolLogger.debug("Encountered an exception associating packet " + type, ex);
            }
        }
    }
}
