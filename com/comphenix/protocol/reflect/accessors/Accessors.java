package com.comphenix.protocol.reflect.accessors;

import java.lang.reflect.Constructor;
import com.google.common.base.Joiner;
import java.lang.reflect.Method;
import com.comphenix.protocol.reflect.ExactReflection;
import java.util.List;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.FuzzyReflection;

public final class Accessors
{
    public static FieldAccessor getFieldAccessor(final Class<?> instanceClass, final Class<?> fieldClass, final boolean forceAccess) {
        final Field field = FuzzyReflection.fromClass(instanceClass, forceAccess).getFieldByType(null, fieldClass);
        return getFieldAccessor(field);
    }
    
    public static FieldAccessor[] getFieldAccessorArray(final Class<?> instanceClass, final Class<?> fieldClass, final boolean forceAccess) {
        final List<Field> fields = FuzzyReflection.fromClass(instanceClass, forceAccess).getFieldListByType(fieldClass);
        final FieldAccessor[] accessors = new FieldAccessor[fields.size()];
        for (int i = 0; i < accessors.length; ++i) {
            accessors[i] = getFieldAccessor(fields.get(i));
        }
        return accessors;
    }
    
    public static FieldAccessor getFieldAccessor(final Class<?> instanceClass, final String fieldName, final boolean forceAccess) {
        return getFieldAccessor(ExactReflection.fromClass(instanceClass, true).getField(fieldName));
    }
    
    public static FieldAccessor getFieldAccessor(final Field field) {
        return getFieldAccessor(field, true);
    }
    
    public static FieldAccessor getFieldAccessor(final Field field, final boolean forceAccess) {
        field.setAccessible(true);
        return new DefaultFieldAccessor(field);
    }
    
    public static FieldAccessor getFieldAcccessorOrNull(final Class<?> clazz, final String fieldName, final Class<?> fieldType) {
        try {
            final FieldAccessor accessor = getFieldAccessor(clazz, fieldName, true);
            if (fieldType.isAssignableFrom(accessor.getField().getType())) {
                return accessor;
            }
            return null;
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public static MethodAccessor getMethodAcccessorOrNull(final Class<?> clazz, final String methodName) {
        try {
            return getMethodAccessor(clazz, methodName, (Class<?>[])new Class[0]);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public static ConstructorAccessor getConstructorAccessorOrNull(final Class<?> clazz, final Class<?>... parameters) {
        try {
            return getConstructorAccessor(clazz, parameters);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public static FieldAccessor getCached(final FieldAccessor inner) {
        return new FieldAccessor() {
            private final Object EMPTY = new Object();
            private volatile Object value = this.EMPTY;
            
            @Override
            public void set(final Object instance, final Object value) {
                inner.set(instance, value);
                this.update(value);
            }
            
            @Override
            public Object get(final Object instance) {
                final Object cache = this.value;
                if (cache != this.EMPTY) {
                    return cache;
                }
                return this.update(inner.get(instance));
            }
            
            private Object update(final Object value) {
                return this.value = value;
            }
            
            @Override
            public Field getField() {
                return inner.getField();
            }
        };
    }
    
    public static FieldAccessor getSynchronized(final FieldAccessor accessor) {
        if (accessor instanceof SynchronizedFieldAccessor) {
            return accessor;
        }
        return new SynchronizedFieldAccessor(accessor);
    }
    
    public static MethodAccessor getConstantAccessor(final Object returnValue, final Method method) {
        return new MethodAccessor() {
            @Override
            public Object invoke(final Object target, final Object... args) {
                return returnValue;
            }
            
            @Override
            public Method getMethod() {
                return method;
            }
        };
    }
    
    public static MethodAccessor getMethodAccessor(final Class<?> instanceClass, final String methodName, final Class<?>... parameters) {
        return new DefaultMethodAccessor(ExactReflection.fromClass(instanceClass, true).getMethod(methodName, parameters));
    }
    
    public static MethodAccessor getMethodAccessor(final Method method) {
        return getMethodAccessor(method, true);
    }
    
    public static MethodAccessor getMethodAccessor(final Method method, final boolean forceAccess) {
        method.setAccessible(forceAccess);
        return new DefaultMethodAccessor(method);
    }
    
    public static ConstructorAccessor getConstructorAccessor(final Class<?> instanceClass, final Class<?>... parameters) {
        try {
            return getConstructorAccessor(instanceClass.getDeclaredConstructor(parameters));
        }
        catch (NoSuchMethodException e2) {
            throw new IllegalArgumentException(String.format("Unable to find constructor %s(%s).", instanceClass, Joiner.on(",").join((Object[])parameters)));
        }
        catch (SecurityException e) {
            throw new IllegalStateException("Cannot access constructors.", e);
        }
    }
    
    public static ConstructorAccessor getConstructorAccessor(final Constructor<?> constructor) {
        return new DefaultConstrutorAccessor(constructor);
    }
    
    private Accessors() {
    }
    
    public static final class SynchronizedFieldAccessor implements FieldAccessor
    {
        private final FieldAccessor accessor;
        
        private SynchronizedFieldAccessor(final FieldAccessor accessor) {
            this.accessor = accessor;
        }
        
        @Override
        public void set(final Object instance, final Object value) {
            final Object lock = this.accessor.get(instance);
            if (lock != null) {
                synchronized (lock) {
                    this.accessor.set(instance, value);
                }
            }
            else {
                this.accessor.set(instance, value);
            }
        }
        
        @Override
        public Object get(final Object instance) {
            return this.accessor.get(instance);
        }
        
        @Override
        public Field getField() {
            return this.accessor.getField();
        }
    }
}
