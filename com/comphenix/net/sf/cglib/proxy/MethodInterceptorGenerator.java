package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.core.ObjectSwitchCallback;
import com.comphenix.net.sf.cglib.core.Local;
import com.comphenix.net.sf.cglib.core.ClassInfo;
import java.util.Collection;
import com.comphenix.net.sf.cglib.core.CollectionUtils;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.util.Iterator;
import java.util.Map;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import java.util.HashMap;
import java.util.List;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.core.Transformer;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;

class MethodInterceptorGenerator implements CallbackGenerator
{
    public static final MethodInterceptorGenerator INSTANCE;
    static final String EMPTY_ARGS_NAME = "CGLIB$emptyArgs";
    static final String FIND_PROXY_NAME = "CGLIB$findMethodProxy";
    static final Class[] FIND_PROXY_TYPES;
    private static final $Type ABSTRACT_METHOD_ERROR;
    private static final $Type METHOD;
    private static final $Type REFLECT_UTILS;
    private static final $Type METHOD_PROXY;
    private static final $Type METHOD_INTERCEPTOR;
    private static final Signature GET_DECLARED_METHODS;
    private static final Signature GET_DECLARING_CLASS;
    private static final Signature FIND_METHODS;
    private static final Signature MAKE_PROXY;
    private static final Signature INTERCEPT;
    private static final Signature FIND_PROXY;
    private static final Signature TO_STRING;
    private static final Transformer METHOD_TO_CLASS;
    private static final Signature CSTRUCT_SIGNATURE;
    
    private String getMethodField(final Signature impl) {
        return impl.getName() + "$Method";
    }
    
    private String getMethodProxyField(final Signature impl) {
        return impl.getName() + "$Proxy";
    }
    
    public void generate(final ClassEmitter ce, final Context context, final List methods) {
        final Map sigMap = new HashMap();
        for (final MethodInfo method : methods) {
            final Signature sig = method.getSignature();
            final Signature impl = context.getImplSignature(method);
            final String methodField = this.getMethodField(impl);
            final String methodProxyField = this.getMethodProxyField(impl);
            sigMap.put(sig.toString(), methodProxyField);
            ce.declare_field(26, methodField, MethodInterceptorGenerator.METHOD, null);
            ce.declare_field(26, methodProxyField, MethodInterceptorGenerator.METHOD_PROXY, null);
            ce.declare_field(26, "CGLIB$emptyArgs", Constants.TYPE_OBJECT_ARRAY, null);
            CodeEmitter e = ce.begin_method(16, impl, method.getExceptionTypes());
            superHelper(e, method, context);
            e.return_value();
            e.end_method();
            e = context.beginMethod(ce, method);
            final $Label nullInterceptor = e.make_label();
            context.emitCallback(e, context.getIndex(method));
            e.dup();
            e.ifnull(nullInterceptor);
            e.load_this();
            e.getfield(methodField);
            if (sig.getArgumentTypes().length == 0) {
                e.getfield("CGLIB$emptyArgs");
            }
            else {
                e.create_arg_array();
            }
            e.getfield(methodProxyField);
            e.invoke_interface(MethodInterceptorGenerator.METHOD_INTERCEPTOR, MethodInterceptorGenerator.INTERCEPT);
            e.unbox_or_zero(sig.getReturnType());
            e.return_value();
            e.mark(nullInterceptor);
            superHelper(e, method, context);
            e.return_value();
            e.end_method();
        }
        this.generateFindProxy(ce, sigMap);
    }
    
    private static void superHelper(final CodeEmitter e, final MethodInfo method, final Context context) {
        if (TypeUtils.isAbstract(method.getModifiers())) {
            e.throw_exception(MethodInterceptorGenerator.ABSTRACT_METHOD_ERROR, method.toString() + " is abstract");
        }
        else {
            e.load_this();
            context.emitLoadArgsAndInvoke(e, method);
        }
    }
    
    public void generateStatic(final CodeEmitter e, final Context context, final List methods) throws Exception {
        e.push(0);
        e.newarray();
        e.putfield("CGLIB$emptyArgs");
        final Local thisclass = e.make_local();
        final Local declaringclass = e.make_local();
        EmitUtils.load_class_this(e);
        e.store_local(thisclass);
        final Map methodsByClass = CollectionUtils.bucket(methods, MethodInterceptorGenerator.METHOD_TO_CLASS);
        for (final ClassInfo classInfo : methodsByClass.keySet()) {
            final List classMethods = methodsByClass.get(classInfo);
            e.push(2 * classMethods.size());
            e.newarray(Constants.TYPE_STRING);
            for (int index = 0; index < classMethods.size(); ++index) {
                final MethodInfo method = classMethods.get(index);
                final Signature sig = method.getSignature();
                e.dup();
                e.push(2 * index);
                e.push(sig.getName());
                e.aastore();
                e.dup();
                e.push(2 * index + 1);
                e.push(sig.getDescriptor());
                e.aastore();
            }
            EmitUtils.load_class(e, classInfo.getType());
            e.dup();
            e.store_local(declaringclass);
            e.invoke_virtual(Constants.TYPE_CLASS, MethodInterceptorGenerator.GET_DECLARED_METHODS);
            e.invoke_static(MethodInterceptorGenerator.REFLECT_UTILS, MethodInterceptorGenerator.FIND_METHODS);
            for (int index = 0; index < classMethods.size(); ++index) {
                final MethodInfo method = classMethods.get(index);
                final Signature sig = method.getSignature();
                final Signature impl = context.getImplSignature(method);
                e.dup();
                e.push(index);
                e.array_load(MethodInterceptorGenerator.METHOD);
                e.putfield(this.getMethodField(impl));
                e.load_local(declaringclass);
                e.load_local(thisclass);
                e.push(sig.getDescriptor());
                e.push(sig.getName());
                e.push(impl.getName());
                e.invoke_static(MethodInterceptorGenerator.METHOD_PROXY, MethodInterceptorGenerator.MAKE_PROXY);
                e.putfield(this.getMethodProxyField(impl));
            }
            e.pop();
        }
    }
    
    public void generateFindProxy(final ClassEmitter ce, final Map sigMap) {
        final CodeEmitter e = ce.begin_method(9, MethodInterceptorGenerator.FIND_PROXY, null);
        e.load_arg(0);
        e.invoke_virtual(Constants.TYPE_OBJECT, MethodInterceptorGenerator.TO_STRING);
        final ObjectSwitchCallback callback = new ObjectSwitchCallback() {
            public void processCase(final Object key, final $Label end) {
                e.getfield(sigMap.get(key));
                e.return_value();
            }
            
            public void processDefault() {
                e.aconst_null();
                e.return_value();
            }
        };
        EmitUtils.string_switch(e, (String[])sigMap.keySet().toArray(new String[0]), 1, callback);
        e.end_method();
    }
    
    static {
        INSTANCE = new MethodInterceptorGenerator();
        FIND_PROXY_TYPES = new Class[] { Signature.class };
        ABSTRACT_METHOD_ERROR = TypeUtils.parseType("AbstractMethodError");
        METHOD = TypeUtils.parseType("java.lang.reflect.Method");
        REFLECT_UTILS = TypeUtils.parseType("com.comphenix.net.sf.cglib.core.ReflectUtils");
        METHOD_PROXY = TypeUtils.parseType("com.comphenix.net.sf.cglib.proxy.MethodProxy");
        METHOD_INTERCEPTOR = TypeUtils.parseType("com.comphenix.net.sf.cglib.proxy.MethodInterceptor");
        GET_DECLARED_METHODS = TypeUtils.parseSignature("java.lang.reflect.Method[] getDeclaredMethods()");
        GET_DECLARING_CLASS = TypeUtils.parseSignature("Class getDeclaringClass()");
        FIND_METHODS = TypeUtils.parseSignature("java.lang.reflect.Method[] findMethods(String[], java.lang.reflect.Method[])");
        MAKE_PROXY = new Signature("create", MethodInterceptorGenerator.METHOD_PROXY, new $Type[] { Constants.TYPE_CLASS, Constants.TYPE_CLASS, Constants.TYPE_STRING, Constants.TYPE_STRING, Constants.TYPE_STRING });
        INTERCEPT = new Signature("intercept", Constants.TYPE_OBJECT, new $Type[] { Constants.TYPE_OBJECT, MethodInterceptorGenerator.METHOD, Constants.TYPE_OBJECT_ARRAY, MethodInterceptorGenerator.METHOD_PROXY });
        FIND_PROXY = new Signature("CGLIB$findMethodProxy", MethodInterceptorGenerator.METHOD_PROXY, new $Type[] { Constants.TYPE_SIGNATURE });
        TO_STRING = TypeUtils.parseSignature("String toString()");
        METHOD_TO_CLASS = new Transformer() {
            public Object transform(final Object value) {
                return ((MethodInfo)value).getClassInfo();
            }
        };
        CSTRUCT_SIGNATURE = TypeUtils.parseConstructor("String, String");
    }
}
