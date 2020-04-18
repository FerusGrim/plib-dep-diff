package com.comphenix.net.sf.cglib.transform.impl;

public class AbstractInterceptFieldCallback implements InterceptFieldCallback
{
    public int writeInt(final Object obj, final String name, final int oldValue, final int newValue) {
        return newValue;
    }
    
    public char writeChar(final Object obj, final String name, final char oldValue, final char newValue) {
        return newValue;
    }
    
    public byte writeByte(final Object obj, final String name, final byte oldValue, final byte newValue) {
        return newValue;
    }
    
    public boolean writeBoolean(final Object obj, final String name, final boolean oldValue, final boolean newValue) {
        return newValue;
    }
    
    public short writeShort(final Object obj, final String name, final short oldValue, final short newValue) {
        return newValue;
    }
    
    public float writeFloat(final Object obj, final String name, final float oldValue, final float newValue) {
        return newValue;
    }
    
    public double writeDouble(final Object obj, final String name, final double oldValue, final double newValue) {
        return newValue;
    }
    
    public long writeLong(final Object obj, final String name, final long oldValue, final long newValue) {
        return newValue;
    }
    
    public Object writeObject(final Object obj, final String name, final Object oldValue, final Object newValue) {
        return newValue;
    }
    
    public int readInt(final Object obj, final String name, final int oldValue) {
        return oldValue;
    }
    
    public char readChar(final Object obj, final String name, final char oldValue) {
        return oldValue;
    }
    
    public byte readByte(final Object obj, final String name, final byte oldValue) {
        return oldValue;
    }
    
    public boolean readBoolean(final Object obj, final String name, final boolean oldValue) {
        return oldValue;
    }
    
    public short readShort(final Object obj, final String name, final short oldValue) {
        return oldValue;
    }
    
    public float readFloat(final Object obj, final String name, final float oldValue) {
        return oldValue;
    }
    
    public double readDouble(final Object obj, final String name, final double oldValue) {
        return oldValue;
    }
    
    public long readLong(final Object obj, final String name, final long oldValue) {
        return oldValue;
    }
    
    public Object readObject(final Object obj, final String name, final Object oldValue) {
        return oldValue;
    }
}
