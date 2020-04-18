package com.comphenix.protocol.injector.netty;

import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelProgressivePromise;
import com.comphenix.protocol.reflect.accessors.Accessors;
import io.netty.channel.EventLoop;
import io.netty.channel.ChannelPromise;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelFuture;
import java.net.SocketAddress;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.util.concurrent.Callable;
import java.util.Map;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import io.netty.channel.Channel;

public abstract class ChannelProxy implements Channel
{
    private static final FieldAccessor MARK_NO_MESSAGE;
    private static Map<Class<?>, FieldAccessor> MESSAGE_LOOKUP;
    protected Channel delegate;
    protected Class<?> messageClass;
    private transient EventLoopProxy loopProxy;
    
    public ChannelProxy(final Channel delegate, final Class<?> messageClass) {
        this.delegate = delegate;
        this.messageClass = messageClass;
    }
    
    protected abstract <T> Callable<T> onMessageScheduled(final Callable<T> p0, final FieldAccessor p1);
    
    protected abstract Runnable onMessageScheduled(final Runnable p0, final FieldAccessor p1);
    
    public <T> Attribute<T> attr(final AttributeKey<T> paramAttributeKey) {
        return (Attribute<T>)this.delegate.attr((AttributeKey)paramAttributeKey);
    }
    
    public ChannelFuture bind(final SocketAddress paramSocketAddress) {
        return this.delegate.bind(paramSocketAddress);
    }
    
    public ChannelPipeline pipeline() {
        return this.delegate.pipeline();
    }
    
    public ChannelFuture connect(final SocketAddress paramSocketAddress) {
        return this.delegate.connect(paramSocketAddress);
    }
    
    public ByteBufAllocator alloc() {
        return this.delegate.alloc();
    }
    
    public ChannelPromise newPromise() {
        return this.delegate.newPromise();
    }
    
    public EventLoop eventLoop() {
        if (this.loopProxy == null) {
            this.loopProxy = new EventLoopProxy() {
                @Override
                protected EventLoop getDelegate() {
                    return ChannelProxy.this.delegate.eventLoop();
                }
                
                @Override
                protected Runnable schedulingRunnable(final Runnable runnable) {
                    final FieldAccessor accessor = ChannelProxy.this.getMessageAccessor(runnable);
                    if (accessor != null) {
                        final Runnable result = ChannelProxy.this.onMessageScheduled(runnable, accessor);
                        return (result != null) ? result : EventLoopProxy.getEmptyRunnable();
                    }
                    return runnable;
                }
                
                @Override
                protected <T> Callable<T> schedulingCallable(final Callable<T> callable) {
                    final FieldAccessor accessor = ChannelProxy.this.getMessageAccessor(callable);
                    if (accessor != null) {
                        final Callable<T> result = ChannelProxy.this.onMessageScheduled(callable, accessor);
                        return (result != null) ? result : EventLoopProxy.getEmptyCallable();
                    }
                    return callable;
                }
            };
        }
        return (EventLoop)this.loopProxy;
    }
    
    private FieldAccessor getMessageAccessor(final Object value) {
        final Class<?> clazz = value.getClass();
        FieldAccessor accessor = ChannelProxy.MESSAGE_LOOKUP.get(clazz);
        if (accessor == null) {
            try {
                accessor = Accessors.getFieldAccessor(clazz, this.messageClass, true);
            }
            catch (IllegalArgumentException e) {
                accessor = ChannelProxy.MARK_NO_MESSAGE;
            }
            ChannelProxy.MESSAGE_LOOKUP.put(clazz, accessor);
        }
        return (accessor != ChannelProxy.MARK_NO_MESSAGE) ? accessor : null;
    }
    
    public ChannelFuture connect(final SocketAddress paramSocketAddress1, final SocketAddress paramSocketAddress2) {
        return this.delegate.connect(paramSocketAddress1, paramSocketAddress2);
    }
    
    public ChannelProgressivePromise newProgressivePromise() {
        return this.delegate.newProgressivePromise();
    }
    
    public Channel parent() {
        return this.delegate.parent();
    }
    
    public ChannelConfig config() {
        return this.delegate.config();
    }
    
    public ChannelFuture newSucceededFuture() {
        return this.delegate.newSucceededFuture();
    }
    
    public boolean isOpen() {
        return this.delegate.isOpen();
    }
    
    public ChannelFuture disconnect() {
        return this.delegate.disconnect();
    }
    
    public boolean isRegistered() {
        return this.delegate.isRegistered();
    }
    
    public ChannelFuture newFailedFuture(final Throwable paramThrowable) {
        return this.delegate.newFailedFuture(paramThrowable);
    }
    
    public ChannelFuture close() {
        return this.delegate.close();
    }
    
    public boolean isActive() {
        return this.delegate.isActive();
    }
    
    @Deprecated
    public ChannelFuture deregister() {
        return this.delegate.deregister();
    }
    
    public ChannelPromise voidPromise() {
        return this.delegate.voidPromise();
    }
    
    public ChannelMetadata metadata() {
        return this.delegate.metadata();
    }
    
    public ChannelFuture bind(final SocketAddress paramSocketAddress, final ChannelPromise paramChannelPromise) {
        return this.delegate.bind(paramSocketAddress, paramChannelPromise);
    }
    
    public SocketAddress localAddress() {
        return this.delegate.localAddress();
    }
    
    public SocketAddress remoteAddress() {
        return this.delegate.remoteAddress();
    }
    
    public ChannelFuture connect(final SocketAddress paramSocketAddress, final ChannelPromise paramChannelPromise) {
        return this.delegate.connect(paramSocketAddress, paramChannelPromise);
    }
    
    public ChannelFuture closeFuture() {
        return this.delegate.closeFuture();
    }
    
    public boolean isWritable() {
        return this.delegate.isWritable();
    }
    
    public Channel flush() {
        return this.delegate.flush();
    }
    
    public ChannelFuture connect(final SocketAddress paramSocketAddress1, final SocketAddress paramSocketAddress2, final ChannelPromise paramChannelPromise) {
        return this.delegate.connect(paramSocketAddress1, paramSocketAddress2, paramChannelPromise);
    }
    
    public Channel read() {
        return this.delegate.read();
    }
    
    public Channel.Unsafe unsafe() {
        return this.delegate.unsafe();
    }
    
    public ChannelFuture disconnect(final ChannelPromise paramChannelPromise) {
        return this.delegate.disconnect(paramChannelPromise);
    }
    
    public ChannelFuture close(final ChannelPromise paramChannelPromise) {
        return this.delegate.close(paramChannelPromise);
    }
    
    @Deprecated
    public ChannelFuture deregister(final ChannelPromise paramChannelPromise) {
        return this.delegate.deregister(paramChannelPromise);
    }
    
    public ChannelFuture write(final Object paramObject) {
        return this.delegate.write(paramObject);
    }
    
    public ChannelFuture write(final Object paramObject, final ChannelPromise paramChannelPromise) {
        return this.delegate.write(paramObject, paramChannelPromise);
    }
    
    public ChannelFuture writeAndFlush(final Object paramObject, final ChannelPromise paramChannelPromise) {
        return this.delegate.writeAndFlush(paramObject, paramChannelPromise);
    }
    
    public ChannelFuture writeAndFlush(final Object paramObject) {
        return this.delegate.writeAndFlush(paramObject);
    }
    
    public int compareTo(final Channel o) {
        return this.delegate.compareTo((Object)o);
    }
    
    static {
        MARK_NO_MESSAGE = new FieldAccessor() {
            @Override
            public void set(final Object instance, final Object value) {
            }
            
            @Override
            public Object get(final Object instance) {
                return null;
            }
            
            @Override
            public Field getField() {
                return null;
            }
        };
        ChannelProxy.MESSAGE_LOOKUP = (Map<Class<?>, FieldAccessor>)Maps.newConcurrentMap();
    }
}
