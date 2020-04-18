package com.comphenix.protocol.wrappers.nbt;

import java.io.DataOutput;
import com.comphenix.protocol.wrappers.ClonableWrapper;

public interface NbtWrapper<TType> extends NbtBase<TType>, ClonableWrapper
{
    Object getHandle();
    
    void write(final DataOutput p0);
}
