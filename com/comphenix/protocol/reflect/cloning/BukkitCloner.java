package com.comphenix.protocol.reflect.cloning;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.comphenix.protocol.wrappers.ChunkPosition;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.BukkitConverters;
import java.util.Iterator;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.collect.Maps;
import java.lang.reflect.Constructor;
import java.util.Map;

public class BukkitCloner implements Cloner
{
    private final Map<Integer, Class<?>> clonableClasses;
    private static Constructor<?> nonNullList;
    
    public BukkitCloner() {
        this.clonableClasses = (Map<Integer, Class<?>>)Maps.newConcurrentMap();
        this.addClass(0, MinecraftReflection.getItemStackClass());
        this.addClass(1, MinecraftReflection.getDataWatcherClass());
        try {
            this.addClass(2, MinecraftReflection.getBlockPositionClass());
        }
        catch (Throwable t) {}
        try {
            this.addClass(3, MinecraftReflection.getChunkPositionClass());
        }
        catch (Throwable t2) {}
        if (MinecraftReflection.isUsingNetty()) {
            this.addClass(4, MinecraftReflection.getServerPingClass());
        }
        if (MinecraftReflection.watcherObjectExists()) {
            this.addClass(5, MinecraftReflection.getDataWatcherSerializerClass());
            this.addClass(6, MinecraftReflection.getMinecraftKeyClass());
        }
        try {
            this.addClass(7, MinecraftReflection.getIBlockDataClass());
        }
        catch (Throwable t3) {}
        try {
            this.addClass(8, MinecraftReflection.getNonNullListClass());
        }
        catch (Throwable t4) {}
    }
    
    private void addClass(final int id, final Class<?> clazz) {
        if (clazz != null) {
            this.clonableClasses.put(id, clazz);
        }
    }
    
    private int findMatchingClass(final Class<?> type) {
        for (final Map.Entry<Integer, Class<?>> entry : this.clonableClasses.entrySet()) {
            if (entry.getValue().isAssignableFrom(type)) {
                return entry.getKey();
            }
        }
        return -1;
    }
    
    @Override
    public boolean canClone(final Object source) {
        return source != null && this.findMatchingClass(source.getClass()) >= 0;
    }
    
    @Override
    public Object clone(final Object source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be NULL.");
        }
        switch (this.findMatchingClass(source.getClass())) {
            case 0: {
                return MinecraftReflection.getMinecraftItemStack(MinecraftReflection.getBukkitItemStack(source).clone());
            }
            case 1: {
                final EquivalentConverter<WrappedDataWatcher> dataConverter = BukkitConverters.getDataWatcherConverter();
                return dataConverter.getGeneric(dataConverter.getSpecific(source).deepClone());
            }
            case 2: {
                final EquivalentConverter<BlockPosition> blockConverter = BlockPosition.getConverter();
                return blockConverter.getGeneric(blockConverter.getSpecific(source));
            }
            case 3: {
                final EquivalentConverter<ChunkPosition> chunkConverter = ChunkPosition.getConverter();
                return chunkConverter.getGeneric(chunkConverter.getSpecific(source));
            }
            case 4: {
                final EquivalentConverter<WrappedServerPing> serverConverter = BukkitConverters.getWrappedServerPingConverter();
                return serverConverter.getGeneric(serverConverter.getSpecific(source).deepClone());
            }
            case 5: {
                return source;
            }
            case 6: {
                final EquivalentConverter<MinecraftKey> keyConverter = MinecraftKey.getConverter();
                return keyConverter.getGeneric(keyConverter.getSpecific(source));
            }
            case 7: {
                final EquivalentConverter<WrappedBlockData> blockDataConverter = BukkitConverters.getWrappedBlockDataConverter();
                return blockDataConverter.getGeneric(blockDataConverter.getSpecific(source).deepClone());
            }
            case 8: {
                return nonNullListCloner().clone(source);
            }
            default: {
                throw new IllegalArgumentException("Cannot clone objects of type " + source.getClass());
            }
        }
    }
    
    private static final Cloner nonNullListCloner() {
        return new Cloner() {
            @Override
            public boolean canClone(final Object source) {
                return MinecraftReflection.is(MinecraftReflection.getNonNullListClass(), source);
            }
            
            @Override
            public Object clone(final Object source) {
                final StructureModifier<Object> modifier = new StructureModifier<Object>(source.getClass(), true).withTarget(source);
                final List<?> list = modifier.read(0);
                final Object empty = modifier.read(1);
                if (BukkitCloner.nonNullList == null) {
                    try {
                        BukkitCloner.nonNullList = source.getClass().getDeclaredConstructor(List.class, Object.class);
                        BukkitCloner.nonNullList.setAccessible(true);
                    }
                    catch (ReflectiveOperationException ex) {
                        throw new RuntimeException("Could not find NonNullList constructor", ex);
                    }
                }
                try {
                    return BukkitCloner.nonNullList.newInstance(new ArrayList(list), empty);
                }
                catch (ReflectiveOperationException ex) {
                    throw new RuntimeException("Could not create new NonNullList", ex);
                }
            }
        };
    }
    
    static {
        BukkitCloner.nonNullList = null;
    }
}
