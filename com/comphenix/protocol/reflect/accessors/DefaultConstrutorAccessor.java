package com.comphenix.protocol.reflect.accessors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

final class DefaultConstrutorAccessor implements ConstructorAccessor
{
    private final Constructor<?> constructor;
    
    public DefaultConstrutorAccessor(final Constructor<?> method) {
        this.constructor = method;
    }
    
    @Override
    public Object invoke(final Object... args) {
        try {
            return this.constructor.newInstance(args);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot use reflection.", e);
        }
        catch (IllegalArgumentException e2) {
            throw e2;
        }
        catch (InvocationTargetException e3) {
            throw new RuntimeException("An internal error occured.", e3.getCause());
        }
        catch (InstantiationException e4) {
            throw new RuntimeException("Cannot instantiate object.", e4);
        }
    }
    
    @Override
    public Constructor<?> getConstructor() {
        return this.constructor;
    }
    
    @Override
    public int hashCode() {
        return (this.constructor != null) ? this.constructor.hashCode() : 0;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultConstrutorAccessor) {
            final DefaultConstrutorAccessor other = (DefaultConstrutorAccessor)obj;
            return other.constructor == this.constructor;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "DefaultConstrutorAccessor [constructor=" + this.constructor + "]";
    }
}
