package com.comphenix.net.sf.cglib.proxy;

import java.lang.reflect.Method;

public interface CallbackFilter
{
    int accept(final Method p0);
    
    boolean equals(final Object p0);
}
