package com.comphenix.executors;

import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.HashSet;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.plugin.Plugin;
import java.util.Set;

class PendingTasks
{
    private Set<CancelableFuture> pending;
    private final Object pendingLock;
    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    private BukkitTask cancellationTask;
    
    public PendingTasks(final Plugin plugin, final BukkitScheduler scheduler) {
        this.pending = new HashSet<CancelableFuture>();
        this.pendingLock = new Object();
        this.plugin = plugin;
        this.scheduler = scheduler;
    }
    
    public void add(final BukkitTask task, final Future<?> future) {
        this.add(new CancelableFuture() {
            @Override
            public boolean isTaskCancelled() {
                if (future.isDone()) {
                    return future.isCancelled();
                }
                return !PendingTasks.this.scheduler.isCurrentlyRunning(task.getTaskId()) && !PendingTasks.this.scheduler.isQueued(task.getTaskId());
            }
            
            @Override
            public void cancel() {
                task.cancel();
                future.cancel(true);
            }
        });
    }
    
    private CancelableFuture add(final CancelableFuture task) {
        synchronized (this.pendingLock) {
            this.pending.add(task);
            this.pendingLock.notifyAll();
            this.beginCancellationTask();
            return task;
        }
    }
    
    private void beginCancellationTask() {
        if (this.cancellationTask == null) {
            this.cancellationTask = this.scheduler.runTaskTimer(this.plugin, (Runnable)new Runnable() {
                @Override
                public void run() {
                    synchronized (PendingTasks.this.pendingLock) {
                        boolean changed = false;
                        final Iterator<CancelableFuture> it = PendingTasks.this.pending.iterator();
                        while (it.hasNext()) {
                            final CancelableFuture future = it.next();
                            if (future.isTaskCancelled()) {
                                future.cancel();
                                it.remove();
                                changed = true;
                            }
                        }
                        if (changed) {
                            PendingTasks.this.pendingLock.notifyAll();
                        }
                    }
                    if (PendingTasks.this.isTerminated()) {
                        PendingTasks.this.cancellationTask.cancel();
                        PendingTasks.this.cancellationTask = null;
                    }
                }
            }, 1L, 1L);
        }
    }
    
    public void cancel() {
        for (final CancelableFuture task : this.pending) {
            task.cancel();
        }
    }
    
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        final long expire = System.nanoTime() + unit.toNanos(timeout);
        synchronized (this.pendingLock) {
            while (!this.isTerminated()) {
                if (expire < System.nanoTime()) {
                    return false;
                }
                unit.timedWait(this.pendingLock, timeout);
            }
        }
        return false;
    }
    
    public boolean isTerminated() {
        return this.pending.isEmpty();
    }
    
    private interface CancelableFuture
    {
        void cancel();
        
        boolean isTaskCancelled();
    }
}
