package com.comphenix.protocol.wrappers;

import com.google.common.base.Preconditions;

public abstract class AbstractWrapper
{
    protected Object handle;
    protected Class<?> handleType;
    
    public AbstractWrapper(final Class<?> handleType) {
        this.handleType = (Class<?>)Preconditions.checkNotNull((Object)handleType, (Object)"handleType cannot be NULL");
    }
    
    protected void setHandle(final Object handle) {
        if (handle == null) {
            throw new IllegalArgumentException("handle cannot be NULL.");
        }
        if (!this.handleType.isAssignableFrom(handle.getClass())) {
            throw new IllegalArgumentException("handle (" + handle + ") is not a " + this.handleType + ", but " + handle.getClass());
        }
        this.handle = handle;
    }
    
    public Object getHandle() {
        return this.handle;
    }
    
    public Class<?> getHandleType() {
        return this.handleType;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AbstractWrapper) {
            final AbstractWrapper that = (AbstractWrapper)obj;
            return this.handle.equals(that.handle);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }
    
    @Override
    public String toString() {
        return this.getClass().getName() + "[handle=" + this.handle + "]";
    }
}
