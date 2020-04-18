package com.comphenix.protocol.async;

import java.util.Iterator;
import java.util.Collection;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import javax.annotation.Nullable;
import java.util.Queue;

class Synchronization
{
    public static <E> Queue<E> queue(final Queue<E> queue, @Nullable final Object mutex) {
        return (queue instanceof SynchronizedQueue) ? queue : new SynchronizedQueue<E>(queue, mutex);
    }
    
    private static class SynchronizedObject implements Serializable
    {
        private static final long serialVersionUID = -4408866092364554628L;
        final Object delegate;
        final Object mutex;
        
        SynchronizedObject(final Object delegate, @Nullable final Object mutex) {
            this.delegate = Preconditions.checkNotNull(delegate);
            this.mutex = ((mutex == null) ? this : mutex);
        }
        
        Object delegate() {
            return this.delegate;
        }
        
        @Override
        public String toString() {
            synchronized (this.mutex) {
                return this.delegate.toString();
            }
        }
    }
    
    private static class SynchronizedCollection<E> extends SynchronizedObject implements Collection<E>
    {
        private static final long serialVersionUID = 5440572373531285692L;
        
        private SynchronizedCollection(final Collection<E> delegate, @Nullable final Object mutex) {
            super(delegate, mutex);
        }
        
        @Override
        Collection<E> delegate() {
            return (Collection<E>)super.delegate();
        }
        
        @Override
        public boolean add(final E e) {
            synchronized (this.mutex) {
                return this.delegate().add(e);
            }
        }
        
        @Override
        public boolean addAll(final Collection<? extends E> c) {
            synchronized (this.mutex) {
                return this.delegate().addAll(c);
            }
        }
        
        @Override
        public void clear() {
            synchronized (this.mutex) {
                this.delegate().clear();
            }
        }
        
        @Override
        public boolean contains(final Object o) {
            synchronized (this.mutex) {
                return this.delegate().contains(o);
            }
        }
        
        @Override
        public boolean containsAll(final Collection<?> c) {
            synchronized (this.mutex) {
                return this.delegate().containsAll(c);
            }
        }
        
        @Override
        public boolean isEmpty() {
            synchronized (this.mutex) {
                return this.delegate().isEmpty();
            }
        }
        
        @Override
        public Iterator<E> iterator() {
            return this.delegate().iterator();
        }
        
        @Override
        public boolean remove(final Object o) {
            synchronized (this.mutex) {
                return this.delegate().remove(o);
            }
        }
        
        @Override
        public boolean removeAll(final Collection<?> c) {
            synchronized (this.mutex) {
                return this.delegate().removeAll(c);
            }
        }
        
        @Override
        public boolean retainAll(final Collection<?> c) {
            synchronized (this.mutex) {
                return this.delegate().retainAll(c);
            }
        }
        
        @Override
        public int size() {
            synchronized (this.mutex) {
                return this.delegate().size();
            }
        }
        
        @Override
        public Object[] toArray() {
            synchronized (this.mutex) {
                return this.delegate().toArray();
            }
        }
        
        @Override
        public <T> T[] toArray(final T[] a) {
            synchronized (this.mutex) {
                return this.delegate().toArray(a);
            }
        }
    }
    
    private static class SynchronizedQueue<E> extends SynchronizedCollection<E> implements Queue<E>
    {
        private static final long serialVersionUID = 1961791630386791902L;
        
        SynchronizedQueue(final Queue<E> delegate, @Nullable final Object mutex) {
            super((Collection)delegate, mutex);
        }
        
        @Override
        Queue<E> delegate() {
            return (Queue<E>)(Queue)super.delegate();
        }
        
        @Override
        public E element() {
            synchronized (this.mutex) {
                return this.delegate().element();
            }
        }
        
        @Override
        public boolean offer(final E e) {
            synchronized (this.mutex) {
                return this.delegate().offer(e);
            }
        }
        
        @Override
        public E peek() {
            synchronized (this.mutex) {
                return this.delegate().peek();
            }
        }
        
        @Override
        public E poll() {
            synchronized (this.mutex) {
                return this.delegate().poll();
            }
        }
        
        @Override
        public E remove() {
            synchronized (this.mutex) {
                return this.delegate().remove();
            }
        }
    }
}
