package com.comphenix.protocol.wrappers;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.lang.reflect.Field;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.Map;
import com.comphenix.protocol.reflect.EquivalentConverter;

public class AutoWrapper<T> implements EquivalentConverter<T>
{
    private Map<Integer, Function<Object, Object>> wrappers;
    private Map<Integer, Function<Object, Object>> unwrappers;
    private Class<T> wrapperClass;
    private Class<?> nmsClass;
    
    private AutoWrapper(final Class<T> wrapperClass, final Class<?> nmsClass) {
        this.wrappers = new HashMap<Integer, Function<Object, Object>>();
        this.unwrappers = new HashMap<Integer, Function<Object, Object>>();
        this.wrapperClass = wrapperClass;
        this.nmsClass = nmsClass;
    }
    
    public static <T> AutoWrapper<T> wrap(final Class<T> wrapperClass, final Class<?> nmsClass) {
        return new AutoWrapper<T>(wrapperClass, nmsClass);
    }
    
    public static <T> AutoWrapper<T> wrap(final Class<T> wrapperClass, final String nmsClassName) {
        return wrap(wrapperClass, MinecraftReflection.getMinecraftClass(nmsClassName));
    }
    
    public AutoWrapper<T> field(final int index, final Function<Object, Object> wrapper, final Function<Object, Object> unwrapper) {
        this.wrappers.put(index, wrapper);
        this.unwrappers.put(index, unwrapper);
        return this;
    }
    
    public AutoWrapper<T> field(final int index, final EquivalentConverter converter) {
        return this.field(index, converter::getSpecific, specific -> converter.getGeneric(specific));
    }
    
    public T wrap(final Object nmsObject) {
        T instance;
        try {
            instance = this.wrapperClass.newInstance();
        }
        catch (ReflectiveOperationException ex) {
            throw new InvalidWrapperException(this.wrapperClass.getSimpleName() + " is not accessible!", (Throwable)ex);
        }
        final Field[] wrapperFields = this.wrapperClass.getDeclaredFields();
        final Field[] nmsFields = Arrays.stream(this.nmsClass.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
        for (int i = 0; i < wrapperFields.length; ++i) {
            try {
                final Field wrapperField = wrapperFields[i];
                final Field nmsField = nmsFields[i];
                if (!nmsField.isAccessible()) {
                    nmsField.setAccessible(true);
                }
                Object value = nmsField.get(nmsObject);
                if (this.wrappers.containsKey(i)) {
                    value = this.wrappers.get(i).apply(value);
                }
                wrapperField.set(instance, value);
            }
            catch (ReflectiveOperationException ex2) {
                throw new InvalidWrapperException("Failed to wrap field", (Throwable)ex2);
            }
        }
        return instance;
    }
    
    public Object unwrap(final Object wrapper) {
        Object instance;
        try {
            instance = this.nmsClass.newInstance();
        }
        catch (ReflectiveOperationException ex) {
            throw new InvalidWrapperException("Failed to construct new " + this.nmsClass.getSimpleName(), (Throwable)ex);
        }
        final Field[] wrapperFields = this.wrapperClass.getDeclaredFields();
        final Field[] nmsFields = Arrays.stream(this.nmsClass.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
        for (int i = 0; i < wrapperFields.length; ++i) {
            try {
                final Field wrapperField = wrapperFields[i];
                final Field nmsField = nmsFields[i];
                if (!nmsField.isAccessible()) {
                    nmsField.setAccessible(true);
                }
                if (Modifier.isFinal(nmsField.getModifiers())) {
                    this.unsetFinal(nmsField);
                }
                Object value = wrapperField.get(wrapper);
                if (this.unwrappers.containsKey(i)) {
                    value = this.unwrappers.get(i).apply(value);
                }
                nmsField.set(instance, value);
            }
            catch (ReflectiveOperationException ex2) {
                throw new InvalidWrapperException("Failed to unwrap field", (Throwable)ex2);
            }
        }
        return instance;
    }
    
    private void unsetFinal(final Field field) throws ReflectiveOperationException {
        final Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & 0xFFFFFFEF);
    }
    
    @Override
    public T getSpecific(final Object generic) {
        return this.wrap(generic);
    }
    
    @Override
    public Object getGeneric(final Object specific) {
        return this.unwrap(specific);
    }
    
    @Override
    public Class<T> getSpecificType() {
        return this.wrapperClass;
    }
    
    public static class InvalidWrapperException extends RuntimeException
    {
        private InvalidWrapperException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
