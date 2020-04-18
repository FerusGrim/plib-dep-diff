package com.comphenix.protocol.reflect.accessors;

import java.lang.reflect.Field;

public interface FieldAccessor
{
    Object get(final Object p0);
    
    void set(final Object p0, final Object p1);
    
    Field getField();
}
