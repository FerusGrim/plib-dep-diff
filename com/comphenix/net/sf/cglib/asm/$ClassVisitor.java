package com.comphenix.net.sf.cglib.asm;

public abstract class $ClassVisitor
{
    protected final int api;
    protected $ClassVisitor cv;
    
    public $ClassVisitor(final int n) {
        this(n, null);
    }
    
    public $ClassVisitor(final int api, final $ClassVisitor cv) {
        if (api != 262144 && api != 327680) {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.cv = cv;
    }
    
    public void visit(final int n, final int n2, final String s, final String s2, final String s3, final String[] array) {
        if (this.cv != null) {
            this.cv.visit(n, n2, s, s2, s3, array);
        }
    }
    
    public void visitSource(final String s, final String s2) {
        if (this.cv != null) {
            this.cv.visitSource(s, s2);
        }
    }
    
    public void visitOuterClass(final String s, final String s2, final String s3) {
        if (this.cv != null) {
            this.cv.visitOuterClass(s, s2, s3);
        }
    }
    
    public $AnnotationVisitor visitAnnotation(final String s, final boolean b) {
        if (this.cv != null) {
            return this.cv.visitAnnotation(s, b);
        }
        return null;
    }
    
    public $AnnotationVisitor visitTypeAnnotation(final int n, final $TypePath $TypePath, final String s, final boolean b) {
        if (this.api < 327680) {
            throw new RuntimeException();
        }
        if (this.cv != null) {
            return this.cv.visitTypeAnnotation(n, $TypePath, s, b);
        }
        return null;
    }
    
    public void visitAttribute(final $Attribute $Attribute) {
        if (this.cv != null) {
            this.cv.visitAttribute($Attribute);
        }
    }
    
    public void visitInnerClass(final String s, final String s2, final String s3, final int n) {
        if (this.cv != null) {
            this.cv.visitInnerClass(s, s2, s3, n);
        }
    }
    
    public $FieldVisitor visitField(final int n, final String s, final String s2, final String s3, final Object o) {
        if (this.cv != null) {
            return this.cv.visitField(n, s, s2, s3, o);
        }
        return null;
    }
    
    public $MethodVisitor visitMethod(final int n, final String s, final String s2, final String s3, final String[] array) {
        if (this.cv != null) {
            return this.cv.visitMethod(n, s, s2, s3, array);
        }
        return null;
    }
    
    public void visitEnd() {
        if (this.cv != null) {
            this.cv.visitEnd();
        }
    }
}
