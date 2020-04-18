package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.wrappers.collection.ConvertedSet;
import java.util.Iterator;
import com.google.common.collect.Multiset;
import java.util.Set;
import java.util.Collection;
import java.util.Map;
import com.google.common.collect.Multimap;

class GuavaWrappers
{
    private static volatile boolean USE_REFLECTION_FALLBACK;
    
    public static <TKey, TValue> Multimap<TKey, TValue> getBukkitMultimap(final Multimap<TKey, TValue> multimap) {
        if (GuavaWrappers.USE_REFLECTION_FALLBACK) {
            return GuavaReflection.getBukkitMultimap(multimap);
        }
        final Multimap<TKey, TValue> result = (Multimap<TKey, TValue>)new Multimap<TKey, TValue>() {
            public Map<TKey, Collection<TValue>> asMap() {
                return (Map<TKey, Collection<TValue>>)multimap.asMap();
            }
            
            public void clear() {
                multimap.clear();
            }
            
            public boolean containsEntry(final Object arg0, final Object arg1) {
                return multimap.containsEntry(arg0, arg1);
            }
            
            public boolean containsKey(final Object arg0) {
                return multimap.containsKey(arg0);
            }
            
            public boolean containsValue(final Object arg0) {
                return multimap.containsValue(arg0);
            }
            
            public Collection<Map.Entry<TKey, TValue>> entries() {
                return (Collection<Map.Entry<TKey, TValue>>)multimap.entries();
            }
            
            @Override
            public boolean equals(final Object arg0) {
                return multimap.equals(arg0);
            }
            
            public Collection<TValue> get(final TKey arg0) {
                return (Collection<TValue>)multimap.get((Object)arg0);
            }
            
            @Override
            public int hashCode() {
                return multimap.hashCode();
            }
            
            public boolean isEmpty() {
                return multimap.isEmpty();
            }
            
            public Set<TKey> keySet() {
                return (Set<TKey>)multimap.keySet();
            }
            
            public Multiset<TKey> keys() {
                return GuavaWrappers.getBukkitMultiset((com.google.common.collect.Multiset<TKey>)multimap.keys());
            }
            
            public boolean put(final TKey arg0, final TValue arg1) {
                return multimap.put((Object)arg0, (Object)arg1);
            }
            
            public boolean putAll(final Multimap<? extends TKey, ? extends TValue> arg0) {
                boolean result = false;
                for (final Map.Entry<? extends TKey, ? extends TValue> entry : arg0.entries()) {
                    result |= multimap.put((Object)entry.getKey(), (Object)entry.getValue());
                }
                return result;
            }
            
            public boolean putAll(final TKey arg0, final Iterable<? extends TValue> arg1) {
                return multimap.putAll((Object)arg0, (Iterable)arg1);
            }
            
            public boolean remove(final Object arg0, final Object arg1) {
                return multimap.remove(arg0, arg1);
            }
            
            public Collection<TValue> removeAll(final Object arg0) {
                return (Collection<TValue>)multimap.removeAll(arg0);
            }
            
            public Collection<TValue> replaceValues(final TKey arg0, final Iterable<? extends TValue> arg1) {
                return (Collection<TValue>)multimap.replaceValues((Object)arg0, (Iterable)arg1);
            }
            
            public int size() {
                return multimap.size();
            }
            
            public Collection<TValue> values() {
                return (Collection<TValue>)multimap.values();
            }
        };
        try {
            result.size();
            return result;
        }
        catch (LinkageError e) {
            GuavaWrappers.USE_REFLECTION_FALLBACK = true;
            return GuavaReflection.getBukkitMultimap(multimap);
        }
    }
    
    public static <TValue> Multiset<TValue> getBukkitMultiset(final Multiset<TValue> multiset) {
        if (GuavaWrappers.USE_REFLECTION_FALLBACK) {
            return GuavaReflection.getBukkitMultiset(multiset);
        }
        final Multiset<TValue> result = (Multiset<TValue>)new Multiset<TValue>() {
            public int add(final TValue arg0, final int arg1) {
                return multiset.add((Object)arg0, arg1);
            }
            
            public boolean add(final TValue arg0) {
                return multiset.add((Object)arg0);
            }
            
            public boolean addAll(final Collection<? extends TValue> c) {
                return multiset.addAll((Collection)c);
            }
            
            public void clear() {
                multiset.clear();
            }
            
            public boolean contains(final Object arg0) {
                return multiset.contains(arg0);
            }
            
            public boolean containsAll(final Collection<?> arg0) {
                return multiset.containsAll((Collection)arg0);
            }
            
            public int count(final Object arg0) {
                return multiset.count(arg0);
            }
            
            public Set<TValue> elementSet() {
                return (Set<TValue>)multiset.elementSet();
            }
            
            public Set<Multiset.Entry<TValue>> entrySet() {
                return new ConvertedSet<Multiset.Entry<TValue>, Multiset.Entry<TValue>>(multiset.entrySet()) {
                    @Override
                    protected Multiset.Entry<TValue> toOuter(final Multiset.Entry<TValue> inner) {
                        return (Multiset.Entry<TValue>)getBukkitEntry((Multiset.Entry<Object>)inner);
                    }
                    
                    @Override
                    protected Multiset.Entry<TValue> toInner(final Multiset.Entry<TValue> outer) {
                        throw new UnsupportedOperationException("Cannot convert " + outer);
                    }
                };
            }
            
            @Override
            public boolean equals(final Object arg0) {
                return multiset.equals(arg0);
            }
            
            @Override
            public int hashCode() {
                return multiset.hashCode();
            }
            
            public boolean isEmpty() {
                return multiset.isEmpty();
            }
            
            public Iterator<TValue> iterator() {
                return (Iterator<TValue>)multiset.iterator();
            }
            
            public int remove(final Object arg0, final int arg1) {
                return multiset.remove(arg0, arg1);
            }
            
            public boolean remove(final Object arg0) {
                return multiset.remove(arg0);
            }
            
            public boolean removeAll(final Collection<?> arg0) {
                return multiset.removeAll((Collection)arg0);
            }
            
            public boolean retainAll(final Collection<?> arg0) {
                return multiset.retainAll((Collection)arg0);
            }
            
            public boolean setCount(final TValue arg0, final int arg1, final int arg2) {
                return multiset.setCount((Object)arg0, arg1, arg2);
            }
            
            public int setCount(final TValue arg0, final int arg1) {
                return multiset.setCount((Object)arg0, arg1);
            }
            
            public int size() {
                return multiset.size();
            }
            
            public Object[] toArray() {
                return multiset.toArray();
            }
            
            public <T> T[] toArray(final T[] a) {
                return (T[])multiset.toArray((Object[])a);
            }
            
            @Override
            public String toString() {
                return multiset.toString();
            }
        };
        try {
            result.size();
            return result;
        }
        catch (LinkageError e) {
            GuavaWrappers.USE_REFLECTION_FALLBACK = true;
            return GuavaReflection.getBukkitMultiset(multiset);
        }
    }
    
    private static <TValue> Multiset.Entry<TValue> getBukkitEntry(final Multiset.Entry<TValue> entry) {
        return (Multiset.Entry<TValue>)new Multiset.Entry<TValue>() {
            @Override
            public boolean equals(final Object arg0) {
                return entry.equals(arg0);
            }
            
            public int getCount() {
                return entry.getCount();
            }
            
            public TValue getElement() {
                return (TValue)entry.getElement();
            }
            
            @Override
            public int hashCode() {
                return entry.hashCode();
            }
            
            @Override
            public String toString() {
                return entry.toString();
            }
        };
    }
    
    static {
        GuavaWrappers.USE_REFLECTION_FALLBACK = false;
    }
}
