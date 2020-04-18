package com.comphenix.protocol.injector.packet;

import com.google.common.collect.Maps;
import com.google.common.collect.ForwardingMap;
import java.util.Iterator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.Multimap;
import com.google.common.base.Predicate;
import java.util.Map;

public class InverseMaps
{
    private InverseMaps() {
    }
    
    public static <K, V> Multimap<K, V> inverseMultimap(final Map<V, K> map, final Predicate<Map.Entry<V, K>> filter) {
        final MapContainer container = new MapContainer(map);
        return (Multimap<K, V>)new ForwardingMultimap<K, V>() {
            private Multimap<K, V> inverseMultimap;
            
            protected Multimap<K, V> delegate() {
                if (container.hasChanged()) {
                    this.inverseMultimap = (Multimap<K, V>)HashMultimap.create();
                    for (final Map.Entry<V, K> entry : map.entrySet()) {
                        if (filter.apply((Object)entry)) {
                            this.inverseMultimap.put((Object)entry.getValue(), (Object)entry.getKey());
                        }
                    }
                    container.setChanged(false);
                }
                return this.inverseMultimap;
            }
        };
    }
    
    public static <K, V> Map<K, V> inverseMap(final Map<V, K> map, final Predicate<Map.Entry<V, K>> filter) {
        final MapContainer container = new MapContainer(map);
        return (Map<K, V>)new ForwardingMap<K, V>() {
            private Map<K, V> inverseMap;
            
            protected Map<K, V> delegate() {
                if (container.hasChanged()) {
                    this.inverseMap = (Map<K, V>)Maps.newHashMap();
                    for (final Map.Entry<V, K> entry : map.entrySet()) {
                        if (filter.apply((Object)entry)) {
                            this.inverseMap.put(entry.getValue(), entry.getKey());
                        }
                    }
                    container.setChanged(false);
                }
                return this.inverseMap;
            }
        };
    }
}
