package com.comphenix.net.sf.cglib.core.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LoadingCache<K, KK, V>
{
    protected final ConcurrentMap<KK, Object> map;
    protected final Function<K, V> loader;
    protected final Function<K, KK> keyMapper;
    public static final Function IDENTITY;
    
    public LoadingCache(final Function<K, KK> keyMapper, final Function<K, V> loader) {
        this.keyMapper = keyMapper;
        this.loader = loader;
        this.map = new ConcurrentHashMap<KK, Object>();
    }
    
    public static <K> Function<K, K> identity() {
        return (Function<K, K>)LoadingCache.IDENTITY;
    }
    
    public V get(final K key) {
        final KK cacheKey = this.keyMapper.apply(key);
        final Object v = this.map.get(cacheKey);
        if (v != null && !(v instanceof FutureTask)) {
            return (V)v;
        }
        return this.createEntry(key, cacheKey, v);
    }
    
    protected V createEntry(final K key, final KK cacheKey, final Object v) {
        boolean creator = false;
        FutureTask<V> task;
        if (v != null) {
            task = (FutureTask<V>)v;
        }
        else {
            task = new FutureTask<V>(new Callable<V>() {
                public V call() throws Exception {
                    return LoadingCache.this.loader.apply(key);
                }
            });
            final Object prevTask = this.map.putIfAbsent(cacheKey, task);
            if (prevTask == null) {
                creator = true;
                task.run();
            }
            else {
                if (!(prevTask instanceof FutureTask)) {
                    return (V)prevTask;
                }
                task = (FutureTask<V>)prevTask;
            }
        }
        V result;
        try {
            result = task.get();
        }
        catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while loading cache item", e);
        }
        catch (ExecutionException e2) {
            final Throwable cause = e2.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw new IllegalStateException("Unable to load cache item", cause);
        }
        if (creator) {
            this.map.put(cacheKey, result);
        }
        return result;
    }
    
    static {
        IDENTITY = new Function() {
            public Object apply(final Object key) {
                return key;
            }
        };
    }
}
