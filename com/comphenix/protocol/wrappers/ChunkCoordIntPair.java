package com.comphenix.protocol.wrappers;

import com.google.common.base.Objects;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;

public class ChunkCoordIntPair
{
    private static Class<?> COORD_PAIR_CLASS;
    private static ConstructorAccessor COORD_CONSTRUCTOR;
    private static FieldAccessor COORD_X;
    private static FieldAccessor COORD_Z;
    protected final int chunkX;
    protected final int chunkZ;
    
    public ChunkCoordIntPair(final int x, final int z) {
        this.chunkX = x;
        this.chunkZ = z;
    }
    
    public ChunkPosition getPosition(final int y) {
        return new ChunkPosition((this.chunkX << 4) + 8, y, (this.chunkZ << 4) + 8);
    }
    
    public int getChunkX() {
        return this.chunkX;
    }
    
    public int getChunkZ() {
        return this.chunkZ;
    }
    
    public static EquivalentConverter<ChunkCoordIntPair> getConverter() {
        return new EquivalentConverter<ChunkCoordIntPair>() {
            @Override
            public Object getGeneric(final ChunkCoordIntPair specific) {
                if (ChunkCoordIntPair.COORD_CONSTRUCTOR == null) {
                    ChunkCoordIntPair.COORD_CONSTRUCTOR = Accessors.getConstructorAccessor(ChunkCoordIntPair.COORD_PAIR_CLASS, Integer.TYPE, Integer.TYPE);
                }
                return ChunkCoordIntPair.COORD_CONSTRUCTOR.invoke(specific.chunkX, specific.chunkZ);
            }
            
            @Override
            public ChunkCoordIntPair getSpecific(final Object generic) {
                if (MinecraftReflection.isChunkCoordIntPair(generic)) {
                    if (ChunkCoordIntPair.COORD_X == null || ChunkCoordIntPair.COORD_Z == null) {
                        final FieldAccessor[] ints = Accessors.getFieldAccessorArray(ChunkCoordIntPair.COORD_PAIR_CLASS, Integer.TYPE, true);
                        ChunkCoordIntPair.COORD_X = ints[0];
                        ChunkCoordIntPair.COORD_Z = ints[1];
                    }
                    return new ChunkCoordIntPair((int)ChunkCoordIntPair.COORD_X.get(generic), (int)ChunkCoordIntPair.COORD_Z.get(generic));
                }
                return null;
            }
            
            @Override
            public Class<ChunkCoordIntPair> getSpecificType() {
                return ChunkCoordIntPair.class;
            }
        };
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChunkCoordIntPair) {
            final ChunkCoordIntPair other = (ChunkCoordIntPair)obj;
            return this.chunkX == other.chunkX && this.chunkZ == other.chunkZ;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.chunkX, this.chunkZ });
    }
    
    @Override
    public String toString() {
        return "ChunkCoordIntPair [x=" + this.chunkX + ", z=" + this.chunkZ + "]";
    }
    
    static {
        ChunkCoordIntPair.COORD_PAIR_CLASS = MinecraftReflection.getChunkCoordIntPair();
    }
}
