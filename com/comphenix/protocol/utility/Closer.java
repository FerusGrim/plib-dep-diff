package com.comphenix.protocol.utility;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.io.Closeable;

public class Closer implements Closeable
{
    private final List<Closeable> list;
    
    private Closer() {
        this.list = new ArrayList<Closeable>();
    }
    
    public static Closer create() {
        return new Closer();
    }
    
    public <C extends Closeable> C register(final C close) {
        this.list.add(close);
        return close;
    }
    
    @Override
    public void close() {
        for (final Closeable close : this.list) {
            closeQuietly(close);
        }
    }
    
    public static void closeQuietly(final Closeable close) {
        try {
            close.close();
        }
        catch (Throwable t) {}
    }
}
