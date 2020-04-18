package com.comphenix.protocol.injector;

import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.plugin.Plugin;

public class DelayedSingleTask
{
    protected int taskID;
    protected Plugin plugin;
    protected BukkitScheduler scheduler;
    protected boolean closed;
    
    public DelayedSingleTask(final Plugin plugin) {
        this.taskID = -1;
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }
    
    public DelayedSingleTask(final Plugin plugin, final BukkitScheduler scheduler) {
        this.taskID = -1;
        this.plugin = plugin;
        this.scheduler = scheduler;
    }
    
    public boolean schedule(final long ticksDelay, final Runnable task) {
        if (ticksDelay < 0L) {
            throw new IllegalArgumentException("Tick delay cannot be negative.");
        }
        if (task == null) {
            throw new IllegalArgumentException("task cannot be NULL");
        }
        if (this.closed) {
            return false;
        }
        if (ticksDelay == 0L) {
            task.run();
            return true;
        }
        final Runnable dispatch = task;
        this.cancel();
        this.taskID = this.scheduler.scheduleSyncDelayedTask(this.plugin, (Runnable)new Runnable() {
            @Override
            public void run() {
                dispatch.run();
                DelayedSingleTask.this.taskID = -1;
            }
        }, ticksDelay);
        return this.isRunning();
    }
    
    public boolean isRunning() {
        return this.taskID >= 0;
    }
    
    public boolean cancel() {
        if (this.isRunning()) {
            this.scheduler.cancelTask(this.taskID);
            this.taskID = -1;
            return true;
        }
        return false;
    }
    
    public int getTaskID() {
        return this.taskID;
    }
    
    public Plugin getPlugin() {
        return this.plugin;
    }
    
    public synchronized void close() {
        if (!this.closed) {
            this.cancel();
            this.plugin = null;
            this.scheduler = null;
            this.closed = true;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        this.close();
    }
}
