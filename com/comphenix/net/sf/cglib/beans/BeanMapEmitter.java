package com.comphenix.net.sf.cglib.beans;

import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.core.ObjectSwitchCallback;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.ClassEmitter;

class BeanMapEmitter extends ClassEmitter
{
    private static final $Type BEAN_MAP;
    private static final $Type FIXED_KEY_SET;
    private static final Signature CSTRUCT_OBJECT;
    private static final Signature CSTRUCT_STRING_ARRAY;
    private static final Signature BEAN_MAP_GET;
    private static final Signature BEAN_MAP_PUT;
    private static final Signature KEY_SET;
    private static final Signature NEW_INSTANCE;
    private static final Signature GET_PROPERTY_TYPE;
    
    public BeanMapEmitter(final $ClassVisitor v, final String className, final Class type, final int require) {
        super(v);
        this.begin_class(46, 1, className, BeanMapEmitter.BEAN_MAP, null, "<generated>");
        EmitUtils.null_constructor(this);
        EmitUtils.factory_method(this, BeanMapEmitter.NEW_INSTANCE);
        this.generateConstructor();
        final Map getters = this.makePropertyMap(ReflectUtils.getBeanGetters(type));
        final Map setters = this.makePropertyMap(ReflectUtils.getBeanSetters(type));
        final Map allProps = new HashMap();
        allProps.putAll(getters);
        allProps.putAll(setters);
        if (require != 0) {
            final Iterator it = allProps.keySet().iterator();
            while (it.hasNext()) {
                final String name = it.next();
                if (((require & 0x1) != 0x0 && !getters.containsKey(name)) || ((require & 0x2) != 0x0 && !setters.containsKey(name))) {
                    it.remove();
                    getters.remove(name);
                    setters.remove(name);
                }
            }
        }
        this.generateGet(type, getters);
        this.generatePut(type, setters);
        final String[] allNames = this.getNames(allProps);
        this.generateKeySet(allNames);
        this.generateGetPropertyType(allProps, allNames);
        this.end_class();
    }
    
    private Map makePropertyMap(final PropertyDescriptor[] props) {
        final Map names = new HashMap();
        for (int i = 0; i < props.length; ++i) {
            names.put(props[i].getName(), props[i]);
        }
        return names;
    }
    
    private String[] getNames(final Map propertyMap) {
        return (String[])propertyMap.keySet().toArray(new String[propertyMap.size()]);
    }
    
    private void generateConstructor() {
        final CodeEmitter e = this.begin_method(1, BeanMapEmitter.CSTRUCT_OBJECT, null);
        e.load_this();
        e.load_arg(0);
        e.super_invoke_constructor(BeanMapEmitter.CSTRUCT_OBJECT);
        e.return_value();
        e.end_method();
    }
    
    private void generateGet(final Class type, final Map getters) {
        final CodeEmitter e = this.begin_method(1, BeanMapEmitter.BEAN_MAP_GET, null);
        e.load_arg(0);
        e.checkcast($Type.getType(type));
        e.load_arg(1);
        e.checkcast(Constants.TYPE_STRING);
        EmitUtils.string_switch(e, this.getNames(getters), 1, new ObjectSwitchCallback() {
            public void processCase(final Object key, final $Label end) {
                final PropertyDescriptor pd = getters.get(key);
                final MethodInfo method = ReflectUtils.getMethodInfo(pd.getReadMethod());
                e.invoke(method);
                e.box(method.getSignature().getReturnType());
                e.return_value();
            }
            
            public void processDefault() {
                e.aconst_null();
                e.return_value();
            }
        });
        e.end_method();
    }
    
    private void generatePut(final Class type, final Map setters) {
        final CodeEmitter e = this.begin_method(1, BeanMapEmitter.BEAN_MAP_PUT, null);
        e.load_arg(0);
        e.checkcast($Type.getType(type));
        e.load_arg(1);
        e.checkcast(Constants.TYPE_STRING);
        EmitUtils.string_switch(e, this.getNames(setters), 1, new ObjectSwitchCallback() {
            public void processCase(final Object key, final $Label end) {
                final PropertyDescriptor pd = setters.get(key);
                if (pd.getReadMethod() == null) {
                    e.aconst_null();
                }
                else {
                    final MethodInfo read = ReflectUtils.getMethodInfo(pd.getReadMethod());
                    e.dup();
                    e.invoke(read);
                    e.box(read.getSignature().getReturnType());
                }
                e.swap();
                e.load_arg(2);
                final MethodInfo write = ReflectUtils.getMethodInfo(pd.getWriteMethod());
                e.unbox(write.getSignature().getArgumentTypes()[0]);
                e.invoke(write);
                e.return_value();
            }
            
            public void processDefault() {
            }
        });
        e.aconst_null();
        e.return_value();
        e.end_method();
    }
    
    private void generateKeySet(final String[] allNames) {
        this.declare_field(10, "keys", BeanMapEmitter.FIXED_KEY_SET, null);
        CodeEmitter e = this.begin_static();
        e.new_instance(BeanMapEmitter.FIXED_KEY_SET);
        e.dup();
        EmitUtils.push_array(e, allNames);
        e.invoke_constructor(BeanMapEmitter.FIXED_KEY_SET, BeanMapEmitter.CSTRUCT_STRING_ARRAY);
        e.putfield("keys");
        e.return_value();
        e.end_method();
        e = this.begin_method(1, BeanMapEmitter.KEY_SET, null);
        e.load_this();
        e.getfield("keys");
        e.return_value();
        e.end_method();
    }
    
    private void generateGetPropertyType(final Map allProps, final String[] allNames) {
        final CodeEmitter e = this.begin_method(1, BeanMapEmitter.GET_PROPERTY_TYPE, null);
        e.load_arg(0);
        EmitUtils.string_switch(e, allNames, 1, new ObjectSwitchCallback() {
            public void processCase(final Object key, final $Label end) {
                final PropertyDescriptor pd = allProps.get(key);
                EmitUtils.load_class(e, $Type.getType(pd.getPropertyType()));
                e.return_value();
            }
            
            public void processDefault() {
                e.aconst_null();
                e.return_value();
            }
        });
        e.end_method();
    }
    
    static {
        BEAN_MAP = TypeUtils.parseType("com.comphenix.net.sf.cglib.beans.BeanMap");
        FIXED_KEY_SET = TypeUtils.parseType("com.comphenix.net.sf.cglib.beans.FixedKeySet");
        CSTRUCT_OBJECT = TypeUtils.parseConstructor("Object");
        CSTRUCT_STRING_ARRAY = TypeUtils.parseConstructor("String[]");
        BEAN_MAP_GET = TypeUtils.parseSignature("Object get(Object, Object)");
        BEAN_MAP_PUT = TypeUtils.parseSignature("Object put(Object, Object, Object)");
        KEY_SET = TypeUtils.parseSignature("java.util.Set keySet()");
        NEW_INSTANCE = new Signature("newInstance", BeanMapEmitter.BEAN_MAP, new $Type[] { Constants.TYPE_OBJECT });
        GET_PROPERTY_TYPE = TypeUtils.parseSignature("Class getPropertyType(String)");
    }
}
