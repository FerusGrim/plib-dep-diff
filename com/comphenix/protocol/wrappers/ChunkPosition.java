package com.comphenix.protocol.wrappers;

import com.google.common.base.Objects;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.EquivalentConverter;
import org.bukkit.util.Vector;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Constructor;

public class ChunkPosition
{
    public static ChunkPosition ORIGIN;
    private static Constructor<?> chunkPositionConstructor;
    protected final int x;
    protected final int y;
    protected final int z;
    private static StructureModifier<Integer> intModifier;
    
    public ChunkPosition(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public ChunkPosition(final Vector vector) {
        if (vector == null) {
            throw new IllegalArgumentException("Vector cannot be NULL.");
        }
        this.x = vector.getBlockX();
        this.y = vector.getBlockY();
        this.z = vector.getBlockZ();
    }
    
    public Vector toVector() {
        return new Vector(this.x, this.y, this.z);
    }
    
    public int getX() {
        return this.x;
    }
    
    public int getY() {
        return this.y;
    }
    
    public int getZ() {
        return this.z;
    }
    
    public ChunkPosition add(final ChunkPosition other) {
        if (other == null) {
            throw new IllegalArgumentException("other cannot be NULL");
        }
        return new ChunkPosition(this.x + other.x, this.y + other.y, this.z + other.z);
    }
    
    public ChunkPosition subtract(final ChunkPosition other) {
        if (other == null) {
            throw new IllegalArgumentException("other cannot be NULL");
        }
        return new ChunkPosition(this.x - other.x, this.y - other.y, this.z - other.z);
    }
    
    public ChunkPosition multiply(final int factor) {
        return new ChunkPosition(this.x * factor, this.y * factor, this.z * factor);
    }
    
    public ChunkPosition divide(final int divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Cannot divide by null.");
        }
        return new ChunkPosition(this.x / divisor, this.y / divisor, this.z / divisor);
    }
    
    public static EquivalentConverter<ChunkPosition> getConverter() {
        return new EquivalentConverter<ChunkPosition>() {
            @Override
            public Object getGeneric(final ChunkPosition specific) {
                if (ChunkPosition.chunkPositionConstructor == null) {
                    try {
                        ChunkPosition.chunkPositionConstructor = MinecraftReflection.getChunkPositionClass().getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Cannot find chunk position constructor.", e);
                    }
                }
                try {
                    final Object result = ChunkPosition.chunkPositionConstructor.newInstance(specific.x, specific.y, specific.z);
                    return result;
                }
                catch (Exception e) {
                    throw new RuntimeException("Cannot construct ChunkPosition.", e);
                }
            }
            
            @Override
            public ChunkPosition getSpecific(final Object generic) {
                if (MinecraftReflection.isChunkPosition(generic)) {
                    ChunkPosition.intModifier = (StructureModifier<Integer>)new StructureModifier(generic.getClass(), null, false).withType(Integer.TYPE);
                    if (ChunkPosition.intModifier.size() < 3) {
                        throw new IllegalStateException("Cannot read class " + generic.getClass() + " for its integer fields.");
                    }
                    if (ChunkPosition.intModifier.size() >= 3) {
                        try {
                            final StructureModifier<Integer> instance = ChunkPosition.intModifier.withTarget(generic);
                            final ChunkPosition result = new ChunkPosition(instance.read(0), instance.read(1), instance.read(2));
                            return result;
                        }
                        catch (FieldAccessException e) {
                            throw new RuntimeException("Field access error.", e);
                        }
                    }
                }
                return null;
            }
            
            @Override
            public Class<ChunkPosition> getSpecificType() {
                return ChunkPosition.class;
            }
        };
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ChunkPosition) {
            final ChunkPosition other = (ChunkPosition)obj;
            return this.x == other.x && this.y == other.y && this.z == other.z;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.x, this.y, this.z });
    }
    
    @Override
    public String toString() {
        return "WrappedChunkPosition [x=" + this.x + ", y=" + this.y + ", z=" + this.z + "]";
    }
    
    static {
        ChunkPosition.ORIGIN = new ChunkPosition(0, 0, 0);
    }
}
