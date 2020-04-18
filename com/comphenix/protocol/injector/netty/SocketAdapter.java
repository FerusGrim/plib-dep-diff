package com.comphenix.protocol.injector.netty;

import java.io.OutputStream;
import java.net.SocketException;
import io.netty.channel.ChannelOption;
import java.io.InputStream;
import java.net.InetAddress;
import java.io.IOException;
import java.net.SocketAddress;
import io.netty.channel.socket.SocketChannel;
import java.net.Socket;

public class SocketAdapter extends Socket
{
    private final SocketChannel ch;
    
    private SocketAdapter(final SocketChannel ch) {
        this.ch = ch;
    }
    
    public static SocketAdapter adapt(final SocketChannel ch) {
        return new SocketAdapter(ch);
    }
    
    @Override
    public void bind(final SocketAddress bindpoint) throws IOException {
        this.ch.bind(bindpoint).syncUninterruptibly();
    }
    
    @Override
    public synchronized void close() throws IOException {
        this.ch.close().syncUninterruptibly();
    }
    
    @Override
    public void connect(final SocketAddress endpoint) throws IOException {
        this.ch.connect(endpoint).syncUninterruptibly();
    }
    
    @Override
    public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
        this.ch.config().setConnectTimeoutMillis(timeout);
        this.ch.connect(endpoint).syncUninterruptibly();
    }
    
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof SocketAdapter && this.ch.equals(((SocketAdapter)obj).ch);
    }
    
    @Override
    public java.nio.channels.SocketChannel getChannel() {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public InetAddress getInetAddress() {
        return this.ch.remoteAddress().getAddress();
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public boolean getKeepAlive() throws SocketException {
        return (boolean)this.ch.config().getOption(ChannelOption.SO_KEEPALIVE);
    }
    
    @Override
    public InetAddress getLocalAddress() {
        return this.ch.localAddress().getAddress();
    }
    
    @Override
    public int getLocalPort() {
        return this.ch.localAddress().getPort();
    }
    
    @Override
    public SocketAddress getLocalSocketAddress() {
        return this.ch.localAddress();
    }
    
    @Override
    public boolean getOOBInline() throws SocketException {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public int getPort() {
        return this.ch.remoteAddress().getPort();
    }
    
    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return (int)this.ch.config().getOption(ChannelOption.SO_RCVBUF);
    }
    
    @Override
    public SocketAddress getRemoteSocketAddress() {
        return this.ch.remoteAddress();
    }
    
    @Override
    public boolean getReuseAddress() throws SocketException {
        return (boolean)this.ch.config().getOption(ChannelOption.SO_REUSEADDR);
    }
    
    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return (int)this.ch.config().getOption(ChannelOption.SO_SNDBUF);
    }
    
    @Override
    public int getSoLinger() throws SocketException {
        return (int)this.ch.config().getOption(ChannelOption.SO_LINGER);
    }
    
    @Override
    public synchronized int getSoTimeout() throws SocketException {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return (boolean)this.ch.config().getOption(ChannelOption.TCP_NODELAY);
    }
    
    @Override
    public int getTrafficClass() throws SocketException {
        return (int)this.ch.config().getOption(ChannelOption.IP_TOS);
    }
    
    @Override
    public int hashCode() {
        return this.ch.hashCode();
    }
    
    @Override
    public boolean isBound() {
        return this.ch.localAddress() != null;
    }
    
    @Override
    public boolean isClosed() {
        return !this.ch.isOpen();
    }
    
    @Override
    public boolean isConnected() {
        return this.ch.isActive();
    }
    
    @Override
    public boolean isInputShutdown() {
        return this.ch.isInputShutdown();
    }
    
    @Override
    public boolean isOutputShutdown() {
        return this.ch.isOutputShutdown();
    }
    
    @Override
    public void sendUrgentData(final int data) throws IOException {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public void setKeepAlive(final boolean on) throws SocketException {
        this.ch.config().setOption(ChannelOption.SO_KEEPALIVE, (Object)on);
    }
    
    @Override
    public void setOOBInline(final boolean on) throws SocketException {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public synchronized void setReceiveBufferSize(final int size) throws SocketException {
        this.ch.config().setOption(ChannelOption.SO_RCVBUF, (Object)size);
    }
    
    @Override
    public void setReuseAddress(final boolean on) throws SocketException {
        this.ch.config().setOption(ChannelOption.SO_REUSEADDR, (Object)on);
    }
    
    @Override
    public synchronized void setSendBufferSize(final int size) throws SocketException {
        this.ch.config().setOption(ChannelOption.SO_SNDBUF, (Object)size);
    }
    
    @Override
    public void setSoLinger(final boolean on, final int linger) throws SocketException {
        this.ch.config().setOption(ChannelOption.SO_LINGER, (Object)linger);
    }
    
    @Override
    public synchronized void setSoTimeout(final int timeout) throws SocketException {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public void setTcpNoDelay(final boolean on) throws SocketException {
        this.ch.config().setOption(ChannelOption.TCP_NODELAY, (Object)on);
    }
    
    @Override
    public void setTrafficClass(final int tc) throws SocketException {
        this.ch.config().setOption(ChannelOption.IP_TOS, (Object)tc);
    }
    
    @Override
    public void shutdownInput() throws IOException {
        throw new UnsupportedOperationException("Operation not supported on Channel wrapper.");
    }
    
    @Override
    public void shutdownOutput() throws IOException {
        this.ch.shutdownOutput().syncUninterruptibly();
    }
    
    @Override
    public String toString() {
        return this.ch.toString();
    }
}
