package com.comphenix.net.sf.cglib.transform.impl;

public interface InterceptFieldCallback
{
    int writeInt(final Object p0, final String p1, final int p2, final int p3);
    
    char writeChar(final Object p0, final String p1, final char p2, final char p3);
    
    byte writeByte(final Object p0, final String p1, final byte p2, final byte p3);
    
    boolean writeBoolean(final Object p0, final String p1, final boolean p2, final boolean p3);
    
    short writeShort(final Object p0, final String p1, final short p2, final short p3);
    
    float writeFloat(final Object p0, final String p1, final float p2, final float p3);
    
    double writeDouble(final Object p0, final String p1, final double p2, final double p3);
    
    long writeLong(final Object p0, final String p1, final long p2, final long p3);
    
    Object writeObject(final Object p0, final String p1, final Object p2, final Object p3);
    
    int readInt(final Object p0, final String p1, final int p2);
    
    char readChar(final Object p0, final String p1, final char p2);
    
    byte readByte(final Object p0, final String p1, final byte p2);
    
    boolean readBoolean(final Object p0, final String p1, final boolean p2);
    
    short readShort(final Object p0, final String p1, final short p2);
    
    float readFloat(final Object p0, final String p1, final float p2);
    
    double readDouble(final Object p0, final String p1, final double p2);
    
    long readLong(final Object p0, final String p1, final long p2);
    
    Object readObject(final Object p0, final String p1, final Object p2);
}
