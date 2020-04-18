package com.comphenix.protocol.reflect;

public interface EquivalentConverter<T>
{
    Object getGeneric(final T p0);
    
    T getSpecific(final Object p0);
    
    Class<T> getSpecificType();
}
