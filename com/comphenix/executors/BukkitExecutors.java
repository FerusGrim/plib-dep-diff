package com.comphenix.executors;

import java.util.concurrent.ExecutorService;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitScheduler;
import com.google.common.base.Preconditions;
import org.bukkit.plugin.Plugin;

public class BukkitExecutors
{
    private BukkitExecutors() {
    }
    
    public static BukkitScheduledExecutorService newSynchronous(final Plugin plugin) {
        final BukkitScheduler scheduler = getScheduler(plugin);
        Preconditions.checkNotNull((Object)plugin, (Object)"plugin cannot be NULL");
        final BukkitScheduledExecutorService service = new AbstractBukkitService(new PendingTasks(plugin, scheduler)) {
            @Override
            protected BukkitTask getTask(final Runnable command) {
                return scheduler.runTask(plugin, command);
            }
            
            @Override
            protected BukkitTask getLaterTask(final Runnable task, final long ticks) {
                return scheduler.runTaskLater(plugin, task, ticks);
            }
            
            @Override
            protected BukkitTask getTimerTask(final long ticksInitial, final long ticksDelay, final Runnable task) {
                return scheduler.runTaskTimer(plugin, task, ticksInitial, ticksDelay);
            }
        };
        PluginDisabledListener.getListener(plugin).addService((ExecutorService)service);
        return service;
    }
    
    public static BukkitScheduledExecutorService newAsynchronous(final Plugin plugin) {
        final BukkitScheduler scheduler = getScheduler(plugin);
        Preconditions.checkNotNull((Object)plugin, (Object)"plugin cannot be NULL");
        final BukkitScheduledExecutorService service = new AbstractBukkitService(new PendingTasks(plugin, scheduler)) {
            @Override
            protected BukkitTask getTask(final Runnable command) {
                return scheduler.runTaskAsynchronously(plugin, command);
            }
            
            @Override
            protected BukkitTask getLaterTask(final Runnable task, final long ticks) {
                return scheduler.runTaskLaterAsynchronously(plugin, task, ticks);
            }
            
            @Override
            protected BukkitTask getTimerTask(final long ticksInitial, final long ticksDelay, final Runnable task) {
                return scheduler.runTaskTimerAsynchronously(plugin, task, ticksInitial, ticksDelay);
            }
        };
        PluginDisabledListener.getListener(plugin).addService((ExecutorService)service);
        return service;
    }
    
    private static BukkitScheduler getScheduler(final Plugin plugin) {
        final BukkitScheduler scheduler = plugin.getServer().getScheduler();
        if (scheduler != null) {
            return scheduler;
        }
        throw new IllegalStateException("Unable to retrieve scheduler.");
    }
}
