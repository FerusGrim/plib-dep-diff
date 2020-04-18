package com.comphenix.protocol.reflect.compiler;

import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.comphenix.net.sf.cglib.asm.$Type;

class BoxingHelper
{
    private static final $Type BYTE_$Type;
    private static final $Type BOOLEAN_$Type;
    private static final $Type SHORT_$Type;
    private static final $Type CHARACTER_$Type;
    private static final $Type INTEGER_$Type;
    private static final $Type FLOAT_$Type;
    private static final $Type LONG_$Type;
    private static final $Type DOUBLE_$Type;
    private static final $Type NUMBER_$Type;
    private static final $Type OBJECT_$Type;
    private static final MethodDescriptor BOOLEAN_VALUE;
    private static final MethodDescriptor CHAR_VALUE;
    private static final MethodDescriptor INT_VALUE;
    private static final MethodDescriptor FLOAT_VALUE;
    private static final MethodDescriptor LONG_VALUE;
    private static final MethodDescriptor DOUBLE_VALUE;
    private $MethodVisitor mv;
    
    public BoxingHelper(final $MethodVisitor mv) {
        this.mv = mv;
    }
    
    public void box(final $Type type) {
        if (type.getSort() == 10 || type.getSort() == 9) {
            return;
        }
        if (type == $Type.VOID_TYPE) {
            this.push(null);
        }
        else {
            $Type boxed = type;
            switch (type.getSort()) {
                case 3: {
                    boxed = BoxingHelper.BYTE_$Type;
                    break;
                }
                case 1: {
                    boxed = BoxingHelper.BOOLEAN_$Type;
                    break;
                }
                case 4: {
                    boxed = BoxingHelper.SHORT_$Type;
                    break;
                }
                case 2: {
                    boxed = BoxingHelper.CHARACTER_$Type;
                    break;
                }
                case 5: {
                    boxed = BoxingHelper.INTEGER_$Type;
                    break;
                }
                case 6: {
                    boxed = BoxingHelper.FLOAT_$Type;
                    break;
                }
                case 7: {
                    boxed = BoxingHelper.LONG_$Type;
                    break;
                }
                case 8: {
                    boxed = BoxingHelper.DOUBLE_$Type;
                    break;
                }
            }
            this.newInstance(boxed);
            if (type.getSize() == 2) {
                this.dupX2();
                this.dupX2();
                this.pop();
            }
            else {
                this.dupX1();
                this.swap();
            }
            this.invokeConstructor(boxed, new MethodDescriptor("<init>", $Type.VOID_TYPE, new $Type[] { type }));
        }
    }
    
    public void invokeConstructor(final $Type $Type, final MethodDescriptor method) {
        this.invokeInsn(183, $Type, method);
    }
    
    public void dupX1() {
        this.mv.visitInsn(90);
    }
    
    public void dupX2() {
        this.mv.visitInsn(91);
    }
    
    public void pop() {
        this.mv.visitInsn(87);
    }
    
    public void swap() {
        this.mv.visitInsn(95);
    }
    
    public void push(final boolean value) {
        this.push(value ? 1 : 0);
    }
    
    public void push(final int value) {
        if (value >= -1 && value <= 5) {
            this.mv.visitInsn(3 + value);
        }
        else if (value >= -128 && value <= 127) {
            this.mv.visitIntInsn(16, value);
        }
        else if (value >= -32768 && value <= 32767) {
            this.mv.visitIntInsn(17, value);
        }
        else {
            this.mv.visitLdcInsn(new Integer(value));
        }
    }
    
    public void newInstance(final $Type $Type) {
        this.$TypeInsn(187, $Type);
    }
    
    public void push(final String value) {
        if (value == null) {
            this.mv.visitInsn(1);
        }
        else {
            this.mv.visitLdcInsn(value);
        }
    }
    
    public void unbox(final $Type type) {
        $Type t = BoxingHelper.NUMBER_$Type;
        MethodDescriptor sig = null;
        switch (type.getSort()) {
            case 0: {
                return;
            }
            case 2: {
                t = BoxingHelper.CHARACTER_$Type;
                sig = BoxingHelper.CHAR_VALUE;
                break;
            }
            case 1: {
                t = BoxingHelper.BOOLEAN_$Type;
                sig = BoxingHelper.BOOLEAN_VALUE;
                break;
            }
            case 8: {
                sig = BoxingHelper.DOUBLE_VALUE;
                break;
            }
            case 6: {
                sig = BoxingHelper.FLOAT_VALUE;
                break;
            }
            case 7: {
                sig = BoxingHelper.LONG_VALUE;
                break;
            }
            case 3:
            case 4:
            case 5: {
                sig = BoxingHelper.INT_VALUE;
                break;
            }
        }
        if (sig == null) {
            this.checkCast(type);
        }
        else {
            this.checkCast(t);
            this.invokeVirtual(t, sig);
        }
    }
    
    public void checkCast(final $Type $Type) {
        if (!$Type.equals(BoxingHelper.OBJECT_$Type)) {
            this.$TypeInsn(192, $Type);
        }
    }
    
    public void invokeVirtual(final $Type owner, final MethodDescriptor method) {
        this.invokeInsn(182, owner, method);
    }
    
    private void invokeInsn(final int opcode, final $Type $Type, final MethodDescriptor method) {
        final String owner = ($Type.getSort() == 9) ? $Type.getDescriptor() : $Type.getInternalName();
        this.mv.visitMethodInsn(opcode, owner, method.getName(), method.getDescriptor());
    }
    
    private void $TypeInsn(final int opcode, final $Type $Type) {
        String desc;
        if ($Type.getSort() == 9) {
            desc = $Type.getDescriptor();
        }
        else {
            desc = $Type.getInternalName();
        }
        this.mv.visitTypeInsn(opcode, desc);
    }
    
    static {
        BYTE_$Type = $Type.getObjectType("java/lang/Byte");
        BOOLEAN_$Type = $Type.getObjectType("java/lang/Boolean");
        SHORT_$Type = $Type.getObjectType("java/lang/Short");
        CHARACTER_$Type = $Type.getObjectType("java/lang/Character");
        INTEGER_$Type = $Type.getObjectType("java/lang/Integer");
        FLOAT_$Type = $Type.getObjectType("java/lang/Float");
        LONG_$Type = $Type.getObjectType("java/lang/Long");
        DOUBLE_$Type = $Type.getObjectType("java/lang/Double");
        NUMBER_$Type = $Type.getObjectType("java/lang/Number");
        OBJECT_$Type = $Type.getObjectType("java/lang/Object");
        BOOLEAN_VALUE = MethodDescriptor.getMethod("boolean booleanValue()");
        CHAR_VALUE = MethodDescriptor.getMethod("char charValue()");
        INT_VALUE = MethodDescriptor.getMethod("int intValue()");
        FLOAT_VALUE = MethodDescriptor.getMethod("float floatValue()");
        LONG_VALUE = MethodDescriptor.getMethod("long longValue()");
        DOUBLE_VALUE = MethodDescriptor.getMethod("double doubleValue()");
    }
}
