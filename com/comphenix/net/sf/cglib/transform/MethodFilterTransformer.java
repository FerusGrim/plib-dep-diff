package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public class MethodFilterTransformer extends AbstractClassTransformer
{
    private MethodFilter filter;
    private ClassTransformer pass;
    private $ClassVisitor direct;
    
    public MethodFilterTransformer(final MethodFilter filter, final ClassTransformer pass) {
        this.filter = filter;
        super.setTarget(this.pass = pass);
    }
    
    @Override
    public $MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        return (this.filter.accept(access, name, desc, signature, exceptions) ? this.pass : this.direct).visitMethod(access, name, desc, signature, exceptions);
    }
    
    @Override
    public void setTarget(final $ClassVisitor target) {
        this.pass.setTarget(target);
        this.direct = target;
    }
}
