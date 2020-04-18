package com.comphenix.protocol.concurrency;

import java.util.Set;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import com.comphenix.protocol.events.ListeningWhitelist;
import java.util.concurrent.ConcurrentHashMap;
import com.comphenix.protocol.injector.PrioritizedListener;
import com.comphenix.protocol.PacketType;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractConcurrentListenerMultimap<TListener>
{
    private ConcurrentMap<PacketType, SortedCopyOnWriteArray<PrioritizedListener<TListener>>> mapListeners;
    
    public AbstractConcurrentListenerMultimap() {
        this.mapListeners = new ConcurrentHashMap<PacketType, SortedCopyOnWriteArray<PrioritizedListener<TListener>>>();
    }
    
    public void addListener(final TListener listener, final ListeningWhitelist whitelist) {
        final PrioritizedListener<TListener> prioritized = new PrioritizedListener<TListener>(listener, whitelist.getPriority());
        for (final PacketType type : whitelist.getTypes()) {
            this.addListener(type, prioritized);
        }
    }
    
    private void addListener(final PacketType type, final PrioritizedListener<TListener> listener) {
        SortedCopyOnWriteArray<PrioritizedListener<TListener>> list = this.mapListeners.get(type);
        if (list == null) {
            final SortedCopyOnWriteArray<PrioritizedListener<TListener>> value = new SortedCopyOnWriteArray<PrioritizedListener<TListener>>();
            list = this.mapListeners.putIfAbsent(type, value);
            if (list == null) {
                list = value;
            }
        }
        list.add(listener);
    }
    
    public List<PacketType> removeListener(final TListener listener, final ListeningWhitelist whitelist) {
        final List<PacketType> removedPackets = new ArrayList<PacketType>();
        for (final PacketType type : whitelist.getTypes()) {
            final SortedCopyOnWriteArray<PrioritizedListener<TListener>> list = this.mapListeners.get(type);
            if (list != null && list.size() > 0) {
                list.remove(new PrioritizedListener(listener, whitelist.getPriority()));
                if (list.size() != 0) {
                    continue;
                }
                this.mapListeners.remove(type);
                removedPackets.add(type);
            }
        }
        return removedPackets;
    }
    
    public Collection<PrioritizedListener<TListener>> getListener(final PacketType type) {
        return this.mapListeners.get(type);
    }
    
    public Iterable<PrioritizedListener<TListener>> values() {
        return (Iterable<PrioritizedListener<TListener>>)Iterables.concat((Iterable)this.mapListeners.values());
    }
    
    public Set<PacketType> keySet() {
        return this.mapListeners.keySet();
    }
    
    protected void clearListeners() {
        this.mapListeners.clear();
    }
}
