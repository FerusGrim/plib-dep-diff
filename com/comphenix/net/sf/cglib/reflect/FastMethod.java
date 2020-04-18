package com.comphenix.net.sf.cglib.reflect;

import java.lang.reflect.InvocationTargetException;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class FastMethod extends FastMember
{
    FastMethod(final FastClass fc, final Method method) {
        super(fc, method, helper(fc, method));
    }
    
    private static int helper(final FastClass fc, final Method method) {
        final int index = fc.getIndex(new Signature(method.getName(), $Type.getMethodDescriptor(method)));
        if (index < 0) {
            final Class[] types = method.getParameterTypes();
            System.err.println("hash=" + method.getName().hashCode() + " size=" + types.length);
            for (int i = 0; i < types.length; ++i) {
                System.err.println("  types[" + i + "]=" + types[i].getName());
            }
            throw new IllegalArgumentException("Cannot find method " + method);
        }
        return index;
    }
    
    public Class getReturnType() {
        return ((Method)this.member).getReturnType();
    }
    
    @Override
    public Class[] getParameterTypes() {
        return ((Method)this.member).getParameterTypes();
    }
    
    @Override
    public Class[] getExceptionTypes() {
        return ((Method)this.member).getExceptionTypes();
    }
    
    public Object invoke(final Object obj, final Object[] args) throws InvocationTargetException {
        return this.fc.invoke(this.index, obj, args);
    }
    
    public Method getJavaMethod() {
        return (Method)this.member;
    }
}
