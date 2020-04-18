package com.comphenix.protocol.wrappers;

import java.lang.reflect.Array;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.EquivalentConverter;
import org.bukkit.World;
import org.bukkit.Location;
import java.lang.reflect.Constructor;

public class MultiBlockChangeInfo
{
    private static Constructor<?> constructor;
    private static Class<?> nmsClass;
    private short location;
    private WrappedBlockData data;
    private ChunkCoordIntPair chunk;
    
    public MultiBlockChangeInfo(final short location, final WrappedBlockData data, final ChunkCoordIntPair chunk) {
        this.location = location;
        this.data = data;
        this.chunk = chunk;
    }
    
    public MultiBlockChangeInfo(final Location location, final WrappedBlockData data) {
        this.data = data;
        this.chunk = new ChunkCoordIntPair(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        this.setLocation(location);
    }
    
    public Location getLocation(final World world) {
        return new Location(world, (double)this.getAbsoluteX(), (double)this.getY(), (double)this.getAbsoluteZ());
    }
    
    public void setLocation(final Location location) {
        this.setLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
    
    public void setLocation(int x, final int y, int z) {
        x &= 0xF;
        z &= 0xF;
        this.location = (short)(x << 12 | z << 8 | y);
    }
    
    public int getX() {
        return this.location >> 12 & 0xF;
    }
    
    public int getAbsoluteX() {
        return (this.chunk.getChunkX() << 4) + this.getX();
    }
    
    public void setX(final int x) {
        this.setLocation(x, this.getY(), this.getZ());
    }
    
    public int getY() {
        return this.location & 0xFF;
    }
    
    public void setY(final int y) {
        this.setLocation(this.getX(), y, this.getZ());
    }
    
    public int getZ() {
        return this.location >> 8 & 0xF;
    }
    
    public int getAbsoluteZ() {
        return (this.chunk.getChunkZ() << 4) + this.getZ();
    }
    
    public void setZ(final int z) {
        this.setLocation(this.getX(), this.getY(), z);
    }
    
    public WrappedBlockData getData() {
        return this.data;
    }
    
    public void setData(final WrappedBlockData data) {
        this.data = data;
    }
    
    public ChunkCoordIntPair getChunk() {
        return this.chunk;
    }
    
    public static EquivalentConverter<MultiBlockChangeInfo> getConverter(final ChunkCoordIntPair chunk) {
        return new EquivalentConverter<MultiBlockChangeInfo>() {
            @Override
            public MultiBlockChangeInfo getSpecific(final Object generic) {
                final StructureModifier<Object> modifier = new StructureModifier<Object>(generic.getClass(), null, false).withTarget(generic);
                final StructureModifier<Short> shorts = modifier.withType(Short.TYPE);
                final short location = shorts.read(0);
                final StructureModifier<WrappedBlockData> dataModifier = modifier.withType(MinecraftReflection.getIBlockDataClass(), BukkitConverters.getWrappedBlockDataConverter());
                final WrappedBlockData data = dataModifier.read(0);
                return new MultiBlockChangeInfo(location, data, chunk);
            }
            
            @Override
            public Object getGeneric(final MultiBlockChangeInfo specific) {
                try {
                    if (MultiBlockChangeInfo.constructor == null) {
                        MultiBlockChangeInfo.constructor = (Constructor<?>)MultiBlockChangeInfo.nmsClass.getConstructor(PacketType.Play.Server.MULTI_BLOCK_CHANGE.getPacketClass(), Short.TYPE, MinecraftReflection.getIBlockDataClass());
                    }
                    return MultiBlockChangeInfo.constructor.newInstance(null, specific.location, BukkitConverters.getWrappedBlockDataConverter().getGeneric(specific.data));
                }
                catch (Throwable ex) {
                    throw new RuntimeException("Failed to construct MultiBlockChangeInfo instance.", ex);
                }
            }
            
            @Override
            public Class<MultiBlockChangeInfo> getSpecificType() {
                return MultiBlockChangeInfo.class;
            }
        };
    }
    
    public static EquivalentConverter<MultiBlockChangeInfo[]> getArrayConverter(final ChunkCoordIntPair chunk) {
        return new EquivalentConverter<MultiBlockChangeInfo[]>() {
            private final EquivalentConverter<MultiBlockChangeInfo> converter = MultiBlockChangeInfo.getConverter(chunk);
            
            @Override
            public MultiBlockChangeInfo[] getSpecific(final Object generic) {
                final Object[] array = (Object[])generic;
                final MultiBlockChangeInfo[] result = new MultiBlockChangeInfo[array.length];
                for (int i = 0; i < result.length; ++i) {
                    result[i] = this.converter.getSpecific(array[i]);
                }
                return result;
            }
            
            @Override
            public Object getGeneric(final MultiBlockChangeInfo[] specific) {
                final Object[] result = (Object[])Array.newInstance(MultiBlockChangeInfo.nmsClass, specific.length);
                for (int i = 0; i < result.length; ++i) {
                    result[i] = this.converter.getGeneric(specific[i]);
                }
                return result;
            }
            
            @Override
            public Class<MultiBlockChangeInfo[]> getSpecificType() {
                return MultiBlockChangeInfo[].class;
            }
        };
    }
    
    static {
        MultiBlockChangeInfo.nmsClass = MinecraftReflection.getMultiBlockChangeInfoClass();
    }
}
