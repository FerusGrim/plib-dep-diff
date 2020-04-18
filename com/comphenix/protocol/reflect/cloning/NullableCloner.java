package com.comphenix.protocol.reflect.cloning;

public class NullableCloner implements Cloner
{
    protected Cloner wrapped;
    
    public NullableCloner(final Cloner wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public boolean canClone(final Object source) {
        return true;
    }
    
    @Override
    public Object clone(final Object source) {
        if (source == null) {
            return null;
        }
        return this.wrapped.clone(source);
    }
    
    public Cloner getWrapped() {
        return this.wrapped;
    }
}
