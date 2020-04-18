package com.comphenix.net.sf.cglib.core;

import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.lang.reflect.Method;

public class MethodWrapper
{
    private static final MethodWrapperKey KEY_FACTORY;
    
    private MethodWrapper() {
    }
    
    public static Object create(final Method method) {
        return MethodWrapper.KEY_FACTORY.newInstance(method.getName(), ReflectUtils.getNames(method.getParameterTypes()), method.getReturnType().getName());
    }
    
    public static Set createSet(final Collection methods) {
        final Set set = new HashSet();
        final Iterator it = methods.iterator();
        while (it.hasNext()) {
            set.add(create(it.next()));
        }
        return set;
    }
    
    static {
        KEY_FACTORY = (MethodWrapperKey)KeyFactory.create(MethodWrapperKey.class);
    }
    
    public interface MethodWrapperKey
    {
        Object newInstance(final String p0, final String[] p1, final String p2);
    }
}
