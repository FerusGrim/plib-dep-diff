package com.comphenix.net.sf.cglib.beans;

import com.comphenix.net.sf.cglib.core.KeyFactory;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.security.ProtectionDomain;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;

public abstract class BeanMap implements Map
{
    public static final int REQUIRE_GETTER = 1;
    public static final int REQUIRE_SETTER = 2;
    protected Object bean;
    
    public static BeanMap create(final Object bean) {
        final Generator gen = new Generator();
        gen.setBean(bean);
        return gen.create();
    }
    
    public abstract BeanMap newInstance(final Object p0);
    
    public abstract Class getPropertyType(final String p0);
    
    protected BeanMap() {
    }
    
    protected BeanMap(final Object bean) {
        this.setBean(bean);
    }
    
    public Object get(final Object key) {
        return this.get(this.bean, key);
    }
    
    public Object put(final Object key, final Object value) {
        return this.put(this.bean, key, value);
    }
    
    public abstract Object get(final Object p0, final Object p1);
    
    public abstract Object put(final Object p0, final Object p1, final Object p2);
    
    public void setBean(final Object bean) {
        this.bean = bean;
    }
    
    public Object getBean() {
        return this.bean;
    }
    
    public void clear() {
        throw new UnsupportedOperationException();
    }
    
    public boolean containsKey(final Object key) {
        return this.keySet().contains(key);
    }
    
    public boolean containsValue(final Object value) {
        final Iterator it = this.keySet().iterator();
        while (it.hasNext()) {
            final Object v = this.get(it.next());
            if ((value == null && v == null) || (value != null && value.equals(v))) {
                return true;
            }
        }
        return false;
    }
    
    public int size() {
        return this.keySet().size();
    }
    
    public boolean isEmpty() {
        return this.size() == 0;
    }
    
    public Object remove(final Object key) {
        throw new UnsupportedOperationException();
    }
    
    public void putAll(final Map t) {
        for (final Object key : t.keySet()) {
            this.put(key, t.get(key));
        }
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == null || !(o instanceof Map)) {
            return false;
        }
        final Map other = (Map)o;
        if (this.size() != other.size()) {
            return false;
        }
        for (final Object key : this.keySet()) {
            if (!other.containsKey(key)) {
                return false;
            }
            final Object v1 = this.get(key);
            final Object v2 = other.get(key);
            if (v1 == null) {
                if (v2 == null) {
                    continue;
                }
                return false;
            }
            else {
                if (!v1.equals(v2)) {
                    return false;
                }
                continue;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int code = 0;
        for (final Object key : this.keySet()) {
            final Object value = this.get(key);
            code += (((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode()));
        }
        return code;
    }
    
    public Set entrySet() {
        final HashMap copy = new HashMap();
        for (final Object key : this.keySet()) {
            copy.put(key, this.get(key));
        }
        return Collections.unmodifiableMap((Map<?, ?>)copy).entrySet();
    }
    
    public Collection values() {
        final Set keys = this.keySet();
        final List values = new ArrayList(keys.size());
        final Iterator it = keys.iterator();
        while (it.hasNext()) {
            values.add(this.get(it.next()));
        }
        return Collections.unmodifiableCollection((Collection<?>)values);
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append('{');
        final Iterator it = this.keySet().iterator();
        while (it.hasNext()) {
            final Object key = it.next();
            sb.append(key);
            sb.append('=');
            sb.append(this.get(key));
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append('}');
        return sb.toString();
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private static final BeanMapKey KEY_FACTORY;
        private Object bean;
        private Class beanClass;
        private int require;
        
        public Generator() {
            super(Generator.SOURCE);
        }
        
        public void setBean(final Object bean) {
            this.bean = bean;
            if (bean != null) {
                this.beanClass = bean.getClass();
            }
        }
        
        public void setBeanClass(final Class beanClass) {
            this.beanClass = beanClass;
        }
        
        public void setRequire(final int require) {
            this.require = require;
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return this.beanClass.getClassLoader();
        }
        
        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(this.beanClass);
        }
        
        public BeanMap create() {
            if (this.beanClass == null) {
                throw new IllegalArgumentException("Class of bean unknown");
            }
            this.setNamePrefix(this.beanClass.getName());
            return (BeanMap)super.create(Generator.KEY_FACTORY.newInstance(this.beanClass, this.require));
        }
        
        public void generateClass(final $ClassVisitor v) throws Exception {
            new BeanMapEmitter(v, this.getClassName(), this.beanClass, this.require);
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            return ((BeanMap)ReflectUtils.newInstance(type)).newInstance(this.bean);
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return ((BeanMap)instance).newInstance(this.bean);
        }
        
        static {
            SOURCE = new Source(BeanMap.class.getName());
            KEY_FACTORY = (BeanMapKey)KeyFactory.create(BeanMapKey.class, KeyFactory.CLASS_BY_NAME);
        }
        
        interface BeanMapKey
        {
            Object newInstance(final Class p0, final int p1);
        }
    }
}
