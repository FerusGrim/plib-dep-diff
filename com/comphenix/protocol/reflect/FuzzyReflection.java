package com.comphenix.protocol.reflect;

import java.util.LinkedHashSet;
import java.util.Collection;
import com.google.common.collect.Sets;
import org.apache.commons.lang.Validate;
import com.google.common.collect.Maps;
import java.util.Map;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.accessors.Accessors;

public class FuzzyReflection
{
    private Class<?> source;
    private boolean forceAccess;
    
    public FuzzyReflection(final Class<?> source, final boolean forceAccess) {
        this.source = source;
        this.forceAccess = forceAccess;
    }
    
    public static FuzzyReflection fromClass(final Class<?> source) {
        return fromClass(source, false);
    }
    
    public static FuzzyReflection fromClass(final Class<?> source, final boolean forceAccess) {
        return new FuzzyReflection(source, forceAccess);
    }
    
    public static FuzzyReflection fromObject(final Object reference) {
        return new FuzzyReflection(reference.getClass(), false);
    }
    
    public static FuzzyReflection fromObject(final Object reference, final boolean forceAccess) {
        return new FuzzyReflection(reference.getClass(), forceAccess);
    }
    
    public static <T> T getFieldValue(final Object instance, final Class<T> fieldClass, final boolean forceAccess) {
        final T result = (T)Accessors.getFieldAccessor(instance.getClass(), fieldClass, forceAccess).get(instance);
        return result;
    }
    
    public Class<?> getSource() {
        return this.source;
    }
    
    public Object getSingleton() {
        Method method = null;
        Field field = null;
        try {
            method = this.getMethod(FuzzyMethodContract.newBuilder().parameterCount(0).returnDerivedOf(this.source).requireModifier(8).build());
        }
        catch (IllegalArgumentException e2) {
            field = this.getFieldByType("instance", this.source);
        }
        if (method != null) {
            try {
                method.setAccessible(true);
                return method.invoke(null, new Object[0]);
            }
            catch (Exception e) {
                throw new RuntimeException("Cannot invoke singleton method " + method, e);
            }
        }
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(null);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Cannot get content of singleton field " + field, e);
            }
        }
        throw new IllegalStateException("Impossible.");
    }
    
    public Method getMethod(final AbstractFuzzyMatcher<MethodInfo> matcher) {
        final List<Method> result = this.getMethodList(matcher);
        if (result.size() > 0) {
            return result.get(0);
        }
        throw new IllegalArgumentException("Unable to find a method that matches " + matcher);
    }
    
    public Method getMethod(final AbstractFuzzyMatcher<MethodInfo> matcher, final String preferred) {
        final List<Method> result = this.getMethodList(matcher);
        if (result.size() > 1) {
            for (final Method method : result) {
                if (method.getName().equals(preferred)) {
                    return method;
                }
            }
        }
        if (result.size() > 0) {
            return result.get(0);
        }
        throw new IllegalArgumentException("Unable to find a method that matches " + matcher);
    }
    
    public List<Method> getMethodList(final AbstractFuzzyMatcher<MethodInfo> matcher) {
        final List<Method> methods = (List<Method>)Lists.newArrayList();
        for (final Method method : this.getMethods()) {
            if (matcher.isMatch(MethodInfo.fromMethod(method), this.source)) {
                methods.add(method);
            }
        }
        return methods;
    }
    
    public Method getMethodByName(final String nameRegex) {
        final Pattern match = Pattern.compile(nameRegex);
        for (final Method method : this.getMethods()) {
            if (match.matcher(method.getName()).matches()) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unable to find a method with the pattern " + nameRegex + " in " + this.source.getName());
    }
    
    public Method getMethodByParameters(final String name, final Class<?>... args) {
        for (final Method method : this.getMethods()) {
            if (Arrays.equals(method.getParameterTypes(), args)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unable to find " + name + " in " + this.source.getName());
    }
    
    public Method getMethodByParameters(final String name, final Class<?> returnType, final Class<?>[] args) {
        final List<Method> methods = this.getMethodListByParameters(returnType, args);
        if (methods.size() > 0) {
            return methods.get(0);
        }
        throw new IllegalArgumentException("Unable to find " + name + " in " + this.source.getName());
    }
    
    public Method getMethodByParameters(final String name, final String returnTypeRegex, final String[] argsRegex) {
        final Pattern match = Pattern.compile(returnTypeRegex);
        final Pattern[] argMatch = new Pattern[argsRegex.length];
        for (int i = 0; i < argsRegex.length; ++i) {
            argMatch[i] = Pattern.compile(argsRegex[i]);
        }
        for (final Method method : this.getMethods()) {
            if (match.matcher(method.getReturnType().getName()).matches() && this.matchParameters(argMatch, method.getParameterTypes())) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unable to find " + name + " in " + this.source.getName());
    }
    
    public Object invokeMethod(final Object target, final String name, final Class<?> returnType, final Object... parameters) {
        final Class<?>[] types = (Class<?>[])new Class[parameters.length];
        for (int i = 0; i < types.length; ++i) {
            types[i] = parameters[i].getClass();
        }
        return Accessors.getMethodAccessor(this.getMethodByParameters(name, returnType, types)).invoke(target, parameters);
    }
    
    private boolean matchParameters(final Pattern[] parameterMatchers, final Class<?>[] argTypes) {
        if (parameterMatchers.length != argTypes.length) {
            throw new IllegalArgumentException("Arrays must have the same cardinality.");
        }
        for (int i = 0; i < argTypes.length; ++i) {
            if (!parameterMatchers[i].matcher(argTypes[i].getName()).matches()) {
                return false;
            }
        }
        return true;
    }
    
    public List<Method> getMethodListByParameters(final Class<?> returnType, final Class<?>[] args) {
        final List<Method> methods = new ArrayList<Method>();
        for (final Method method : this.getMethods()) {
            if (method.getReturnType().equals(returnType) && Arrays.equals(method.getParameterTypes(), args)) {
                methods.add(method);
            }
        }
        return methods;
    }
    
    public Field getFieldByName(final String nameRegex) {
        final Pattern match = Pattern.compile(nameRegex);
        for (final Field field : this.getFields()) {
            if (match.matcher(field.getName()).matches()) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unable to find a field with the pattern " + nameRegex + " in " + this.source.getName());
    }
    
    public Field getFieldByType(final String name, final Class<?> type) {
        final List<Field> fields = this.getFieldListByType(type);
        if (fields.size() > 0) {
            return fields.get(0);
        }
        throw new IllegalArgumentException(String.format("Unable to find a field %s with the type %s in %s", name, type.getName(), this.source.getName()));
    }
    
    public List<Field> getFieldListByType(final Class<?> type) {
        final List<Field> fields = new ArrayList<Field>();
        for (final Field field : this.getFields()) {
            if (type.isAssignableFrom(field.getType())) {
                fields.add(field);
            }
        }
        return fields;
    }
    
    public Field getParameterizedField(final Class<?> fieldType, final Class<?>... params) {
        for (final Field field : this.getFields()) {
            if (field.getType().equals(fieldType)) {
                final Type type = field.getGenericType();
                if (type instanceof ParameterizedType && Arrays.equals(((ParameterizedType)type).getActualTypeArguments(), params)) {
                    return field;
                }
                continue;
            }
        }
        throw new IllegalArgumentException("Unable to find a field with type " + fieldType + " and params " + Arrays.toString(params));
    }
    
    public Field getField(final AbstractFuzzyMatcher<Field> matcher) {
        final List<Field> result = this.getFieldList(matcher);
        if (result.size() > 0) {
            return result.get(0);
        }
        throw new IllegalArgumentException("Unable to find a field that matches " + matcher);
    }
    
    public List<Field> getFieldList(final AbstractFuzzyMatcher<Field> matcher) {
        final List<Field> fields = (List<Field>)Lists.newArrayList();
        for (final Field field : this.getFields()) {
            if (matcher.isMatch(field, this.source)) {
                fields.add(field);
            }
        }
        return fields;
    }
    
    public Field getFieldByType(final String typeRegex) {
        final Pattern match = Pattern.compile(typeRegex);
        for (final Field field : this.getFields()) {
            final String name = field.getType().getName();
            if (match.matcher(name).matches()) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unable to find a field with the type " + typeRegex + " in " + this.source.getName());
    }
    
    public Field getFieldByType(final String typeRegex, final Set<Class> ignored) {
        final Pattern match = Pattern.compile(typeRegex);
        for (final Field field : this.getFields()) {
            final Class type = field.getType();
            if (!ignored.contains(type) && match.matcher(type.getName()).matches()) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unable to find a field with the type " + typeRegex + " in " + this.source.getName());
    }
    
    public Constructor<?> getConstructor(final AbstractFuzzyMatcher<MethodInfo> matcher) {
        final List<Constructor<?>> result = this.getConstructorList(matcher);
        if (result.size() > 0) {
            return result.get(0);
        }
        throw new IllegalArgumentException("Unable to find a method that matches " + matcher);
    }
    
    public Map<String, Method> getMappedMethods(final List<Method> methods) {
        final Map<String, Method> map = (Map<String, Method>)Maps.newHashMap();
        for (final Method method : methods) {
            map.put(method.getName(), method);
        }
        return map;
    }
    
    public List<Constructor<?>> getConstructorList(final AbstractFuzzyMatcher<MethodInfo> matcher) {
        final List<Constructor<?>> constructors = (List<Constructor<?>>)Lists.newArrayList();
        for (final Constructor<?> constructor : this.getConstructors()) {
            if (matcher.isMatch(MethodInfo.fromConstructor(constructor), this.source)) {
                constructors.add(constructor);
            }
        }
        return constructors;
    }
    
    public Set<Field> getFields() {
        Validate.notNull((Object)this.source, "source cannot be null!");
        if (this.forceAccess) {
            return setUnion(new Field[][] { this.source.getDeclaredFields(), this.source.getFields() });
        }
        return setUnion(new Field[][] { this.source.getFields() });
    }
    
    public Set<Field> getDeclaredFields(final Class<?> excludeClass) {
        if (this.forceAccess) {
            Class<?> current = this.source;
            final Set<Field> fields = (Set<Field>)Sets.newLinkedHashSet();
            while (current != null && current != excludeClass) {
                fields.addAll(Arrays.asList(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
            return fields;
        }
        return this.getFields();
    }
    
    public Set<Method> getMethods() {
        if (this.forceAccess) {
            return setUnion(new Method[][] { this.source.getDeclaredMethods(), this.source.getMethods() });
        }
        return setUnion(new Method[][] { this.source.getMethods() });
    }
    
    public Set<Constructor<?>> getConstructors() {
        if (this.forceAccess) {
            return setUnion((Constructor<?>[][])new Constructor[][] { this.source.getDeclaredConstructors() });
        }
        return setUnion((Constructor<?>[][])new Constructor[][] { this.source.getConstructors() });
    }
    
    @SafeVarargs
    private static <T> Set<T> setUnion(final T[]... array) {
        final Set<T> result = new LinkedHashSet<T>();
        for (final T[] array2 : array) {
            final T[] elements = array2;
            for (final T element : array2) {
                result.add(element);
            }
        }
        return result;
    }
    
    public boolean isForceAccess() {
        return this.forceAccess;
    }
    
    public void setForceAccess(final boolean forceAccess) {
        this.forceAccess = forceAccess;
    }
}
