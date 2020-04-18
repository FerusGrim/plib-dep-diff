package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Attribute;
import java.util.Arrays;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.asm.$MethodVisitor;

public class CodeEmitter extends LocalVariablesSorter
{
    private static final Signature BOOLEAN_VALUE;
    private static final Signature CHAR_VALUE;
    private static final Signature LONG_VALUE;
    private static final Signature DOUBLE_VALUE;
    private static final Signature FLOAT_VALUE;
    private static final Signature INT_VALUE;
    private static final Signature CSTRUCT_NULL;
    private static final Signature CSTRUCT_STRING;
    public static final int ADD = 96;
    public static final int MUL = 104;
    public static final int XOR = 130;
    public static final int USHR = 124;
    public static final int SUB = 100;
    public static final int DIV = 108;
    public static final int NEG = 116;
    public static final int REM = 112;
    public static final int AND = 126;
    public static final int OR = 128;
    public static final int GT = 157;
    public static final int LT = 155;
    public static final int GE = 156;
    public static final int LE = 158;
    public static final int NE = 154;
    public static final int EQ = 153;
    private ClassEmitter ce;
    private State state;
    
    CodeEmitter(final ClassEmitter ce, final $MethodVisitor mv, final int access, final Signature sig, final $Type[] exceptionTypes) {
        super(access, sig.getDescriptor(), mv);
        this.ce = ce;
        this.state = new State(ce.getClassInfo(), access, sig, exceptionTypes);
    }
    
    public CodeEmitter(final CodeEmitter wrap) {
        super(wrap);
        this.ce = wrap.ce;
        this.state = wrap.state;
    }
    
    public boolean isStaticHook() {
        return false;
    }
    
    public Signature getSignature() {
        return this.state.sig;
    }
    
    public $Type getReturnType() {
        return this.state.sig.getReturnType();
    }
    
    public MethodInfo getMethodInfo() {
        return this.state;
    }
    
    public ClassEmitter getClassEmitter() {
        return this.ce;
    }
    
    public void end_method() {
        this.visitMaxs(0, 0);
    }
    
    public Block begin_block() {
        return new Block(this);
    }
    
    public void catch_exception(final Block block, final $Type exception) {
        if (block.getEnd() == null) {
            throw new IllegalStateException("end of block is unset");
        }
        this.mv.visitTryCatchBlock(block.getStart(), block.getEnd(), this.mark(), exception.getInternalName());
    }
    
    public void goTo(final $Label label) {
        this.mv.visitJumpInsn(167, label);
    }
    
    public void ifnull(final $Label label) {
        this.mv.visitJumpInsn(198, label);
    }
    
    public void ifnonnull(final $Label label) {
        this.mv.visitJumpInsn(199, label);
    }
    
    public void if_jump(final int mode, final $Label label) {
        this.mv.visitJumpInsn(mode, label);
    }
    
    public void if_icmp(final int mode, final $Label label) {
        this.if_cmp($Type.INT_TYPE, mode, label);
    }
    
    public void if_cmp(final $Type type, final int mode, final $Label label) {
        int intOp = -1;
        int jumpmode = mode;
        switch (mode) {
            case 156: {
                jumpmode = 155;
                break;
            }
            case 158: {
                jumpmode = 157;
                break;
            }
        }
        switch (type.getSort()) {
            case 7: {
                this.mv.visitInsn(148);
                break;
            }
            case 8: {
                this.mv.visitInsn(152);
                break;
            }
            case 6: {
                this.mv.visitInsn(150);
                break;
            }
            case 9:
            case 10: {
                switch (mode) {
                    case 153: {
                        this.mv.visitJumpInsn(165, label);
                        return;
                    }
                    case 154: {
                        this.mv.visitJumpInsn(166, label);
                        return;
                    }
                    default: {
                        throw new IllegalArgumentException("Bad comparison for type " + type);
                    }
                }
                break;
            }
            default: {
                switch (mode) {
                    case 153: {
                        intOp = 159;
                        break;
                    }
                    case 154: {
                        intOp = 160;
                        break;
                    }
                    case 156: {
                        this.swap();
                    }
                    case 155: {
                        intOp = 161;
                        break;
                    }
                    case 158: {
                        this.swap();
                    }
                    case 157: {
                        intOp = 163;
                        break;
                    }
                }
                this.mv.visitJumpInsn(intOp, label);
                return;
            }
        }
        this.if_jump(jumpmode, label);
    }
    
    public void pop() {
        this.mv.visitInsn(87);
    }
    
    public void pop2() {
        this.mv.visitInsn(88);
    }
    
    public void dup() {
        this.mv.visitInsn(89);
    }
    
    public void dup2() {
        this.mv.visitInsn(92);
    }
    
    public void dup_x1() {
        this.mv.visitInsn(90);
    }
    
    public void dup_x2() {
        this.mv.visitInsn(91);
    }
    
    public void dup2_x1() {
        this.mv.visitInsn(93);
    }
    
    public void dup2_x2() {
        this.mv.visitInsn(94);
    }
    
    public void swap() {
        this.mv.visitInsn(95);
    }
    
    public void aconst_null() {
        this.mv.visitInsn(1);
    }
    
    public void swap(final $Type prev, final $Type type) {
        if (type.getSize() == 1) {
            if (prev.getSize() == 1) {
                this.swap();
            }
            else {
                this.dup_x2();
                this.pop();
            }
        }
        else if (prev.getSize() == 1) {
            this.dup2_x1();
            this.pop2();
        }
        else {
            this.dup2_x2();
            this.pop2();
        }
    }
    
    public void monitorenter() {
        this.mv.visitInsn(194);
    }
    
    public void monitorexit() {
        this.mv.visitInsn(195);
    }
    
    public void math(final int op, final $Type type) {
        this.mv.visitInsn(type.getOpcode(op));
    }
    
    public void array_load(final $Type type) {
        this.mv.visitInsn(type.getOpcode(46));
    }
    
    public void array_store(final $Type type) {
        this.mv.visitInsn(type.getOpcode(79));
    }
    
    public void cast_numeric(final $Type from, final $Type to) {
        if (from != to) {
            if (from == $Type.DOUBLE_TYPE) {
                if (to == $Type.FLOAT_TYPE) {
                    this.mv.visitInsn(144);
                }
                else if (to == $Type.LONG_TYPE) {
                    this.mv.visitInsn(143);
                }
                else {
                    this.mv.visitInsn(142);
                    this.cast_numeric($Type.INT_TYPE, to);
                }
            }
            else if (from == $Type.FLOAT_TYPE) {
                if (to == $Type.DOUBLE_TYPE) {
                    this.mv.visitInsn(141);
                }
                else if (to == $Type.LONG_TYPE) {
                    this.mv.visitInsn(140);
                }
                else {
                    this.mv.visitInsn(139);
                    this.cast_numeric($Type.INT_TYPE, to);
                }
            }
            else if (from == $Type.LONG_TYPE) {
                if (to == $Type.DOUBLE_TYPE) {
                    this.mv.visitInsn(138);
                }
                else if (to == $Type.FLOAT_TYPE) {
                    this.mv.visitInsn(137);
                }
                else {
                    this.mv.visitInsn(136);
                    this.cast_numeric($Type.INT_TYPE, to);
                }
            }
            else if (to == $Type.BYTE_TYPE) {
                this.mv.visitInsn(145);
            }
            else if (to == $Type.CHAR_TYPE) {
                this.mv.visitInsn(146);
            }
            else if (to == $Type.DOUBLE_TYPE) {
                this.mv.visitInsn(135);
            }
            else if (to == $Type.FLOAT_TYPE) {
                this.mv.visitInsn(134);
            }
            else if (to == $Type.LONG_TYPE) {
                this.mv.visitInsn(133);
            }
            else if (to == $Type.SHORT_TYPE) {
                this.mv.visitInsn(147);
            }
        }
    }
    
    public void push(final int i) {
        if (i < -1) {
            this.mv.visitLdcInsn(new Integer(i));
        }
        else if (i <= 5) {
            this.mv.visitInsn(TypeUtils.ICONST(i));
        }
        else if (i <= 127) {
            this.mv.visitIntInsn(16, i);
        }
        else if (i <= 32767) {
            this.mv.visitIntInsn(17, i);
        }
        else {
            this.mv.visitLdcInsn(new Integer(i));
        }
    }
    
    public void push(final long value) {
        if (value == 0L || value == 1L) {
            this.mv.visitInsn(TypeUtils.LCONST(value));
        }
        else {
            this.mv.visitLdcInsn(new Long(value));
        }
    }
    
    public void push(final float value) {
        if (value == 0.0f || value == 1.0f || value == 2.0f) {
            this.mv.visitInsn(TypeUtils.FCONST(value));
        }
        else {
            this.mv.visitLdcInsn(new Float(value));
        }
    }
    
    public void push(final double value) {
        if (value == 0.0 || value == 1.0) {
            this.mv.visitInsn(TypeUtils.DCONST(value));
        }
        else {
            this.mv.visitLdcInsn(new Double(value));
        }
    }
    
    public void push(final String value) {
        this.mv.visitLdcInsn(value);
    }
    
    public void newarray() {
        this.newarray(Constants.TYPE_OBJECT);
    }
    
    public void newarray(final $Type type) {
        if (TypeUtils.isPrimitive(type)) {
            this.mv.visitIntInsn(188, TypeUtils.NEWARRAY(type));
        }
        else {
            this.emit_type(189, type);
        }
    }
    
    public void arraylength() {
        this.mv.visitInsn(190);
    }
    
    public void load_this() {
        if (TypeUtils.isStatic(this.state.access)) {
            throw new IllegalStateException("no 'this' pointer within static method");
        }
        this.mv.visitVarInsn(25, 0);
    }
    
    public void load_args() {
        this.load_args(0, this.state.argumentTypes.length);
    }
    
    public void load_arg(final int index) {
        this.load_local(this.state.argumentTypes[index], this.state.localOffset + this.skipArgs(index));
    }
    
    public void load_args(final int fromArg, final int count) {
        int pos = this.state.localOffset + this.skipArgs(fromArg);
        for (int i = 0; i < count; ++i) {
            final $Type t = this.state.argumentTypes[fromArg + i];
            this.load_local(t, pos);
            pos += t.getSize();
        }
    }
    
    private int skipArgs(final int numArgs) {
        int amount = 0;
        for (int i = 0; i < numArgs; ++i) {
            amount += this.state.argumentTypes[i].getSize();
        }
        return amount;
    }
    
    private void load_local(final $Type t, final int pos) {
        this.mv.visitVarInsn(t.getOpcode(21), pos);
    }
    
    private void store_local(final $Type t, final int pos) {
        this.mv.visitVarInsn(t.getOpcode(54), pos);
    }
    
    public void iinc(final Local local, final int amount) {
        this.mv.visitIincInsn(local.getIndex(), amount);
    }
    
    public void store_local(final Local local) {
        this.store_local(local.getType(), local.getIndex());
    }
    
    public void load_local(final Local local) {
        this.load_local(local.getType(), local.getIndex());
    }
    
    public void return_value() {
        this.mv.visitInsn(this.state.sig.getReturnType().getOpcode(172));
    }
    
    public void getfield(final String name) {
        final ClassEmitter.FieldInfo info = this.ce.getFieldInfo(name);
        final int opcode = TypeUtils.isStatic(info.access) ? 178 : 180;
        this.emit_field(opcode, this.ce.getClassType(), name, info.type);
    }
    
    public void putfield(final String name) {
        final ClassEmitter.FieldInfo info = this.ce.getFieldInfo(name);
        final int opcode = TypeUtils.isStatic(info.access) ? 179 : 181;
        this.emit_field(opcode, this.ce.getClassType(), name, info.type);
    }
    
    public void super_getfield(final String name, final $Type type) {
        this.emit_field(180, this.ce.getSuperType(), name, type);
    }
    
    public void super_putfield(final String name, final $Type type) {
        this.emit_field(181, this.ce.getSuperType(), name, type);
    }
    
    public void super_getstatic(final String name, final $Type type) {
        this.emit_field(178, this.ce.getSuperType(), name, type);
    }
    
    public void super_putstatic(final String name, final $Type type) {
        this.emit_field(179, this.ce.getSuperType(), name, type);
    }
    
    public void getfield(final $Type owner, final String name, final $Type type) {
        this.emit_field(180, owner, name, type);
    }
    
    public void putfield(final $Type owner, final String name, final $Type type) {
        this.emit_field(181, owner, name, type);
    }
    
    public void getstatic(final $Type owner, final String name, final $Type type) {
        this.emit_field(178, owner, name, type);
    }
    
    public void putstatic(final $Type owner, final String name, final $Type type) {
        this.emit_field(179, owner, name, type);
    }
    
    void emit_field(final int opcode, final $Type ctype, final String name, final $Type ftype) {
        this.mv.visitFieldInsn(opcode, ctype.getInternalName(), name, ftype.getDescriptor());
    }
    
    public void super_invoke() {
        this.super_invoke(this.state.sig);
    }
    
    public void super_invoke(final Signature sig) {
        this.emit_invoke(183, this.ce.getSuperType(), sig);
    }
    
    public void invoke_constructor(final $Type type) {
        this.invoke_constructor(type, CodeEmitter.CSTRUCT_NULL);
    }
    
    public void super_invoke_constructor() {
        this.invoke_constructor(this.ce.getSuperType());
    }
    
    public void invoke_constructor_this() {
        this.invoke_constructor(this.ce.getClassType());
    }
    
    private void emit_invoke(final int opcode, final $Type type, final Signature sig) {
        if (!sig.getName().equals("<init>") || opcode == 182 || opcode == 184) {}
        this.mv.visitMethodInsn(opcode, type.getInternalName(), sig.getName(), sig.getDescriptor(), opcode == 185);
    }
    
    public void invoke_interface(final $Type owner, final Signature sig) {
        this.emit_invoke(185, owner, sig);
    }
    
    public void invoke_virtual(final $Type owner, final Signature sig) {
        this.emit_invoke(182, owner, sig);
    }
    
    public void invoke_static(final $Type owner, final Signature sig) {
        this.emit_invoke(184, owner, sig);
    }
    
    public void invoke_virtual_this(final Signature sig) {
        this.invoke_virtual(this.ce.getClassType(), sig);
    }
    
    public void invoke_static_this(final Signature sig) {
        this.invoke_static(this.ce.getClassType(), sig);
    }
    
    public void invoke_constructor(final $Type type, final Signature sig) {
        this.emit_invoke(183, type, sig);
    }
    
    public void invoke_constructor_this(final Signature sig) {
        this.invoke_constructor(this.ce.getClassType(), sig);
    }
    
    public void super_invoke_constructor(final Signature sig) {
        this.invoke_constructor(this.ce.getSuperType(), sig);
    }
    
    public void new_instance_this() {
        this.new_instance(this.ce.getClassType());
    }
    
    public void new_instance(final $Type type) {
        this.emit_type(187, type);
    }
    
    private void emit_type(final int opcode, final $Type type) {
        String desc;
        if (TypeUtils.isArray(type)) {
            desc = type.getDescriptor();
        }
        else {
            desc = type.getInternalName();
        }
        this.mv.visitTypeInsn(opcode, desc);
    }
    
    public void aaload(final int index) {
        this.push(index);
        this.aaload();
    }
    
    public void aaload() {
        this.mv.visitInsn(50);
    }
    
    public void aastore() {
        this.mv.visitInsn(83);
    }
    
    public void athrow() {
        this.mv.visitInsn(191);
    }
    
    public $Label make_label() {
        return new $Label();
    }
    
    public Local make_local() {
        return this.make_local(Constants.TYPE_OBJECT);
    }
    
    public Local make_local(final $Type type) {
        return new Local(this.newLocal(type.getSize()), type);
    }
    
    public void checkcast_this() {
        this.checkcast(this.ce.getClassType());
    }
    
    public void checkcast(final $Type type) {
        if (!type.equals(Constants.TYPE_OBJECT)) {
            this.emit_type(192, type);
        }
    }
    
    public void instance_of(final $Type type) {
        this.emit_type(193, type);
    }
    
    public void instance_of_this() {
        this.instance_of(this.ce.getClassType());
    }
    
    public void process_switch(final int[] keys, final ProcessSwitchCallback callback) {
        float density;
        if (keys.length == 0) {
            density = 0.0f;
        }
        else {
            density = keys.length / (float)(keys[keys.length - 1] - keys[0] + 1);
        }
        this.process_switch(keys, callback, density >= 0.5f);
    }
    
    public void process_switch(final int[] keys, final ProcessSwitchCallback callback, final boolean useTable) {
        if (!isSorted(keys)) {
            throw new IllegalArgumentException("keys to switch must be sorted ascending");
        }
        final $Label def = this.make_label();
        final $Label end = this.make_label();
        try {
            if (keys.length > 0) {
                final int len = keys.length;
                final int min = keys[0];
                final int max = keys[len - 1];
                final int range = max - min + 1;
                if (useTable) {
                    final $Label[] labels = new $Label[range];
                    Arrays.fill(labels, def);
                    for (int i = 0; i < len; ++i) {
                        labels[keys[i] - min] = this.make_label();
                    }
                    this.mv.visitTableSwitchInsn(min, max, def, labels);
                    for (int i = 0; i < range; ++i) {
                        final $Label label = labels[i];
                        if (label != def) {
                            this.mark(label);
                            callback.processCase(i + min, end);
                        }
                    }
                }
                else {
                    final $Label[] labels = new $Label[len];
                    for (int i = 0; i < len; ++i) {
                        labels[i] = this.make_label();
                    }
                    this.mv.visitLookupSwitchInsn(def, keys, labels);
                    for (int i = 0; i < len; ++i) {
                        this.mark(labels[i]);
                        callback.processCase(keys[i], end);
                    }
                }
            }
            this.mark(def);
            callback.processDefault();
            this.mark(end);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Error e2) {
            throw e2;
        }
        catch (Exception e3) {
            throw new CodeGenerationException(e3);
        }
    }
    
    private static boolean isSorted(final int[] keys) {
        for (int i = 1; i < keys.length; ++i) {
            if (keys[i] < keys[i - 1]) {
                return false;
            }
        }
        return true;
    }
    
    public void mark(final $Label label) {
        this.mv.visitLabel(label);
    }
    
    $Label mark() {
        final $Label label = this.make_label();
        this.mv.visitLabel(label);
        return label;
    }
    
    public void push(final boolean value) {
        this.push(value ? 1 : 0);
    }
    
    public void not() {
        this.push(1);
        this.math(130, $Type.INT_TYPE);
    }
    
    public void throw_exception(final $Type type, final String msg) {
        this.new_instance(type);
        this.dup();
        this.push(msg);
        this.invoke_constructor(type, CodeEmitter.CSTRUCT_STRING);
        this.athrow();
    }
    
    public void box(final $Type type) {
        if (TypeUtils.isPrimitive(type)) {
            if (type == $Type.VOID_TYPE) {
                this.aconst_null();
            }
            else {
                final $Type boxed = TypeUtils.getBoxedType(type);
                this.new_instance(boxed);
                if (type.getSize() == 2) {
                    this.dup_x2();
                    this.dup_x2();
                    this.pop();
                }
                else {
                    this.dup_x1();
                    this.swap();
                }
                this.invoke_constructor(boxed, new Signature("<init>", $Type.VOID_TYPE, new $Type[] { type }));
            }
        }
    }
    
    public void unbox(final $Type type) {
        $Type t = Constants.TYPE_NUMBER;
        Signature sig = null;
        switch (type.getSort()) {
            case 0: {
                return;
            }
            case 2: {
                t = Constants.TYPE_CHARACTER;
                sig = CodeEmitter.CHAR_VALUE;
                break;
            }
            case 1: {
                t = Constants.TYPE_BOOLEAN;
                sig = CodeEmitter.BOOLEAN_VALUE;
                break;
            }
            case 8: {
                sig = CodeEmitter.DOUBLE_VALUE;
                break;
            }
            case 6: {
                sig = CodeEmitter.FLOAT_VALUE;
                break;
            }
            case 7: {
                sig = CodeEmitter.LONG_VALUE;
                break;
            }
            case 3:
            case 4:
            case 5: {
                sig = CodeEmitter.INT_VALUE;
                break;
            }
        }
        if (sig == null) {
            this.checkcast(type);
        }
        else {
            this.checkcast(t);
            this.invoke_virtual(t, sig);
        }
    }
    
    public void create_arg_array() {
        this.push(this.state.argumentTypes.length);
        this.newarray();
        for (int i = 0; i < this.state.argumentTypes.length; ++i) {
            this.dup();
            this.push(i);
            this.load_arg(i);
            this.box(this.state.argumentTypes[i]);
            this.aastore();
        }
    }
    
    public void zero_or_null(final $Type type) {
        if (TypeUtils.isPrimitive(type)) {
            switch (type.getSort()) {
                case 8: {
                    this.push(0.0);
                    return;
                }
                case 7: {
                    this.push(0L);
                    return;
                }
                case 6: {
                    this.push(0.0f);
                    return;
                }
                case 0: {
                    this.aconst_null();
                    break;
                }
            }
            this.push(0);
        }
        else {
            this.aconst_null();
        }
    }
    
    public void unbox_or_zero(final $Type type) {
        if (TypeUtils.isPrimitive(type)) {
            if (type != $Type.VOID_TYPE) {
                final $Label nonNull = this.make_label();
                final $Label end = this.make_label();
                this.dup();
                this.ifnonnull(nonNull);
                this.pop();
                this.zero_or_null(type);
                this.goTo(end);
                this.mark(nonNull);
                this.unbox(type);
                this.mark(end);
            }
        }
        else {
            this.checkcast(type);
        }
    }
    
    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        if (!TypeUtils.isAbstract(this.state.access)) {
            this.mv.visitMaxs(0, 0);
        }
    }
    
    public void invoke(final MethodInfo method, final $Type virtualType) {
        final ClassInfo classInfo = method.getClassInfo();
        final $Type type = classInfo.getType();
        final Signature sig = method.getSignature();
        if (sig.getName().equals("<init>")) {
            this.invoke_constructor(type, sig);
        }
        else if (TypeUtils.isInterface(classInfo.getModifiers())) {
            this.invoke_interface(type, sig);
        }
        else if (TypeUtils.isStatic(method.getModifiers())) {
            this.invoke_static(type, sig);
        }
        else {
            this.invoke_virtual(virtualType, sig);
        }
    }
    
    public void invoke(final MethodInfo method) {
        this.invoke(method, method.getClassInfo().getType());
    }
    
    static {
        BOOLEAN_VALUE = TypeUtils.parseSignature("boolean booleanValue()");
        CHAR_VALUE = TypeUtils.parseSignature("char charValue()");
        LONG_VALUE = TypeUtils.parseSignature("long longValue()");
        DOUBLE_VALUE = TypeUtils.parseSignature("double doubleValue()");
        FLOAT_VALUE = TypeUtils.parseSignature("float floatValue()");
        INT_VALUE = TypeUtils.parseSignature("int intValue()");
        CSTRUCT_NULL = TypeUtils.parseConstructor("");
        CSTRUCT_STRING = TypeUtils.parseConstructor("String");
    }
    
    private static class State extends MethodInfo
    {
        ClassInfo classInfo;
        int access;
        Signature sig;
        $Type[] argumentTypes;
        int localOffset;
        $Type[] exceptionTypes;
        
        State(final ClassInfo classInfo, final int access, final Signature sig, final $Type[] exceptionTypes) {
            this.classInfo = classInfo;
            this.access = access;
            this.sig = sig;
            this.exceptionTypes = exceptionTypes;
            this.localOffset = (TypeUtils.isStatic(access) ? 0 : 1);
            this.argumentTypes = sig.getArgumentTypes();
        }
        
        @Override
        public ClassInfo getClassInfo() {
            return this.classInfo;
        }
        
        @Override
        public int getModifiers() {
            return this.access;
        }
        
        @Override
        public Signature getSignature() {
            return this.sig;
        }
        
        @Override
        public $Type[] getExceptionTypes() {
            return this.exceptionTypes;
        }
        
        public $Attribute getAttribute() {
            return null;
        }
    }
}
