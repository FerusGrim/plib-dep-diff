package com.comphenix.protocol.reflect.accessors;

public abstract class ReadOnlyFieldAccessor implements FieldAccessor
{
    @Override
    public final void set(final Object instance, final Object value) {
        throw new UnsupportedOperationException("Cannot update the content of a read-only field accessor.");
    }
}
