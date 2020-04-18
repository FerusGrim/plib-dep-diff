package com.comphenix.protocol.injector.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FilterInputStream;

class CaptureInputStream extends FilterInputStream
{
    protected OutputStream out;
    
    public CaptureInputStream(final InputStream in, final OutputStream out) {
        super(in);
        this.out = out;
    }
    
    @Override
    public int read() throws IOException {
        final int value = super.read();
        if (value >= 0) {
            this.out.write(value);
        }
        return value;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        this.out.close();
    }
    
    @Override
    public int read(final byte[] b) throws IOException {
        final int count = super.read(b);
        if (count > 0) {
            this.out.write(b, 0, count);
        }
        return count;
    }
    
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int count = super.read(b, off, len);
        if (count > 0) {
            this.out.write(b, off, count);
        }
        return count;
    }
    
    public OutputStream getOutputStream() {
        return this.out;
    }
}
