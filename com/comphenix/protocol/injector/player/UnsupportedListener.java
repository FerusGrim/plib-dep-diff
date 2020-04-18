package com.comphenix.protocol.injector.player;

import java.util.Arrays;
import com.google.common.base.Joiner;

class UnsupportedListener
{
    private String message;
    private int[] packets;
    
    public UnsupportedListener(final String message, final int[] packets) {
        this.message = message;
        this.packets = packets;
    }
    
    public String getMessage() {
        return this.message;
    }
    
    public int[] getPackets() {
        return this.packets;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", this.message, Joiner.on(", ").join((Iterable)Arrays.asList(new int[][] { this.packets })));
    }
}
