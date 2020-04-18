package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public abstract class ClassTransformer extends $ClassVisitor
{
    public ClassTransformer() {
        super(327680);
    }
    
    public ClassTransformer(final int opcode) {
        super(opcode);
    }
    
    public abstract void setTarget(final $ClassVisitor p0);
}
