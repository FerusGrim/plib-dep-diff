package com.comphenix.protocol.reflect.accessors;

import java.lang.reflect.Field;

final class DefaultFieldAccessor implements FieldAccessor
{
    private final Field field;
    
    public DefaultFieldAccessor(final Field field) {
        this.field = field;
    }
    
    @Override
    public Object get(final Object instance) {
        try {
            return this.field.get(instance);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("Cannot read  " + this.field, e);
        }
        catch (IllegalAccessException e2) {
            throw new IllegalStateException("Cannot use reflection.", e2);
        }
    }
    
    @Override
    public void set(final Object instance, final Object value) {
        try {
            this.field.set(instance, value);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("Cannot set field " + this.field + " to value " + value, e);
        }
        catch (IllegalAccessException e2) {
            throw new IllegalStateException("Cannot use reflection.", e2);
        }
    }
    
    @Override
    public Field getField() {
        return this.field;
    }
    
    @Override
    public int hashCode() {
        return (this.field != null) ? this.field.hashCode() : 0;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultFieldAccessor) {
            final DefaultFieldAccessor other = (DefaultFieldAccessor)obj;
            return other.field == this.field;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "DefaultFieldAccessor [field=" + this.field + "]";
    }
}
