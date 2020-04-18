package com.comphenix.protocol.wrappers.nbt;

import java.util.Iterator;
import java.util.Collection;
import java.util.List;

public interface NbtList<TType> extends NbtBase<List<NbtBase<TType>>>, Iterable<TType>
{
    public static final String EMPTY_NAME = "";
    
    NbtType getElementType();
    
    void setElementType(final NbtType p0);
    
    void addClosest(final Object p0);
    
    void add(final NbtBase<TType> p0);
    
    void add(final String p0);
    
    void add(final byte p0);
    
    void add(final short p0);
    
    void add(final int p0);
    
    void add(final long p0);
    
    void add(final double p0);
    
    void add(final byte[] p0);
    
    void add(final int[] p0);
    
    void remove(final Object p0);
    
    TType getValue(final int p0);
    
    int size();
    
    Collection<NbtBase<TType>> asCollection();
    
    Iterator<TType> iterator();
}
