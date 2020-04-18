package com.comphenix.protocol.wrappers.nbt;

import com.comphenix.protocol.wrappers.ClonableWrapper;

class MemoryElement<TType> implements NbtBase<TType>
{
    private String name;
    private TType value;
    private NbtType type;
    
    public MemoryElement(final String name, final TType value) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be NULL.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Element cannot be NULL.");
        }
        this.name = name;
        this.value = value;
        this.type = NbtType.getTypeFromClass(value.getClass());
    }
    
    public MemoryElement(final String name, final TType value, final NbtType type) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be NULL.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be NULL.");
        }
        this.name = name;
        this.value = value;
        this.type = type;
    }
    
    @Override
    public boolean accept(final NbtVisitor visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public NbtType getType() {
        return this.type;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public void setName(final String name) {
        this.name = name;
    }
    
    @Override
    public TType getValue() {
        return this.value;
    }
    
    @Override
    public void setValue(final TType newValue) {
        this.value = newValue;
    }
    
    @Override
    public NbtBase<TType> deepClone() {
        return new MemoryElement(this.name, this.value, this.type);
    }
}
