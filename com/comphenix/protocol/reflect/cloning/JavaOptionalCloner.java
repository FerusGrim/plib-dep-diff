package com.comphenix.protocol.reflect.cloning;

import java.util.Optional;

public class JavaOptionalCloner implements Cloner
{
    protected Cloner wrapped;
    
    public JavaOptionalCloner(final Cloner wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public boolean canClone(final Object source) {
        return source instanceof Optional;
    }
    
    @Override
    public Object clone(final Object source) {
        final Optional<?> optional = (Optional<?>)source;
        return optional.map(o -> this.wrapped.clone(o));
    }
    
    public Cloner getWrapped() {
        return this.wrapped;
    }
}
