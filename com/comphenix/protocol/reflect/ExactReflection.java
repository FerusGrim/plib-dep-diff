package com.comphenix.protocol.reflect;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import com.google.common.base.Preconditions;

public class ExactReflection
{
    private Class<?> source;
    private boolean forceAccess;
    
    private ExactReflection(final Class<?> source, final boolean forceAccess) {
        this.source = (Class<?>)Preconditions.checkNotNull((Object)source, (Object)"source class cannot be NULL");
        this.forceAccess = forceAccess;
    }
    
    public static ExactReflection fromClass(final Class<?> source) {
        return fromClass(source, false);
    }
    
    public static ExactReflection fromClass(final Class<?> source, final boolean forceAccess) {
        return new ExactReflection(source, forceAccess);
    }
    
    public static ExactReflection fromObject(final Object reference) {
        return new ExactReflection(reference.getClass(), false);
    }
    
    public static ExactReflection fromObject(final Object reference, final boolean forceAccess) {
        return new ExactReflection(reference.getClass(), forceAccess);
    }
    
    public Method getMethod(final String methodName, final Class<?>... parameters) {
        return this.getMethod(this.source, methodName, parameters);
    }
    
    private Method getMethod(final Class<?> instanceClass, final String methodName, final Class<?>... parameters) {
        for (final Method method : instanceClass.getDeclaredMethods()) {
            if ((this.forceAccess || Modifier.isPublic(method.getModifiers())) && (methodName == null || method.getName().equals(methodName)) && Arrays.equals(method.getParameterTypes(), parameters)) {
                method.setAccessible(true);
                return method;
            }
        }
        if (instanceClass.getSuperclass() != null) {
            return this.getMethod(instanceClass.getSuperclass(), methodName, parameters);
        }
        throw new IllegalArgumentException(String.format("Unable to find method %s (%s) in %s.", methodName, Arrays.asList(parameters), this.source));
    }
    
    public Field getField(final String fieldName) {
        return this.getField(this.source, fieldName);
    }
    
    private Field getField(final Class<?> instanceClass, @Nonnull final String fieldName) {
        for (final Field field : instanceClass.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                field.setAccessible(true);
                return field;
            }
        }
        if (instanceClass.getSuperclass() != null) {
            return this.getField(instanceClass.getSuperclass(), fieldName);
        }
        throw new IllegalArgumentException(String.format("Unable to find field %s in %s.", fieldName, this.source));
    }
    
    public ExactReflection forceAccess() {
        return new ExactReflection(this.source, true);
    }
    
    public boolean isForceAccess() {
        return this.forceAccess;
    }
    
    public Class<?> getSource() {
        return this.source;
    }
}
