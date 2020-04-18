package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$TypePath;
import com.comphenix.net.sf.cglib.asm.$Attribute;
import com.comphenix.net.sf.cglib.asm.$AnnotationVisitor;
import com.comphenix.net.sf.cglib.asm.$FieldVisitor;

public class FieldVisitorTee extends $FieldVisitor
{
    private $FieldVisitor fv1;
    private $FieldVisitor fv2;
    
    public FieldVisitorTee(final $FieldVisitor fv1, final $FieldVisitor fv2) {
        super(327680);
        this.fv1 = fv1;
        this.fv2 = fv2;
    }
    
    @Override
    public $AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.fv1.visitAnnotation(desc, visible), this.fv2.visitAnnotation(desc, visible));
    }
    
    @Override
    public void visitAttribute(final $Attribute attr) {
        this.fv1.visitAttribute(attr);
        this.fv2.visitAttribute(attr);
    }
    
    @Override
    public void visitEnd() {
        this.fv1.visitEnd();
        this.fv2.visitEnd();
    }
    
    @Override
    public $AnnotationVisitor visitTypeAnnotation(final int typeRef, final $TypePath typePath, final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.fv1.visitTypeAnnotation(typeRef, typePath, desc, visible), this.fv2.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }
}
