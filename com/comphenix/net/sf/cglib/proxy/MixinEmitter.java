package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.core.MethodInfo;
import java.lang.reflect.Method;
import java.util.Set;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import com.comphenix.net.sf.cglib.core.MethodWrapper;
import java.util.HashSet;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.core.ClassEmitter;

class MixinEmitter extends ClassEmitter
{
    private static final String FIELD_NAME = "CGLIB$DELEGATES";
    private static final Signature CSTRUCT_OBJECT_ARRAY;
    private static final $Type MIXIN;
    private static final Signature NEW_INSTANCE;
    
    public MixinEmitter(final $ClassVisitor v, final String className, final Class[] classes, final int[] route) {
        super(v);
        this.begin_class(46, 1, className, MixinEmitter.MIXIN, TypeUtils.getTypes(this.getInterfaces(classes)), "<generated>");
        EmitUtils.null_constructor(this);
        EmitUtils.factory_method(this, MixinEmitter.NEW_INSTANCE);
        this.declare_field(2, "CGLIB$DELEGATES", Constants.TYPE_OBJECT_ARRAY, null);
        CodeEmitter e = this.begin_method(1, MixinEmitter.CSTRUCT_OBJECT_ARRAY, null);
        e.load_this();
        e.super_invoke_constructor();
        e.load_this();
        e.load_arg(0);
        e.putfield("CGLIB$DELEGATES");
        e.return_value();
        e.end_method();
        final Set unique = new HashSet();
        for (int i = 0; i < classes.length; ++i) {
            final Method[] methods = this.getMethods(classes[i]);
            for (int j = 0; j < methods.length; ++j) {
                if (unique.add(MethodWrapper.create(methods[j]))) {
                    final MethodInfo method = ReflectUtils.getMethodInfo(methods[j]);
                    int modifiers = 1;
                    if ((method.getModifiers() & 0x80) == 0x80) {
                        modifiers |= 0x80;
                    }
                    e = EmitUtils.begin_method(this, method, modifiers);
                    e.load_this();
                    e.getfield("CGLIB$DELEGATES");
                    e.aaload((route != null) ? route[i] : i);
                    e.checkcast(method.getClassInfo().getType());
                    e.load_args();
                    e.invoke(method);
                    e.return_value();
                    e.end_method();
                }
            }
        }
        this.end_class();
    }
    
    protected Class[] getInterfaces(final Class[] classes) {
        return classes;
    }
    
    protected Method[] getMethods(final Class type) {
        return type.getMethods();
    }
    
    static {
        CSTRUCT_OBJECT_ARRAY = TypeUtils.parseConstructor("Object[]");
        MIXIN = TypeUtils.parseType("com.comphenix.net.sf.cglib.proxy.Mixin");
        NEW_INSTANCE = new Signature("newInstance", MixinEmitter.MIXIN, new $Type[] { Constants.TYPE_OBJECT_ARRAY });
    }
}
