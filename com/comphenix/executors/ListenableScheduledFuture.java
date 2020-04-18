package com.comphenix.executors;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.RunnableScheduledFuture;

public interface ListenableScheduledFuture<V> extends RunnableScheduledFuture<V>, ListenableFuture<V>
{
}
