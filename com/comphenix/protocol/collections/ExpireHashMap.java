package com.comphenix.protocol.collections;

import com.google.common.primitives.Longs;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import com.google.common.collect.Maps;
import com.google.common.base.Function;
import java.util.HashMap;
import com.google.common.base.Ticker;
import java.util.PriorityQueue;
import java.util.Map;

public class ExpireHashMap<K, V>
{
    private Map<K, ExpireEntry> keyLookup;
    private PriorityQueue<ExpireEntry> expireQueue;
    private Map<K, V> valueView;
    private Ticker ticker;
    
    public ExpireHashMap() {
        this(Ticker.systemTicker());
    }
    
    public ExpireHashMap(final Ticker ticker) {
        this.keyLookup = new HashMap<K, ExpireEntry>();
        this.expireQueue = new PriorityQueue<ExpireEntry>();
        this.valueView = (Map<K, V>)Maps.transformValues((Map)this.keyLookup, (Function)new Function<ExpireEntry, V>() {
            public V apply(final ExpireEntry entry) {
                return entry.expireValue;
            }
        });
        this.ticker = ticker;
    }
    
    public V get(final K key) {
        this.evictExpired();
        final ExpireEntry entry = this.keyLookup.get(key);
        return (entry != null) ? entry.expireValue : null;
    }
    
    public V put(final K key, final V value, final long expireDelay, final TimeUnit expireUnit) {
        Preconditions.checkNotNull((Object)expireUnit, (Object)"expireUnit cannot be NULL");
        Preconditions.checkState(expireDelay > 0L, (Object)"expireDelay cannot be equal or less than zero.");
        this.evictExpired();
        final ExpireEntry entry = new ExpireEntry(this.ticker.read() + TimeUnit.NANOSECONDS.convert(expireDelay, expireUnit), key, value);
        final ExpireEntry previous = this.keyLookup.put(key, entry);
        this.expireQueue.add(entry);
        return (previous != null) ? previous.expireValue : null;
    }
    
    public boolean containsKey(final K key) {
        this.evictExpired();
        return this.keyLookup.containsKey(key);
    }
    
    public boolean containsValue(final V value) {
        this.evictExpired();
        for (final ExpireEntry entry : this.keyLookup.values()) {
            if (Objects.equal((Object)value, (Object)entry.expireValue)) {
                return true;
            }
        }
        return false;
    }
    
    public V removeKey(final K key) {
        this.evictExpired();
        final ExpireEntry entry = this.keyLookup.remove(key);
        return (entry != null) ? entry.expireValue : null;
    }
    
    public int size() {
        this.evictExpired();
        return this.keyLookup.size();
    }
    
    public Set<K> keySet() {
        this.evictExpired();
        return this.keyLookup.keySet();
    }
    
    public Collection<V> values() {
        this.evictExpired();
        return this.valueView.values();
    }
    
    public Set<Map.Entry<K, V>> entrySet() {
        this.evictExpired();
        return this.valueView.entrySet();
    }
    
    public Map<K, V> asMap() {
        this.evictExpired();
        return this.valueView;
    }
    
    public void collect() {
        this.evictExpired();
        this.expireQueue.clear();
        this.expireQueue.addAll((Collection<?>)this.keyLookup.values());
    }
    
    public void clear() {
        this.keyLookup.clear();
        this.expireQueue.clear();
    }
    
    protected void evictExpired() {
        final long currentTime = this.ticker.read();
        while (this.expireQueue.size() > 0 && this.expireQueue.peek().expireTime <= currentTime) {
            final ExpireEntry entry = this.expireQueue.poll();
            if (entry == this.keyLookup.get(entry.expireKey)) {
                this.keyLookup.remove(entry.expireKey);
            }
        }
    }
    
    @Override
    public String toString() {
        return this.valueView.toString();
    }
    
    private class ExpireEntry implements Comparable<ExpireEntry>
    {
        public final long expireTime;
        public final K expireKey;
        public final V expireValue;
        
        public ExpireEntry(final long expireTime, final K expireKey, final V expireValue) {
            this.expireTime = expireTime;
            this.expireKey = expireKey;
            this.expireValue = expireValue;
        }
        
        @Override
        public int compareTo(final ExpireEntry o) {
            return Longs.compare(this.expireTime, o.expireTime);
        }
        
        @Override
        public String toString() {
            return "ExpireEntry [expireTime=" + this.expireTime + ", expireKey=" + this.expireKey + ", expireValue=" + this.expireValue + "]";
        }
    }
}
