package com.comphenix.protocol.wrappers.nbt;

import java.io.DataOutput;

public interface NbtWrapper<TType> extends NbtBase<TType>
{
    Object getHandle();
    
    void write(final DataOutput p0);
}
