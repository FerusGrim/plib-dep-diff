package com.comphenix.protocol.injector.player;

import com.google.common.base.Objects;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Collection;
import com.google.common.collect.HashBiMap;
import java.util.List;
import com.google.common.collect.BiMap;
import java.util.ArrayList;

class ReplacedArrayList<TKey> extends ArrayList<TKey>
{
    private static final long serialVersionUID = 1008492765999744804L;
    private BiMap<TKey, TKey> replaceMap;
    private List<TKey> underlyingList;
    
    public ReplacedArrayList(final List<TKey> underlyingList) {
        this.replaceMap = (BiMap<TKey, TKey>)HashBiMap.create();
        this.underlyingList = underlyingList;
    }
    
    protected void onReplacing(final TKey inserting, final TKey replacement) {
    }
    
    protected void onInserting(final TKey inserting) {
    }
    
    protected void onRemoved(final TKey removing) {
    }
    
    @Override
    public boolean add(final TKey element) {
        this.onInserting(element);
        if (this.replaceMap.containsKey((Object)element)) {
            final TKey replacement = (TKey)this.replaceMap.get((Object)element);
            this.onReplacing(element, replacement);
            return this.delegate().add(replacement);
        }
        return this.delegate().add(element);
    }
    
    @Override
    public void add(final int index, final TKey element) {
        this.onInserting(element);
        if (this.replaceMap.containsKey((Object)element)) {
            final TKey replacement = (TKey)this.replaceMap.get((Object)element);
            this.onReplacing(element, replacement);
            this.delegate().add(index, replacement);
        }
        else {
            this.delegate().add(index, element);
        }
    }
    
    @Override
    public boolean addAll(final Collection<? extends TKey> collection) {
        final int oldSize = this.size();
        for (final TKey element : collection) {
            this.add(element);
        }
        return this.size() != oldSize;
    }
    
    @Override
    public boolean addAll(int index, final Collection<? extends TKey> elements) {
        final int oldSize = this.size();
        for (final TKey element : elements) {
            this.add(index++, element);
        }
        return this.size() != oldSize;
    }
    
    @Override
    public boolean remove(final Object object) {
        final boolean success = this.delegate().remove(object);
        if (success) {
            this.onRemoved(object);
        }
        return success;
    }
    
    @Override
    public TKey remove(final int index) {
        final TKey removed = this.delegate().remove(index);
        if (removed != null) {
            this.onRemoved(removed);
        }
        return removed;
    }
    
    @Override
    public boolean removeAll(final Collection<?> collection) {
        final int oldSize = this.size();
        for (final Object element : collection) {
            this.remove(element);
        }
        return this.size() != oldSize;
    }
    
    protected List<TKey> delegate() {
        return this.underlyingList;
    }
    
    @Override
    public void clear() {
        for (final TKey element : this.delegate()) {
            this.onRemoved(element);
        }
        this.delegate().clear();
    }
    
    @Override
    public boolean contains(final Object o) {
        return this.delegate().contains(o);
    }
    
    @Override
    public boolean containsAll(final Collection<?> c) {
        return this.delegate().containsAll(c);
    }
    
    @Override
    public TKey get(final int index) {
        return this.delegate().get(index);
    }
    
    @Override
    public int indexOf(final Object o) {
        return this.delegate().indexOf(o);
    }
    
    @Override
    public boolean isEmpty() {
        return this.delegate().isEmpty();
    }
    
    @Override
    public Iterator<TKey> iterator() {
        return this.delegate().iterator();
    }
    
    @Override
    public int lastIndexOf(final Object o) {
        return this.delegate().lastIndexOf(o);
    }
    
    @Override
    public ListIterator<TKey> listIterator() {
        return this.delegate().listIterator();
    }
    
    @Override
    public ListIterator<TKey> listIterator(final int index) {
        return this.delegate().listIterator(index);
    }
    
    @Override
    public boolean retainAll(final Collection<?> c) {
        final int oldSize = this.size();
        final Iterator<TKey> it = this.delegate().iterator();
        while (it.hasNext()) {
            final TKey current = it.next();
            if (!c.contains(current)) {
                it.remove();
                this.onRemoved(current);
            }
        }
        return this.size() != oldSize;
    }
    
    @Override
    public TKey set(final int index, final TKey element) {
        if (this.replaceMap.containsKey((Object)element)) {
            final TKey replacement = (TKey)this.replaceMap.get((Object)element);
            this.onReplacing(element, replacement);
            return this.delegate().set(index, replacement);
        }
        return this.delegate().set(index, element);
    }
    
    @Override
    public int size() {
        return this.delegate().size();
    }
    
    @Override
    public List<TKey> subList(final int fromIndex, final int toIndex) {
        return this.delegate().subList(fromIndex, toIndex);
    }
    
    @Override
    public Object[] toArray() {
        return this.delegate().toArray();
    }
    
    @Override
    public <T> T[] toArray(final T[] a) {
        return this.delegate().toArray(a);
    }
    
    public synchronized void addMapping(final TKey target, final TKey replacement) {
        this.addMapping(target, replacement, false);
    }
    
    public TKey getMapping(final TKey target) {
        return (TKey)this.replaceMap.get((Object)target);
    }
    
    public synchronized void addMapping(final TKey target, final TKey replacement, final boolean ignoreExisting) {
        this.replaceMap.put((Object)target, (Object)replacement);
        if (!ignoreExisting) {
            this.replaceAll(target, replacement);
        }
    }
    
    public synchronized TKey removeMapping(final TKey target) {
        if (this.replaceMap.containsKey((Object)target)) {
            final TKey replacement = (TKey)this.replaceMap.get((Object)target);
            this.replaceMap.remove((Object)target);
            this.replaceAll(replacement, target);
            return replacement;
        }
        return null;
    }
    
    public synchronized TKey swapMapping(final TKey target) {
        final TKey replacement = this.removeMapping(target);
        if (replacement != null) {
            this.replaceMap.put((Object)replacement, (Object)target);
        }
        return replacement;
    }
    
    public synchronized void replaceAll(final TKey find, final TKey replace) {
        for (int i = 0; i < this.underlyingList.size(); ++i) {
            if (Objects.equal((Object)this.underlyingList.get(i), (Object)find)) {
                this.onReplacing(find, replace);
                this.underlyingList.set(i, replace);
            }
        }
    }
    
    public synchronized void revertAll() {
        if (this.replaceMap.size() < 1) {
            return;
        }
        final BiMap<TKey, TKey> inverse = (BiMap<TKey, TKey>)this.replaceMap.inverse();
        for (int i = 0; i < this.underlyingList.size(); ++i) {
            final TKey replaced = this.underlyingList.get(i);
            if (inverse.containsKey((Object)replaced)) {
                final TKey original = (TKey)inverse.get((Object)replaced);
                this.onReplacing(replaced, original);
                this.underlyingList.set(i, original);
            }
        }
        this.replaceMap.clear();
    }
    
    @Override
    protected void finalize() throws Throwable {
        this.revertAll();
        super.finalize();
    }
}
