package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

class MixinBeanEmitter extends MixinEmitter
{
    public MixinBeanEmitter(final $ClassVisitor v, final String className, final Class[] classes) {
        super(v, className, classes, null);
    }
    
    @Override
    protected Class[] getInterfaces(final Class[] classes) {
        return null;
    }
    
    @Override
    protected Method[] getMethods(final Class type) {
        return ReflectUtils.getPropertyMethods(ReflectUtils.getBeanProperties(type), true, true);
    }
}
