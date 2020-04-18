package com.comphenix.protocol.wrappers.nbt;

public interface NbtVisitor
{
    boolean visit(final NbtBase<?> p0);
    
    boolean visitEnter(final NbtList<?> p0);
    
    boolean visitEnter(final NbtCompound p0);
    
    boolean visitLeave(final NbtList<?> p0);
    
    boolean visitLeave(final NbtCompound p0);
}
