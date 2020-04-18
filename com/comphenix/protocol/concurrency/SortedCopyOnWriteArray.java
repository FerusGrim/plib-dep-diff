package com.comphenix.protocol.concurrency;

import com.google.common.collect.Iterables;
import com.google.common.base.Objects;
import java.util.Iterator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class SortedCopyOnWriteArray<T extends Comparable<T>> implements Collection<T>
{
    private volatile List<T> list;
    
    public SortedCopyOnWriteArray() {
        this.list = new ArrayList<T>();
    }
    
    public SortedCopyOnWriteArray(final Collection<T> wrapped) {
        this.list = new ArrayList<T>((Collection<? extends T>)wrapped);
    }
    
    public SortedCopyOnWriteArray(final Collection<T> wrapped, final boolean sort) {
        this.list = new ArrayList<T>((Collection<? extends T>)wrapped);
        if (sort) {
            Collections.sort(this.list);
        }
    }
    
    @Override
    public synchronized boolean add(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be NULL");
        }
        final List<T> copy = new ArrayList<T>();
        for (final T element : this.list) {
            if (value != null && value.compareTo(element) < 0) {
                copy.add(value);
                value = null;
            }
            copy.add(element);
        }
        if (value != null) {
            copy.add(value);
        }
        this.list = copy;
        return true;
    }
    
    @Override
    public synchronized boolean addAll(final Collection<? extends T> values) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be NULL");
        }
        if (values.size() == 0) {
            return false;
        }
        final List<T> copy = new ArrayList<T>();
        copy.addAll((Collection<? extends T>)this.list);
        copy.addAll(values);
        Collections.sort(copy);
        this.list = copy;
        return true;
    }
    
    @Override
    public synchronized boolean remove(final Object value) {
        final List<T> copy = new ArrayList<T>();
        boolean result = false;
        for (final T element : this.list) {
            if (!Objects.equal(value, (Object)element)) {
                copy.add(element);
            }
            else {
                result = true;
            }
        }
        this.list = copy;
        return result;
    }
    
    @Override
    public boolean removeAll(final Collection<?> values) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be NULL");
        }
        if (values.size() == 0) {
            return false;
        }
        final List<T> copy = new ArrayList<T>();
        copy.addAll((Collection<? extends T>)this.list);
        copy.removeAll(values);
        this.list = copy;
        return true;
    }
    
    @Override
    public boolean retainAll(final Collection<?> values) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be NULL");
        }
        if (values.size() == 0) {
            return false;
        }
        final List<T> copy = new ArrayList<T>();
        copy.addAll((Collection<? extends T>)this.list);
        copy.removeAll(values);
        this.list = copy;
        return true;
    }
    
    public synchronized void remove(final int index) {
        final List<T> copy = new ArrayList<T>((Collection<? extends T>)this.list);
        copy.remove(index);
        this.list = copy;
    }
    
    public T get(final int index) {
        return this.list.get(index);
    }
    
    @Override
    public int size() {
        return this.list.size();
    }
    
    @Override
    public Iterator<T> iterator() {
        return Iterables.unmodifiableIterable((Iterable)this.list).iterator();
    }
    
    @Override
    public void clear() {
        this.list = new ArrayList<T>();
    }
    
    @Override
    public boolean contains(final Object value) {
        return this.list.contains(value);
    }
    
    @Override
    public boolean containsAll(final Collection<?> values) {
        return this.list.containsAll(values);
    }
    
    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }
    
    @Override
    public Object[] toArray() {
        return this.list.toArray();
    }
    
    @Override
    public <T> T[] toArray(final T[] a) {
        return this.list.toArray(a);
    }
    
    @Override
    public String toString() {
        return this.list.toString();
    }
}
