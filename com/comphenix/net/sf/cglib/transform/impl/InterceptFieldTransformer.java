package com.comphenix.net.sf.cglib.transform.impl;

import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.Local;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.transform.ClassEmitterTransformer;

public class InterceptFieldTransformer extends ClassEmitterTransformer
{
    private static final String CALLBACK_FIELD = "$CGLIB_READ_WRITE_CALLBACK";
    private static final $Type CALLBACK;
    private static final $Type ENABLED;
    private static final Signature ENABLED_SET;
    private static final Signature ENABLED_GET;
    private InterceptFieldFilter filter;
    
    public InterceptFieldTransformer(final InterceptFieldFilter filter) {
        this.filter = filter;
    }
    
    @Override
    public void begin_class(final int version, final int access, final String className, final $Type superType, final $Type[] interfaces, final String sourceFile) {
        if (!TypeUtils.isInterface(access)) {
            super.begin_class(version, access, className, superType, TypeUtils.add(interfaces, InterceptFieldTransformer.ENABLED), sourceFile);
            super.declare_field(130, "$CGLIB_READ_WRITE_CALLBACK", InterceptFieldTransformer.CALLBACK, null);
            CodeEmitter e = super.begin_method(1, InterceptFieldTransformer.ENABLED_GET, null);
            e.load_this();
            e.getfield("$CGLIB_READ_WRITE_CALLBACK");
            e.return_value();
            e.end_method();
            e = super.begin_method(1, InterceptFieldTransformer.ENABLED_SET, null);
            e.load_this();
            e.load_arg(0);
            e.putfield("$CGLIB_READ_WRITE_CALLBACK");
            e.return_value();
            e.end_method();
        }
        else {
            super.begin_class(version, access, className, superType, interfaces, sourceFile);
        }
    }
    
    @Override
    public void declare_field(final int access, final String name, final $Type type, final Object value) {
        super.declare_field(access, name, type, value);
        if (!TypeUtils.isStatic(access)) {
            if (this.filter.acceptRead(this.getClassType(), name)) {
                this.addReadMethod(name, type);
            }
            if (this.filter.acceptWrite(this.getClassType(), name)) {
                this.addWriteMethod(name, type);
            }
        }
    }
    
    private void addReadMethod(final String name, final $Type type) {
        final CodeEmitter e = super.begin_method(1, readMethodSig(name, type.getDescriptor()), null);
        e.load_this();
        e.getfield(name);
        e.load_this();
        e.invoke_interface(InterceptFieldTransformer.ENABLED, InterceptFieldTransformer.ENABLED_GET);
        final $Label intercept = e.make_label();
        e.ifnonnull(intercept);
        e.return_value();
        e.mark(intercept);
        final Local result = e.make_local(type);
        e.store_local(result);
        e.load_this();
        e.invoke_interface(InterceptFieldTransformer.ENABLED, InterceptFieldTransformer.ENABLED_GET);
        e.load_this();
        e.push(name);
        e.load_local(result);
        e.invoke_interface(InterceptFieldTransformer.CALLBACK, readCallbackSig(type));
        if (!TypeUtils.isPrimitive(type)) {
            e.checkcast(type);
        }
        e.return_value();
        e.end_method();
    }
    
    private void addWriteMethod(final String name, final $Type type) {
        final CodeEmitter e = super.begin_method(1, writeMethodSig(name, type.getDescriptor()), null);
        e.load_this();
        e.dup();
        e.invoke_interface(InterceptFieldTransformer.ENABLED, InterceptFieldTransformer.ENABLED_GET);
        final $Label skip = e.make_label();
        e.ifnull(skip);
        e.load_this();
        e.invoke_interface(InterceptFieldTransformer.ENABLED, InterceptFieldTransformer.ENABLED_GET);
        e.load_this();
        e.push(name);
        e.load_this();
        e.getfield(name);
        e.load_arg(0);
        e.invoke_interface(InterceptFieldTransformer.CALLBACK, writeCallbackSig(type));
        if (!TypeUtils.isPrimitive(type)) {
            e.checkcast(type);
        }
        final $Label go = e.make_label();
        e.goTo(go);
        e.mark(skip);
        e.load_arg(0);
        e.mark(go);
        e.putfield(name);
        e.return_value();
        e.end_method();
    }
    
    @Override
    public CodeEmitter begin_method(final int access, final Signature sig, final $Type[] exceptions) {
        return new CodeEmitter(super.begin_method(access, sig, exceptions)) {
            @Override
            public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
                final $Type towner = TypeUtils.fromInternalName(owner);
                switch (opcode) {
                    case 180: {
                        if (InterceptFieldTransformer.this.filter.acceptRead(towner, name)) {
                            this.helper(towner, readMethodSig(name, desc));
                            return;
                        }
                        break;
                    }
                    case 181: {
                        if (InterceptFieldTransformer.this.filter.acceptWrite(towner, name)) {
                            this.helper(towner, writeMethodSig(name, desc));
                            return;
                        }
                        break;
                    }
                }
                super.visitFieldInsn(opcode, owner, name, desc);
            }
            
            private void helper(final $Type owner, final Signature sig) {
                this.invoke_virtual(owner, sig);
            }
        };
    }
    
    private static Signature readMethodSig(final String name, final String desc) {
        return new Signature("$cglib_read_" + name, "()" + desc);
    }
    
    private static Signature writeMethodSig(final String name, final String desc) {
        return new Signature("$cglib_write_" + name, "(" + desc + ")V");
    }
    
    private static Signature readCallbackSig(final $Type type) {
        final $Type remap = remap(type);
        return new Signature("read" + callbackName(remap), remap, new $Type[] { Constants.TYPE_OBJECT, Constants.TYPE_STRING, remap });
    }
    
    private static Signature writeCallbackSig(final $Type type) {
        final $Type remap = remap(type);
        return new Signature("write" + callbackName(remap), remap, new $Type[] { Constants.TYPE_OBJECT, Constants.TYPE_STRING, remap, remap });
    }
    
    private static $Type remap(final $Type type) {
        switch (type.getSort()) {
            case 9:
            case 10: {
                return Constants.TYPE_OBJECT;
            }
            default: {
                return type;
            }
        }
    }
    
    private static String callbackName(final $Type type) {
        return (type == Constants.TYPE_OBJECT) ? "Object" : TypeUtils.upperFirst(TypeUtils.getClassName(type));
    }
    
    static {
        CALLBACK = TypeUtils.parseType("com.comphenix.net.sf.cglib.transform.impl.InterceptFieldCallback");
        ENABLED = TypeUtils.parseType("com.comphenix.net.sf.cglib.transform.impl.InterceptFieldEnabled");
        ENABLED_SET = new Signature("setInterceptFieldCallback", $Type.VOID_TYPE, new $Type[] { InterceptFieldTransformer.CALLBACK });
        ENABLED_GET = new Signature("getInterceptFieldCallback", InterceptFieldTransformer.CALLBACK, new $Type[0]);
    }
}
