package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$Handle;
import com.comphenix.net.sf.cglib.asm.$TypePath;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.asm.$Attribute;
import com.comphenix.net.sf.cglib.asm.$AnnotationVisitor;
import com.comphenix.net.sf.cglib.asm.$MethodVisitor;

public class MethodVisitorTee extends $MethodVisitor
{
    private final $MethodVisitor mv1;
    private final $MethodVisitor mv2;
    
    public MethodVisitorTee(final $MethodVisitor mv1, final $MethodVisitor mv2) {
        super(327680);
        this.mv1 = mv1;
        this.mv2 = mv2;
    }
    
    @Override
    public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack, final Object[] stack) {
        this.mv1.visitFrame(type, nLocal, local, nStack, stack);
        this.mv2.visitFrame(type, nLocal, local, nStack, stack);
    }
    
    @Override
    public $AnnotationVisitor visitAnnotationDefault() {
        return AnnotationVisitorTee.getInstance(this.mv1.visitAnnotationDefault(), this.mv2.visitAnnotationDefault());
    }
    
    @Override
    public $AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.mv1.visitAnnotation(desc, visible), this.mv2.visitAnnotation(desc, visible));
    }
    
    @Override
    public $AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.mv1.visitParameterAnnotation(parameter, desc, visible), this.mv2.visitParameterAnnotation(parameter, desc, visible));
    }
    
    @Override
    public void visitAttribute(final $Attribute attr) {
        this.mv1.visitAttribute(attr);
        this.mv2.visitAttribute(attr);
    }
    
    @Override
    public void visitCode() {
        this.mv1.visitCode();
        this.mv2.visitCode();
    }
    
    @Override
    public void visitInsn(final int opcode) {
        this.mv1.visitInsn(opcode);
        this.mv2.visitInsn(opcode);
    }
    
    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        this.mv1.visitIntInsn(opcode, operand);
        this.mv2.visitIntInsn(opcode, operand);
    }
    
    @Override
    public void visitVarInsn(final int opcode, final int var) {
        this.mv1.visitVarInsn(opcode, var);
        this.mv2.visitVarInsn(opcode, var);
    }
    
    @Override
    public void visitTypeInsn(final int opcode, final String desc) {
        this.mv1.visitTypeInsn(opcode, desc);
        this.mv2.visitTypeInsn(opcode, desc);
    }
    
    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        this.mv1.visitFieldInsn(opcode, owner, name, desc);
        this.mv2.visitFieldInsn(opcode, owner, name, desc);
    }
    
    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
        this.mv1.visitMethodInsn(opcode, owner, name, desc);
        this.mv2.visitMethodInsn(opcode, owner, name, desc);
    }
    
    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
        this.mv1.visitMethodInsn(opcode, owner, name, desc, itf);
        this.mv2.visitMethodInsn(opcode, owner, name, desc, itf);
    }
    
    @Override
    public void visitJumpInsn(final int opcode, final $Label label) {
        this.mv1.visitJumpInsn(opcode, label);
        this.mv2.visitJumpInsn(opcode, label);
    }
    
    @Override
    public void visitLabel(final $Label label) {
        this.mv1.visitLabel(label);
        this.mv2.visitLabel(label);
    }
    
    @Override
    public void visitLdcInsn(final Object cst) {
        this.mv1.visitLdcInsn(cst);
        this.mv2.visitLdcInsn(cst);
    }
    
    @Override
    public void visitIincInsn(final int var, final int increment) {
        this.mv1.visitIincInsn(var, increment);
        this.mv2.visitIincInsn(var, increment);
    }
    
    @Override
    public void visitTableSwitchInsn(final int min, final int max, final $Label dflt, final $Label[] labels) {
        this.mv1.visitTableSwitchInsn(min, max, dflt, labels);
        this.mv2.visitTableSwitchInsn(min, max, dflt, labels);
    }
    
    @Override
    public void visitLookupSwitchInsn(final $Label dflt, final int[] keys, final $Label[] labels) {
        this.mv1.visitLookupSwitchInsn(dflt, keys, labels);
        this.mv2.visitLookupSwitchInsn(dflt, keys, labels);
    }
    
    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        this.mv1.visitMultiANewArrayInsn(desc, dims);
        this.mv2.visitMultiANewArrayInsn(desc, dims);
    }
    
    @Override
    public void visitTryCatchBlock(final $Label start, final $Label end, final $Label handler, final String type) {
        this.mv1.visitTryCatchBlock(start, end, handler, type);
        this.mv2.visitTryCatchBlock(start, end, handler, type);
    }
    
    @Override
    public void visitLocalVariable(final String name, final String desc, final String signature, final $Label start, final $Label end, final int index) {
        this.mv1.visitLocalVariable(name, desc, signature, start, end, index);
        this.mv2.visitLocalVariable(name, desc, signature, start, end, index);
    }
    
    @Override
    public void visitLineNumber(final int line, final $Label start) {
        this.mv1.visitLineNumber(line, start);
        this.mv2.visitLineNumber(line, start);
    }
    
    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        this.mv1.visitMaxs(maxStack, maxLocals);
        this.mv2.visitMaxs(maxStack, maxLocals);
    }
    
    @Override
    public void visitEnd() {
        this.mv1.visitEnd();
        this.mv2.visitEnd();
    }
    
    @Override
    public void visitParameter(final String name, final int access) {
        this.mv1.visitParameter(name, access);
        this.mv2.visitParameter(name, access);
    }
    
    @Override
    public $AnnotationVisitor visitTypeAnnotation(final int typeRef, final $TypePath typePath, final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.mv1.visitTypeAnnotation(typeRef, typePath, desc, visible), this.mv2.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }
    
    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc, final $Handle bsm, final Object... bsmArgs) {
        this.mv1.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        this.mv2.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }
    
    @Override
    public $AnnotationVisitor visitInsnAnnotation(final int typeRef, final $TypePath typePath, final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.mv1.visitInsnAnnotation(typeRef, typePath, desc, visible), this.mv2.visitInsnAnnotation(typeRef, typePath, desc, visible));
    }
    
    @Override
    public $AnnotationVisitor visitTryCatchAnnotation(final int typeRef, final $TypePath typePath, final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.mv1.visitTryCatchAnnotation(typeRef, typePath, desc, visible), this.mv2.visitTryCatchAnnotation(typeRef, typePath, desc, visible));
    }
    
    @Override
    public $AnnotationVisitor visitLocalVariableAnnotation(final int typeRef, final $TypePath typePath, final $Label[] start, final $Label[] end, final int[] index, final String desc, final boolean visible) {
        return AnnotationVisitorTee.getInstance(this.mv1.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible), this.mv2.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible));
    }
}
