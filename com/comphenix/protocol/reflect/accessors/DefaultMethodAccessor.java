package com.comphenix.protocol.reflect.accessors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class DefaultMethodAccessor implements MethodAccessor
{
    private final Method method;
    
    public DefaultMethodAccessor(final Method method) {
        this.method = method;
    }
    
    @Override
    public Object invoke(final Object target, final Object... args) {
        try {
            return this.method.invoke(target, args);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot use reflection.", e);
        }
        catch (InvocationTargetException e2) {
            throw new RuntimeException("An internal error occured.", e2.getCause());
        }
        catch (IllegalArgumentException e3) {
            throw e3;
        }
    }
    
    @Override
    public Method getMethod() {
        return this.method;
    }
    
    @Override
    public int hashCode() {
        return (this.method != null) ? this.method.hashCode() : 0;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultMethodAccessor) {
            final DefaultMethodAccessor other = (DefaultMethodAccessor)obj;
            return other.method == this.method;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "DefaultMethodAccessor [method=" + this.method + "]";
    }
}
