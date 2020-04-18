package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$TypePath;
import com.comphenix.net.sf.cglib.asm.$Attribute;
import com.comphenix.net.sf.cglib.asm.$AnnotationVisitor;
import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.comphenix.net.sf.cglib.asm.$FieldVisitor;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public class ClassVisitorTee extends $ClassVisitor
{
    private $ClassVisitor cv1;
    private $ClassVisitor cv2;
    
    public ClassVisitorTee(final $ClassVisitor cv1, final $ClassVisitor cv2) {
        super(327680);
        this.cv1 = cv1;
        this.cv2 = cv2;
    }
    
    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.cv1.visit(version, access, name, signature, superName, interfaces);
        this.cv2.visit(version, access, name, signature, superName, interfaces);
    }
    
    @Override
    public void visitEnd() {
        this.cv1.visitEnd();
        this.cv2.visitEnd();
        final $ClassVisitor $ClassVisitor = null;
        this.cv2 = $ClassVisitor;
        this.cv1 = $ClassVisitor;
    }
    
    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        this.cv1.visitInnerClass(name, outerName, innerName, access);
        this.cv2.visitInnerClass(name, outerName, innerName, access);
    }
    
    @Override
    public $FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        final $FieldVisitor fv1 = this.cv1.visitField(access, name, desc, signature, value);
        final $FieldVisitor fv2 = this.cv2.visitField(access, name, desc, signature, value);
        if (fv1 == null) {
            return fv2;
        }
        if (fv2 == null) {
            return fv1;
        }
        return new FieldVisitorTee(fv1, fv2);
    }
    
    @Override
    public $MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        final $MethodVisitor mv1 = this.cv1.visitMethod(access, name, desc, signature, exceptions);
        final $MethodVisitor mv2 = this.cv2.visitMethod(access, name, desc, signature, exceptions);
        if (mv1 == null) {
            return mv2;
        }
        if (mv2 == null) {
            return mv1;
        }
        return new MethodVisitorTee(mv1, mv2);
    }
    
    @Override
    public void visitSource(final String source, final String debug) {
        this.cv1.visitSource(source, debug);
        this.cv2.visitSource(source, debug);
    }
    
    @Override
    public void visitOuterClass(final String owner, final String name, final String desc) {
        this.cv1.visitOuterClass(owner, name, desc);
        this.cv2.visitOuterClass(owner, name, desc);
    }
    
    @Override
    public $AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.cv1.visitAnnotation(desc, visible), this.cv2.visitAnnotation(desc, visible));
    }
    
    @Override
    public void visitAttribute(final $Attribute attrs) {
        this.cv1.visitAttribute(attrs);
        this.cv2.visitAttribute(attrs);
    }
    
    @Override
    public $AnnotationVisitor visitTypeAnnotation(final int typeRef, final $TypePath typePath, final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.cv1.visitTypeAnnotation(typeRef, typePath, desc, visible), this.cv2.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }
}
