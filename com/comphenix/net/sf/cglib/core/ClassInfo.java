package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Type;

public abstract class ClassInfo
{
    protected ClassInfo() {
    }
    
    public abstract $Type getType();
    
    public abstract $Type getSuperType();
    
    public abstract $Type[] getInterfaces();
    
    public abstract int getModifiers();
    
    @Override
    public boolean equals(final Object o) {
        return o != null && o instanceof ClassInfo && this.getType().equals(((ClassInfo)o).getType());
    }
    
    @Override
    public int hashCode() {
        return this.getType().hashCode();
    }
    
    @Override
    public String toString() {
        return this.getType().getClassName();
    }
}
