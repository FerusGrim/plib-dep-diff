package com.comphenix.protocol.reflect.compiler;

import java.util.Iterator;
import java.lang.reflect.Field;
import java.util.Map;
import com.comphenix.protocol.reflect.instances.DefaultInstances;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.google.common.collect.Sets;
import java.util.Set;
import com.comphenix.protocol.reflect.StructureModifier;

public abstract class CompiledStructureModifier extends StructureModifier<Object>
{
    protected StructureCompiler compiler;
    private Set<Integer> exempted;
    
    public CompiledStructureModifier() {
        this.customConvertHandling = true;
    }
    
    @Override
    public void setReadOnly(final int fieldIndex, final boolean value) throws FieldAccessException {
        if (this.isReadOnly(fieldIndex) && !value) {
            if (this.exempted == null) {
                this.exempted = (Set<Integer>)Sets.newHashSet();
            }
            this.exempted.add(fieldIndex);
        }
        if (!this.isReadOnly(fieldIndex) && value && (this.exempted == null || !this.exempted.contains(fieldIndex))) {
            throw new IllegalStateException("Cannot make compiled field " + fieldIndex + " read only.");
        }
        super.setReadOnly(fieldIndex, value);
    }
    
    @Override
    public StructureModifier<Object> writeDefaults() throws FieldAccessException {
        final DefaultInstances generator = DefaultInstances.DEFAULT;
        for (final Map.Entry<Field, Integer> entry : this.defaultFields.entrySet()) {
            final Integer index = entry.getValue();
            final Field field = entry.getKey();
            if (field.getType().getCanonicalName().equals("net.md_5.bungee.api.chat.BaseComponent[]")) {
                this.write(index, null);
            }
            else {
                this.write(index, generator.getDefault(field.getType()));
            }
        }
        return this;
    }
    
    @Override
    public final Object read(final int fieldIndex) throws FieldAccessException {
        final Object result = this.readGenerated(fieldIndex);
        if (this.converter != null) {
            return this.converter.getSpecific(result);
        }
        return result;
    }
    
    protected Object readReflected(final int index) throws FieldAccessException {
        return super.read(index);
    }
    
    protected abstract Object readGenerated(final int p0) throws FieldAccessException;
    
    @Override
    public StructureModifier<Object> write(final int index, Object value) throws FieldAccessException {
        if (this.converter != null) {
            value = this.converter.getGeneric((TField)value);
        }
        return this.writeGenerated(index, value);
    }
    
    protected void writeReflected(final int index, final Object value) throws FieldAccessException {
        super.write(index, value);
    }
    
    protected abstract StructureModifier<Object> writeGenerated(final int p0, final Object p1) throws FieldAccessException;
    
    @Override
    public StructureModifier<Object> withTarget(final Object target) {
        if (this.compiler != null) {
            return this.compiler.compile(super.withTarget(target));
        }
        return super.withTarget(target);
    }
}
