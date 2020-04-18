package com.comphenix.protocol.reflect;

import com.comphenix.protocol.ProtocolLogger;
import com.google.common.base.Objects;
import com.comphenix.protocol.reflect.accessors.Accessors;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;

public class VolatileField
{
    private FieldAccessor accessor;
    private Object container;
    private Object previous;
    private Object current;
    private boolean previousLoaded;
    private boolean currentSet;
    private boolean forceAccess;
    
    public VolatileField(final Field field, final Object container) {
        this.accessor = Accessors.getFieldAccessor(field);
        this.container = container;
    }
    
    public VolatileField(final Field field, final Object container, final boolean forceAccess) {
        this.accessor = Accessors.getFieldAccessor(field, true);
        this.container = container;
        this.forceAccess = forceAccess;
    }
    
    public VolatileField(final FieldAccessor accessor, final Object container) {
        this.accessor = accessor;
        this.container = container;
    }
    
    public Field getField() {
        return this.accessor.getField();
    }
    
    public Object getContainer() {
        return this.container;
    }
    
    public boolean isForceAccess() {
        return this.forceAccess;
    }
    
    public void setForceAccess(final boolean forceAccess) {
        this.forceAccess = forceAccess;
    }
    
    public Object getValue() {
        if (!this.currentSet) {
            this.ensureLoaded();
            return this.previous;
        }
        return this.current;
    }
    
    public Object getOldValue() {
        this.ensureLoaded();
        return this.previous;
    }
    
    public void setValue(final Object newValue) {
        this.ensureLoaded();
        this.writeFieldValue(newValue);
        this.current = newValue;
        this.currentSet = true;
    }
    
    public void refreshValue() {
        final Object fieldValue = this.readFieldValue();
        if (this.currentSet) {
            if (!Objects.equal(this.current, fieldValue)) {
                this.previous = this.readFieldValue();
                this.previousLoaded = true;
                this.writeFieldValue(this.current);
            }
        }
        else if (this.previousLoaded) {
            this.previous = fieldValue;
        }
    }
    
    public void saveValue() {
        this.previous = this.current;
        this.currentSet = false;
    }
    
    public void revertValue() {
        if (this.currentSet) {
            if (this.getValue() == this.current) {
                this.setValue(this.previous);
                this.currentSet = false;
            }
            else {
                ProtocolLogger.log("Unable to switch {0} to {1}. Expected {2}, but got {3}.", this.getField().toGenericString(), this.previous, this.current, this.getValue());
            }
        }
    }
    
    public VolatileField toSynchronized() {
        return new VolatileField(Accessors.getSynchronized(this.accessor), this.container);
    }
    
    public boolean isCurrentSet() {
        return this.currentSet;
    }
    
    private void ensureLoaded() {
        if (!this.previousLoaded) {
            this.previous = this.readFieldValue();
            this.previousLoaded = true;
        }
    }
    
    private Object readFieldValue() {
        return this.accessor.get(this.container);
    }
    
    private void writeFieldValue(final Object newValue) {
        this.accessor.set(this.container, newValue);
    }
    
    @Override
    protected void finalize() throws Throwable {
        this.revertValue();
    }
    
    @Override
    public String toString() {
        return "VolatileField [accessor=" + this.accessor + ", container=" + this.container + ", previous=" + this.previous + ", current=" + this.current + ", previousLoaded=" + this.previousLoaded + ", currentSet=" + this.currentSet + ", forceAccess=" + this.forceAccess + "]";
    }
}
