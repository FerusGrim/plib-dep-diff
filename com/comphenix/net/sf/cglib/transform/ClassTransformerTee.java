package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public class ClassTransformerTee extends ClassTransformer
{
    private $ClassVisitor branch;
    
    public ClassTransformerTee(final $ClassVisitor branch) {
        super(327680);
        this.branch = branch;
    }
    
    @Override
    public void setTarget(final $ClassVisitor target) {
        this.cv = new ClassVisitorTee(this.branch, target);
    }
}
