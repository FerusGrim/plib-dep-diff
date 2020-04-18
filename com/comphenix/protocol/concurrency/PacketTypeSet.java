package com.comphenix.protocol.concurrency;

import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.Collections;
import com.google.common.collect.Maps;
import com.comphenix.protocol.PacketType;
import java.util.Set;

public class PacketTypeSet
{
    private Set<PacketType> types;
    private Set<Class<?>> classes;
    
    public PacketTypeSet() {
        this.types = Collections.newSetFromMap((Map<PacketType, Boolean>)Maps.newConcurrentMap());
        this.classes = Collections.newSetFromMap((Map<Class<?>, Boolean>)Maps.newConcurrentMap());
    }
    
    public PacketTypeSet(final Collection<? extends PacketType> values) {
        this.types = Collections.newSetFromMap((Map<PacketType, Boolean>)Maps.newConcurrentMap());
        this.classes = Collections.newSetFromMap((Map<Class<?>, Boolean>)Maps.newConcurrentMap());
        for (final PacketType type : values) {
            this.addType(type);
        }
    }
    
    public synchronized void addType(final PacketType type) {
        final Class<?> packetClass = this.getPacketClass(type);
        this.types.add((PacketType)Preconditions.checkNotNull((Object)type, (Object)"type cannot be NULL."));
        if (packetClass != null) {
            this.classes.add(this.getPacketClass(type));
        }
    }
    
    public synchronized void addAll(final Iterable<? extends PacketType> types) {
        for (final PacketType type : types) {
            this.addType(type);
        }
    }
    
    public synchronized void removeType(final PacketType type) {
        final Class<?> packetClass = this.getPacketClass(type);
        this.types.remove(Preconditions.checkNotNull((Object)type, (Object)"type cannot be NULL."));
        if (packetClass != null) {
            this.classes.remove(this.getPacketClass(type));
        }
    }
    
    public synchronized void removeAll(final Iterable<? extends PacketType> types) {
        for (final PacketType type : types) {
            this.removeType(type);
        }
    }
    
    protected Class<?> getPacketClass(final PacketType type) {
        return (Class<?>)PacketRegistry.getPacketClassFromType(type);
    }
    
    public boolean contains(final PacketType type) {
        return this.types.contains(type);
    }
    
    public boolean contains(final Class<?> packetClass) {
        return this.classes.contains(packetClass);
    }
    
    public boolean containsPacket(final Object packet) {
        return packet != null && this.classes.contains(packet.getClass());
    }
    
    public Set<PacketType> values() {
        return this.types;
    }
    
    public int size() {
        return this.types.size();
    }
    
    public synchronized void clear() {
        this.types.clear();
        this.classes.clear();
    }
}
