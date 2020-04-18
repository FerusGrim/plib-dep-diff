package com.comphenix.executors;

import java.util.concurrent.ScheduledFuture;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.Collections;
import java.util.List;
import org.bukkit.scheduler.BukkitTask;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

abstract class AbstractBukkitService extends AbstractListeningService implements BukkitScheduledExecutorService
{
    private static final long MILLISECONDS_PER_TICK = 50L;
    private static final long NANOSECONDS_PER_TICK = 50000000L;
    private volatile boolean shutdown;
    private PendingTasks tasks;
    
    public AbstractBukkitService(final PendingTasks tasks) {
        this.tasks = tasks;
    }
    
    @Override
    protected <T> RunnableAbstractFuture<T> newTaskFor(final Runnable runnable, final T value) {
        return this.newTaskFor((Callable<T>)Executors.callable(runnable, (T)value));
    }
    
    @Override
    protected <T> RunnableAbstractFuture<T> newTaskFor(final Callable<T> callable) {
        this.validateState();
        return new CallableTask<T>(callable);
    }
    
    public void execute(final Runnable command) {
        this.validateState();
        if (command instanceof RunnableFuture) {
            this.tasks.add(this.getTask(command), (Future<?>)command);
        }
        else {
            this.submit(command);
        }
    }
    
    protected abstract BukkitTask getTask(final Runnable p0);
    
    protected abstract BukkitTask getLaterTask(final Runnable p0, final long p1);
    
    protected abstract BukkitTask getTimerTask(final long p0, final long p1, final Runnable p2);
    
    public List<Runnable> shutdownNow() {
        this.shutdown();
        this.tasks.cancel();
        return Collections.emptyList();
    }
    
    public void shutdown() {
        this.shutdown = true;
    }
    
    private void validateState() {
        if (this.shutdown) {
            throw new RejectedExecutionException("Executor service has shut down. Cannot start new tasks.");
        }
    }
    
    private long toTicks(final long delay, final TimeUnit unit) {
        return Math.round(unit.toMillis(delay) / 50.0);
    }
    
    @Override
    public ListenableScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return this.schedule((Callable<?>)Executors.callable(command), delay, unit);
    }
    
    @Override
    public <V> ListenableScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        final long ticks = this.toTicks(delay, unit);
        final CallableTask<V> task = new CallableTask<V>(callable);
        final BukkitTask bukkitTask = this.getLaterTask(task, ticks);
        this.tasks.add(bukkitTask, task);
        return task.getScheduledFuture(System.nanoTime() + delay * 50000000L, 0L);
    }
    
    @Override
    public ListenableScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        final long ticksInitial = this.toTicks(initialDelay, unit);
        final long ticksDelay = this.toTicks(period, unit);
        final CallableTask<?> task = new CallableTask<Object>(Executors.callable(command)) {
            @Override
            protected void compute() {
                try {
                    this.compute.call();
                }
                catch (Exception e) {
                    throw Throwables.propagate((Throwable)e);
                }
            }
        };
        final BukkitTask bukkitTask = this.getTimerTask(ticksInitial, ticksDelay, task);
        this.tasks.add(bukkitTask, task);
        return task.getScheduledFuture(System.nanoTime() + ticksInitial * 50000000L, ticksDelay * 50000000L);
    }
    
    @Deprecated
    @Override
    public ListenableScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        return this.scheduleAtFixedRate(command, initialDelay, delay, unit);
    }
    
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return this.tasks.awaitTermination(timeout, unit);
    }
    
    public boolean isShutdown() {
        return this.shutdown;
    }
    
    public boolean isTerminated() {
        return this.tasks.isTerminated();
    }
}
