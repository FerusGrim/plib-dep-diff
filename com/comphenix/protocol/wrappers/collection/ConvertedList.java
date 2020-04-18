package com.comphenix.protocol.wrappers.collection;

import java.util.ListIterator;
import java.util.Collection;
import java.util.List;

public abstract class ConvertedList<VInner, VOuter> extends ConvertedCollection<VInner, VOuter> implements List<VOuter>
{
    private List<VInner> inner;
    
    public ConvertedList(final List<VInner> inner) {
        super(inner);
        this.inner = inner;
    }
    
    @Override
    public void add(final int index, final VOuter element) {
        this.inner.add(index, this.toInner(element));
    }
    
    @Override
    public boolean addAll(final int index, final Collection<? extends VOuter> c) {
        return this.inner.addAll(index, (Collection<? extends VInner>)this.getInnerCollection(c));
    }
    
    @Override
    public VOuter get(final int index) {
        return this.toOuter(this.inner.get(index));
    }
    
    @Override
    public int indexOf(final Object o) {
        return this.inner.indexOf(this.toInner((VOuter)o));
    }
    
    @Override
    public int lastIndexOf(final Object o) {
        return this.inner.lastIndexOf(this.toInner((VOuter)o));
    }
    
    @Override
    public ListIterator<VOuter> listIterator() {
        return this.listIterator(0);
    }
    
    @Override
    public ListIterator<VOuter> listIterator(final int index) {
        final ListIterator<VInner> innerIterator = this.inner.listIterator(index);
        return new ListIterator<VOuter>() {
            @Override
            public void add(final VOuter e) {
                innerIterator.add(ConvertedList.this.toInner(e));
            }
            
            @Override
            public boolean hasNext() {
                return innerIterator.hasNext();
            }
            
            @Override
            public boolean hasPrevious() {
                return innerIterator.hasPrevious();
            }
            
            @Override
            public VOuter next() {
                return ConvertedList.this.toOuter(innerIterator.next());
            }
            
            @Override
            public int nextIndex() {
                return innerIterator.nextIndex();
            }
            
            @Override
            public VOuter previous() {
                return ConvertedList.this.toOuter(innerIterator.previous());
            }
            
            @Override
            public int previousIndex() {
                return innerIterator.previousIndex();
            }
            
            @Override
            public void remove() {
                innerIterator.remove();
            }
            
            @Override
            public void set(final VOuter e) {
                innerIterator.set(ConvertedList.this.toInner(e));
            }
        };
    }
    
    @Override
    public VOuter remove(final int index) {
        return this.toOuter(this.inner.remove(index));
    }
    
    @Override
    public VOuter set(final int index, final VOuter element) {
        return this.toOuter(this.inner.set(index, this.toInner(element)));
    }
    
    @Override
    public List<VOuter> subList(final int fromIndex, final int toIndex) {
        return new ConvertedList<VInner, VOuter>(this.inner.subList(fromIndex, toIndex)) {
            @Override
            protected VInner toInner(final VOuter outer) {
                return ConvertedList.this.toInner(outer);
            }
            
            @Override
            protected VOuter toOuter(final VInner inner) {
                return ConvertedList.this.toOuter(inner);
            }
        };
    }
    
    private ConvertedCollection<VOuter, VInner> getInnerCollection(final Collection c) {
        return new ConvertedCollection<VOuter, VInner>(c) {
            @Override
            protected VOuter toInner(final VInner outer) {
                return ConvertedList.this.toOuter(outer);
            }
            
            @Override
            protected VInner toOuter(final VOuter inner) {
                return ConvertedList.this.toInner(inner);
            }
        };
    }
}
