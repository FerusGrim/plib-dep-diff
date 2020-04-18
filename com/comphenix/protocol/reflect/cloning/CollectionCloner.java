package com.comphenix.protocol.reflect.cloning;

import java.lang.reflect.Constructor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.lang.reflect.Array;
import com.google.common.collect.BiMap;
import java.util.Map;
import java.util.Collection;

public class CollectionCloner implements Cloner
{
    private final Cloner defaultCloner;
    
    public CollectionCloner(final Cloner defaultCloner) {
        this.defaultCloner = defaultCloner;
    }
    
    @Override
    public boolean canClone(final Object source) {
        if (source == null) {
            return false;
        }
        final Class<?> clazz = source.getClass();
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz) || clazz.isArray();
    }
    
    @Override
    public Object clone(final Object source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be NULL.");
        }
        final Class<?> clazz = source.getClass();
        try {
            if (source instanceof Collection) {
                final Collection<Object> copy = this.cloneConstructor(Collection.class, clazz, source);
                try {
                    copy.clear();
                    for (final Object element : (Collection)source) {
                        copy.add(this.getClone(element, source));
                    }
                }
                catch (UnsupportedOperationException ex2) {}
                return copy;
            }
            if (source instanceof Map) {
                Map<Object, Object> copy2;
                if (source instanceof BiMap) {
                    final BiMap original = (BiMap)source;
                    if (clazz.getDeclaringClass() != null) {
                        final Method create = clazz.getDeclaringClass().getMethod("create", Integer.TYPE);
                        copy2 = (Map<Object, Object>)((BiMap)create.invoke(null, original.size())).inverse();
                    }
                    else {
                        final Method create = clazz.getMethod("create", Integer.TYPE);
                        copy2 = (Map<Object, Object>)create.invoke(null, original.size());
                    }
                }
                else {
                    copy2 = this.cloneConstructor(Map.class, clazz, source);
                }
                try {
                    copy2.clear();
                    for (final Map.Entry<Object, Object> element2 : ((Map)source).entrySet()) {
                        final Object key = this.getClone(element2.getKey(), source);
                        final Object value = this.getClone(element2.getValue(), source);
                        copy2.put(key, value);
                    }
                }
                catch (UnsupportedOperationException ex3) {}
                return copy2;
            }
            if (clazz.isArray()) {
                final int length = Array.getLength(source);
                final Class<?> component = clazz.getComponentType();
                if (ImmutableDetector.isImmutable(component)) {
                    return this.clonePrimitive(component, source);
                }
                final Object copy3 = Array.newInstance(clazz.getComponentType(), length);
                for (int i = 0; i < length; ++i) {
                    final Object element3 = Array.get(source, i);
                    if (!this.defaultCloner.canClone(element3)) {
                        throw new IllegalArgumentException("Cannot clone " + element3 + " in array " + source);
                    }
                    Array.set(copy3, i, this.defaultCloner.clone(element3));
                }
                return copy3;
            }
        }
        catch (Exception ex) {
            throw new RuntimeException("Failed to clone " + source + " (" + source.getClass() + ")", ex);
        }
        throw new IllegalArgumentException(source + " is not an array nor a Collection.");
    }
    
    private Object getClone(final Object element, final Object container) {
        if (this.defaultCloner.canClone(element)) {
            return this.defaultCloner.clone(element);
        }
        throw new IllegalArgumentException("Cannot clone " + element + " in container " + container);
    }
    
    private Object clonePrimitive(final Class<?> component, final Object source) {
        if (Byte.TYPE.equals(component)) {
            return ((byte[])source).clone();
        }
        if (Short.TYPE.equals(component)) {
            return ((short[])source).clone();
        }
        if (Integer.TYPE.equals(component)) {
            return ((int[])source).clone();
        }
        if (Long.TYPE.equals(component)) {
            return ((long[])source).clone();
        }
        if (Float.TYPE.equals(component)) {
            return ((float[])source).clone();
        }
        if (Double.TYPE.equals(component)) {
            return ((double[])source).clone();
        }
        if (Character.TYPE.equals(component)) {
            return ((char[])source).clone();
        }
        if (Boolean.TYPE.equals(component)) {
            return ((boolean[])source).clone();
        }
        return ((Object[])source).clone();
    }
    
    private <T> T cloneConstructor(final Class<?> superclass, final Class<?> clazz, final Object source) {
        try {
            final Constructor<?> constructCopy = clazz.getConstructor(superclass);
            return (T)constructCopy.newInstance(source);
        }
        catch (NoSuchMethodException e2) {
            if (source instanceof Serializable) {
                return (T)new SerializableCloner().clone(source);
            }
            return (T)this.cloneObject(clazz, source);
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot construct collection.", e);
        }
    }
    
    private Object cloneObject(final Class<?> clazz, final Object source) {
        try {
            return clazz.getMethod("clone", (Class<?>[])new Class[0]).invoke(source, new Object[0]);
        }
        catch (Exception e1) {
            throw new RuntimeException("Cannot copy " + source + " (" + clazz + ")", e1);
        }
    }
    
    public Cloner getDefaultCloner() {
        return this.defaultCloner;
    }
}
