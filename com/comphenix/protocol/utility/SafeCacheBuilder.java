package com.comphenix.protocol.utility;

import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.concurrent.ConcurrentMap;
import com.google.common.cache.CacheLoader;
import com.google.common.base.Ticker;
import com.google.common.cache.RemovalListener;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Method;
import com.google.common.cache.CacheBuilder;

public class SafeCacheBuilder<K, V>
{
    private CacheBuilder<K, V> builder;
    private static Method BUILD_METHOD;
    private static Method AS_MAP_METHOD;
    
    private SafeCacheBuilder() {
        this.builder = (CacheBuilder<K, V>)CacheBuilder.newBuilder();
    }
    
    public static <K, V> SafeCacheBuilder<K, V> newBuilder() {
        return new SafeCacheBuilder<K, V>();
    }
    
    public SafeCacheBuilder<K, V> concurrencyLevel(final int concurrencyLevel) {
        this.builder.concurrencyLevel(concurrencyLevel);
        return this;
    }
    
    public SafeCacheBuilder<K, V> expireAfterAccess(final long duration, final TimeUnit unit) {
        this.builder.expireAfterAccess(duration, unit);
        return this;
    }
    
    public SafeCacheBuilder<K, V> expireAfterWrite(final long duration, final TimeUnit unit) {
        this.builder.expireAfterWrite(duration, unit);
        return this;
    }
    
    public SafeCacheBuilder<K, V> initialCapacity(final int initialCapacity) {
        this.builder.initialCapacity(initialCapacity);
        return this;
    }
    
    public SafeCacheBuilder<K, V> maximumSize(final int size) {
        this.builder.maximumSize((long)size);
        return this;
    }
    
    public <K1 extends K, V1 extends V> SafeCacheBuilder<K1, V1> removalListener(final RemovalListener<? super K1, ? super V1> listener) {
        this.builder.removalListener((RemovalListener)listener);
        return (SafeCacheBuilder<K1, V1>)this;
    }
    
    public SafeCacheBuilder<K, V> ticker(final Ticker ticker) {
        this.builder.ticker(ticker);
        return this;
    }
    
    public SafeCacheBuilder<K, V> softValues() {
        this.builder.softValues();
        return this;
    }
    
    public SafeCacheBuilder<K, V> weakKeys() {
        this.builder.weakKeys();
        return this;
    }
    
    public SafeCacheBuilder<K, V> weakValues() {
        this.builder.weakValues();
        return this;
    }
    
    public <K1 extends K, V1 extends V> ConcurrentMap<K1, V1> build(final CacheLoader<? super K1, V1> loader) {
        Object cache = null;
        if (SafeCacheBuilder.BUILD_METHOD == null) {
            try {
                (SafeCacheBuilder.BUILD_METHOD = this.builder.getClass().getDeclaredMethod("build", CacheLoader.class)).setAccessible(true);
            }
            catch (Exception e) {
                throw new FieldAccessException("Unable to find CacheBuilder.build(CacheLoader)", e);
            }
        }
        try {
            cache = SafeCacheBuilder.BUILD_METHOD.invoke(this.builder, loader);
        }
        catch (Exception e) {
            throw new FieldAccessException("Unable to invoke " + SafeCacheBuilder.BUILD_METHOD + " on " + this.builder, e);
        }
        if (SafeCacheBuilder.AS_MAP_METHOD == null) {
            try {
                (SafeCacheBuilder.AS_MAP_METHOD = cache.getClass().getMethod("asMap", (Class<?>[])new Class[0])).setAccessible(true);
            }
            catch (Exception e) {
                throw new FieldAccessException("Unable to find Cache.asMap() in " + cache, e);
            }
        }
        try {
            return (ConcurrentMap<K1, V1>)SafeCacheBuilder.AS_MAP_METHOD.invoke(cache, new Object[0]);
        }
        catch (Exception e) {
            throw new FieldAccessException("Unable to invoke " + SafeCacheBuilder.AS_MAP_METHOD + " on " + cache, e);
        }
    }
}
