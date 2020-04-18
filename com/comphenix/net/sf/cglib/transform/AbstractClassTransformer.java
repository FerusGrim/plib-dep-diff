package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public abstract class AbstractClassTransformer extends ClassTransformer
{
    protected AbstractClassTransformer() {
        super(327680);
    }
    
    @Override
    public void setTarget(final $ClassVisitor target) {
        this.cv = target;
    }
}
