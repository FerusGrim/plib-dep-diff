package com.comphenix.protocol.injector.netty;

import io.netty.util.ReferenceCounted;
import java.nio.channels.FileChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.GatheringByteChannel;
import java.io.OutputStream;
import java.io.InputStream;
import com.google.common.io.ByteStreams;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import io.netty.buffer.ByteBufAllocator;
import java.io.IOException;
import com.comphenix.protocol.utility.MinecraftReflection;
import io.netty.buffer.ByteBuf;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import io.netty.buffer.AbstractByteBuf;

public class NettyByteBufAdapter extends AbstractByteBuf
{
    private DataInputStream input;
    private DataOutputStream output;
    private static FieldAccessor READER_INDEX;
    private static FieldAccessor WRITER_INDEX;
    private static final int CAPACITY = 32767;
    
    private NettyByteBufAdapter(final DataInputStream input, final DataOutputStream output) {
        super(32767);
        this.input = input;
        this.output = output;
        try {
            if (NettyByteBufAdapter.READER_INDEX == null) {
                NettyByteBufAdapter.READER_INDEX = Accessors.getFieldAccessor(AbstractByteBuf.class.getDeclaredField("readerIndex"));
            }
            if (NettyByteBufAdapter.WRITER_INDEX == null) {
                NettyByteBufAdapter.WRITER_INDEX = Accessors.getFieldAccessor(AbstractByteBuf.class.getDeclaredField("writerIndex"));
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot initialize ByteBufAdapter.", e);
        }
        if (input == null) {
            NettyByteBufAdapter.READER_INDEX.set(this, Integer.MAX_VALUE);
        }
        if (output == null) {
            NettyByteBufAdapter.WRITER_INDEX.set(this, Integer.MAX_VALUE);
        }
    }
    
    public static ByteBuf packetReader(final DataInputStream input) {
        return (ByteBuf)MinecraftReflection.getPacketDataSerializer(new NettyByteBufAdapter(input, null));
    }
    
    public static ByteBuf packetWriter(final DataOutputStream output) {
        return (ByteBuf)MinecraftReflection.getPacketDataSerializer(new NettyByteBufAdapter(null, output));
    }
    
    public int refCnt() {
        return 1;
    }
    
    public boolean release() {
        return false;
    }
    
    public boolean release(final int paramInt) {
        return false;
    }
    
    protected byte _getByte(final int paramInt) {
        try {
            return this.input.readByte();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read input.", e);
        }
    }
    
    protected short _getShort(final int paramInt) {
        try {
            return this.input.readShort();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read input.", e);
        }
    }
    
    protected int _getUnsignedMedium(final int paramInt) {
        try {
            return this.input.readUnsignedShort();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read input.", e);
        }
    }
    
    protected int _getInt(final int paramInt) {
        try {
            return this.input.readInt();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read input.", e);
        }
    }
    
    protected long _getLong(final int paramInt) {
        try {
            return this.input.readLong();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read input.", e);
        }
    }
    
    protected void _setByte(final int index, final int value) {
        try {
            this.output.writeByte(value);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write output.", e);
        }
    }
    
    protected void _setShort(final int index, final int value) {
        try {
            this.output.writeShort(value);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write output.", e);
        }
    }
    
    protected void _setMedium(final int index, final int value) {
        try {
            this.output.writeShort(value);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write output.", e);
        }
    }
    
    protected void _setInt(final int index, final int value) {
        try {
            this.output.writeInt(value);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write output.", e);
        }
    }
    
    protected void _setLong(final int index, final long value) {
        try {
            this.output.writeLong(value);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write output.", e);
        }
    }
    
    public int capacity() {
        return 32767;
    }
    
    public ByteBuf capacity(final int paramInt) {
        return (ByteBuf)this;
    }
    
    public ByteBufAllocator alloc() {
        return ByteBufAllocator.DEFAULT;
    }
    
    public ByteOrder order() {
        return ByteOrder.LITTLE_ENDIAN;
    }
    
    public ByteBuf unwrap() {
        return null;
    }
    
    public boolean isDirect() {
        return false;
    }
    
    public ByteBuf getBytes(final int index, final ByteBuf dst, final int dstIndex, final int length) {
        try {
            for (int i = 0; i < length; ++i) {
                dst.setByte(dstIndex + i, this.input.read());
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read input.", e);
        }
        return (ByteBuf)this;
    }
    
    public ByteBuf getBytes(final int index, final byte[] dst, final int dstIndex, final int length) {
        try {
            this.input.read(dst, dstIndex, length);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read input.", e);
        }
        return (ByteBuf)this;
    }
    
    public ByteBuf getBytes(final int index, final ByteBuffer dst) {
        try {
            dst.put(ByteStreams.toByteArray((InputStream)this.input));
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read input.", e);
        }
        return (ByteBuf)this;
    }
    
    public ByteBuf getBytes(final int index, final OutputStream dst, final int length) throws IOException {
        ByteStreams.copy(ByteStreams.limit((InputStream)this.input, (long)length), dst);
        return (ByteBuf)this;
    }
    
    public int getBytes(final int index, final GatheringByteChannel out, final int length) throws IOException {
        final byte[] data = ByteStreams.toByteArray(ByteStreams.limit((InputStream)this.input, (long)length));
        out.write(ByteBuffer.wrap(data));
        return data.length;
    }
    
    public ByteBuf setBytes(final int index, final ByteBuf src, final int srcIndex, final int length) {
        final byte[] buffer = new byte[length];
        src.getBytes(srcIndex, buffer);
        try {
            this.output.write(buffer);
            return (ByteBuf)this;
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write output.", e);
        }
    }
    
    public ByteBuf setBytes(final int index, final byte[] src, final int srcIndex, final int length) {
        try {
            this.output.write(src, srcIndex, length);
            return (ByteBuf)this;
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write output.", e);
        }
    }
    
    public ByteBuf setBytes(final int index, final ByteBuffer src) {
        try {
            final WritableByteChannel channel = Channels.newChannel(this.output);
            channel.write(src);
            return (ByteBuf)this;
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write output.", e);
        }
    }
    
    public int setBytes(final int index, final InputStream in, final int length) throws IOException {
        final InputStream limit = ByteStreams.limit(in, (long)length);
        ByteStreams.copy(limit, (OutputStream)this.output);
        return length - limit.available();
    }
    
    public int setBytes(final int index, final ScatteringByteChannel in, final int length) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(length);
        final WritableByteChannel channel = Channels.newChannel(this.output);
        final int count = in.read(buffer);
        channel.write(buffer);
        return count;
    }
    
    public ByteBuf copy(final int index, final int length) {
        throw new UnsupportedOperationException("Cannot seek in input stream.");
    }
    
    public int nioBufferCount() {
        return 0;
    }
    
    public ByteBuffer nioBuffer(final int paramInt1, final int paramInt2) {
        throw new UnsupportedOperationException();
    }
    
    public ByteBuffer internalNioBuffer(final int paramInt1, final int paramInt2) {
        return null;
    }
    
    public ByteBuffer[] nioBuffers(final int paramInt1, final int paramInt2) {
        return null;
    }
    
    public boolean hasArray() {
        return false;
    }
    
    public byte[] array() {
        return null;
    }
    
    public int arrayOffset() {
        return 0;
    }
    
    public boolean hasMemoryAddress() {
        return false;
    }
    
    public long memoryAddress() {
        return 0L;
    }
    
    public ByteBuf retain(final int paramInt) {
        return (ByteBuf)this;
    }
    
    public ByteBuf retain() {
        return (ByteBuf)this;
    }
    
    protected int _getIntLE(final int arg0) {
        return 0;
    }
    
    protected long _getLongLE(final int arg0) {
        return 0L;
    }
    
    protected short _getShortLE(final int arg0) {
        return 0;
    }
    
    protected int _getUnsignedMediumLE(final int arg0) {
        return 0;
    }
    
    protected void _setIntLE(final int arg0, final int arg1) {
    }
    
    protected void _setLongLE(final int arg0, final long arg1) {
    }
    
    protected void _setMediumLE(final int arg0, final int arg1) {
    }
    
    protected void _setShortLE(final int arg0, final int arg1) {
    }
    
    public int getBytes(final int arg0, final FileChannel arg1, final long arg2, final int arg3) throws IOException {
        return 0;
    }
    
    public int setBytes(final int arg0, final FileChannel arg1, final long arg2, final int arg3) throws IOException {
        return 0;
    }
    
    public ByteBuf touch() {
        return null;
    }
    
    public ByteBuf touch(final Object arg0) {
        return null;
    }
}
