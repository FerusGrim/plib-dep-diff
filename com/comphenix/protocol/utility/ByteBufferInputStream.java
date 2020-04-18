package com.comphenix.protocol.utility;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.InputStream;

public class ByteBufferInputStream extends InputStream
{
    private ByteBuffer buf;
    
    public ByteBufferInputStream(final ByteBuffer buf) {
        this.buf = buf;
    }
    
    @Override
    public int read() throws IOException {
        if (!this.buf.hasRemaining()) {
            return -1;
        }
        return this.buf.get() & 0xFF;
    }
    
    @Override
    public int read(final byte[] bytes, final int off, int len) throws IOException {
        if (!this.buf.hasRemaining()) {
            return -1;
        }
        len = Math.min(len, this.buf.remaining());
        this.buf.get(bytes, off, len);
        return len;
    }
}
