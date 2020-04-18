package com.comphenix.executors;

import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.base.Preconditions;
import java.util.concurrent.Callable;

class CallableTask<T> extends AbstractListeningService.RunnableAbstractFuture<T>
{
    protected final Callable<T> compute;
    
    public CallableTask(final Callable<T> compute) {
        Preconditions.checkNotNull((Object)compute, (Object)"compute cannot be NULL");
        this.compute = compute;
    }
    
    public ListenableScheduledFuture<T> getScheduledFuture(final long startTime, final long nextDelay) {
        return (ListenableScheduledFuture<T>)new ListenableScheduledFuture<T>() {
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return CallableTask.this.cancel(mayInterruptIfRunning);
            }
            
            public T get() throws InterruptedException, ExecutionException {
                return CallableTask.this.get();
            }
            
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return CallableTask.this.get(timeout, unit);
            }
            
            public boolean isCancelled() {
                return CallableTask.this.isCancelled();
            }
            
            public boolean isDone() {
                return CallableTask.this.isDone();
            }
            
            public void addListener(final Runnable listener, final Executor executor) {
                CallableTask.this.addListener(listener, executor);
            }
            
            public int compareTo(final Delayed o) {
                return Long.valueOf(this.getDelay(TimeUnit.NANOSECONDS)).compareTo(o.getDelay(TimeUnit.NANOSECONDS));
            }
            
            public long getDelay(final TimeUnit unit) {
                final long current = System.nanoTime();
                if (current < startTime || !this.isPeriodic()) {
                    return unit.convert(startTime - current, TimeUnit.NANOSECONDS);
                }
                return unit.convert((current - startTime) % nextDelay, TimeUnit.NANOSECONDS);
            }
            
            public boolean isPeriodic() {
                return nextDelay > 0L;
            }
        };
    }
    
    protected void compute() {
        try {
            if (!this.isCancelled()) {
                this.set((Object)this.compute.call());
            }
        }
        catch (Throwable e) {
            this.setException(e);
        }
    }
    
    public void run() {
        this.compute();
    }
}
