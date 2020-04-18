package com.comphenix.protocol.injector.netty;

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.EventExecutor;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.Collection;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import io.netty.util.concurrent.Future;
import java.util.concurrent.Callable;
import io.netty.channel.EventLoop;

abstract class EventLoopProxy implements EventLoop
{
    private static final Runnable EMPTY_RUNNABLE;
    private static final Callable<?> EMPTY_CALLABLE;
    
    protected abstract EventLoop getDelegate();
    
    public static <T> Callable<T> getEmptyCallable() {
        return (Callable<T>)EventLoopProxy.EMPTY_CALLABLE;
    }
    
    public static Runnable getEmptyRunnable() {
        return EventLoopProxy.EMPTY_RUNNABLE;
    }
    
    protected abstract Runnable schedulingRunnable(final Runnable p0);
    
    protected abstract <T> Callable<T> schedulingCallable(final Callable<T> p0);
    
    public void execute(final Runnable command) {
        this.getDelegate().execute(this.schedulingRunnable(command));
    }
    
    public <T> Future<T> submit(final Callable<T> action) {
        return (Future<T>)this.getDelegate().submit((Callable)this.schedulingCallable(action));
    }
    
    public <T> Future<T> submit(final Runnable action, final T arg1) {
        return (Future<T>)this.getDelegate().submit(this.schedulingRunnable(action), (Object)arg1);
    }
    
    public Future<?> submit(final Runnable action) {
        return (Future<?>)this.getDelegate().submit(this.schedulingRunnable(action));
    }
    
    public <V> ScheduledFuture<V> schedule(final Callable<V> action, final long arg1, final TimeUnit arg2) {
        return (ScheduledFuture<V>)this.getDelegate().schedule((Callable)this.schedulingCallable(action), arg1, arg2);
    }
    
    public ScheduledFuture<?> schedule(final Runnable action, final long arg1, final TimeUnit arg2) {
        return (ScheduledFuture<?>)this.getDelegate().schedule(this.schedulingRunnable(action), arg1, arg2);
    }
    
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable action, final long arg1, final long arg2, final TimeUnit arg3) {
        return (ScheduledFuture<?>)this.getDelegate().scheduleAtFixedRate(this.schedulingRunnable(action), arg1, arg2, arg3);
    }
    
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable action, final long arg1, final long arg2, final TimeUnit arg3) {
        return (ScheduledFuture<?>)this.getDelegate().scheduleWithFixedDelay(this.schedulingRunnable(action), arg1, arg2, arg3);
    }
    
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return this.getDelegate().awaitTermination(timeout, unit);
    }
    
    public boolean inEventLoop() {
        return this.getDelegate().inEventLoop();
    }
    
    public boolean inEventLoop(final Thread arg0) {
        return this.getDelegate().inEventLoop(arg0);
    }
    
    public boolean isShutdown() {
        return this.getDelegate().isShutdown();
    }
    
    public boolean isTerminated() {
        return this.getDelegate().isTerminated();
    }
    
    public <T> List<java.util.concurrent.Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return (List<java.util.concurrent.Future<T>>)this.getDelegate().invokeAll((Collection)tasks);
    }
    
    public <T> List<java.util.concurrent.Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
        return (List<java.util.concurrent.Future<T>>)this.getDelegate().invokeAll((Collection)tasks, timeout, unit);
    }
    
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return (T)this.getDelegate().invokeAny((Collection)tasks);
    }
    
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return (T)this.getDelegate().invokeAny((Collection)tasks, timeout, unit);
    }
    
    public boolean isShuttingDown() {
        return this.getDelegate().isShuttingDown();
    }
    
    public Iterator<EventExecutor> iterator() {
        return (Iterator<EventExecutor>)this.getDelegate().iterator();
    }
    
    public <V> Future<V> newFailedFuture(final Throwable arg0) {
        return (Future<V>)this.getDelegate().newFailedFuture(arg0);
    }
    
    public EventLoop next() {
        return ((EventLoopGroup)this.getDelegate()).next();
    }
    
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return (ProgressivePromise<V>)this.getDelegate().newProgressivePromise();
    }
    
    public <V> Promise<V> newPromise() {
        return (Promise<V>)this.getDelegate().newPromise();
    }
    
    public <V> Future<V> newSucceededFuture(final V arg0) {
        return (Future<V>)this.getDelegate().newSucceededFuture((Object)arg0);
    }
    
    public EventLoopGroup parent() {
        return this.getDelegate().parent();
    }
    
    public ChannelFuture register(final Channel arg0, final ChannelPromise arg1) {
        return this.getDelegate().register(arg0, arg1);
    }
    
    public ChannelFuture register(final Channel arg0) {
        return this.getDelegate().register(arg0);
    }
    
    public Future<?> shutdownGracefully() {
        return (Future<?>)this.getDelegate().shutdownGracefully();
    }
    
    public Future<?> shutdownGracefully(final long arg0, final long arg1, final TimeUnit arg2) {
        return (Future<?>)this.getDelegate().shutdownGracefully(arg0, arg1, arg2);
    }
    
    public Future<?> terminationFuture() {
        return (Future<?>)this.getDelegate().terminationFuture();
    }
    
    @Deprecated
    public void shutdown() {
        this.getDelegate().shutdown();
    }
    
    @Deprecated
    public List<Runnable> shutdownNow() {
        return (List<Runnable>)this.getDelegate().shutdownNow();
    }
    
    static {
        EMPTY_RUNNABLE = new Runnable() {
            @Override
            public void run() {
            }
        };
        EMPTY_CALLABLE = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };
    }
}
