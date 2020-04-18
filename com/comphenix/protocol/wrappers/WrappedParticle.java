package com.comphenix.protocol.wrappers;

import org.bukkit.Color;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.Particle;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

public class WrappedParticle<T>
{
    private static MethodAccessor toBukkit;
    private static MethodAccessor toNMS;
    private static MethodAccessor toCraftData;
    private final Particle particle;
    private final T data;
    private final Object handle;
    
    private static void ensureMethods() {
        if (WrappedParticle.toBukkit != null && WrappedParticle.toNMS != null) {
            return;
        }
        FuzzyReflection fuzzy = FuzzyReflection.fromClass(MinecraftReflection.getCraftBukkitClass("CraftParticle"));
        FuzzyMethodContract contract = FuzzyMethodContract.newBuilder().requireModifier(8).returnTypeExact(Particle.class).parameterExactType(MinecraftReflection.getMinecraftClass("ParticleParam")).build();
        WrappedParticle.toBukkit = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
        contract = FuzzyMethodContract.newBuilder().requireModifier(8).returnTypeExact(MinecraftReflection.getMinecraftClass("ParticleParam")).parameterCount(2).build();
        WrappedParticle.toNMS = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
        final Class<?> cbData = MinecraftReflection.getCraftBukkitClass("block.data.CraftBlockData");
        fuzzy = FuzzyReflection.fromClass(cbData);
        contract = FuzzyMethodContract.newBuilder().requireModifier(8).returnTypeExact(cbData).parameterExactArray(MinecraftReflection.getIBlockDataClass()).build();
        WrappedParticle.toCraftData = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
    }
    
    private WrappedParticle(final Object handle, final Particle particle, final T data) {
        this.handle = handle;
        this.particle = particle;
        this.data = data;
    }
    
    public Particle getParticle() {
        return this.particle;
    }
    
    public T getData() {
        return this.data;
    }
    
    public Object getHandle() {
        return this.handle;
    }
    
    public static WrappedParticle fromHandle(final Object handle) {
        ensureMethods();
        final Particle bukkit = (Particle)WrappedParticle.toBukkit.invoke(null, handle);
        Object data = null;
        switch (bukkit) {
            case BLOCK_CRACK:
            case BLOCK_DUST:
            case FALLING_DUST: {
                data = getBlockData(handle);
                break;
            }
            case ITEM_CRACK: {
                data = getItem(handle);
                break;
            }
            case REDSTONE: {
                data = getRedstone(handle);
                break;
            }
        }
        return new WrappedParticle(handle, bukkit, (T)data);
    }
    
    private static WrappedBlockData getBlockData(final Object handle) {
        return (WrappedBlockData)new StructureModifier(handle.getClass()).withTarget(handle).withType(MinecraftReflection.getIBlockDataClass(), BukkitConverters.getWrappedBlockDataConverter()).read(0);
    }
    
    private static Object getItem(final Object handle) {
        return new StructureModifier(handle.getClass()).withTarget(handle).withType(MinecraftReflection.getItemStackClass(), BukkitConverters.getItemStackConverter()).read(0);
    }
    
    private static Object getRedstone(final Object handle) {
        final StructureModifier<Float> modifier = (StructureModifier<Float>)new StructureModifier(handle.getClass()).withTarget(handle).withType(Float.TYPE);
        return new Particle.DustOptions(Color.fromRGB((int)(modifier.read(0) * 255.0f), (int)(modifier.read(1) * 255.0f), (int)(modifier.read(2) * 255.0f)), (float)modifier.read(3));
    }
    
    public static <T> WrappedParticle<T> create(final Particle particle, final T data) {
        ensureMethods();
        Object bukkitData = data;
        if (data instanceof WrappedBlockData) {
            final WrappedBlockData blockData = (WrappedBlockData)data;
            bukkitData = WrappedParticle.toCraftData.invoke(null, blockData.getHandle());
        }
        final Object handle = WrappedParticle.toNMS.invoke(null, particle, bukkitData);
        return new WrappedParticle<T>(handle, particle, data);
    }
}
