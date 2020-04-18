package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.wrappers.collection.ConvertedSet;
import java.util.Iterator;
import com.google.common.collect.Multiset;
import java.util.Set;
import java.util.Collection;
import java.util.Map;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.google.common.collect.Multimap;

class GuavaReflection
{
    public static <TKey, TValue> Multimap<TKey, TValue> getBukkitMultimap(final Object multimap) {
        return (Multimap<TKey, TValue>)new Multimap<TKey, TValue>() {
            private Class<?> multimapClass = multimap.getClass();
            private MethodAccessor methodAsMap = Accessors.getMethodAccessor(this.multimapClass, "asMap", (Class<?>[])new Class[0]);
            private MethodAccessor methodClear = Accessors.getMethodAccessor(this.multimapClass, "clear", (Class<?>[])new Class[0]);
            private MethodAccessor methodContainsEntry = Accessors.getMethodAccessor(this.multimapClass, "containsEntry", Object.class, Object.class);
            private MethodAccessor methodContainsKey = Accessors.getMethodAccessor(this.multimapClass, "containsKey", Object.class);
            private MethodAccessor methodContainsValue = Accessors.getMethodAccessor(this.multimapClass, "containsValue", Object.class);
            private MethodAccessor methodEntries = Accessors.getMethodAccessor(this.multimapClass, "entries", (Class<?>[])new Class[0]);
            private MethodAccessor methodGet = Accessors.getMethodAccessor(this.multimapClass, "get", Object.class);
            private MethodAccessor methodIsEmpty = Accessors.getMethodAccessor(this.multimapClass, "isEmpty", (Class<?>[])new Class[0]);
            private MethodAccessor methodKeySet = Accessors.getMethodAccessor(this.multimapClass, "keySet", (Class<?>[])new Class[0]);
            private MethodAccessor methodKeys = Accessors.getMethodAccessor(this.multimapClass, "keys", (Class<?>[])new Class[0]);
            private MethodAccessor methodPut = Accessors.getMethodAccessor(this.multimapClass, "put", Object.class, Object.class);
            private MethodAccessor methodPutAll = Accessors.getMethodAccessor(this.multimapClass, "putAll", Object.class, Iterable.class);
            private MethodAccessor methodRemove = Accessors.getMethodAccessor(this.multimapClass, "remove", Object.class, Object.class);
            private MethodAccessor methodRemoveAll = Accessors.getMethodAccessor(this.multimapClass, "removeAll", Object.class);
            private MethodAccessor methodReplaceValues = Accessors.getMethodAccessor(this.multimapClass, "replaceValues", Object.class, Iterable.class);
            private MethodAccessor methodSize = Accessors.getMethodAccessor(this.multimapClass, "size", (Class<?>[])new Class[0]);
            private MethodAccessor methodValues = Accessors.getMethodAccessor(this.multimapClass, "values", (Class<?>[])new Class[0]);
            
            public Map<TKey, Collection<TValue>> asMap() {
                return (Map<TKey, Collection<TValue>>)this.methodAsMap.invoke(multimap, new Object[0]);
            }
            
            public void clear() {
                this.methodClear.invoke(multimap, new Object[0]);
            }
            
            public boolean containsEntry(final Object arg0, final Object arg1) {
                return (boolean)this.methodContainsEntry.invoke(multimap, arg0, arg1);
            }
            
            public boolean containsKey(final Object arg0) {
                return (boolean)this.methodContainsKey.invoke(multimap, arg0);
            }
            
            public boolean containsValue(final Object arg0) {
                return (boolean)this.methodContainsValue.invoke(multimap, arg0);
            }
            
            public Collection<Map.Entry<TKey, TValue>> entries() {
                return (Collection<Map.Entry<TKey, TValue>>)this.methodEntries.invoke(multimap, new Object[0]);
            }
            
            @Override
            public boolean equals(final Object arg0) {
                return multimap.equals(arg0);
            }
            
            @Override
            public int hashCode() {
                return multimap.hashCode();
            }
            
            @Override
            public String toString() {
                return multimap.toString();
            }
            
            public Collection<TValue> get(final TKey arg0) {
                return (Collection<TValue>)this.methodGet.invoke(multimap, arg0);
            }
            
            public boolean isEmpty() {
                return (boolean)this.methodIsEmpty.invoke(multimap, new Object[0]);
            }
            
            public Set<TKey> keySet() {
                return (Set<TKey>)this.methodKeySet.invoke(multimap, new Object[0]);
            }
            
            public Multiset<TKey> keys() {
                return GuavaReflection.getBukkitMultiset(this.methodKeys.invoke(multimap, new Object[0]));
            }
            
            public boolean put(final TKey arg0, final TValue arg1) {
                return (boolean)this.methodPut.invoke(multimap, arg0, arg1);
            }
            
            public boolean putAll(final Multimap<? extends TKey, ? extends TValue> arg0) {
                boolean result = false;
                for (final Map.Entry<? extends TKey, ? extends TValue> entry : arg0.entries()) {
                    result |= (boolean)this.methodPut.invoke(multimap, entry.getKey(), entry.getValue());
                }
                return result;
            }
            
            public boolean putAll(final TKey arg0, final Iterable<? extends TValue> arg1) {
                return (boolean)this.methodPutAll.invoke(arg0, arg1);
            }
            
            public boolean remove(final Object arg0, final Object arg1) {
                return (boolean)this.methodRemove.invoke(multimap, arg0, arg1);
            }
            
            public Collection<TValue> removeAll(final Object arg0) {
                return (Collection<TValue>)this.methodRemoveAll.invoke(multimap, arg0);
            }
            
            public Collection<TValue> replaceValues(final TKey arg0, final Iterable<? extends TValue> arg1) {
                return (Collection<TValue>)this.methodReplaceValues.invoke(multimap, arg0, arg1);
            }
            
            public int size() {
                return (int)this.methodSize.invoke(multimap, new Object[0]);
            }
            
            public Collection<TValue> values() {
                return (Collection<TValue>)this.methodValues.invoke(multimap, new Object[0]);
            }
        };
    }
    
    public static <TValue> Multiset<TValue> getBukkitMultiset(final Object multiset) {
        return (Multiset<TValue>)new Multiset<TValue>() {
            private Class<?> multisetClass = multiset.getClass();
            private MethodAccessor methodAddMany = Accessors.getMethodAccessor(this.multisetClass, "add", Object.class, Integer.TYPE);
            private MethodAccessor methodAddOne = Accessors.getMethodAccessor(this.multisetClass, "add", Object.class);
            private MethodAccessor methodAddAll = Accessors.getMethodAccessor(this.multisetClass, "addAll", Collection.class);
            private MethodAccessor methodClear = Accessors.getMethodAccessor(this.multisetClass, "clear", (Class<?>[])new Class[0]);
            private MethodAccessor methodContains = Accessors.getMethodAccessor(this.multisetClass, "contains", Object.class);
            private MethodAccessor methodContainsAll = Accessors.getMethodAccessor(this.multisetClass, "containsAll", Collection.class);
            private MethodAccessor methodCount = Accessors.getMethodAccessor(this.multisetClass, "count", Object.class);
            private MethodAccessor methodElementSet = Accessors.getMethodAccessor(this.multisetClass, "elementSet", (Class<?>[])new Class[0]);
            private MethodAccessor methodEntrySet = Accessors.getMethodAccessor(this.multisetClass, "entrySet", (Class<?>[])new Class[0]);
            private MethodAccessor methodIsEmpty = Accessors.getMethodAccessor(this.multisetClass, "isEmpty", (Class<?>[])new Class[0]);
            private MethodAccessor methodIterator = Accessors.getMethodAccessor(this.multisetClass, "iterator", (Class<?>[])new Class[0]);
            private MethodAccessor methodRemoveCount = Accessors.getMethodAccessor(this.multisetClass, "remove", Object.class, Integer.TYPE);
            private MethodAccessor methodRemoveOne = Accessors.getMethodAccessor(this.multisetClass, "remove", Object.class);
            private MethodAccessor methodRemoveAll = Accessors.getMethodAccessor(this.multisetClass, "removeAll", Collection.class);
            private MethodAccessor methodRetainAll = Accessors.getMethodAccessor(this.multisetClass, "retainAll", Collection.class);
            private MethodAccessor methodSetCountOldNew = Accessors.getMethodAccessor(this.multisetClass, "setCount", Object.class, Integer.TYPE, Integer.TYPE);
            private MethodAccessor methodSetCountNew = Accessors.getMethodAccessor(this.multisetClass, "setCount", Object.class, Integer.TYPE);
            private MethodAccessor methodSize = Accessors.getMethodAccessor(this.multisetClass, "size", (Class<?>[])new Class[0]);
            private MethodAccessor methodToArray = Accessors.getMethodAccessor(this.multisetClass, "toArray", (Class<?>[])new Class[0]);
            private MethodAccessor methodToArrayBuffer = Accessors.getMethodAccessor(this.multisetClass, "toArray", Object[].class);
            
            public int add(final TValue arg0, final int arg1) {
                return (int)this.methodAddMany.invoke(multiset, arg0, arg1);
            }
            
            public boolean add(final TValue arg0) {
                return (boolean)this.methodAddOne.invoke(multiset, arg0);
            }
            
            public boolean addAll(final Collection<? extends TValue> c) {
                return (boolean)this.methodAddAll.invoke(multiset, c);
            }
            
            public void clear() {
                this.methodClear.invoke(multiset, new Object[0]);
            }
            
            public boolean contains(final Object arg0) {
                return (boolean)this.methodContains.invoke(multiset, arg0);
            }
            
            public boolean containsAll(final Collection<?> arg0) {
                return (boolean)this.methodContainsAll.invoke(multiset, arg0);
            }
            
            public int count(final Object arg0) {
                return (int)this.methodCount.invoke(multiset, arg0);
            }
            
            public Set<TValue> elementSet() {
                return (Set<TValue>)this.methodElementSet.invoke(multiset, new Object[0]);
            }
            
            public Set<Multiset.Entry<TValue>> entrySet() {
                return new ConvertedSet<Object, Multiset.Entry<TValue>>((Set)this.methodEntrySet.invoke(multiset, new Object[0])) {
                    @Override
                    protected Multiset.Entry<TValue> toOuter(final Object inner) {
                        return (Multiset.Entry<TValue>)getBukkitEntry(inner);
                    }
                    
                    @Override
                    protected Object toInner(final Multiset.Entry<TValue> outer) {
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
                return (boolean)this.methodIsEmpty.invoke(multiset, new Object[0]);
            }
            
            public Iterator<TValue> iterator() {
                return (Iterator<TValue>)this.methodIterator.invoke(multiset, new Object[0]);
            }
            
            public int remove(final Object arg0, final int arg1) {
                return (int)this.methodRemoveCount.invoke(multiset, arg0, arg1);
            }
            
            public boolean remove(final Object arg0) {
                return (boolean)this.methodRemoveOne.invoke(multiset, arg0);
            }
            
            public boolean removeAll(final Collection<?> arg0) {
                return (boolean)this.methodRemoveAll.invoke(multiset, arg0);
            }
            
            public boolean retainAll(final Collection<?> arg0) {
                return (boolean)this.methodRetainAll.invoke(multiset, arg0);
            }
            
            public boolean setCount(final TValue arg0, final int arg1, final int arg2) {
                return (boolean)this.methodSetCountOldNew.invoke(multiset, arg0, arg1, arg2);
            }
            
            public int setCount(final TValue arg0, final int arg1) {
                return (int)this.methodSetCountNew.invoke(multiset, arg0, arg1);
            }
            
            public int size() {
                return (int)this.methodSize.invoke(multiset, new Object[0]);
            }
            
            public Object[] toArray() {
                return (Object[])this.methodToArray.invoke(multiset, new Object[0]);
            }
            
            public <T> T[] toArray(final T[] a) {
                return (T[])this.methodToArrayBuffer.invoke(multiset, (Object[])a);
            }
            
            @Override
            public String toString() {
                return multiset.toString();
            }
        };
    }
    
    private static <TValue> Multiset.Entry<TValue> getBukkitEntry(final Object entry) {
        return (Multiset.Entry<TValue>)new Multiset.Entry<TValue>() {
            private Class<?> entryClass = entry.getClass();
            private MethodAccessor methodEquals = Accessors.getMethodAccessor(this.entryClass, "equals", Object.class);
            private MethodAccessor methodGetCount = Accessors.getMethodAccessor(this.entryClass, "getCount", (Class<?>[])new Class[0]);
            private MethodAccessor methodGetElement = Accessors.getMethodAccessor(this.entryClass, "getElement", (Class<?>[])new Class[0]);
            
            @Override
            public boolean equals(final Object arg0) {
                return (boolean)this.methodEquals.invoke(entry, arg0);
            }
            
            public int getCount() {
                return (int)this.methodGetCount.invoke(entry, new Object[0]);
            }
            
            public TValue getElement() {
                return (TValue)this.methodGetElement.invoke(entry, new Object[0]);
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
}
