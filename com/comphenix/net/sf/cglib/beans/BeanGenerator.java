package com.comphenix.net.sf.cglib.beans;

import com.comphenix.net.sf.cglib.core.KeyFactory;
import java.beans.PropertyDescriptor;
import java.util.Iterator;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.security.ProtectionDomain;
import com.comphenix.net.sf.cglib.asm.$Type;
import java.util.HashMap;
import java.util.Map;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;

public class BeanGenerator extends AbstractClassGenerator
{
    private static final Source SOURCE;
    private static final BeanGeneratorKey KEY_FACTORY;
    private Class superclass;
    private Map props;
    private boolean classOnly;
    
    public BeanGenerator() {
        super(BeanGenerator.SOURCE);
        this.props = new HashMap();
    }
    
    public void setSuperclass(Class superclass) {
        if (superclass != null && superclass.equals(Object.class)) {
            superclass = null;
        }
        this.superclass = superclass;
    }
    
    public void addProperty(final String name, final Class type) {
        if (this.props.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate property name \"" + name + "\"");
        }
        this.props.put(name, $Type.getType(type));
    }
    
    @Override
    protected ClassLoader getDefaultClassLoader() {
        if (this.superclass != null) {
            return this.superclass.getClassLoader();
        }
        return null;
    }
    
    @Override
    protected ProtectionDomain getProtectionDomain() {
        return ReflectUtils.getProtectionDomain(this.superclass);
    }
    
    public Object create() {
        this.classOnly = false;
        return this.createHelper();
    }
    
    public Object createClass() {
        this.classOnly = true;
        return this.createHelper();
    }
    
    private Object createHelper() {
        if (this.superclass != null) {
            this.setNamePrefix(this.superclass.getName());
        }
        final String superName = (this.superclass != null) ? this.superclass.getName() : "java.lang.Object";
        final Object key = BeanGenerator.KEY_FACTORY.newInstance(superName, this.props);
        return super.create(key);
    }
    
    public void generateClass(final $ClassVisitor v) throws Exception {
        final int size = this.props.size();
        final String[] names = (String[])this.props.keySet().toArray(new String[size]);
        final $Type[] types = new $Type[size];
        for (int i = 0; i < size; ++i) {
            types[i] = this.props.get(names[i]);
        }
        final ClassEmitter ce = new ClassEmitter(v);
        ce.begin_class(46, 1, this.getClassName(), (this.superclass != null) ? $Type.getType(this.superclass) : Constants.TYPE_OBJECT, null, null);
        EmitUtils.null_constructor(ce);
        EmitUtils.add_properties(ce, names, types);
        ce.end_class();
    }
    
    @Override
    protected Object firstInstance(final Class type) {
        if (this.classOnly) {
            return type;
        }
        return ReflectUtils.newInstance(type);
    }
    
    @Override
    protected Object nextInstance(final Object instance) {
        final Class protoclass = (instance instanceof Class) ? ((Class)instance) : instance.getClass();
        if (this.classOnly) {
            return protoclass;
        }
        return ReflectUtils.newInstance(protoclass);
    }
    
    public static void addProperties(final BeanGenerator gen, final Map props) {
        for (final String name : props.keySet()) {
            gen.addProperty(name, props.get(name));
        }
    }
    
    public static void addProperties(final BeanGenerator gen, final Class type) {
        addProperties(gen, ReflectUtils.getBeanProperties(type));
    }
    
    public static void addProperties(final BeanGenerator gen, final PropertyDescriptor[] descriptors) {
        for (int i = 0; i < descriptors.length; ++i) {
            gen.addProperty(descriptors[i].getName(), descriptors[i].getPropertyType());
        }
    }
    
    static {
        SOURCE = new Source(BeanGenerator.class.getName());
        KEY_FACTORY = (BeanGeneratorKey)KeyFactory.create(BeanGeneratorKey.class);
    }
    
    interface BeanGeneratorKey
    {
        Object newInstance(final String p0, final Map p1);
    }
}
