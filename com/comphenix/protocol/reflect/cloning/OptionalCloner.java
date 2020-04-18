package com.comphenix.protocol.reflect.cloning;

import com.google.common.base.Optional;

public class OptionalCloner implements Cloner
{
    protected Cloner wrapped;
    
    public OptionalCloner(final Cloner wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public boolean canClone(final Object source) {
        return source instanceof Optional;
    }
    
    @Override
    public Object clone(final Object source) {
        final Optional<?> optional = (Optional<?>)source;
        if (!optional.isPresent()) {
            return Optional.absent();
        }
        return Optional.of(this.wrapped.clone(optional.get()));
    }
    
    public Cloner getWrapped() {
        return this.wrapped;
    }
}
