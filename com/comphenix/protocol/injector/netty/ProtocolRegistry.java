package com.comphenix.protocol.injector.netty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.HashBiMap;
import com.comphenix.protocol.injector.packet.MapContainer;
import java.util.List;
import com.google.common.collect.BiMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;
import com.comphenix.protocol.PacketType;
import java.util.Map;
import com.comphenix.protocol.utility.MinecraftReflection;

public abstract class ProtocolRegistry
{
    protected Class<?> enumProtocol;
    protected volatile Register register;
    
    public ProtocolRegistry() {
        this.enumProtocol = MinecraftReflection.getEnumProtocolClass();
        this.initialize();
    }
    
    public Map<PacketType, Class<?>> getPacketTypeLookup() {
        return Collections.unmodifiableMap((Map<? extends PacketType, ? extends Class<?>>)this.register.typeToClass);
    }
    
    public Map<Class<?>, PacketType> getPacketClassLookup() {
        return Collections.unmodifiableMap((Map<? extends Class<?>, ? extends PacketType>)this.register.typeToClass.inverse());
    }
    
    public Set<PacketType> getClientPackets() {
        return Collections.unmodifiableSet((Set<? extends PacketType>)this.register.clientPackets);
    }
    
    public Set<PacketType> getServerPackets() {
        return Collections.unmodifiableSet((Set<? extends PacketType>)this.register.serverPackets);
    }
    
    public synchronized void synchronize() {
        if (this.register.isOutdated()) {
            this.initialize();
        }
    }
    
    protected abstract void initialize();
    
    protected abstract void associatePackets(final Register p0, final Map<Integer, Class<?>> p1, final PacketType.Protocol p2, final PacketType.Sender p3);
    
    protected final int sum(final Iterable<? extends Map<Integer, Class<?>>> maps) {
        int count = 0;
        for (final Map<Integer, Class<?>> map : maps) {
            count += map.size();
        }
        return count;
    }
    
    protected static class Register
    {
        public BiMap<PacketType, Class<?>> typeToClass;
        public volatile Set<PacketType> serverPackets;
        public volatile Set<PacketType> clientPackets;
        public List<MapContainer> containers;
        
        public Register() {
            this.typeToClass = (BiMap<PacketType, Class<?>>)HashBiMap.create();
            this.serverPackets = (Set<PacketType>)Sets.newHashSet();
            this.clientPackets = (Set<PacketType>)Sets.newHashSet();
            this.containers = (List<MapContainer>)Lists.newArrayList();
        }
        
        public boolean isOutdated() {
            for (final MapContainer container : this.containers) {
                if (container.hasChanged()) {
                    return true;
                }
            }
            return false;
        }
    }
}
