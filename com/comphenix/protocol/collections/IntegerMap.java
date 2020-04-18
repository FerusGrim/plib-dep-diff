package com.comphenix.protocol.collections;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Arrays;
import com.google.common.base.Preconditions;

public class IntegerMap<T>
{
    private T[] array;
    private int size;
    
    public static <T> IntegerMap<T> newMap() {
        return new IntegerMap<T>();
    }
    
    public IntegerMap() {
        this(8);
    }
    
    public IntegerMap(final int initialCapacity) {
        final T[] backingArray = (T[])new Object[initialCapacity];
        this.array = backingArray;
        this.size = 0;
    }
    
    public T put(final int key, final T value) {
        this.ensureCapacity(key);
        final T old = this.array[key];
        this.array[key] = (T)Preconditions.checkNotNull((Object)value, (Object)"value cannot be NULL");
        if (old == null) {
            ++this.size;
        }
        return old;
    }
    
    public T remove(final int key) {
        final T old = this.array[key];
        this.array[key] = null;
        if (old != null) {
            --this.size;
        }
        return old;
    }
    
    protected void ensureCapacity(final int key) {
        int newLength = this.array.length;
        if (key < 0) {
            throw new IllegalArgumentException("Negative key values are not permitted.");
        }
        if (key < newLength) {
            return;
        }
        while (newLength <= key) {
            final int next = newLength * 2;
            newLength = ((next > newLength) ? next : Integer.MAX_VALUE);
        }
        this.array = Arrays.copyOf(this.array, newLength);
    }
    
    public int size() {
        return this.size;
    }
    
    public T get(final int key) {
        if (key >= 0 && key < this.array.length) {
            return this.array[key];
        }
        return null;
    }
    
    public boolean containsKey(final int key) {
        return this.get(key) != null;
    }
    
    public Map<Integer, Object> toMap() {
        final Map<Integer, Object> map = (Map<Integer, Object>)Maps.newHashMap();
        for (int i = 0; i < this.array.length; ++i) {
            if (this.array[i] != null) {
                map.put(i, this.array[i]);
            }
        }
        return map;
    }
}
