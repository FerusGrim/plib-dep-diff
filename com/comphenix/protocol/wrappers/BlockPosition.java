package com.comphenix.protocol.wrappers;

import com.google.common.base.Objects;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.EquivalentConverter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Constructor;

public class BlockPosition
{
    public static BlockPosition ORIGIN;
    private static Constructor<?> blockPositionConstructor;
    protected final int x;
    protected final int y;
    protected final int z;
    private static StructureModifier<Integer> intModifier;
    
    public BlockPosition(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public BlockPosition(final Vector vector) {
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
    
    public Location toLocation(final World world) {
        return new Location(world, (double)this.x, (double)this.y, (double)this.z);
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
    
    public BlockPosition add(final BlockPosition other) {
        if (other == null) {
            throw new IllegalArgumentException("other cannot be NULL");
        }
        return new BlockPosition(this.x + other.x, this.y + other.y, this.z + other.z);
    }
    
    public BlockPosition subtract(final BlockPosition other) {
        if (other == null) {
            throw new IllegalArgumentException("other cannot be NULL");
        }
        return new BlockPosition(this.x - other.x, this.y - other.y, this.z - other.z);
    }
    
    public BlockPosition multiply(final int factor) {
        return new BlockPosition(this.x * factor, this.y * factor, this.z * factor);
    }
    
    public BlockPosition divide(final int divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Cannot divide by null.");
        }
        return new BlockPosition(this.x / divisor, this.y / divisor, this.z / divisor);
    }
    
    public static EquivalentConverter<BlockPosition> getConverter() {
        return new EquivalentConverter<BlockPosition>() {
            @Override
            public Object getGeneric(final BlockPosition specific) {
                if (BlockPosition.blockPositionConstructor == null) {
                    try {
                        BlockPosition.blockPositionConstructor = MinecraftReflection.getBlockPositionClass().getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Cannot find block position constructor.", e);
                    }
                }
                try {
                    final Object result = BlockPosition.blockPositionConstructor.newInstance(specific.x, specific.y, specific.z);
                    return result;
                }
                catch (Exception e) {
                    throw new RuntimeException("Cannot construct BlockPosition.", e);
                }
            }
            
            @Override
            public BlockPosition getSpecific(final Object generic) {
                if (MinecraftReflection.isBlockPosition(generic)) {
                    BlockPosition.intModifier = (StructureModifier<Integer>)new StructureModifier(generic.getClass(), null, false).withType(Integer.TYPE);
                    if (BlockPosition.intModifier.size() < 3) {
                        throw new IllegalStateException("Cannot read class " + generic.getClass() + " for its integer fields.");
                    }
                    if (BlockPosition.intModifier.size() >= 3) {
                        try {
                            final StructureModifier<Integer> instance = BlockPosition.intModifier.withTarget(generic);
                            final BlockPosition result = new BlockPosition(instance.read(0), instance.read(1), instance.read(2));
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
            public Class<BlockPosition> getSpecificType() {
                return BlockPosition.class;
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
        if (obj instanceof BlockPosition) {
            final BlockPosition other = (BlockPosition)obj;
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
        return "BlockPosition [x=" + this.x + ", y=" + this.y + ", z=" + this.z + "]";
    }
    
    static {
        BlockPosition.ORIGIN = new BlockPosition(0, 0, 0);
    }
}
