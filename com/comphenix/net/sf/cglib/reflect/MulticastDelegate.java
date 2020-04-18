package com.comphenix.net.sf.cglib.reflect;

import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.Local;
import com.comphenix.net.sf.cglib.core.ProcessArrayCallback;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.security.ProtectionDomain;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class MulticastDelegate implements Cloneable
{
    protected Object[] targets;
    
    protected MulticastDelegate() {
        this.targets = new Object[0];
    }
    
    public List getTargets() {
        return new ArrayList(Arrays.asList(this.targets));
    }
    
    public abstract MulticastDelegate add(final Object p0);
    
    protected MulticastDelegate addHelper(final Object target) {
        final MulticastDelegate copy = this.newInstance();
        copy.targets = new Object[this.targets.length + 1];
        System.arraycopy(this.targets, 0, copy.targets, 0, this.targets.length);
        copy.targets[this.targets.length] = target;
        return copy;
    }
    
    public MulticastDelegate remove(final Object target) {
        for (int i = this.targets.length - 1; i >= 0; --i) {
            if (this.targets[i].equals(target)) {
                final MulticastDelegate copy = this.newInstance();
                copy.targets = new Object[this.targets.length - 1];
                System.arraycopy(this.targets, 0, copy.targets, 0, i);
                System.arraycopy(this.targets, i + 1, copy.targets, i, this.targets.length - i - 1);
                return copy;
            }
        }
        return this;
    }
    
    public abstract MulticastDelegate newInstance();
    
    public static MulticastDelegate create(final Class iface) {
        final Generator gen = new Generator();
        gen.setInterface(iface);
        return gen.create();
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private static final $Type MULTICAST_DELEGATE;
        private static final Signature NEW_INSTANCE;
        private static final Signature ADD_DELEGATE;
        private static final Signature ADD_HELPER;
        private Class iface;
        
        public Generator() {
            super(Generator.SOURCE);
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return this.iface.getClassLoader();
        }
        
        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(this.iface);
        }
        
        public void setInterface(final Class iface) {
            this.iface = iface;
        }
        
        public MulticastDelegate create() {
            this.setNamePrefix(MulticastDelegate.class.getName());
            return (MulticastDelegate)super.create(this.iface.getName());
        }
        
        public void generateClass(final $ClassVisitor cv) {
            final MethodInfo method = ReflectUtils.getMethodInfo(ReflectUtils.findInterfaceMethod(this.iface));
            final ClassEmitter ce = new ClassEmitter(cv);
            ce.begin_class(46, 1, this.getClassName(), Generator.MULTICAST_DELEGATE, new $Type[] { $Type.getType(this.iface) }, "<generated>");
            EmitUtils.null_constructor(ce);
            this.emitProxy(ce, method);
            CodeEmitter e = ce.begin_method(1, Generator.NEW_INSTANCE, null);
            e.new_instance_this();
            e.dup();
            e.invoke_constructor_this();
            e.return_value();
            e.end_method();
            e = ce.begin_method(1, Generator.ADD_DELEGATE, null);
            e.load_this();
            e.load_arg(0);
            e.checkcast($Type.getType(this.iface));
            e.invoke_virtual_this(Generator.ADD_HELPER);
            e.return_value();
            e.end_method();
            ce.end_class();
        }
        
        private void emitProxy(final ClassEmitter ce, final MethodInfo method) {
            int modifiers = 1;
            if ((method.getModifiers() & 0x80) == 0x80) {
                modifiers |= 0x80;
            }
            final CodeEmitter e = EmitUtils.begin_method(ce, method, modifiers);
            final $Type returnType = method.getSignature().getReturnType();
            final boolean returns = returnType != $Type.VOID_TYPE;
            Local result = null;
            if (returns) {
                result = e.make_local(returnType);
                e.zero_or_null(returnType);
                e.store_local(result);
            }
            e.load_this();
            e.super_getfield("targets", Constants.TYPE_OBJECT_ARRAY);
            final Local result2 = result;
            EmitUtils.process_array(e, Constants.TYPE_OBJECT_ARRAY, new ProcessArrayCallback() {
                public void processElement(final $Type type) {
                    e.checkcast($Type.getType(Generator.this.iface));
                    e.load_args();
                    e.invoke(method);
                    if (returns) {
                        e.store_local(result2);
                    }
                }
            });
            if (returns) {
                e.load_local(result);
            }
            e.return_value();
            e.end_method();
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            return ((MulticastDelegate)ReflectUtils.newInstance(type)).newInstance();
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return ((MulticastDelegate)instance).newInstance();
        }
        
        static {
            SOURCE = new Source(MulticastDelegate.class.getName());
            MULTICAST_DELEGATE = TypeUtils.parseType("com.comphenix.net.sf.cglib.reflect.MulticastDelegate");
            NEW_INSTANCE = new Signature("newInstance", Generator.MULTICAST_DELEGATE, new $Type[0]);
            ADD_DELEGATE = new Signature("add", Generator.MULTICAST_DELEGATE, new $Type[] { Constants.TYPE_OBJECT });
            ADD_HELPER = new Signature("addHelper", Generator.MULTICAST_DELEGATE, new $Type[] { Constants.TYPE_OBJECT });
        }
    }
}
