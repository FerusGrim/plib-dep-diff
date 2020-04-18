package com.comphenix.protocol.utility;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.OutputStream;

public class ByteBufferOutputStream extends OutputStream
{
    ByteBuffer buf;
    
    public ByteBufferOutputStream(final ByteBuffer buf) {
        this.buf = buf;
    }
    
    @Override
    public void write(final int b) throws IOException {
        this.buf.put((byte)b);
    }
    
    @Override
    public void write(final byte[] bytes, final int off, final int len) throws IOException {
        this.buf.put(bytes, off, len);
    }
}
