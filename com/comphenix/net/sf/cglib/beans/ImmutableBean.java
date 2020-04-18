package com.comphenix.net.sf.cglib.beans;

import com.comphenix.net.sf.cglib.core.MethodInfo;
import java.lang.reflect.Method;
import java.beans.PropertyDescriptor;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.security.ProtectionDomain;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;

public class ImmutableBean
{
    private static final $Type ILLEGAL_STATE_EXCEPTION;
    private static final Signature CSTRUCT_OBJECT;
    private static final Class[] OBJECT_CLASSES;
    private static final String FIELD_NAME = "CGLIB$RWBean";
    
    private ImmutableBean() {
    }
    
    public static Object create(final Object bean) {
        final Generator gen = new Generator();
        gen.setBean(bean);
        return gen.create();
    }
    
    static {
        ILLEGAL_STATE_EXCEPTION = TypeUtils.parseType("IllegalStateException");
        CSTRUCT_OBJECT = TypeUtils.parseConstructor("Object");
        OBJECT_CLASSES = new Class[] { Object.class };
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private Object bean;
        private Class target;
        
        public Generator() {
            super(Generator.SOURCE);
        }
        
        public void setBean(final Object bean) {
            this.bean = bean;
            this.target = bean.getClass();
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return this.target.getClassLoader();
        }
        
        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(this.target);
        }
        
        public Object create() {
            final String name = this.target.getName();
            this.setNamePrefix(name);
            return super.create(name);
        }
        
        public void generateClass(final $ClassVisitor v) {
            final $Type targetType = $Type.getType(this.target);
            final ClassEmitter ce = new ClassEmitter(v);
            ce.begin_class(46, 1, this.getClassName(), targetType, null, "<generated>");
            ce.declare_field(18, "CGLIB$RWBean", targetType, null);
            CodeEmitter e = ce.begin_method(1, ImmutableBean.CSTRUCT_OBJECT, null);
            e.load_this();
            e.super_invoke_constructor();
            e.load_this();
            e.load_arg(0);
            e.checkcast(targetType);
            e.putfield("CGLIB$RWBean");
            e.return_value();
            e.end_method();
            final PropertyDescriptor[] descriptors = ReflectUtils.getBeanProperties(this.target);
            final Method[] getters = ReflectUtils.getPropertyMethods(descriptors, true, false);
            final Method[] setters = ReflectUtils.getPropertyMethods(descriptors, false, true);
            for (int i = 0; i < getters.length; ++i) {
                final MethodInfo getter = ReflectUtils.getMethodInfo(getters[i]);
                e = EmitUtils.begin_method(ce, getter, 1);
                e.load_this();
                e.getfield("CGLIB$RWBean");
                e.invoke(getter);
                e.return_value();
                e.end_method();
            }
            for (int i = 0; i < setters.length; ++i) {
                final MethodInfo setter = ReflectUtils.getMethodInfo(setters[i]);
                e = EmitUtils.begin_method(ce, setter, 1);
                e.throw_exception(ImmutableBean.ILLEGAL_STATE_EXCEPTION, "Bean is immutable");
                e.end_method();
            }
            ce.end_class();
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            return ReflectUtils.newInstance(type, ImmutableBean.OBJECT_CLASSES, new Object[] { this.bean });
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return this.firstInstance(instance.getClass());
        }
        
        static {
            SOURCE = new Source(ImmutableBean.class.getName());
        }
    }
}
