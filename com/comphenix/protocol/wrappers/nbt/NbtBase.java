package com.comphenix.protocol.wrappers.nbt;

import com.comphenix.protocol.wrappers.ClonableWrapper;

public interface NbtBase<TType> extends ClonableWrapper
{
    boolean accept(final NbtVisitor p0);
    
    NbtType getType();
    
    String getName();
    
    void setName(final String p0);
    
    TType getValue();
    
    void setValue(final TType p0);
    
    NbtBase<TType> deepClone();
    
    default Object getHandle() {
        return null;
    }
}
