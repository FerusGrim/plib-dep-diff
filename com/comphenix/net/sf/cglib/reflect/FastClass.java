package com.comphenix.net.sf.cglib.reflect;

import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.security.ProtectionDomain;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.Signature;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.core.Constants;
import java.lang.reflect.InvocationTargetException;

public abstract class FastClass
{
    private Class type;
    
    protected FastClass() {
        throw new Error("Using the FastClass empty constructor--please report to the cglib-devel mailing list");
    }
    
    protected FastClass(final Class type) {
        this.type = type;
    }
    
    public static FastClass create(final Class type) {
        return create(type.getClassLoader(), type);
    }
    
    public static FastClass create(final ClassLoader loader, final Class type) {
        final Generator gen = new Generator();
        gen.setType(type);
        gen.setClassLoader(loader);
        return gen.create();
    }
    
    public Object invoke(final String name, final Class[] parameterTypes, final Object obj, final Object[] args) throws InvocationTargetException {
        return this.invoke(this.getIndex(name, parameterTypes), obj, args);
    }
    
    public Object newInstance() throws InvocationTargetException {
        return this.newInstance(this.getIndex(Constants.EMPTY_CLASS_ARRAY), null);
    }
    
    public Object newInstance(final Class[] parameterTypes, final Object[] args) throws InvocationTargetException {
        return this.newInstance(this.getIndex(parameterTypes), args);
    }
    
    public FastMethod getMethod(final Method method) {
        return new FastMethod(this, method);
    }
    
    public FastConstructor getConstructor(final Constructor constructor) {
        return new FastConstructor(this, constructor);
    }
    
    public FastMethod getMethod(final String name, final Class[] parameterTypes) {
        try {
            return this.getMethod(this.type.getMethod(name, (Class[])parameterTypes));
        }
        catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }
    
    public FastConstructor getConstructor(final Class[] parameterTypes) {
        try {
            return this.getConstructor(this.type.getConstructor((Class[])parameterTypes));
        }
        catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }
    
    public String getName() {
        return this.type.getName();
    }
    
    public Class getJavaClass() {
        return this.type;
    }
    
    @Override
    public String toString() {
        return this.type.toString();
    }
    
    @Override
    public int hashCode() {
        return this.type.hashCode();
    }
    
    @Override
    public boolean equals(final Object o) {
        return o != null && o instanceof FastClass && this.type.equals(((FastClass)o).type);
    }
    
    public abstract int getIndex(final String p0, final Class[] p1);
    
    public abstract int getIndex(final Class[] p0);
    
    public abstract Object invoke(final int p0, final Object p1, final Object[] p2) throws InvocationTargetException;
    
    public abstract Object newInstance(final int p0, final Object[] p1) throws InvocationTargetException;
    
    public abstract int getIndex(final Signature p0);
    
    public abstract int getMaxIndex();
    
    protected static String getSignatureWithoutReturnType(final String name, final Class[] parameterTypes) {
        final StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append('(');
        for (int i = 0; i < parameterTypes.length; ++i) {
            sb.append($Type.getDescriptor(parameterTypes[i]));
        }
        sb.append(')');
        return sb.toString();
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private Class type;
        
        public Generator() {
            super(Generator.SOURCE);
        }
        
        public void setType(final Class type) {
            this.type = type;
        }
        
        public FastClass create() {
            this.setNamePrefix(this.type.getName());
            return (FastClass)super.create(this.type.getName());
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return this.type.getClassLoader();
        }
        
        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(this.type);
        }
        
        public void generateClass(final $ClassVisitor v) throws Exception {
            new FastClassEmitter(v, this.getClassName(), this.type);
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            return ReflectUtils.newInstance(type, new Class[] { Class.class }, new Object[] { this.type });
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return instance;
        }
        
        static {
            SOURCE = new Source(FastClass.class.getName());
        }
    }
}
