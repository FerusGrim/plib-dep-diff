package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.comphenix.net.sf.cglib.asm.$FieldVisitor;
import com.comphenix.net.sf.cglib.asm.$Attribute;
import com.comphenix.net.sf.cglib.asm.$AnnotationVisitor;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public abstract class AbstractClassFilterTransformer extends AbstractClassTransformer
{
    private ClassTransformer pass;
    private $ClassVisitor target;
    
    @Override
    public void setTarget(final $ClassVisitor target) {
        super.setTarget(target);
        this.pass.setTarget(target);
    }
    
    protected AbstractClassFilterTransformer(final ClassTransformer pass) {
        this.pass = pass;
    }
    
    protected abstract boolean accept(final int p0, final int p1, final String p2, final String p3, final String p4, final String[] p5);
    
    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        (this.target = (this.accept(version, access, name, signature, superName, interfaces) ? this.pass : this.cv)).visit(version, access, name, signature, superName, interfaces);
    }
    
    @Override
    public void visitSource(final String source, final String debug) {
        this.target.visitSource(source, debug);
    }
    
    @Override
    public void visitOuterClass(final String owner, final String name, final String desc) {
        this.target.visitOuterClass(owner, name, desc);
    }
    
    @Override
    public $AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return this.target.visitAnnotation(desc, visible);
    }
    
    @Override
    public void visitAttribute(final $Attribute attr) {
        this.target.visitAttribute(attr);
    }
    
    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        this.target.visitInnerClass(name, outerName, innerName, access);
    }
    
    @Override
    public $FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        return this.target.visitField(access, name, desc, signature, value);
    }
    
    @Override
    public $MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        return this.target.visitMethod(access, name, desc, signature, exceptions);
    }
    
    @Override
    public void visitEnd() {
        this.target.visitEnd();
        this.target = null;
    }
}
