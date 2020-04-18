package com.comphenix.net.sf.cglib.beans;

import com.comphenix.net.sf.cglib.core.TypeUtils;
import java.lang.reflect.Modifier;
import com.comphenix.net.sf.cglib.core.Block;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import com.comphenix.net.sf.cglib.core.Local;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.core.ClassEmitter;

class BulkBeanEmitter extends ClassEmitter
{
    private static final Signature GET_PROPERTY_VALUES;
    private static final Signature SET_PROPERTY_VALUES;
    private static final Signature CSTRUCT_EXCEPTION;
    private static final $Type BULK_BEAN;
    private static final $Type BULK_BEAN_EXCEPTION;
    
    public BulkBeanEmitter(final $ClassVisitor v, final String className, final Class target, final String[] getterNames, final String[] setterNames, final Class[] types) {
        super(v);
        final Method[] getters = new Method[getterNames.length];
        final Method[] setters = new Method[setterNames.length];
        validate(target, getterNames, setterNames, types, getters, setters);
        this.begin_class(46, 1, className, BulkBeanEmitter.BULK_BEAN, null, "<generated>");
        EmitUtils.null_constructor(this);
        this.generateGet(target, getters);
        this.generateSet(target, setters);
        this.end_class();
    }
    
    private void generateGet(final Class target, final Method[] getters) {
        final CodeEmitter e = this.begin_method(1, BulkBeanEmitter.GET_PROPERTY_VALUES, null);
        if (getters.length >= 0) {
            e.load_arg(0);
            e.checkcast($Type.getType(target));
            final Local bean = e.make_local();
            e.store_local(bean);
            for (int i = 0; i < getters.length; ++i) {
                if (getters[i] != null) {
                    final MethodInfo getter = ReflectUtils.getMethodInfo(getters[i]);
                    e.load_arg(1);
                    e.push(i);
                    e.load_local(bean);
                    e.invoke(getter);
                    e.box(getter.getSignature().getReturnType());
                    e.aastore();
                }
            }
        }
        e.return_value();
        e.end_method();
    }
    
    private void generateSet(final Class target, final Method[] setters) {
        final CodeEmitter e = this.begin_method(1, BulkBeanEmitter.SET_PROPERTY_VALUES, null);
        if (setters.length > 0) {
            final Local index = e.make_local($Type.INT_TYPE);
            e.push(0);
            e.store_local(index);
            e.load_arg(0);
            e.checkcast($Type.getType(target));
            e.load_arg(1);
            final Block handler = e.begin_block();
            int lastIndex = 0;
            for (int i = 0; i < setters.length; ++i) {
                if (setters[i] != null) {
                    final MethodInfo setter = ReflectUtils.getMethodInfo(setters[i]);
                    final int diff = i - lastIndex;
                    if (diff > 0) {
                        e.iinc(index, diff);
                        lastIndex = i;
                    }
                    e.dup2();
                    e.aaload(i);
                    e.unbox(setter.getSignature().getArgumentTypes()[0]);
                    e.invoke(setter);
                }
            }
            handler.end();
            e.return_value();
            e.catch_exception(handler, Constants.TYPE_THROWABLE);
            e.new_instance(BulkBeanEmitter.BULK_BEAN_EXCEPTION);
            e.dup_x1();
            e.swap();
            e.load_local(index);
            e.invoke_constructor(BulkBeanEmitter.BULK_BEAN_EXCEPTION, BulkBeanEmitter.CSTRUCT_EXCEPTION);
            e.athrow();
        }
        else {
            e.return_value();
        }
        e.end_method();
    }
    
    private static void validate(final Class target, final String[] getters, final String[] setters, final Class[] types, final Method[] getters_out, final Method[] setters_out) {
        int i = -1;
        if (setters.length != types.length || getters.length != types.length) {
            throw new BulkBeanException("accessor array length must be equal type array length", i);
        }
        try {
            for (i = 0; i < types.length; ++i) {
                if (getters[i] != null) {
                    final Method method = ReflectUtils.findDeclaredMethod(target, getters[i], null);
                    if (method.getReturnType() != types[i]) {
                        throw new BulkBeanException("Specified type " + types[i] + " does not match declared type " + method.getReturnType(), i);
                    }
                    if (Modifier.isPrivate(method.getModifiers())) {
                        throw new BulkBeanException("Property is private", i);
                    }
                    getters_out[i] = method;
                }
                if (setters[i] != null) {
                    final Method method = ReflectUtils.findDeclaredMethod(target, setters[i], new Class[] { types[i] });
                    if (Modifier.isPrivate(method.getModifiers())) {
                        throw new BulkBeanException("Property is private", i);
                    }
                    setters_out[i] = method;
                }
            }
        }
        catch (NoSuchMethodException e) {
            throw new BulkBeanException("Cannot find specified property", i);
        }
    }
    
    static {
        GET_PROPERTY_VALUES = TypeUtils.parseSignature("void getPropertyValues(Object, Object[])");
        SET_PROPERTY_VALUES = TypeUtils.parseSignature("void setPropertyValues(Object, Object[])");
        CSTRUCT_EXCEPTION = TypeUtils.parseConstructor("Throwable, int");
        BULK_BEAN = TypeUtils.parseType("com.comphenix.net.sf.cglib.beans.BulkBean");
        BULK_BEAN_EXCEPTION = TypeUtils.parseType("com.comphenix.net.sf.cglib.beans.BulkBeanException");
    }
}
