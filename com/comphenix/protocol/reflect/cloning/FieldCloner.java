package com.comphenix.protocol.reflect.cloning;

import com.comphenix.protocol.reflect.instances.NotConstructableException;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.ObjectWriter;
import com.comphenix.protocol.reflect.instances.InstanceProvider;

public class FieldCloner implements Cloner
{
    protected Cloner defaultCloner;
    protected InstanceProvider instanceProvider;
    protected ObjectWriter writer;
    
    public FieldCloner(final Cloner defaultCloner, final InstanceProvider instanceProvider) {
        this.defaultCloner = defaultCloner;
        this.instanceProvider = instanceProvider;
        this.writer = new ObjectWriter() {
            @Override
            protected void transformField(final StructureModifier<Object> modifierSource, final StructureModifier<Object> modifierDest, final int fieldIndex) {
                FieldCloner.this.defaultTransform(modifierDest, modifierDest, FieldCloner.this.getDefaultCloner(), fieldIndex);
            }
        };
    }
    
    protected void defaultTransform(final StructureModifier<Object> modifierSource, final StructureModifier<Object> modifierDest, final Cloner defaultCloner, final int fieldIndex) {
        final Object value = modifierSource.read(fieldIndex);
        modifierDest.write(fieldIndex, defaultCloner.clone(value));
    }
    
    @Override
    public boolean canClone(final Object source) {
        if (source == null) {
            return false;
        }
        try {
            return this.instanceProvider.create(source.getClass()) != null;
        }
        catch (NotConstructableException e) {
            return false;
        }
    }
    
    @Override
    public Object clone(final Object source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be NULL.");
        }
        final Object copy = this.instanceProvider.create(source.getClass());
        this.writer.copyTo(source, copy, source.getClass());
        return copy;
    }
    
    public Cloner getDefaultCloner() {
        return this.defaultCloner;
    }
    
    public InstanceProvider getInstanceProvider() {
        return this.instanceProvider;
    }
}
