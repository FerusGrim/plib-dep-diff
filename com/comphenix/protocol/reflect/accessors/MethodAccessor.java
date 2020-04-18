package com.comphenix.protocol.reflect.accessors;

import java.lang.reflect.Method;

public interface MethodAccessor
{
    Object invoke(final Object p0, final Object... p1);
    
    Method getMethod();
}
