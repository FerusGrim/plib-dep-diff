package com.comphenix.protocol.wrappers;

import java.util.Iterator;
import java.lang.reflect.Modifier;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.lang.reflect.InvocationTargetException;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.lang.reflect.Method;

public class WrappedIntHashMap extends AbstractWrapper
{
    private static Method PUT_METHOD;
    private static Method GET_METHOD;
    private static Method REMOVE_METHOD;
    
    private WrappedIntHashMap(final Object handle) {
        super(MinecraftReflection.getIntHashMapClass());
        this.setHandle(handle);
    }
    
    public static WrappedIntHashMap newMap() {
        try {
            return new WrappedIntHashMap(MinecraftReflection.getIntHashMapClass().newInstance());
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to construct IntHashMap.", e);
        }
    }
    
    public static WrappedIntHashMap fromHandle(@Nonnull final Object handle) {
        return new WrappedIntHashMap(handle);
    }
    
    public void put(final int key, final Object value) {
        Preconditions.checkNotNull(value, (Object)"value cannot be NULL.");
        this.initializePutMethod();
        this.putInternal(key, value);
    }
    
    private void putInternal(final int key, final Object value) {
        this.invokeMethod(WrappedIntHashMap.PUT_METHOD, key, value);
    }
    
    public Object get(final int key) {
        this.initializeGetMethod();
        return this.invokeMethod(WrappedIntHashMap.GET_METHOD, key);
    }
    
    public Object remove(final int key) {
        this.initializeGetMethod();
        if (WrappedIntHashMap.REMOVE_METHOD == null) {
            return this.removeFallback(key);
        }
        return this.invokeMethod(WrappedIntHashMap.REMOVE_METHOD, key);
    }
    
    private Object removeFallback(final int key) {
        final Object old = this.get(key);
        this.invokeMethod(WrappedIntHashMap.PUT_METHOD, key, null);
        return old;
    }
    
    private Object invokeMethod(final Method method, final Object... params) {
        try {
            return method.invoke(this.handle, params);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("Illegal argument.", e);
        }
        catch (IllegalAccessException e2) {
            throw new RuntimeException("Cannot access method.", e2);
        }
        catch (InvocationTargetException e3) {
            throw new RuntimeException("Unable to invoke " + method + " on " + this.handle, e3);
        }
    }
    
    private void initializePutMethod() {
        if (WrappedIntHashMap.PUT_METHOD == null) {
            WrappedIntHashMap.PUT_METHOD = FuzzyReflection.fromClass(MinecraftReflection.getIntHashMapClass()).getMethod(FuzzyMethodContract.newBuilder().banModifier(8).parameterCount(2).parameterExactType(Integer.TYPE).parameterExactType(Object.class).build());
        }
    }
    
    private void initializeGetMethod() {
        if (WrappedIntHashMap.GET_METHOD == null) {
            final WrappedIntHashMap temp = newMap();
            final String expected = "hello";
            for (final Method method : FuzzyReflection.fromClass(MinecraftReflection.getIntHashMapClass()).getMethodListByParameters(Object.class, new Class[] { Integer.TYPE })) {
                temp.put(1, expected);
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                try {
                    final boolean first = expected.equals(method.invoke(temp.getHandle(), 1));
                    final boolean second = expected.equals(method.invoke(temp.getHandle(), 1));
                    if (first && !second) {
                        WrappedIntHashMap.REMOVE_METHOD = method;
                    }
                    else {
                        if (!first || !second) {
                            continue;
                        }
                        WrappedIntHashMap.GET_METHOD = method;
                    }
                }
                catch (Exception ex) {}
            }
            if (WrappedIntHashMap.GET_METHOD == null) {
                throw new IllegalStateException("Unable to find appropriate GET_METHOD for IntHashMap.");
            }
        }
    }
}
