package com.comphenix.protocol.wrappers.collection;

import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.Arrays;
import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.Collection;

public class CachedCollection<T> implements Collection<T>
{
    protected Set<T> delegate;
    protected Object[] cache;
    
    public CachedCollection(final Set<T> delegate) {
        this.delegate = (Set<T>)Preconditions.checkNotNull((Object)delegate, (Object)"delegate cannot be NULL.");
    }
    
    private void initializeCache() {
        if (this.cache == null) {
            this.cache = new Object[this.delegate.size()];
        }
    }
    
    private void growCache() {
        if (this.cache == null) {
            return;
        }
        int newLength;
        for (newLength = this.cache.length; newLength < this.delegate.size(); newLength *= 2) {}
        if (newLength != this.cache.length) {
            this.cache = Arrays.copyOf(this.cache, newLength);
        }
    }
    
    @Override
    public int size() {
        return this.delegate.size();
    }
    
    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }
    
    @Override
    public boolean contains(final Object o) {
        return this.delegate.contains(o);
    }
    
    @Override
    public Iterator<T> iterator() {
        final Iterator<T> source = this.delegate.iterator();
        this.initializeCache();
        return new Iterator<T>() {
            int currentIndex = -1;
            int iteratorIndex = -1;
            
            @Override
            public boolean hasNext() {
                return this.currentIndex < CachedCollection.this.delegate.size() - 1;
            }
            
            @Override
            public T next() {
                ++this.currentIndex;
                if (CachedCollection.this.cache[this.currentIndex] == null) {
                    CachedCollection.this.cache[this.currentIndex] = this.getSourceValue();
                }
                return (T)CachedCollection.this.cache[this.currentIndex];
            }
            
            @Override
            public void remove() {
                this.getSourceValue();
                source.remove();
            }
            
            private T getSourceValue() {
                T last = null;
                while (this.iteratorIndex < this.currentIndex) {
                    ++this.iteratorIndex;
                    last = source.next();
                }
                return last;
            }
        };
    }
    
    @Override
    public Object[] toArray() {
        Iterators.size((Iterator)this.iterator());
        return this.cache.clone();
    }
    
    @Override
    public <T> T[] toArray(final T[] a) {
        Iterators.size((Iterator)this.iterator());
        return Arrays.copyOf(this.cache, this.size(), (Class<? extends T[]>)a.getClass().getComponentType());
    }
    
    @Override
    public boolean add(final T e) {
        final boolean result = this.delegate.add(e);
        this.growCache();
        return result;
    }
    
    @Override
    public boolean addAll(final Collection<? extends T> c) {
        final boolean result = this.delegate.addAll(c);
        this.growCache();
        return result;
    }
    
    @Override
    public boolean containsAll(final Collection<?> c) {
        return this.delegate.containsAll(c);
    }
    
    @Override
    public boolean remove(final Object o) {
        this.cache = null;
        return this.delegate.remove(o);
    }
    
    @Override
    public boolean removeAll(final Collection<?> c) {
        this.cache = null;
        return this.delegate.removeAll(c);
    }
    
    @Override
    public boolean retainAll(final Collection<?> c) {
        this.cache = null;
        return this.delegate.retainAll(c);
    }
    
    @Override
    public void clear() {
        this.cache = null;
        this.delegate.clear();
    }
    
    @Override
    public int hashCode() {
        int result = 1;
        for (final Object element : this) {
            result = 31 * result + ((element == null) ? 0 : element.hashCode());
        }
        return result;
    }
    
    @Override
    public String toString() {
        Iterators.size((Iterator)this.iterator());
        final StringBuilder result = new StringBuilder("[");
        for (final T element : this) {
            if (result.length() > 1) {
                result.append(", ");
            }
            result.append(element);
        }
        return result.append("]").toString();
    }
}
