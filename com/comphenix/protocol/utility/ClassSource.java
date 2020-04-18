package com.comphenix.protocol.utility;

import java.util.Collections;
import java.util.Map;

public abstract class ClassSource
{
    public static ClassSource fromClassLoader() {
        return fromClassLoader(ClassSource.class.getClassLoader());
    }
    
    public static ClassSource fromPackage(final String packageName) {
        return fromClassLoader().usingPackage(packageName);
    }
    
    public static ClassSource fromClassLoader(final ClassLoader loader) {
        return new ClassSource() {
            @Override
            public Class<?> loadClass(final String canonicalName) throws ClassNotFoundException {
                return loader.loadClass(canonicalName);
            }
        };
    }
    
    public static ClassSource fromMap(final Map<String, Class<?>> map) {
        return new ClassSource() {
            @Override
            public Class<?> loadClass(final String canonicalName) throws ClassNotFoundException {
                final Class<?> loaded = (map == null) ? null : map.get(canonicalName);
                if (loaded == null) {
                    throw new ClassNotFoundException("The specified class could not be found by this ClassLoader.");
                }
                return loaded;
            }
        };
    }
    
    public static ClassSource empty() {
        return fromMap(Collections.emptyMap());
    }
    
    public static ClassSource attemptLoadFrom(final ClassSource... sources) {
        if (sources.length == 0) {
            return empty();
        }
        ClassSource source = null;
        for (int i = 0; i < sources.length; ++i) {
            if (sources[i] == null) {
                throw new IllegalArgumentException("Null values are not permitted as ClassSources.");
            }
            source = ((source == null) ? sources[i] : source.retry(sources[i]));
        }
        return source;
    }
    
    public ClassSource retry(final ClassSource other) {
        return new ClassSource() {
            @Override
            public Class<?> loadClass(final String canonicalName) throws ClassNotFoundException {
                try {
                    return ClassSource.this.loadClass(canonicalName);
                }
                catch (ClassNotFoundException e) {
                    return other.loadClass(canonicalName);
                }
            }
        };
    }
    
    public ClassSource usingPackage(final String packageName) {
        return new ClassSource() {
            @Override
            public Class<?> loadClass(final String canonicalName) throws ClassNotFoundException {
                return ClassSource.this.loadClass(ClassSource.append(packageName, canonicalName));
            }
        };
    }
    
    protected static String append(final String a, final String b) {
        final boolean left = a.endsWith(".");
        final boolean right = b.endsWith(".");
        if (left && right) {
            return a.substring(0, a.length() - 1) + b;
        }
        if (left != right) {
            return a + b;
        }
        return a + "." + b;
    }
    
    public abstract Class<?> loadClass(final String p0) throws ClassNotFoundException;
}
