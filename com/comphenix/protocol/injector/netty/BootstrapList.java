package com.comphenix.protocol.injector.netty;

import java.util.ListIterator;
import io.netty.channel.Channel;
import java.util.concurrent.Callable;
import io.netty.channel.ChannelFuture;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Iterator;
import io.netty.channel.ChannelHandler;
import java.util.List;

public class BootstrapList implements List<Object>
{
    private List<Object> delegate;
    private ChannelHandler handler;
    
    public BootstrapList(final List<Object> delegate, final ChannelHandler handler) {
        this.delegate = delegate;
        this.handler = handler;
        for (final Object item : this) {
            this.processElement(item);
        }
    }
    
    @Override
    public synchronized boolean add(final Object element) {
        this.processElement(element);
        return this.delegate.add(element);
    }
    
    @Override
    public synchronized boolean addAll(final Collection<?> collection) {
        final List<Object> copy = (List<Object>)Lists.newArrayList((Iterable)collection);
        for (final Object element : copy) {
            this.processElement(element);
        }
        return this.delegate.addAll(copy);
    }
    
    @Override
    public synchronized Object set(final int index, final Object element) {
        final Object old = this.delegate.set(index, element);
        if (old != element) {
            this.unprocessElement(old);
            this.processElement(element);
        }
        return old;
    }
    
    protected void processElement(final Object element) {
        if (element instanceof ChannelFuture) {
            this.processBootstrap((ChannelFuture)element);
        }
    }
    
    protected void unprocessElement(final Object element) {
        if (element instanceof ChannelFuture) {
            this.unprocessBootstrap((ChannelFuture)element);
        }
    }
    
    protected void processBootstrap(final ChannelFuture future) {
        future.channel().pipeline().addFirst(new ChannelHandler[] { this.handler });
    }
    
    protected void unprocessBootstrap(final ChannelFuture future) {
        final Channel channel = future.channel();
        channel.eventLoop().submit((Callable)new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                channel.pipeline().remove(BootstrapList.this.handler);
                return null;
            }
        });
    }
    
    public synchronized void close() {
        for (final Object element : this) {
            this.unprocessElement(element);
        }
    }
    
    @Override
    public synchronized int size() {
        return this.delegate.size();
    }
    
    @Override
    public synchronized boolean isEmpty() {
        return this.delegate.isEmpty();
    }
    
    @Override
    public boolean contains(final Object o) {
        return this.delegate.contains(o);
    }
    
    @Override
    public synchronized Iterator<Object> iterator() {
        return this.delegate.iterator();
    }
    
    @Override
    public synchronized Object[] toArray() {
        return this.delegate.toArray();
    }
    
    @Override
    public synchronized <T> T[] toArray(final T[] a) {
        return this.delegate.toArray(a);
    }
    
    @Override
    public synchronized boolean remove(final Object o) {
        return this.delegate.remove(o);
    }
    
    @Override
    public synchronized boolean containsAll(final Collection<?> c) {
        return this.delegate.containsAll(c);
    }
    
    @Override
    public synchronized boolean addAll(final int index, final Collection<?> c) {
        return this.delegate.addAll(index, c);
    }
    
    @Override
    public synchronized boolean removeAll(final Collection<?> c) {
        return this.delegate.removeAll(c);
    }
    
    @Override
    public synchronized boolean retainAll(final Collection<?> c) {
        return this.delegate.retainAll(c);
    }
    
    @Override
    public synchronized void clear() {
        this.delegate.clear();
    }
    
    @Override
    public synchronized Object get(final int index) {
        return this.delegate.get(index);
    }
    
    @Override
    public synchronized void add(final int index, final Object element) {
        this.delegate.add(index, element);
    }
    
    @Override
    public synchronized Object remove(final int index) {
        return this.delegate.remove(index);
    }
    
    @Override
    public synchronized int indexOf(final Object o) {
        return this.delegate.indexOf(o);
    }
    
    @Override
    public synchronized int lastIndexOf(final Object o) {
        return this.delegate.lastIndexOf(o);
    }
    
    @Override
    public synchronized ListIterator<Object> listIterator() {
        return this.delegate.listIterator();
    }
    
    @Override
    public synchronized ListIterator<Object> listIterator(final int index) {
        return this.delegate.listIterator(index);
    }
    
    @Override
    public synchronized List<Object> subList(final int fromIndex, final int toIndex) {
        return this.delegate.subList(fromIndex, toIndex);
    }
}
