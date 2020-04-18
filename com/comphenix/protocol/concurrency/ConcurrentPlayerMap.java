package com.comphenix.protocol.concurrency;

import com.google.common.cache.RemovalNotification;
import com.google.common.collect.AbstractIterator;
import java.util.AbstractSet;
import java.util.Map;
import java.util.Set;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.Iterator;
import com.comphenix.protocol.utility.Util;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import com.comphenix.protocol.utility.SafeCacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.base.Function;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.entity.Player;
import java.util.AbstractMap;

public class ConcurrentPlayerMap<TValue> extends AbstractMap<Player, TValue> implements ConcurrentMap<Player, TValue>
{
    private ConcurrentMap<Object, TValue> valueLookup;
    private ConcurrentMap<Object, Player> keyLookup;
    private final Function<Player, Object> keyMethod;
    
    public static <T> ConcurrentPlayerMap<T> usingAddress() {
        return new ConcurrentPlayerMap<T>(PlayerKey.ADDRESS);
    }
    
    public static <T> ConcurrentPlayerMap<T> usingName() {
        return new ConcurrentPlayerMap<T>(PlayerKey.NAME);
    }
    
    private ConcurrentPlayerMap(final PlayerKey standardMethod) {
        this.valueLookup = this.createValueMap();
        this.keyLookup = this.createKeyCache();
        this.keyMethod = (Function<Player, Object>)standardMethod;
    }
    
    public ConcurrentPlayerMap(final Function<Player, Object> method) {
        this.valueLookup = this.createValueMap();
        this.keyLookup = this.createKeyCache();
        this.keyMethod = method;
    }
    
    private ConcurrentMap<Object, TValue> createValueMap() {
        return (ConcurrentMap<Object, TValue>)Maps.newConcurrentMap();
    }
    
    private ConcurrentMap<Object, Player> createKeyCache() {
        return SafeCacheBuilder.newBuilder().weakValues().removalListener((com.google.common.cache.RemovalListener<? super Object, ? super Object>)(removed -> {
            if (removed.wasEvicted()) {
                this.onCacheEvicted(removed.getKey());
            }
        })).build((com.google.common.cache.CacheLoader<? super Object, Player>)new CacheLoader<Object, Player>() {
            public Player load(final Object key) throws Exception {
                final Player player = ConcurrentPlayerMap.this.findOnlinePlayer(key);
                if (player != null) {
                    return player;
                }
                throw new IllegalArgumentException("Unable to find a player associated with: " + key);
            }
        });
    }
    
    private void onCacheEvicted(final Object key) {
        final Player newPlayer = this.findOnlinePlayer(key);
        if (newPlayer != null) {
            this.keyLookup.put(key, newPlayer);
        }
        else {
            this.valueLookup.remove(key);
        }
    }
    
    private Player findOnlinePlayer(final Object key) {
        for (final Player player : Util.getOnlinePlayers()) {
            if (key.equals(this.keyMethod.apply((Object)player))) {
                return player;
            }
        }
        return null;
    }
    
    private Player lookupPlayer(final Object key) {
        try {
            return this.keyLookup.get(key);
        }
        catch (UncheckedExecutionException e) {
            return null;
        }
    }
    
    private Object cachePlayerKey(final Player player) {
        Preconditions.checkNotNull((Object)player, (Object)"player cannot be null");
        final Object key = this.keyMethod.apply((Object)player);
        this.keyLookup.put(key, player);
        return key;
    }
    
    @Override
    public TValue put(final Player key, final TValue value) {
        return this.valueLookup.put(this.cachePlayerKey(key), value);
    }
    
    @Override
    public TValue putIfAbsent(final Player key, final TValue value) {
        return this.valueLookup.putIfAbsent(this.cachePlayerKey(key), value);
    }
    
    @Override
    public TValue replace(final Player key, final TValue value) {
        return this.valueLookup.replace(this.cachePlayerKey(key), value);
    }
    
    @Override
    public boolean replace(final Player key, final TValue oldValue, final TValue newValue) {
        return this.valueLookup.replace(this.cachePlayerKey(key), oldValue, newValue);
    }
    
    @Override
    public TValue remove(final Object key) {
        if (key instanceof Player) {
            final Object playerKey = this.keyMethod.apply((Object)key);
            if (playerKey != null) {
                final TValue value = this.valueLookup.remove(playerKey);
                this.keyLookup.remove(playerKey);
                return value;
            }
        }
        return null;
    }
    
    @Override
    public boolean remove(final Object key, final Object value) {
        if (key instanceof Player) {
            final Object playerKey = this.keyMethod.apply((Object)key);
            if (playerKey != null && this.valueLookup.remove(playerKey, value)) {
                this.keyLookup.remove(playerKey);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public TValue get(final Object key) {
        if (key instanceof Player) {
            final Object playerKey = this.keyMethod.apply((Object)key);
            return (playerKey != null) ? this.valueLookup.get(playerKey) : null;
        }
        return null;
    }
    
    @Override
    public boolean containsKey(final Object key) {
        if (key instanceof Player) {
            final Object playerKey = this.keyMethod.apply((Object)key);
            return playerKey != null && this.valueLookup.containsKey(playerKey);
        }
        return false;
    }
    
    @Override
    public Set<Map.Entry<Player, TValue>> entrySet() {
        return new AbstractSet<Map.Entry<Player, TValue>>() {
            @Override
            public Iterator<Map.Entry<Player, TValue>> iterator() {
                return (Iterator<Map.Entry<Player, TValue>>)ConcurrentPlayerMap.this.entryIterator();
            }
            
            @Override
            public int size() {
                return ConcurrentPlayerMap.this.valueLookup.size();
            }
            
            @Override
            public void clear() {
                ConcurrentPlayerMap.this.valueLookup.clear();
                ConcurrentPlayerMap.this.keyLookup.clear();
            }
        };
    }
    
    private Iterator<Map.Entry<Player, TValue>> entryIterator() {
        final Iterator<Map.Entry<Object, TValue>> source = (Iterator<Map.Entry<Object, TValue>>)this.valueLookup.entrySet().iterator();
        final AbstractIterator<Map.Entry<Player, TValue>> filtered = new AbstractIterator<Map.Entry<Player, TValue>>() {
            protected Map.Entry<Player, TValue> computeNext() {
                while (source.hasNext()) {
                    final Map.Entry<Object, TValue> entry = source.next();
                    final Player player = ConcurrentPlayerMap.this.lookupPlayer(entry.getKey());
                    if (player != null) {
                        return new SimpleEntry<Player, TValue>(player, entry.getValue());
                    }
                    source.remove();
                    ConcurrentPlayerMap.this.keyLookup.remove(entry.getKey());
                }
                return (Map.Entry<Player, TValue>)this.endOfData();
            }
        };
        return new Iterator<Map.Entry<Player, TValue>>() {
            @Override
            public boolean hasNext() {
                return filtered.hasNext();
            }
            
            @Override
            public Map.Entry<Player, TValue> next() {
                return (Map.Entry<Player, TValue>)filtered.next();
            }
            
            @Override
            public void remove() {
                source.remove();
            }
        };
    }
    
    public enum PlayerKey implements Function<Player, Object>
    {
        ADDRESS {
            public Object apply(final Player player) {
                return (player == null) ? null : player.getAddress();
            }
        }, 
        NAME {
            public Object apply(final Player player) {
                return (player == null) ? null : player.getName();
            }
        };
    }
}
