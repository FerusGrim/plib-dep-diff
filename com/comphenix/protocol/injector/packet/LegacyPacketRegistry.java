package com.comphenix.protocol.injector.packet;

import com.comphenix.net.sf.cglib.proxy.Factory;
import com.google.common.collect.Iterables;
import com.comphenix.protocol.wrappers.TroveWrapper;
import com.google.common.base.Function;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.fuzzy.FuzzyClassContract;
import java.util.List;
import java.util.Collection;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.lang.reflect.Field;
import com.comphenix.protocol.utility.MinecraftReflection;
import javax.annotation.Nullable;
import com.google.common.base.Predicate;
import com.comphenix.protocol.reflect.FieldUtils;
import java.util.HashMap;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Map;
import com.comphenix.protocol.reflect.FuzzyReflection;

class LegacyPacketRegistry
{
    private static final int MIN_SERVER_PACKETS = 5;
    private static final int MIN_CLIENT_PACKETS = 5;
    private FuzzyReflection packetRegistry;
    private Map<Class, Integer> packetToID;
    private Multimap<Integer, Class> customIdToPacket;
    private Map<Integer, Class> vanillaIdToPacket;
    private ImmutableSet<Integer> serverPackets;
    private ImmutableSet<Integer> clientPackets;
    private Set<Integer> serverPacketsRef;
    private Set<Integer> clientPacketsRef;
    private Map<Integer, Class> overwrittenPackets;
    private Map<Integer, Class> previousValues;
    
    LegacyPacketRegistry() {
        this.overwrittenPackets = new HashMap<Integer, Class>();
        this.previousValues = new HashMap<Integer, Class>();
    }
    
    public void initialize() {
        if (this.packetToID == null) {
            try {
                final Field packetsField = this.getPacketRegistry().getFieldByType("packetsField", Map.class);
                this.packetToID = (Map<Class, Integer>)FieldUtils.readStaticField(packetsField, true);
            }
            catch (IllegalArgumentException e) {
                try {
                    this.packetToID = this.getSpigotWrapper();
                }
                catch (Exception e2) {
                    throw new IllegalArgumentException(e.getMessage() + "; Spigot workaround failed.", e2);
                }
            }
            catch (IllegalAccessException e3) {
                throw new RuntimeException("Unable to retrieve the packetClassToIdMap", e3);
            }
            this.customIdToPacket = (Multimap<Integer, Class>)InverseMaps.inverseMultimap(this.packetToID, (com.google.common.base.Predicate<Map.Entry<Class, Integer>>)new Predicate<Map.Entry<Class, Integer>>() {
                public boolean apply(@Nullable final Map.Entry<Class, Integer> entry) {
                    return !MinecraftReflection.isMinecraftClass(entry.getKey());
                }
            });
            this.vanillaIdToPacket = (Map<Integer, Class>)InverseMaps.inverseMap(this.packetToID, (com.google.common.base.Predicate<Map.Entry<Class, Integer>>)new Predicate<Map.Entry<Class, Integer>>() {
                public boolean apply(@Nullable final Map.Entry<Class, Integer> entry) {
                    return MinecraftReflection.isMinecraftClass(entry.getKey());
                }
            });
        }
        this.initializeSets();
    }
    
    private void initializeSets() throws FieldAccessException {
        if (this.serverPacketsRef == null || this.clientPacketsRef == null) {
            final List<Field> sets = this.getPacketRegistry().getFieldListByType(Set.class);
            try {
                if (sets.size() <= 1) {
                    throw new FieldAccessException("Cannot retrieve packet client/server sets.");
                }
                this.serverPacketsRef = (Set<Integer>)FieldUtils.readStaticField(sets.get(0), true);
                this.clientPacketsRef = (Set<Integer>)FieldUtils.readStaticField(sets.get(1), true);
                if (this.serverPacketsRef == null || this.clientPacketsRef == null) {
                    throw new FieldAccessException("Packet sets are in an illegal state.");
                }
                this.serverPackets = (ImmutableSet<Integer>)ImmutableSet.copyOf((Collection)this.serverPacketsRef);
                this.clientPackets = (ImmutableSet<Integer>)ImmutableSet.copyOf((Collection)this.clientPacketsRef);
                if (this.serverPackets.size() < 5) {
                    throw new InsufficientPacketsException("Insufficient server packets.", false, this.serverPackets.size());
                }
                if (this.clientPackets.size() < 5) {
                    throw new InsufficientPacketsException("Insufficient client packets.", true, this.clientPackets.size());
                }
            }
            catch (IllegalAccessException e) {
                throw new FieldAccessException("Cannot access field.", e);
            }
        }
        else {
            if (this.serverPacketsRef != null && this.serverPacketsRef.size() != this.serverPackets.size()) {
                this.serverPackets = (ImmutableSet<Integer>)ImmutableSet.copyOf((Collection)this.serverPacketsRef);
            }
            if (this.clientPacketsRef != null && this.clientPacketsRef.size() != this.clientPackets.size()) {
                this.clientPackets = (ImmutableSet<Integer>)ImmutableSet.copyOf((Collection)this.clientPacketsRef);
            }
        }
    }
    
    public Map<Class, Integer> getPacketToID() {
        if (this.packetToID == null) {
            this.initialize();
        }
        return this.packetToID;
    }
    
    private Map<Class, Integer> getSpigotWrapper() throws IllegalAccessException {
        final FuzzyClassContract mapLike = FuzzyClassContract.newBuilder().method(FuzzyMethodContract.newBuilder().nameExact("size").returnTypeExact(Integer.TYPE)).method(FuzzyMethodContract.newBuilder().nameExact("put").parameterCount(2)).method(FuzzyMethodContract.newBuilder().nameExact("get").parameterCount(1)).build();
        final Field packetsField = this.getPacketRegistry().getField(FuzzyFieldContract.newBuilder().typeMatches(mapLike).build());
        final Object troveMap = FieldUtils.readStaticField(packetsField, true);
        TroveWrapper.transformNoEntryValue(troveMap, (Function<Integer, Integer>)new Function<Integer, Integer>() {
            public Integer apply(final Integer value) {
                if (value >= 0 && value < 256) {
                    return -1;
                }
                return value;
            }
        });
        return (Map<Class, Integer>)TroveWrapper.getDecoratedMap(troveMap);
    }
    
    private FuzzyReflection getPacketRegistry() {
        if (this.packetRegistry == null) {
            this.packetRegistry = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass(), true);
        }
        return this.packetRegistry;
    }
    
    public Map<Integer, Class> getOverwrittenPackets() {
        return this.overwrittenPackets;
    }
    
    public Map<Integer, Class> getPreviousPackets() {
        return this.previousValues;
    }
    
    public Set<Integer> getServerPackets() throws FieldAccessException {
        this.initializeSets();
        if (this.serverPackets != null && this.serverPackets.size() < 5) {
            throw new FieldAccessException("Server packet list is empty. Seems to be unsupported");
        }
        return (Set<Integer>)this.serverPackets;
    }
    
    public Set<Integer> getClientPackets() throws FieldAccessException {
        this.initializeSets();
        if (this.clientPackets != null && this.clientPackets.size() < 5) {
            throw new FieldAccessException("Client packet list is empty. Seems to be unsupported");
        }
        return (Set<Integer>)this.clientPackets;
    }
    
    public Class getPacketClassFromID(final int packetID) {
        return this.getPacketClassFromID(packetID, false);
    }
    
    public Class getPacketClassFromID(final int packetID, final boolean forceVanilla) {
        final Map<Integer, Class> lookup = forceVanilla ? this.previousValues : this.overwrittenPackets;
        Class<?> result = null;
        if (lookup.containsKey(packetID)) {
            return removeEnhancer(lookup.get(packetID), forceVanilla);
        }
        this.getPacketToID();
        if (!forceVanilla) {
            result = (Class<?>)Iterables.getFirst((Iterable)this.customIdToPacket.get((Object)packetID), (Object)null);
        }
        if (result == null) {
            result = this.vanillaIdToPacket.get(packetID);
        }
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("The packet ID " + packetID + " is not registered.");
    }
    
    public int getPacketID(final Class<?> packet) {
        if (packet == null) {
            throw new IllegalArgumentException("Packet type class cannot be NULL.");
        }
        if (!MinecraftReflection.getPacketClass().isAssignableFrom(packet)) {
            throw new IllegalArgumentException("Type must be a packet.");
        }
        return this.getPacketToID().get(packet);
    }
    
    private static Class removeEnhancer(Class clazz, final boolean remove) {
        if (remove) {
            while (Factory.class.isAssignableFrom(clazz) && !clazz.equals(Object.class)) {
                clazz = clazz.getSuperclass();
            }
        }
        return clazz;
    }
    
    public static class InsufficientPacketsException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private final boolean client;
        private final int packetCount;
        
        private InsufficientPacketsException(final String message, final boolean client, final int packetCount) {
            super(message);
            this.client = client;
            this.packetCount = packetCount;
        }
        
        public boolean isClient() {
            return this.client;
        }
        
        public int getPacketCount() {
            return this.packetCount;
        }
    }
}
