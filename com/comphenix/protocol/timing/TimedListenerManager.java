package com.comphenix.protocol.timing;

import com.comphenix.protocol.events.PacketListener;
import org.bukkit.plugin.Plugin;
import java.util.Set;
import java.util.Calendar;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimedListenerManager
{
    private static final TimedListenerManager INSTANCE;
    private static final AtomicBoolean timing;
    private volatile Date started;
    private volatile Date stopped;
    private ConcurrentMap<String, ImmutableMap<ListenerType, TimedTracker>> map;
    
    public TimedListenerManager() {
        this.map = (ConcurrentMap<String, ImmutableMap<ListenerType, TimedTracker>>)Maps.newConcurrentMap();
    }
    
    public static TimedListenerManager getInstance() {
        return TimedListenerManager.INSTANCE;
    }
    
    public boolean startTiming() {
        if (this.setTiming(true)) {
            this.started = Calendar.getInstance().getTime();
            return true;
        }
        return false;
    }
    
    public boolean stopTiming() {
        if (this.setTiming(false)) {
            this.stopped = Calendar.getInstance().getTime();
            return true;
        }
        return false;
    }
    
    public Date getStarted() {
        return this.started;
    }
    
    public Date getStopped() {
        return this.stopped;
    }
    
    private boolean setTiming(final boolean value) {
        return TimedListenerManager.timing.compareAndSet(!value, value);
    }
    
    public boolean isTiming() {
        return TimedListenerManager.timing.get();
    }
    
    public void clear() {
        this.map.clear();
    }
    
    public Set<String> getTrackedPlugins() {
        return this.map.keySet();
    }
    
    public TimedTracker getTracker(final Plugin plugin, final ListenerType type) {
        return this.getTracker(plugin.getName(), type);
    }
    
    public TimedTracker getTracker(final PacketListener listener, final ListenerType type) {
        return this.getTracker(listener.getPlugin().getName(), type);
    }
    
    public TimedTracker getTracker(final String pluginName, final ListenerType type) {
        return (TimedTracker)this.getTrackers(pluginName).get((Object)type);
    }
    
    private ImmutableMap<ListenerType, TimedTracker> getTrackers(final String pluginName) {
        ImmutableMap<ListenerType, TimedTracker> trackers = (ImmutableMap<ListenerType, TimedTracker>)this.map.get(pluginName);
        if (trackers == null) {
            final ImmutableMap<ListenerType, TimedTracker> created = this.newTrackerMap();
            trackers = this.map.putIfAbsent(pluginName, created);
            if (trackers == null) {
                trackers = created;
            }
        }
        return trackers;
    }
    
    private ImmutableMap<ListenerType, TimedTracker> newTrackerMap() {
        final ImmutableMap.Builder<ListenerType, TimedTracker> builder = (ImmutableMap.Builder<ListenerType, TimedTracker>)ImmutableMap.builder();
        for (final ListenerType type : ListenerType.values()) {
            builder.put((Object)type, (Object)new TimedTracker());
        }
        return (ImmutableMap<ListenerType, TimedTracker>)builder.build();
    }
    
    static {
        INSTANCE = new TimedListenerManager();
        timing = new AtomicBoolean();
    }
    
    public enum ListenerType
    {
        ASYNC_SERVER_SIDE, 
        ASYNC_CLIENT_SIDE, 
        SYNC_SERVER_SIDE, 
        SYNC_CLIENT_SIDE;
    }
}
