package com.comphenix.protocol.reflect.cloning;

import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.comphenix.protocol.wrappers.ChunkPosition;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.Iterator;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.wrappers.ClonableWrapper;
import java.util.function.Supplier;
import java.lang.reflect.Constructor;
import java.util.function.Function;
import java.util.Map;

public class BukkitCloner implements Cloner
{
    private static final Map<Class<?>, Function<Object, Object>> CLONERS;
    private static Constructor<?> nonNullList;
    
    private static void fromWrapper(final Supplier<Class<?>> getClass, final Function<Object, ClonableWrapper> fromHandle) {
        try {
            final Class<?> nmsClass = getClass.get();
            if (nmsClass != null) {
                BukkitCloner.CLONERS.put(nmsClass, nmsObject -> fromHandle.apply(nmsObject).deepClone().getHandle());
            }
        }
        catch (RuntimeException ex) {}
    }
    
    private static void fromConverter(final Supplier<Class<?>> getClass, final EquivalentConverter converter) {
        try {
            final Class<?> nmsClass = getClass.get();
            if (nmsClass != null) {
                BukkitCloner.CLONERS.put(nmsClass, nmsObject -> converter.getGeneric(converter.getSpecific(nmsObject)));
            }
        }
        catch (RuntimeException ex) {}
    }
    
    private static void fromManual(final Supplier<Class<?>> getClass, final Function<Object, Object> cloner) {
        try {
            final Class<?> nmsClass = getClass.get();
            if (nmsClass != null) {
                BukkitCloner.CLONERS.put(nmsClass, cloner);
            }
        }
        catch (RuntimeException ex) {}
    }
    
    private Function<Object, Object> findCloner(final Class<?> type) {
        for (final Map.Entry<Class<?>, Function<Object, Object>> entry : BukkitCloner.CLONERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    @Override
    public boolean canClone(final Object source) {
        return source != null && this.findCloner(source.getClass()) != null;
    }
    
    @Override
    public Object clone(final Object source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be NULL.");
        }
        return this.findCloner(source.getClass()).apply(source);
    }
    
    private static Cloner nonNullListCloner() {
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
        CLONERS = Maps.newConcurrentMap();
        fromManual(MinecraftReflection::getItemStackClass, source -> MinecraftReflection.getMinecraftItemStack(MinecraftReflection.getBukkitItemStack(source).clone()));
        fromWrapper(MinecraftReflection::getDataWatcherClass, (Function<Object, ClonableWrapper>)WrappedDataWatcher::new);
        fromConverter(MinecraftReflection::getBlockPositionClass, BlockPosition.getConverter());
        fromConverter(MinecraftReflection::getChunkPositionClass, ChunkPosition.getConverter());
        fromWrapper(MinecraftReflection::getServerPingClass, (Function<Object, ClonableWrapper>)WrappedServerPing::fromHandle);
        fromConverter(MinecraftReflection::getMinecraftKeyClass, MinecraftKey.getConverter());
        fromWrapper(MinecraftReflection::getIBlockDataClass, (Function<Object, ClonableWrapper>)WrappedBlockData::fromHandle);
        fromManual(MinecraftReflection::getNonNullListClass, source -> nonNullListCloner().clone(source));
        fromWrapper(MinecraftReflection::getNBTBaseClass, (Function<Object, ClonableWrapper>)NbtFactory::fromNMS);
        BukkitCloner.nonNullList = null;
    }
}
