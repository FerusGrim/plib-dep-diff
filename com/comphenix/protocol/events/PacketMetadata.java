package com.comphenix.protocol.events;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import java.util.Iterator;
import org.apache.commons.lang.Validate;
import java.util.Optional;
import java.util.List;
import com.google.common.cache.Cache;

class PacketMetadata
{
    private static Cache<Object, List<MetaObject>> META_CACHE;
    
    public static <T> Optional<T> get(final Object packet, final String key) {
        Validate.notNull((Object)key, "Null keys are not permitted!");
        if (PacketMetadata.META_CACHE == null) {
            return Optional.empty();
        }
        final List<MetaObject> meta = (List<MetaObject>)PacketMetadata.META_CACHE.getIfPresent(packet);
        if (meta == null) {
            return Optional.empty();
        }
        for (final MetaObject object : meta) {
            if (object.key.equals(key)) {
                return Optional.of(object.value);
            }
        }
        return Optional.empty();
    }
    
    private static void createCache() {
        PacketMetadata.META_CACHE = (Cache<Object, List<MetaObject>>)CacheBuilder.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES).build();
    }
    
    public static <T> void set(final Object packet, final String key, final T value) {
        Validate.notNull((Object)key, "Null keys are not permitted!");
        if (PacketMetadata.META_CACHE == null) {
            createCache();
        }
        List<MetaObject> packetMeta;
        try {
            packetMeta = (List<MetaObject>)PacketMetadata.META_CACHE.get(packet, (Callable)ArrayList::new);
        }
        catch (ExecutionException ex) {
            packetMeta = new ArrayList<MetaObject>();
        }
        packetMeta.removeIf(meta -> meta.key.equals(key));
        packetMeta.add(new MetaObject(key, (Object)value));
        PacketMetadata.META_CACHE.put(packet, (Object)packetMeta);
    }
    
    public static <T> Optional<T> remove(final Object packet, final String key) {
        Validate.notNull((Object)key, "Null keys are not permitted!");
        if (PacketMetadata.META_CACHE == null) {
            return Optional.empty();
        }
        final List<MetaObject> packetMeta = (List<MetaObject>)PacketMetadata.META_CACHE.getIfPresent(packet);
        if (packetMeta == null) {
            return Optional.empty();
        }
        Optional<T> value = Optional.empty();
        final Iterator<MetaObject> iter = (Iterator<MetaObject>)packetMeta.iterator();
        while (iter.hasNext()) {
            final MetaObject meta = iter.next();
            if (meta.key.equals(key)) {
                value = Optional.of(meta.value);
                iter.remove();
            }
        }
        return value;
    }
    
    private static class MetaObject<T>
    {
        private final String key;
        private final T value;
        
        private MetaObject(final String key, final T value) {
            this.key = key;
            this.value = value;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(this.key, this.value);
        }
        
        @Override
        public boolean equals(final Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof MetaObject) {
                final MetaObject that = (MetaObject)o;
                return that.key.equals(this.key) && that.value.equals(this.value);
            }
            return false;
        }
        
        @Override
        public String toString() {
            return "MetaObject[" + this.key + "=" + this.value + "]";
        }
    }
}
