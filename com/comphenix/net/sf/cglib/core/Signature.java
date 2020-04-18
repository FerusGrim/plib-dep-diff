package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Type;

public class Signature
{
    private String name;
    private String desc;
    
    public Signature(final String name, final String desc) {
        if (name.indexOf(40) >= 0) {
            throw new IllegalArgumentException("Name '" + name + "' is invalid");
        }
        this.name = name;
        this.desc = desc;
    }
    
    public Signature(final String name, final $Type returnType, final $Type[] argumentTypes) {
        this(name, $Type.getMethodDescriptor(returnType, argumentTypes));
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDescriptor() {
        return this.desc;
    }
    
    public $Type getReturnType() {
        return $Type.getReturnType(this.desc);
    }
    
    public $Type[] getArgumentTypes() {
        return $Type.getArgumentTypes(this.desc);
    }
    
    @Override
    public String toString() {
        return this.name + this.desc;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Signature)) {
            return false;
        }
        final Signature other = (Signature)o;
        return this.name.equals(other.name) && this.desc.equals(other.desc);
    }
    
    @Override
    public int hashCode() {
        return this.name.hashCode() ^ this.desc.hashCode();
    }
}
