package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.Block;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.util.Iterator;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import java.util.List;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;

class InvocationHandlerGenerator implements CallbackGenerator
{
    public static final InvocationHandlerGenerator INSTANCE;
    private static final $Type INVOCATION_HANDLER;
    private static final $Type UNDECLARED_THROWABLE_EXCEPTION;
    private static final $Type METHOD;
    private static final Signature INVOKE;
    
    public void generate(final ClassEmitter ce, final Context context, final List methods) {
        for (final MethodInfo method : methods) {
            final Signature impl = context.getImplSignature(method);
            ce.declare_field(26, impl.getName(), InvocationHandlerGenerator.METHOD, null);
            final CodeEmitter e = context.beginMethod(ce, method);
            final Block handler = e.begin_block();
            context.emitCallback(e, context.getIndex(method));
            e.load_this();
            e.getfield(impl.getName());
            e.create_arg_array();
            e.invoke_interface(InvocationHandlerGenerator.INVOCATION_HANDLER, InvocationHandlerGenerator.INVOKE);
            e.unbox(method.getSignature().getReturnType());
            e.return_value();
            handler.end();
            EmitUtils.wrap_undeclared_throwable(e, handler, method.getExceptionTypes(), InvocationHandlerGenerator.UNDECLARED_THROWABLE_EXCEPTION);
            e.end_method();
        }
    }
    
    public void generateStatic(final CodeEmitter e, final Context context, final List methods) {
        for (final MethodInfo method : methods) {
            EmitUtils.load_method(e, method);
            e.putfield(context.getImplSignature(method).getName());
        }
    }
    
    static {
        INSTANCE = new InvocationHandlerGenerator();
        INVOCATION_HANDLER = TypeUtils.parseType("com.comphenix.net.sf.cglib.proxy.InvocationHandler");
        UNDECLARED_THROWABLE_EXCEPTION = TypeUtils.parseType("com.comphenix.net.sf.cglib.proxy.UndeclaredThrowableException");
        METHOD = TypeUtils.parseType("java.lang.reflect.Method");
        INVOKE = TypeUtils.parseSignature("Object invoke(Object, java.lang.reflect.Method, Object[])");
    }
}
