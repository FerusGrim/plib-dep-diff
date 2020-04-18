package com.comphenix.protocol.concurrency;

import java.util.Set;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.RemovalListener;
import com.comphenix.protocol.utility.SafeCacheBuilder;
import com.google.common.cache.CacheLoader;
import java.util.concurrent.ConcurrentMap;

public class BlockingHashMap<TKey, TValue>
{
    private final ConcurrentMap<TKey, TValue> backingMap;
    private final ConcurrentMap<TKey, Object> locks;
    
    public static <TKey, TValue> CacheLoader<TKey, TValue> newInvalidCacheLoader() {
        return new CacheLoader<TKey, TValue>() {
            public TValue load(final TKey key) throws Exception {
                throw new IllegalStateException("Illegal use. Access the map directly instead.");
            }
        };
    }
    
    public BlockingHashMap() {
        this.backingMap = SafeCacheBuilder.newBuilder().weakValues().removalListener((com.google.common.cache.RemovalListener<? super Object, ? super Object>)new RemovalListener<TKey, TValue>() {
            public void onRemoval(final RemovalNotification<TKey, TValue> entry) {
                if (entry.getCause() != RemovalCause.REPLACED) {
                    BlockingHashMap.this.locks.remove(entry.getKey());
                }
            }
        }).build(newInvalidCacheLoader());
        this.locks = new ConcurrentHashMap<TKey, Object>();
    }
    
    public static <TKey, TValue> BlockingHashMap<TKey, TValue> create() {
        return new BlockingHashMap<TKey, TValue>();
    }
    
    public TValue get(final TKey key) throws InterruptedException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be NULL.");
        }
        TValue value = this.backingMap.get(key);
        if (value == null) {
            final Object lock = this.getLock(key);
            synchronized (lock) {
                while (value == null) {
                    lock.wait();
                    value = this.backingMap.get(key);
                }
            }
        }
        return value;
    }
    
    public TValue get(final TKey key, final long timeout, final TimeUnit unit) throws InterruptedException {
        return this.get(key, timeout, unit, false);
    }
    
    public TValue get(final TKey key, final long timeout, final TimeUnit unit, final boolean ignoreInterrupted) throws InterruptedException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be NULL.");
        }
        if (unit == null) {
            throw new IllegalArgumentException("Unit cannot be NULL.");
        }
        if (timeout < 0L) {
            throw new IllegalArgumentException("Timeout cannot be less than zero.");
        }
        TValue value = this.backingMap.get(key);
        if (value == null && timeout > 0L) {
            final Object lock = this.getLock(key);
            final long stopTimeNS = System.nanoTime() + unit.toNanos(timeout);
            synchronized (lock) {
                while (value == null) {
                    try {
                        final long remainingTime = stopTimeNS - System.nanoTime();
                        if (remainingTime <= 0L) {
                            break;
                        }
                        TimeUnit.NANOSECONDS.timedWait(lock, remainingTime);
                        value = this.backingMap.get(key);
                    }
                    catch (InterruptedException e) {
                        if (!ignoreInterrupted) {
                            throw e;
                        }
                        continue;
                    }
                }
            }
        }
        return value;
    }
    
    public TValue put(final TKey key, final TValue value) {
        if (value == null) {
            throw new IllegalArgumentException("This map doesn't support NULL values.");
        }
        final TValue previous = this.backingMap.put(key, value);
        final Object lock = this.getLock(key);
        synchronized (lock) {
            lock.notifyAll();
            return previous;
        }
    }
    
    public TValue putIfAbsent(final TKey key, final TValue value) {
        if (value == null) {
            throw new IllegalArgumentException("This map doesn't support NULL values.");
        }
        final TValue previous = this.backingMap.putIfAbsent(key, value);
        if (previous == null) {
            final Object lock = this.getLock(key);
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        return previous;
    }
    
    public int size() {
        return this.backingMap.size();
    }
    
    public Collection<TValue> values() {
        return this.backingMap.values();
    }
    
    public Set<TKey> keys() {
        return this.backingMap.keySet();
    }
    
    private Object getLock(final TKey key) {
        Object lock = this.locks.get(key);
        if (lock == null) {
            final Object created = new Object();
            lock = this.locks.putIfAbsent(key, created);
            if (lock == null) {
                lock = created;
            }
        }
        return lock;
    }
}
