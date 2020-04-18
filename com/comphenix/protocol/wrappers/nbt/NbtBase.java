package com.comphenix.protocol.wrappers.nbt;

public interface NbtBase<TType>
{
    boolean accept(final NbtVisitor p0);
    
    NbtType getType();
    
    String getName();
    
    void setName(final String p0);
    
    TType getValue();
    
    void setValue(final TType p0);
    
    NbtBase<TType> deepClone();
}
