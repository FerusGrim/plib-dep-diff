package com.comphenix.protocol.injector.packet;

import com.google.common.base.Preconditions;
import com.comphenix.protocol.reflect.FieldUtils;
import java.lang.reflect.Field;

public class MapContainer
{
    private final Field modCountField;
    private int lastModCount;
    private final Object source;
    private boolean changed;
    
    public MapContainer(final Object source) {
        this.source = source;
        this.changed = false;
        final Field modCountField = FieldUtils.getField(source.getClass(), "modCount", true);
        this.modCountField = (Field)Preconditions.checkNotNull((Object)modCountField, (Object)"Could not obtain modCount field");
        this.lastModCount = this.getModificationCount();
    }
    
    public boolean hasChanged() {
        this.checkChanged();
        return this.changed;
    }
    
    public void setChanged(final boolean changed) {
        this.changed = changed;
    }
    
    protected void checkChanged() {
        if (!this.changed && this.getModificationCount() != this.lastModCount) {
            this.lastModCount = this.getModificationCount();
            this.changed = true;
        }
    }
    
    private int getModificationCount() {
        try {
            return this.modCountField.getInt(this.source);
        }
        catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Unable to retrieve modCount.", ex);
        }
    }
}
