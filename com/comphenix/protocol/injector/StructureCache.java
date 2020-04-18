package com.comphenix.protocol.injector;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import com.comphenix.protocol.reflect.compiler.CompileListener;
import com.comphenix.protocol.reflect.compiler.BackgroundCompiler;
import com.comphenix.protocol.reflect.compiler.CompiledStructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.instances.DefaultInstances;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import java.util.Set;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.PacketType;
import java.util.concurrent.ConcurrentMap;

public class StructureCache
{
    private static ConcurrentMap<PacketType, StructureModifier<Object>> structureModifiers;
    private static Set<PacketType> compiling;
    
    @Deprecated
    public static Object newPacket(final int legacyId) {
        return newPacket(PacketType.findLegacy(legacyId));
    }
    
    public static Object newPacket(final PacketType type) {
        final Class<?> clazz = (Class<?>)PacketRegistry.getPacketClassFromType(type, true);
        if (clazz == null) {
            throw new IllegalArgumentException("Cannot find associated packet class: " + type);
        }
        final Object result = DefaultInstances.DEFAULT.create(clazz);
        if (result == null) {
            throw new IllegalArgumentException("Failed to create packet for type: " + type);
        }
        return result;
    }
    
    @Deprecated
    public static StructureModifier<Object> getStructure(final int legacyId) {
        return getStructure(PacketType.findLegacy(legacyId));
    }
    
    public static StructureModifier<Object> getStructure(final PacketType type) {
        return getStructure(type, true);
    }
    
    public static StructureModifier<Object> getStructure(final Class<?> packetType) {
        return getStructure(packetType, true);
    }
    
    public static StructureModifier<Object> getStructure(final Class<?> packetType, final boolean compile) {
        return getStructure(PacketRegistry.getPacketType(packetType), compile);
    }
    
    @Deprecated
    public static StructureModifier<Object> getStructure(final int legacyId, final boolean compile) {
        return getStructure(PacketType.findLegacy(legacyId), compile);
    }
    
    public static StructureModifier<Object> getStructure(final PacketType type, final boolean compile) {
        StructureModifier<Object> result = StructureCache.structureModifiers.get(type);
        if (result == null) {
            final StructureModifier<Object> value = new StructureModifier<Object>(PacketRegistry.getPacketClassFromType(type, true), MinecraftReflection.getPacketClass(), true);
            result = StructureCache.structureModifiers.putIfAbsent(type, value);
            if (result == null) {
                result = value;
            }
        }
        if (compile && !(result instanceof CompiledStructureModifier)) {
            synchronized (StructureCache.compiling) {
                final BackgroundCompiler compiler = BackgroundCompiler.getInstance();
                if (!StructureCache.compiling.contains(type) && compiler != null) {
                    compiler.scheduleCompilation(result, new CompileListener<Object>() {
                        @Override
                        public void onCompiled(final StructureModifier<Object> compiledModifier) {
                            StructureCache.structureModifiers.put(type, compiledModifier);
                        }
                    });
                    StructureCache.compiling.add(type);
                }
            }
        }
        return result;
    }
    
    static {
        StructureCache.structureModifiers = new ConcurrentHashMap<PacketType, StructureModifier<Object>>();
        StructureCache.compiling = new HashSet<PacketType>();
    }
}
