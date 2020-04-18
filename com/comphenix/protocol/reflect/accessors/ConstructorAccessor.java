package com.comphenix.protocol.reflect.accessors;

import java.lang.reflect.Constructor;

public interface ConstructorAccessor
{
    Object invoke(final Object... p0);
    
    Constructor<?> getConstructor();
}
