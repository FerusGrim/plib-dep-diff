package com.comphenix.protocol.reflect;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.lang.reflect.TypeVariable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.lang.reflect.GenericDeclaration;

public abstract class MethodInfo implements GenericDeclaration, Member
{
    public static MethodInfo fromMethod(final Method method) {
        return new MethodInfo() {
            @Override
            public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
                return method.getAnnotation(annotationClass);
            }
            
            @Override
            public Annotation[] getAnnotations() {
                return method.getAnnotations();
            }
            
            @Override
            public Annotation[] getDeclaredAnnotations() {
                return method.getDeclaredAnnotations();
            }
            
            @Override
            public String getName() {
                return method.getName();
            }
            
            @Override
            public Class<?>[] getParameterTypes() {
                return method.getParameterTypes();
            }
            
            @Override
            public Class<?> getDeclaringClass() {
                return method.getDeclaringClass();
            }
            
            @Override
            public Class<?> getReturnType() {
                return method.getReturnType();
            }
            
            @Override
            public int getModifiers() {
                return method.getModifiers();
            }
            
            @Override
            public Class<?>[] getExceptionTypes() {
                return method.getExceptionTypes();
            }
            
            @Override
            public TypeVariable<?>[] getTypeParameters() {
                return method.getTypeParameters();
            }
            
            @Override
            public String toGenericString() {
                return method.toGenericString();
            }
            
            @Override
            public String toString() {
                return method.toString();
            }
            
            @Override
            public boolean isSynthetic() {
                return method.isSynthetic();
            }
            
            @Override
            public int hashCode() {
                return method.hashCode();
            }
            
            @Override
            public boolean isConstructor() {
                return false;
            }
        };
    }
    
    public static Collection<MethodInfo> fromMethods(final Method[] methods) {
        return fromMethods(Arrays.asList(methods));
    }
    
    public static List<MethodInfo> fromMethods(final Collection<Method> methods) {
        final List<MethodInfo> infos = (List<MethodInfo>)Lists.newArrayList();
        for (final Method method : methods) {
            infos.add(fromMethod(method));
        }
        return infos;
    }
    
    public static MethodInfo fromConstructor(final Constructor<?> constructor) {
        return new MethodInfo() {
            @Override
            public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
                return constructor.getAnnotation(annotationClass);
            }
            
            @Override
            public Annotation[] getAnnotations() {
                return constructor.getAnnotations();
            }
            
            @Override
            public Annotation[] getDeclaredAnnotations() {
                return constructor.getDeclaredAnnotations();
            }
            
            @Override
            public String getName() {
                return constructor.getName();
            }
            
            @Override
            public Class<?>[] getParameterTypes() {
                return (Class<?>[])constructor.getParameterTypes();
            }
            
            @Override
            public Class<?> getDeclaringClass() {
                return constructor.getDeclaringClass();
            }
            
            @Override
            public Class<?> getReturnType() {
                return Void.class;
            }
            
            @Override
            public int getModifiers() {
                return constructor.getModifiers();
            }
            
            @Override
            public Class<?>[] getExceptionTypes() {
                return (Class<?>[])constructor.getExceptionTypes();
            }
            
            @Override
            public TypeVariable<?>[] getTypeParameters() {
                return (TypeVariable<?>[])constructor.getTypeParameters();
            }
            
            @Override
            public String toGenericString() {
                return constructor.toGenericString();
            }
            
            @Override
            public String toString() {
                return constructor.toString();
            }
            
            @Override
            public boolean isSynthetic() {
                return constructor.isSynthetic();
            }
            
            @Override
            public int hashCode() {
                return constructor.hashCode();
            }
            
            @Override
            public boolean isConstructor() {
                return true;
            }
        };
    }
    
    public static Collection<MethodInfo> fromConstructors(final Constructor<?>[] constructors) {
        return fromConstructors(Arrays.asList(constructors));
    }
    
    public static List<MethodInfo> fromConstructors(final Collection<Constructor<?>> constructors) {
        final List<MethodInfo> infos = (List<MethodInfo>)Lists.newArrayList();
        for (final Constructor<?> constructor : constructors) {
            infos.add(fromConstructor(constructor));
        }
        return infos;
    }
    
    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }
    
    public abstract String toGenericString();
    
    public abstract Class<?>[] getExceptionTypes();
    
    public abstract Class<?> getReturnType();
    
    public abstract Class<?>[] getParameterTypes();
    
    public abstract boolean isConstructor();
}
