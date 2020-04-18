package com.comphenix.executors;

import com.google.common.collect.MapMaker;
import java.util.Iterator;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.bukkit.event.EventException;
import org.bukkit.event.Event;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginDisableEvent;
import java.util.Map;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.Set;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.event.Listener;

class PluginDisabledListener implements Listener
{
    private static ConcurrentMap<Plugin, PluginDisabledListener> listeners;
    private Set<Future<?>> futures;
    private Set<ExecutorService> services;
    private Object setLock;
    private final Plugin plugin;
    private boolean disabled;
    
    private PluginDisabledListener(final Plugin plugin) {
        this.futures = Collections.newSetFromMap(new WeakHashMap<Future<?>, Boolean>());
        this.services = Collections.newSetFromMap(new WeakHashMap<ExecutorService, Boolean>());
        this.setLock = new Object();
        this.plugin = plugin;
    }
    
    public static PluginDisabledListener getListener(final Plugin plugin) {
        PluginDisabledListener result = PluginDisabledListener.listeners.get(plugin);
        if (result == null) {
            final PluginDisabledListener created = new PluginDisabledListener(plugin);
            result = PluginDisabledListener.listeners.putIfAbsent(plugin, created);
            if (result == null) {
                BukkitFutures.registerEventExecutor(plugin, (Class<? extends Event>)PluginDisableEvent.class, EventPriority.NORMAL, (EventExecutor)new EventExecutor() {
                    public void execute(final Listener listener, final Event event) throws EventException {
                        if (event instanceof PluginDisableEvent) {
                            created.onPluginDisabled((PluginDisableEvent)event);
                        }
                    }
                });
                result = created;
            }
        }
        return result;
    }
    
    public void addFuture(final ListenableFuture<?> future) {
        synchronized (this.setLock) {
            if (this.disabled) {
                this.processFuture((Future<?>)future);
            }
            else {
                this.futures.add((Future<?>)future);
            }
        }
        Futures.addCallback((ListenableFuture)future, (FutureCallback)new FutureCallback<Object>() {
            public void onSuccess(final Object value) {
                synchronized (PluginDisabledListener.this.setLock) {
                    PluginDisabledListener.this.futures.remove(future);
                }
            }
            
            public void onFailure(final Throwable ex) {
                synchronized (PluginDisabledListener.this.setLock) {
                    PluginDisabledListener.this.futures.remove(future);
                }
            }
        });
    }
    
    public void addService(final ExecutorService service) {
        synchronized (this.setLock) {
            if (this.disabled) {
                this.processService(service);
            }
            else {
                this.services.add(service);
            }
        }
    }
    
    public void onPluginDisabled(final PluginDisableEvent e) {
        if (e.getPlugin().equals(this.plugin)) {
            synchronized (this.setLock) {
                this.disabled = true;
                for (final Future<?> future : this.futures) {
                    this.processFuture(future);
                }
                for (final ExecutorService service : this.services) {
                    this.processService(service);
                }
            }
        }
    }
    
    private void processFuture(final Future<?> future) {
        if (!future.isDone()) {
            future.cancel(true);
        }
    }
    
    private void processService(final ExecutorService service) {
        service.shutdownNow();
    }
    
    static {
        PluginDisabledListener.listeners = (ConcurrentMap<Plugin, PluginDisabledListener>)new MapMaker().weakKeys().makeMap();
    }
}
