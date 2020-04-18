package com.comphenix.protocol.wrappers.collection;

import java.util.Iterator;
import java.util.Map;
import com.google.common.collect.Multiset;
import java.util.Set;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import javax.annotation.Nullable;
import com.google.common.base.Joiner;
import java.util.Collection;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

public abstract class ConvertedMultimap<Key, VInner, VOuter> extends AbstractConverted<VInner, VOuter> implements Multimap<Key, VOuter>
{
    private Multimap<Key, VInner> inner;
    
    public ConvertedMultimap(final Multimap<Key, VInner> inner) {
        this.inner = (Multimap<Key, VInner>)Preconditions.checkNotNull((Object)inner, (Object)"inner map cannot be NULL.");
    }
    
    protected Collection<VOuter> toOuterCollection(final Collection<VInner> inner) {
        return new ConvertedCollection<VInner, VOuter>(inner) {
            @Override
            protected VInner toInner(final VOuter outer) {
                return ConvertedMultimap.this.toInner(outer);
            }
            
            @Override
            protected VOuter toOuter(final VInner inner) {
                return ConvertedMultimap.this.toOuter(inner);
            }
            
            @Override
            public String toString() {
                return "[" + Joiner.on(", ").join((Iterable)this) + "]";
            }
        };
    }
    
    protected Collection<VInner> toInnerCollection(final Collection<VOuter> outer) {
        return new ConvertedCollection<VOuter, VInner>(outer) {
            @Override
            protected VOuter toInner(final VInner outer) {
                return ConvertedMultimap.this.toOuter(outer);
            }
            
            @Override
            protected VInner toOuter(final VOuter inner) {
                return ConvertedMultimap.this.toInner(inner);
            }
            
            @Override
            public String toString() {
                return "[" + Joiner.on(", ").join((Iterable)this) + "]";
            }
        };
    }
    
    protected Object toInnerObject(final Object outer) {
        return this.toInner((VOuter)outer);
    }
    
    public int size() {
        return this.inner.size();
    }
    
    public boolean isEmpty() {
        return this.inner.isEmpty();
    }
    
    public boolean containsKey(@Nullable final Object key) {
        return this.inner.containsKey(key);
    }
    
    public boolean containsValue(@Nullable final Object value) {
        return this.inner.containsValue(this.toInnerObject(value));
    }
    
    public boolean containsEntry(@Nullable final Object key, @Nullable final Object value) {
        return this.inner.containsEntry(key, this.toInnerObject(value));
    }
    
    public boolean put(@Nullable final Key key, @Nullable final VOuter value) {
        return this.inner.put((Object)key, this.toInner(value));
    }
    
    public boolean remove(@Nullable final Object key, @Nullable final Object value) {
        return this.inner.remove(key, this.toInnerObject(value));
    }
    
    public boolean putAll(@Nullable final Key key, final Iterable<? extends VOuter> values) {
        return this.inner.putAll((Object)key, Iterables.transform((Iterable)values, (Function)this.getInnerConverter()));
    }
    
    public boolean putAll(final Multimap<? extends Key, ? extends VOuter> multimap) {
        return this.inner.putAll((Multimap)new ConvertedMultimap<Key, VOuter, VInner>(multimap) {
            @Override
            protected VOuter toInner(final VInner outer) {
                return ConvertedMultimap.this.toOuter(outer);
            }
            
            @Override
            protected VInner toOuter(final VOuter inner) {
                return ConvertedMultimap.this.toInner(inner);
            }
        });
    }
    
    public Collection<VOuter> replaceValues(@Nullable final Key key, final Iterable<? extends VOuter> values) {
        return this.toOuterCollection(this.inner.replaceValues((Object)key, Iterables.transform((Iterable)values, (Function)this.getInnerConverter())));
    }
    
    public Collection<VOuter> removeAll(@Nullable final Object key) {
        return this.toOuterCollection(this.inner.removeAll(key));
    }
    
    public void clear() {
        this.inner.clear();
    }
    
    public Collection<VOuter> get(@Nullable final Key key) {
        return this.toOuterCollection(this.inner.get((Object)key));
    }
    
    public Set<Key> keySet() {
        return (Set<Key>)this.inner.keySet();
    }
    
    public Multiset<Key> keys() {
        return (Multiset<Key>)this.inner.keys();
    }
    
    public Collection<VOuter> values() {
        return this.toOuterCollection(this.inner.values());
    }
    
    public Collection<Map.Entry<Key, VOuter>> entries() {
        return (Collection<Map.Entry<Key, VOuter>>)ConvertedMap.convertedEntrySet(this.inner.entries(), (BiFunction<Object, Object, Object>)new BiFunction<Key, VOuter, VInner>() {
            @Override
            public VInner apply(final Key key, final VOuter outer) {
                return ConvertedMultimap.this.toInner(outer);
            }
        }, (BiFunction<Object, Object, Object>)new BiFunction<Key, VInner, VOuter>() {
            @Override
            public VOuter apply(final Key key, final VInner inner) {
                return ConvertedMultimap.this.toOuter(inner);
            }
        });
    }
    
    public Map<Key, Collection<VOuter>> asMap() {
        return new ConvertedMap<Key, Collection<VInner>, Collection<VOuter>>(this.inner.asMap()) {
            protected Collection<VInner> toInner(final Collection<VOuter> outer) {
                return ConvertedMultimap.this.toInnerCollection(outer);
            }
            
            protected Collection<VOuter> toOuter(final Collection<VInner> inner) {
                return ConvertedMultimap.this.toOuterCollection(inner);
            }
        };
    }
    
    public String toString() {
        final Iterator<Map.Entry<Key, VOuter>> i = this.entries().iterator();
        if (!i.hasNext()) {
            return "{}";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        while (true) {
            final Map.Entry<Key, VOuter> e = i.next();
            final Key key = e.getKey();
            final VOuter value = e.getValue();
            sb.append((key == this) ? "(this Map)" : key);
            sb.append('=');
            sb.append((value == this) ? "(this Map)" : value);
            if (!i.hasNext()) {
                break;
            }
            sb.append(", ");
        }
        return sb.append('}').toString();
    }
}
