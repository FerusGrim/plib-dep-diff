package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.StructureModifier;

public class WrappedChunkCoordinate extends AbstractWrapper implements Comparable<WrappedChunkCoordinate>
{
    private static final boolean LARGER_THAN_NULL = true;
    private static StructureModifier<Integer> SHARED_MODIFIER;
    private StructureModifier<Integer> handleModifier;
    
    public WrappedChunkCoordinate() {
        super(MinecraftReflection.getChunkCoordinatesClass());
        try {
            this.setHandle(this.getHandleType().newInstance());
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot construct chunk coordinate.");
        }
    }
    
    public WrappedChunkCoordinate(final Comparable handle) {
        super(MinecraftReflection.getChunkCoordinatesClass());
        this.setHandle(handle);
    }
    
    private StructureModifier<Integer> getModifier() {
        if (WrappedChunkCoordinate.SHARED_MODIFIER == null) {
            WrappedChunkCoordinate.SHARED_MODIFIER = (StructureModifier<Integer>)new StructureModifier(this.handle.getClass(), null, false).withType(Integer.TYPE);
        }
        if (this.handleModifier == null) {
            this.handleModifier = WrappedChunkCoordinate.SHARED_MODIFIER.withTarget(this.handle);
        }
        return this.handleModifier;
    }
    
    public WrappedChunkCoordinate(final int x, final int y, final int z) {
        this();
        this.setX(x);
        this.setY(y);
        this.setZ(z);
    }
    
    public WrappedChunkCoordinate(final ChunkPosition position) {
        this(position.getX(), position.getY(), position.getZ());
    }
    
    @Override
    public Object getHandle() {
        return this.handle;
    }
    
    public int getX() {
        return this.getModifier().read(0);
    }
    
    public void setX(final int newX) {
        this.getModifier().write(0, newX);
    }
    
    public int getY() {
        return this.getModifier().read(1);
    }
    
    public void setY(final int newY) {
        this.getModifier().write(1, newY);
    }
    
    public int getZ() {
        return this.getModifier().read(2);
    }
    
    public void setZ(final int newZ) {
        this.getModifier().write(2, newZ);
    }
    
    public ChunkPosition toPosition() {
        return new ChunkPosition(this.getX(), this.getY(), this.getZ());
    }
    
    @Override
    public int compareTo(final WrappedChunkCoordinate other) {
        if (other.handle == null) {
            return -1;
        }
        return ((Comparable)this.handle).compareTo(other.handle);
    }
    
    @Override
    public String toString() {
        return String.format("ChunkCoordinate [x: %s, y: %s, z: %s]", this.getX(), this.getY(), this.getZ());
    }
}
