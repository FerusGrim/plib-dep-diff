package com.comphenix.protocol.injector.netty;

import com.comphenix.protocol.ProtocolLogger;
import java.util.Iterator;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.injector.packet.MapContainer;
import java.util.Map;
import com.comphenix.protocol.reflect.StructureModifier;
import com.google.common.collect.Maps;

public class NettyProtocolRegistry extends ProtocolRegistry
{
    @Override
    protected synchronized void initialize() {
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
        for (final Map<Integer, Class<?>> map : serverMaps.values()) {
            result.containers.add(new MapContainer(map));
        }
        for (final Map<Integer, Class<?>> map : clientMaps.values()) {
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
