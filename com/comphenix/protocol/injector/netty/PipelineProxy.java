package com.comphenix.protocol.injector.netty;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import java.net.SocketAddress;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

public class PipelineProxy implements ChannelPipeline
{
    protected final ChannelPipeline pipeline;
    protected final Channel channel;
    
    public PipelineProxy(final ChannelPipeline pipeline, final Channel channel) {
        this.pipeline = pipeline;
        this.channel = channel;
    }
    
    public ChannelPipeline addAfter(final EventExecutorGroup arg0, final String arg1, final String arg2, final ChannelHandler arg3) {
        this.pipeline.addAfter(arg0, arg1, arg2, arg3);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addAfter(final String arg0, final String arg1, final ChannelHandler arg2) {
        this.pipeline.addAfter(arg0, arg1, arg2);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addBefore(final EventExecutorGroup arg0, final String arg1, final String arg2, final ChannelHandler arg3) {
        this.pipeline.addBefore(arg0, arg1, arg2, arg3);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addBefore(final String arg0, final String arg1, final ChannelHandler arg2) {
        this.pipeline.addBefore(arg0, arg1, arg2);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addFirst(final ChannelHandler... arg0) {
        this.pipeline.addFirst(arg0);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addFirst(final EventExecutorGroup arg0, final ChannelHandler... arg1) {
        this.pipeline.addFirst(arg0, arg1);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addFirst(final EventExecutorGroup arg0, final String arg1, final ChannelHandler arg2) {
        this.pipeline.addFirst(arg0, arg1, arg2);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addFirst(final String arg0, final ChannelHandler arg1) {
        this.pipeline.addFirst(arg0, arg1);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addLast(final ChannelHandler... arg0) {
        this.pipeline.addLast(arg0);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addLast(final EventExecutorGroup arg0, final ChannelHandler... arg1) {
        this.pipeline.addLast(arg0, arg1);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addLast(final EventExecutorGroup arg0, final String arg1, final ChannelHandler arg2) {
        this.pipeline.addLast(arg0, arg1, arg2);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline addLast(final String arg0, final ChannelHandler arg1) {
        this.pipeline.addLast(arg0, arg1);
        return (ChannelPipeline)this;
    }
    
    public ChannelFuture bind(final SocketAddress arg0, final ChannelPromise arg1) {
        return this.pipeline.bind(arg0, arg1);
    }
    
    public ChannelFuture bind(final SocketAddress arg0) {
        return this.pipeline.bind(arg0);
    }
    
    public Channel channel() {
        return this.channel;
    }
    
    public ChannelFuture close() {
        return this.pipeline.close();
    }
    
    public ChannelFuture close(final ChannelPromise arg0) {
        return this.pipeline.close(arg0);
    }
    
    public ChannelFuture connect(final SocketAddress arg0, final ChannelPromise arg1) {
        return this.pipeline.connect(arg0, arg1);
    }
    
    public ChannelFuture connect(final SocketAddress arg0, final SocketAddress arg1, final ChannelPromise arg2) {
        return this.pipeline.connect(arg0, arg1, arg2);
    }
    
    public ChannelFuture connect(final SocketAddress arg0, final SocketAddress arg1) {
        return this.pipeline.connect(arg0, arg1);
    }
    
    public ChannelFuture connect(final SocketAddress arg0) {
        return this.pipeline.connect(arg0);
    }
    
    public ChannelHandlerContext context(final ChannelHandler arg0) {
        return this.pipeline.context(arg0);
    }
    
    public ChannelHandlerContext context(final Class<? extends ChannelHandler> arg0) {
        return this.pipeline.context((Class)arg0);
    }
    
    public ChannelHandlerContext context(final String arg0) {
        return this.pipeline.context(arg0);
    }
    
    public ChannelFuture deregister() {
        return this.pipeline.deregister();
    }
    
    public ChannelFuture deregister(final ChannelPromise arg0) {
        return this.pipeline.deregister(arg0);
    }
    
    public ChannelPipeline fireChannelUnregistered() {
        this.pipeline.fireChannelUnregistered();
        return (ChannelPipeline)this;
    }
    
    public ChannelFuture disconnect() {
        return this.pipeline.disconnect();
    }
    
    public ChannelFuture disconnect(final ChannelPromise arg0) {
        return this.pipeline.disconnect(arg0);
    }
    
    public ChannelPipeline fireChannelActive() {
        this.pipeline.fireChannelActive();
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline fireChannelInactive() {
        this.pipeline.fireChannelInactive();
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline fireChannelRead(final Object arg0) {
        this.pipeline.fireChannelRead(arg0);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline fireChannelReadComplete() {
        this.pipeline.fireChannelReadComplete();
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline fireChannelRegistered() {
        this.pipeline.fireChannelRegistered();
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline fireChannelWritabilityChanged() {
        this.pipeline.fireChannelWritabilityChanged();
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline fireExceptionCaught(final Throwable arg0) {
        this.pipeline.fireExceptionCaught(arg0);
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline fireUserEventTriggered(final Object arg0) {
        this.pipeline.fireUserEventTriggered(arg0);
        return (ChannelPipeline)this;
    }
    
    public ChannelHandler first() {
        return this.pipeline.first();
    }
    
    public ChannelHandlerContext firstContext() {
        return this.pipeline.firstContext();
    }
    
    public ChannelPipeline flush() {
        this.pipeline.flush();
        return (ChannelPipeline)this;
    }
    
    public <T extends ChannelHandler> T get(final Class<T> arg0) {
        return (T)this.pipeline.get((Class)arg0);
    }
    
    public ChannelHandler get(final String arg0) {
        return this.pipeline.get(arg0);
    }
    
    public Iterator<Map.Entry<String, ChannelHandler>> iterator() {
        return (Iterator<Map.Entry<String, ChannelHandler>>)this.pipeline.iterator();
    }
    
    public ChannelHandler last() {
        return this.pipeline.last();
    }
    
    public ChannelHandlerContext lastContext() {
        return this.pipeline.lastContext();
    }
    
    public List<String> names() {
        return (List<String>)this.pipeline.names();
    }
    
    public ChannelPipeline read() {
        this.pipeline.read();
        return (ChannelPipeline)this;
    }
    
    public ChannelPipeline remove(final ChannelHandler arg0) {
        this.pipeline.remove(arg0);
        return (ChannelPipeline)this;
    }
    
    public <T extends ChannelHandler> T remove(final Class<T> arg0) {
        return (T)this.pipeline.remove((Class)arg0);
    }
    
    public ChannelHandler remove(final String arg0) {
        return this.pipeline.remove(arg0);
    }
    
    public ChannelHandler removeFirst() {
        return this.pipeline.removeFirst();
    }
    
    public ChannelHandler removeLast() {
        return this.pipeline.removeLast();
    }
    
    public ChannelPipeline replace(final ChannelHandler arg0, final String arg1, final ChannelHandler arg2) {
        this.pipeline.replace(arg0, arg1, arg2);
        return (ChannelPipeline)this;
    }
    
    public <T extends ChannelHandler> T replace(final Class<T> arg0, final String arg1, final ChannelHandler arg2) {
        return (T)this.pipeline.replace((Class)arg0, arg1, arg2);
    }
    
    public ChannelHandler replace(final String arg0, final String arg1, final ChannelHandler arg2) {
        return this.pipeline.replace(arg0, arg1, arg2);
    }
    
    public Map<String, ChannelHandler> toMap() {
        return (Map<String, ChannelHandler>)this.pipeline.toMap();
    }
    
    public ChannelFuture write(final Object arg0, final ChannelPromise arg1) {
        return this.pipeline.write(arg0, arg1);
    }
    
    public ChannelFuture write(final Object arg0) {
        return this.pipeline.write(arg0);
    }
    
    public ChannelFuture writeAndFlush(final Object arg0, final ChannelPromise arg1) {
        return this.pipeline.writeAndFlush(arg0, arg1);
    }
    
    public ChannelFuture writeAndFlush(final Object arg0) {
        return this.pipeline.writeAndFlush(arg0);
    }
}
