package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.util.Iterator;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import java.util.List;
import com.comphenix.net.sf.cglib.core.ClassEmitter;

class NoOpGenerator implements CallbackGenerator
{
    public static final NoOpGenerator INSTANCE;
    
    public void generate(final ClassEmitter ce, final Context context, final List methods) {
        for (final MethodInfo method : methods) {
            if (TypeUtils.isBridge(method.getModifiers()) || (TypeUtils.isProtected(context.getOriginalModifiers(method)) && TypeUtils.isPublic(method.getModifiers()))) {
                final CodeEmitter e = EmitUtils.begin_method(ce, method);
                e.load_this();
                context.emitLoadArgsAndInvoke(e, method);
                e.return_value();
                e.end_method();
            }
        }
    }
    
    public void generateStatic(final CodeEmitter e, final Context context, final List methods) {
    }
    
    static {
        INSTANCE = new NoOpGenerator();
    }
}
