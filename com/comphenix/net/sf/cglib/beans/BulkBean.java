package com.comphenix.net.sf.cglib.beans;

import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.security.ProtectionDomain;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;
import com.comphenix.net.sf.cglib.core.KeyFactory;

public abstract class BulkBean
{
    private static final BulkBeanKey KEY_FACTORY;
    protected Class target;
    protected String[] getters;
    protected String[] setters;
    protected Class[] types;
    
    protected BulkBean() {
    }
    
    public abstract void getPropertyValues(final Object p0, final Object[] p1);
    
    public abstract void setPropertyValues(final Object p0, final Object[] p1);
    
    public Object[] getPropertyValues(final Object bean) {
        final Object[] values = new Object[this.getters.length];
        this.getPropertyValues(bean, values);
        return values;
    }
    
    public Class[] getPropertyTypes() {
        return this.types.clone();
    }
    
    public String[] getGetters() {
        return this.getters.clone();
    }
    
    public String[] getSetters() {
        return this.setters.clone();
    }
    
    public static BulkBean create(final Class target, final String[] getters, final String[] setters, final Class[] types) {
        final Generator gen = new Generator();
        gen.setTarget(target);
        gen.setGetters(getters);
        gen.setSetters(setters);
        gen.setTypes(types);
        return gen.create();
    }
    
    static {
        KEY_FACTORY = (BulkBeanKey)KeyFactory.create(BulkBeanKey.class);
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private Class target;
        private String[] getters;
        private String[] setters;
        private Class[] types;
        
        public Generator() {
            super(Generator.SOURCE);
        }
        
        public void setTarget(final Class target) {
            this.target = target;
        }
        
        public void setGetters(final String[] getters) {
            this.getters = getters;
        }
        
        public void setSetters(final String[] setters) {
            this.setters = setters;
        }
        
        public void setTypes(final Class[] types) {
            this.types = types;
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return this.target.getClassLoader();
        }
        
        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(this.target);
        }
        
        public BulkBean create() {
            this.setNamePrefix(this.target.getName());
            final String targetClassName = this.target.getName();
            final String[] typeClassNames = ReflectUtils.getNames(this.types);
            final Object key = BulkBean.KEY_FACTORY.newInstance(targetClassName, this.getters, this.setters, typeClassNames);
            return (BulkBean)super.create(key);
        }
        
        public void generateClass(final $ClassVisitor v) throws Exception {
            new BulkBeanEmitter(v, this.getClassName(), this.target, this.getters, this.setters, this.types);
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            final BulkBean instance = (BulkBean)ReflectUtils.newInstance(type);
            instance.target = this.target;
            final int length = this.getters.length;
            instance.getters = new String[length];
            System.arraycopy(this.getters, 0, instance.getters, 0, length);
            instance.setters = new String[length];
            System.arraycopy(this.setters, 0, instance.setters, 0, length);
            instance.types = new Class[this.types.length];
            System.arraycopy(this.types, 0, instance.types, 0, this.types.length);
            return instance;
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return instance;
        }
        
        static {
            SOURCE = new Source(BulkBean.class.getName());
        }
    }
    
    interface BulkBeanKey
    {
        Object newInstance(final String p0, final String[] p1, final String[] p2, final String[] p3);
    }
}
