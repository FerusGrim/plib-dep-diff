package com.comphenix.protocol.wrappers.collection;

import com.google.common.collect.Collections2;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import java.util.Map;

public abstract class ConvertedMap<Key, VInner, VOuter> extends AbstractConverted<VInner, VOuter> implements Map<Key, VOuter>
{
    private Map<Key, VInner> inner;
    private BiFunction<Key, VOuter, VInner> innerConverter;
    private BiFunction<Key, VInner, VOuter> outerConverter;
    
    public ConvertedMap(final Map<Key, VInner> inner) {
        this.innerConverter = new BiFunction<Key, VOuter, VInner>() {
            @Override
            public VInner apply(final Key key, final VOuter outer) {
                return ConvertedMap.this.toInner(key, outer);
            }
        };
        this.outerConverter = new BiFunction<Key, VInner, VOuter>() {
            @Override
            public VOuter apply(final Key key, final VInner inner) {
                return ConvertedMap.this.toOuter(key, inner);
            }
        };
        if (inner == null) {
            throw new IllegalArgumentException("Inner map cannot be NULL.");
        }
        this.inner = inner;
    }
    
    @Override
    public void clear() {
        this.inner.clear();
    }
    
    @Override
    public boolean containsKey(final Object key) {
        return this.inner.containsKey(key);
    }
    
    @Override
    public boolean containsValue(final Object value) {
        return this.inner.containsValue(this.toInner((VOuter)value));
    }
    
    @Override
    public Set<Entry<Key, VOuter>> entrySet() {
        return convertedEntrySet(this.inner.entrySet(), this.innerConverter, this.outerConverter);
    }
    
    protected VOuter toOuter(final Key key, final VInner inner) {
        return this.toOuter(inner);
    }
    
    protected VInner toInner(final Key key, final VOuter outer) {
        return this.toInner(outer);
    }
    
    @Override
    public VOuter get(final Object key) {
        return this.toOuter(key, this.inner.get(key));
    }
    
    @Override
    public boolean isEmpty() {
        return this.inner.isEmpty();
    }
    
    @Override
    public Set<Key> keySet() {
        return this.inner.keySet();
    }
    
    @Override
    public VOuter put(final Key key, final VOuter value) {
        return this.toOuter(key, this.inner.put(key, this.toInner(key, value)));
    }
    
    @Override
    public void putAll(final Map<? extends Key, ? extends VOuter> m) {
        for (final Entry<? extends Key, ? extends VOuter> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public VOuter remove(final Object key) {
        return this.toOuter(key, this.inner.remove(key));
    }
    
    @Override
    public int size() {
        return this.inner.size();
    }
    
    @Override
    public Collection<VOuter> values() {
        return (Collection<VOuter>)Collections2.transform((Collection)this.entrySet(), (Function)new Function<Entry<Key, VOuter>, VOuter>() {
            public VOuter apply(@Nullable final Entry<Key, VOuter> entry) {
                return entry.getValue();
            }
        });
    }
    
    @Override
    public String toString() {
        final Iterator<Entry<Key, VOuter>> i = this.entrySet().iterator();
        if (!i.hasNext()) {
            return "{}";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        while (true) {
            final Entry<Key, VOuter> e = i.next();
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
    
    static <Key, VInner, VOuter> Set<Entry<Key, VOuter>> convertedEntrySet(final Collection<Entry<Key, VInner>> entries, final BiFunction<Key, VOuter, VInner> innerFunction, final BiFunction<Key, VInner, VOuter> outerFunction) {
        return new ConvertedSet<Entry<Key, VInner>, Entry<Key, VOuter>>(entries) {
            @Override
            protected Entry<Key, VInner> toInner(final Entry<Key, VOuter> outer) {
                return new Entry<Key, VInner>() {
                    @Override
                    public Key getKey() {
                        return outer.getKey();
                    }
                    
                    @Override
                    public VInner getValue() {
                        return innerFunction.apply(this.getKey(), outer.getValue());
                    }
                    
                    @Override
                    public VInner setValue(final VInner value) {
                        return innerFunction.apply(this.getKey(), outer.setValue(outerFunction.apply(this.getKey(), value)));
                    }
                    
                    @Override
                    public String toString() {
                        return String.format("\"%s\": %s", this.getKey(), this.getValue());
                    }
                };
            }
            
            @Override
            protected Entry<Key, VOuter> toOuter(final Entry<Key, VInner> inner) {
                return new Entry<Key, VOuter>() {
                    @Override
                    public Key getKey() {
                        return inner.getKey();
                    }
                    
                    @Override
                    public VOuter getValue() {
                        return outerFunction.apply(this.getKey(), inner.getValue());
                    }
                    
                    @Override
                    public VOuter setValue(final VOuter value) {
                        final VInner converted = innerFunction.apply(this.getKey(), value);
                        return outerFunction.apply(this.getKey(), inner.setValue(converted));
                    }
                    
                    @Override
                    public String toString() {
                        return String.format("\"%s\": %s", this.getKey(), this.getValue());
                    }
                };
            }
        };
    }
}
