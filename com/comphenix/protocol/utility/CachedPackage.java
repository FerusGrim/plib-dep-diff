package com.comphenix.protocol.utility;

import com.google.common.base.Strings;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.base.Optional;
import java.util.Map;

class CachedPackage
{
    private final Map<String, Optional<Class<?>>> cache;
    private final String packageName;
    private final ClassSource source;
    
    public CachedPackage(final String packageName, final ClassSource source) {
        this.packageName = packageName;
        this.cache = (Map<String, Optional<Class<?>>>)Maps.newConcurrentMap();
        this.source = source;
    }
    
    public void setPackageClass(final String className, final Class<?> clazz) {
        if (clazz != null) {
            this.cache.put(className, (Optional<Class<?>>)Optional.of((Object)clazz));
        }
        else {
            this.cache.remove(className);
        }
    }
    
    public Class<?> getPackageClass(final String className) {
        Preconditions.checkNotNull((Object)className, (Object)"className cannot be null!");
        if (this.cache.containsKey(className)) {
            final Optional<Class<?>> result = this.cache.get(className);
            if (!result.isPresent()) {
                throw new RuntimeException("Cannot find class " + className);
            }
            return (Class<?>)result.get();
        }
        else {
            try {
                final Class<?> clazz = this.source.loadClass(combine(this.packageName, className));
                if (clazz == null) {
                    throw new IllegalArgumentException("Source " + this.source + " returned null for " + className);
                }
                this.cache.put(className, (Optional<Class<?>>)Optional.of((Object)clazz));
                return clazz;
            }
            catch (ClassNotFoundException ex) {
                this.cache.put(className, (Optional<Class<?>>)Optional.absent());
                throw new RuntimeException("Cannot find class " + className, ex);
            }
        }
    }
    
    public static String combine(final String packageName, final String className) {
        if (Strings.isNullOrEmpty(packageName)) {
            return className;
        }
        if (Strings.isNullOrEmpty(className)) {
            return packageName;
        }
        return packageName + "." + className;
    }
}
