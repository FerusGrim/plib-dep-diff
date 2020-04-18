package com.comphenix.protocol.utility;

import com.google.common.base.Strings;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Optional;
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
            this.cache.put(className, Optional.of(clazz));
        }
        else {
            this.cache.remove(className);
        }
    }
    
    public Optional<Class<?>> getPackageClass(final String className) {
        Preconditions.checkNotNull((Object)className, (Object)"className cannot be null!");
        return this.cache.computeIfAbsent(className, x -> {
            try {
                return Optional.ofNullable(this.source.loadClass(combine(this.packageName, className)));
            }
            catch (ClassNotFoundException ex) {
                return Optional.empty();
            }
        });
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
