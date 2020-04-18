package com.comphenix.protocol.reflect;

import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import com.comphenix.protocol.injector.StructureCache;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.concurrent.ConcurrentMap;

public class ObjectWriter
{
    private static ConcurrentMap<Class, StructureModifier<Object>> cache;
    
    private StructureModifier<Object> getModifier(final Class<?> type) {
        final Class<?> packetClass = MinecraftReflection.getPacketClass();
        if (!type.equals(packetClass) && packetClass.isAssignableFrom(type)) {
            return StructureCache.getStructure(type);
        }
        StructureModifier<Object> modifier = ObjectWriter.cache.get(type);
        if (modifier == null) {
            final StructureModifier<Object> value = new StructureModifier<Object>(type, null, false);
            modifier = ObjectWriter.cache.putIfAbsent(type, value);
            if (modifier == null) {
                modifier = value;
            }
        }
        return modifier;
    }
    
    public void copyTo(final Object source, final Object destination, final Class<?> commonType) {
        this.copyToInternal(source, destination, commonType, true);
    }
    
    protected void transformField(final StructureModifier<Object> modifierSource, final StructureModifier<Object> modifierDest, final int fieldIndex) {
        final Object value = modifierSource.read(fieldIndex);
        modifierDest.write(fieldIndex, value);
    }
    
    private void copyToInternal(final Object source, final Object destination, final Class<?> commonType, final boolean copyPublic) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be NULL");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination cannot be NULL");
        }
        final StructureModifier<Object> modifier = this.getModifier(commonType);
        final StructureModifier<Object> modifierSource = modifier.withTarget(source);
        final StructureModifier<Object> modifierDest = modifier.withTarget(destination);
        try {
            for (int i = 0; i < modifierSource.size(); ++i) {
                final Field field = modifierSource.getField(i);
                final int mod = field.getModifiers();
                if (!Modifier.isStatic(mod) && (!Modifier.isPublic(mod) || copyPublic)) {
                    this.transformField(modifierSource, modifierDest, i);
                }
            }
            final Class<?> superclass = commonType.getSuperclass();
            if (superclass != null && !superclass.equals(Object.class)) {
                this.copyToInternal(source, destination, superclass, false);
            }
        }
        catch (FieldAccessException e) {
            throw new RuntimeException("Unable to copy fields from " + commonType.getName(), e);
        }
    }
    
    static {
        ObjectWriter.cache = new ConcurrentHashMap<Class, StructureModifier<Object>>();
    }
}
