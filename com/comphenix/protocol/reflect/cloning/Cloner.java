package com.comphenix.protocol.reflect.cloning;

public interface Cloner
{
    boolean canClone(final Object p0);
    
    Object clone(final Object p0);
}
