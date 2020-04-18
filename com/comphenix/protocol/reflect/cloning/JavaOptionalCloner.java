package com.comphenix.protocol.reflect.cloning;

import java.util.OptionalInt;
import java.util.Optional;

public class JavaOptionalCloner implements Cloner
{
    protected Cloner wrapped;
    
    public JavaOptionalCloner(final Cloner wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public boolean canClone(final Object source) {
        return source instanceof Optional || source instanceof OptionalInt;
    }
    
    @Override
    public Object clone(final Object source) {
        if (source instanceof Optional) {
            final Optional<?> optional = (Optional<?>)source;
            return optional.map(o -> this.wrapped.clone(o));
        }
        if (source instanceof OptionalInt) {
            final OptionalInt optional2 = (OptionalInt)source;
            return optional2.isPresent() ? OptionalInt.of(optional2.getAsInt()) : OptionalInt.empty();
        }
        return null;
    }
    
    public Cloner getWrapped() {
        return this.wrapped;
    }
}
