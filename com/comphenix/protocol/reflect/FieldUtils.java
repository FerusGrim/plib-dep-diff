package com.comphenix.protocol.reflect;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.lang.reflect.Modifier;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

public class FieldUtils
{
    public static Field getField(final Class cls, final String fieldName) {
        final Field field = getField(cls, fieldName, false);
        MemberUtils.setAccessibleWorkaround(field);
        return field;
    }
    
    public static Field getField(final Class cls, final String fieldName, final boolean forceAccess) {
        if (cls == null) {
            throw new IllegalArgumentException("The class must not be null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("The field name must not be null");
        }
        for (Class acls = cls; acls != null; acls = acls.getSuperclass()) {
            try {
                final Field field = acls.getDeclaredField(fieldName);
                if (!Modifier.isPublic(field.getModifiers())) {
                    if (!forceAccess) {
                        continue;
                    }
                    field.setAccessible(true);
                }
                return field;
            }
            catch (NoSuchFieldException ex) {}
        }
        Field match = null;
        final Iterator intf = getAllInterfaces(cls).iterator();
        while (intf.hasNext()) {
            try {
                final Field test = intf.next().getField(fieldName);
                if (match != null) {
                    throw new IllegalArgumentException("Reference to field " + fieldName + " is ambiguous relative to " + cls + "; a matching field exists on two or more implemented interfaces.");
                }
                match = test;
            }
            catch (NoSuchFieldException ex2) {}
        }
        return match;
    }
    
    private static List getAllInterfaces(Class cls) {
        if (cls == null) {
            return null;
        }
        final List<Class> list = new ArrayList<Class>();
        while (cls != null) {
            final Class[] interfaces = cls.getInterfaces();
            for (int i = 0; i < interfaces.length; ++i) {
                if (!list.contains(interfaces[i])) {
                    list.add(interfaces[i]);
                }
                final List superInterfaces = getAllInterfaces(interfaces[i]);
                for (final Class intface : superInterfaces) {
                    if (!list.contains(intface)) {
                        list.add(intface);
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return list;
    }
    
    public static Object readStaticField(final Field field) throws IllegalAccessException {
        return readStaticField(field, false);
    }
    
    public static Object readStaticField(final Field field, final boolean forceAccess) throws IllegalAccessException {
        if (field == null) {
            throw new IllegalArgumentException("The field must not be null");
        }
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("The field '" + field.getName() + "' is not static");
        }
        return readField(field, (Object)null, forceAccess);
    }
    
    public static Object readStaticField(final Class cls, final String fieldName) throws IllegalAccessException {
        return readStaticField(cls, fieldName, false);
    }
    
    public static Object readStaticField(final Class cls, final String fieldName, final boolean forceAccess) throws IllegalAccessException {
        final Field field = getField(cls, fieldName, forceAccess);
        if (field == null) {
            throw new IllegalArgumentException("Cannot locate field " + fieldName + " on " + cls);
        }
        return readStaticField(field, false);
    }
    
    public static Object readField(final Field field, final Object target) throws IllegalAccessException {
        return readField(field, target, false);
    }
    
    public static Object readField(final Field field, final Object target, final boolean forceAccess) throws IllegalAccessException {
        if (field == null) {
            throw new IllegalArgumentException("The field must not be null");
        }
        if (forceAccess && !field.isAccessible()) {
            field.setAccessible(true);
        }
        else {
            MemberUtils.setAccessibleWorkaround(field);
        }
        return field.get(target);
    }
    
    public static Object readField(final Object target, final String fieldName) throws IllegalAccessException {
        return readField(target, fieldName, false);
    }
    
    public static Object readField(final Object target, final String fieldName, final boolean forceAccess) throws IllegalAccessException {
        if (target == null) {
            throw new IllegalArgumentException("target object must not be null");
        }
        final Class cls = target.getClass();
        final Field field = getField(cls, fieldName, forceAccess);
        if (field == null) {
            throw new IllegalArgumentException("Cannot locate field " + fieldName + " on " + cls);
        }
        return readField(field, target);
    }
    
    public static void writeStaticField(final Field field, final Object value) throws IllegalAccessException {
        writeStaticField(field, value, false);
    }
    
    public static void writeStaticField(final Field field, final Object value, final boolean forceAccess) throws IllegalAccessException {
        if (field == null) {
            throw new IllegalArgumentException("The field must not be null");
        }
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("The field '" + field.getName() + "' is not static");
        }
        writeField(field, (Object)null, value, forceAccess);
    }
    
    public static void writeStaticField(final Class cls, final String fieldName, final Object value) throws IllegalAccessException {
        writeStaticField(cls, fieldName, value, false);
    }
    
    public static void writeStaticField(final Class cls, final String fieldName, final Object value, final boolean forceAccess) throws IllegalAccessException {
        final Field field = getField(cls, fieldName, forceAccess);
        if (field == null) {
            throw new IllegalArgumentException("Cannot locate field " + fieldName + " on " + cls);
        }
        writeStaticField(field, value);
    }
    
    public static void writeStaticFinalField(final Class<?> clazz, final String fieldName, final Object value, final boolean forceAccess) throws Exception {
        final Field field = getField(clazz, fieldName, forceAccess);
        if (field == null) {
            throw new IllegalArgumentException("Cannot locate field " + fieldName + " in " + clazz);
        }
        field.setAccessible(true);
        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEF);
        field.setAccessible(true);
        field.set(null, value);
    }
    
    public static void writeField(final Field field, final Object target, final Object value) throws IllegalAccessException {
        writeField(field, target, value, false);
    }
    
    public static void writeField(final Field field, final Object target, final Object value, final boolean forceAccess) throws IllegalAccessException {
        if (field == null) {
            throw new IllegalArgumentException("The field must not be null");
        }
        if (forceAccess && !field.isAccessible()) {
            field.setAccessible(true);
        }
        else {
            MemberUtils.setAccessibleWorkaround(field);
        }
        field.set(target, value);
    }
    
    public static void writeField(final Object target, final String fieldName, final Object value) throws IllegalAccessException {
        writeField(target, fieldName, value, false);
    }
    
    public static void writeField(final Object target, final String fieldName, final Object value, final boolean forceAccess) throws IllegalAccessException {
        if (target == null) {
            throw new IllegalArgumentException("target object must not be null");
        }
        final Class cls = target.getClass();
        final Field field = getField(cls, fieldName, forceAccess);
        if (field == null) {
            throw new IllegalArgumentException("Cannot locate declared field " + cls.getName() + "." + fieldName);
        }
        writeField(field, target, value);
    }
    
    private static class MemberUtils
    {
        private static final int ACCESS_TEST = 7;
        
        public static void setAccessibleWorkaround(final AccessibleObject o) {
            if (o == null || o.isAccessible()) {
                return;
            }
            final Member m = (Member)o;
            if (Modifier.isPublic(m.getModifiers()) && isPackageAccess(m.getDeclaringClass().getModifiers())) {
                try {
                    o.setAccessible(true);
                }
                catch (SecurityException ex) {}
            }
        }
        
        public static boolean isPackageAccess(final int modifiers) {
            return (modifiers & 0x7) == 0x0;
        }
    }
}
