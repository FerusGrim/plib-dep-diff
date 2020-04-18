package com.comphenix.net.sf.cglib.transform.impl;

import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import com.comphenix.net.sf.cglib.transform.ClassEmitterTransformer;

public class AddStaticInitTransformer extends ClassEmitterTransformer
{
    private MethodInfo info;
    
    public AddStaticInitTransformer(final Method classInit) {
        this.info = ReflectUtils.getMethodInfo(classInit);
        if (!TypeUtils.isStatic(this.info.getModifiers())) {
            throw new IllegalArgumentException(classInit + " is not static");
        }
        final $Type[] types = this.info.getSignature().getArgumentTypes();
        if (types.length != 1 || !types[0].equals(Constants.TYPE_CLASS) || !this.info.getSignature().getReturnType().equals($Type.VOID_TYPE)) {
            throw new IllegalArgumentException(classInit + " illegal signature");
        }
    }
    
    @Override
    protected void init() {
        if (!TypeUtils.isInterface(this.getAccess())) {
            final CodeEmitter e = this.getStaticHook();
            EmitUtils.load_class_this(e);
            e.invoke(this.info);
        }
    }
}
