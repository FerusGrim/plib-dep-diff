package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Type;

public abstract class MethodInfo
{
    protected MethodInfo() {
    }
    
    public abstract ClassInfo getClassInfo();
    
    public abstract int getModifiers();
    
    public abstract Signature getSignature();
    
    public abstract $Type[] getExceptionTypes();
    
    @Override
    public boolean equals(final Object o) {
        return o != null && o instanceof MethodInfo && this.getSignature().equals(((MethodInfo)o).getSignature());
    }
    
    @Override
    public int hashCode() {
        return this.getSignature().hashCode();
    }
    
    @Override
    public String toString() {
        return this.getSignature().toString();
    }
}
