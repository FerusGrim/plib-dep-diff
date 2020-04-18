package com.comphenix.executors;

import java.util.concurrent.Callable;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

public interface BukkitScheduledExecutorService extends ListeningScheduledExecutorService
{
    ListenableScheduledFuture<?> schedule(final Runnable p0, final long p1, final TimeUnit p2);
    
     <V> ListenableScheduledFuture<V> schedule(final Callable<V> p0, final long p1, final TimeUnit p2);
    
    ListenableScheduledFuture<?> scheduleAtFixedRate(final Runnable p0, final long p1, final long p2, final TimeUnit p3);
    
    @Deprecated
    ListenableScheduledFuture<?> scheduleWithFixedDelay(final Runnable p0, final long p1, final long p2, final TimeUnit p3);
}
