package com.comphenix.net.sf.cglib.reflect;

import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.security.ProtectionDomain;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;
import com.comphenix.net.sf.cglib.core.KeyFactory;

public abstract class MethodDelegate
{
    private static final MethodDelegateKey KEY_FACTORY;
    protected Object target;
    protected String eqMethod;
    
    public static MethodDelegate createStatic(final Class targetClass, final String methodName, final Class iface) {
        final Generator gen = new Generator();
        gen.setTargetClass(targetClass);
        gen.setMethodName(methodName);
        gen.setInterface(iface);
        return gen.create();
    }
    
    public static MethodDelegate create(final Object target, final String methodName, final Class iface) {
        final Generator gen = new Generator();
        gen.setTarget(target);
        gen.setMethodName(methodName);
        gen.setInterface(iface);
        return gen.create();
    }
    
    @Override
    public boolean equals(final Object obj) {
        final MethodDelegate other = (MethodDelegate)obj;
        return other != null && this.target == other.target && this.eqMethod.equals(other.eqMethod);
    }
    
    @Override
    public int hashCode() {
        return this.target.hashCode() ^ this.eqMethod.hashCode();
    }
    
    public Object getTarget() {
        return this.target;
    }
    
    public abstract MethodDelegate newInstance(final Object p0);
    
    static {
        KEY_FACTORY = (MethodDelegateKey)KeyFactory.create(MethodDelegateKey.class, KeyFactory.CLASS_BY_NAME);
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private static final $Type METHOD_DELEGATE;
        private static final Signature NEW_INSTANCE;
        private Object target;
        private Class targetClass;
        private String methodName;
        private Class iface;
        
        public Generator() {
            super(Generator.SOURCE);
        }
        
        public void setTarget(final Object target) {
            this.target = target;
            this.targetClass = target.getClass();
        }
        
        public void setTargetClass(final Class targetClass) {
            this.targetClass = targetClass;
        }
        
        public void setMethodName(final String methodName) {
            this.methodName = methodName;
        }
        
        public void setInterface(final Class iface) {
            this.iface = iface;
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return this.targetClass.getClassLoader();
        }
        
        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(this.targetClass);
        }
        
        public MethodDelegate create() {
            this.setNamePrefix(this.targetClass.getName());
            final Object key = MethodDelegate.KEY_FACTORY.newInstance(this.targetClass, this.methodName, this.iface);
            return (MethodDelegate)super.create(key);
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            return ((MethodDelegate)ReflectUtils.newInstance(type)).newInstance(this.target);
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return ((MethodDelegate)instance).newInstance(this.target);
        }
        
        public void generateClass(final $ClassVisitor v) throws NoSuchMethodException {
            final Method proxy = ReflectUtils.findInterfaceMethod(this.iface);
            final Method method = this.targetClass.getMethod(this.methodName, (Class[])proxy.getParameterTypes());
            if (!proxy.getReturnType().isAssignableFrom(method.getReturnType())) {
                throw new IllegalArgumentException("incompatible return types");
            }
            final MethodInfo methodInfo = ReflectUtils.getMethodInfo(method);
            final boolean isStatic = TypeUtils.isStatic(methodInfo.getModifiers());
            if (this.target == null ^ isStatic) {
                throw new IllegalArgumentException("Static method " + (isStatic ? "not " : "") + "expected");
            }
            final ClassEmitter ce = new ClassEmitter(v);
            ce.begin_class(46, 1, this.getClassName(), Generator.METHOD_DELEGATE, new $Type[] { $Type.getType(this.iface) }, "<generated>");
            ce.declare_field(26, "eqMethod", Constants.TYPE_STRING, null);
            EmitUtils.null_constructor(ce);
            final MethodInfo proxied = ReflectUtils.getMethodInfo(this.iface.getDeclaredMethods()[0]);
            int modifiers = 1;
            if ((proxied.getModifiers() & 0x80) == 0x80) {
                modifiers |= 0x80;
            }
            CodeEmitter e = EmitUtils.begin_method(ce, proxied, modifiers);
            e.load_this();
            e.super_getfield("target", Constants.TYPE_OBJECT);
            e.checkcast(methodInfo.getClassInfo().getType());
            e.load_args();
            e.invoke(methodInfo);
            e.return_value();
            e.end_method();
            e = ce.begin_method(1, Generator.NEW_INSTANCE, null);
            e.new_instance_this();
            e.dup();
            e.dup2();
            e.invoke_constructor_this();
            e.getfield("eqMethod");
            e.super_putfield("eqMethod", Constants.TYPE_STRING);
            e.load_arg(0);
            e.super_putfield("target", Constants.TYPE_OBJECT);
            e.return_value();
            e.end_method();
            e = ce.begin_static();
            e.push(methodInfo.getSignature().toString());
            e.putfield("eqMethod");
            e.return_value();
            e.end_method();
            ce.end_class();
        }
        
        static {
            SOURCE = new Source(MethodDelegate.class.getName());
            METHOD_DELEGATE = TypeUtils.parseType("com.comphenix.net.sf.cglib.reflect.MethodDelegate");
            NEW_INSTANCE = new Signature("newInstance", Generator.METHOD_DELEGATE, new $Type[] { Constants.TYPE_OBJECT });
        }
    }
    
    interface MethodDelegateKey
    {
        Object newInstance(final Class p0, final String p1, final Class p2);
    }
}
