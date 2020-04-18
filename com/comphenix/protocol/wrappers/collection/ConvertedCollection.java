package com.comphenix.protocol.wrappers.collection;

import java.lang.reflect.Array;
import java.util.List;
import com.google.common.collect.Lists;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.Collection;

public abstract class ConvertedCollection<VInner, VOuter> extends AbstractConverted<VInner, VOuter> implements Collection<VOuter>
{
    private Collection<VInner> inner;
    
    public ConvertedCollection(final Collection<VInner> inner) {
        this.inner = inner;
    }
    
    @Override
    public boolean add(final VOuter e) {
        return this.inner.add(this.toInner(e));
    }
    
    @Override
    public boolean addAll(final Collection<? extends VOuter> c) {
        boolean modified = false;
        for (final VOuter outer : c) {
            modified |= this.add(outer);
        }
        return modified;
    }
    
    @Override
    public void clear() {
        this.inner.clear();
    }
    
    @Override
    public boolean contains(final Object o) {
        return this.inner.contains(this.toInner((VOuter)o));
    }
    
    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object outer : c) {
            if (!this.contains(outer)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean isEmpty() {
        return this.inner.isEmpty();
    }
    
    @Override
    public Iterator<VOuter> iterator() {
        return (Iterator<VOuter>)Iterators.transform((Iterator)this.inner.iterator(), (Function)this.getOuterConverter());
    }
    
    @Override
    public boolean remove(final Object o) {
        return this.inner.remove(this.toInner((VOuter)o));
    }
    
    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean modified = false;
        for (final Object outer : c) {
            modified |= this.remove(outer);
        }
        return modified;
    }
    
    @Override
    public boolean retainAll(final Collection<?> c) {
        final List<VInner> innerCopy = (List<VInner>)Lists.newArrayList();
        for (final Object outer : c) {
            innerCopy.add(this.toInner((VOuter)outer));
        }
        return this.inner.retainAll(innerCopy);
    }
    
    @Override
    public int size() {
        return this.inner.size();
    }
    
    @Override
    public Object[] toArray() {
        final Object[] array = this.inner.toArray();
        for (int i = 0; i < array.length; ++i) {
            array[i] = this.toOuter((VInner)array[i]);
        }
        return array;
    }
    
    @Override
    public <T> T[] toArray(final T[] a) {
        T[] array = a;
        int index = 0;
        if (array.length < this.size()) {
            array = (T[])Array.newInstance(a.getClass().getComponentType(), this.size());
        }
        for (final VInner innerValue : this.inner) {
            array[index++] = (T)this.toOuter(innerValue);
        }
        return array;
    }
}
