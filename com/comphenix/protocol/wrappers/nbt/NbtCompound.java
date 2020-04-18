package com.comphenix.protocol.wrappers.nbt;

import java.util.Iterator;
import java.util.Collection;
import javax.annotation.Nonnull;
import java.util.Set;
import java.util.Map;

public interface NbtCompound extends NbtBase<Map<String, NbtBase<?>>>, Iterable<NbtBase<?>>
{
    @Deprecated
    Map<String, NbtBase<?>> getValue();
    
    boolean containsKey(final String p0);
    
    Set<String> getKeys();
    
     <T> NbtBase<T> getValue(final String p0);
    
    NbtBase<?> getValueOrDefault(final String p0, final NbtType p1);
    
     <T> NbtCompound put(@Nonnull final NbtBase<T> p0);
    
    String getString(final String p0);
    
    String getStringOrDefault(final String p0);
    
    NbtCompound put(final String p0, final String p1);
    
    NbtCompound put(final String p0, final NbtBase<?> p1);
    
    byte getByte(final String p0);
    
    byte getByteOrDefault(final String p0);
    
    NbtCompound put(final String p0, final byte p1);
    
    Short getShort(final String p0);
    
    short getShortOrDefault(final String p0);
    
    NbtCompound put(final String p0, final short p1);
    
    int getInteger(final String p0);
    
    int getIntegerOrDefault(final String p0);
    
    NbtCompound put(final String p0, final int p1);
    
    long getLong(final String p0);
    
    long getLongOrDefault(final String p0);
    
    NbtCompound put(final String p0, final long p1);
    
    float getFloat(final String p0);
    
    float getFloatOrDefault(final String p0);
    
    NbtCompound put(final String p0, final float p1);
    
    double getDouble(final String p0);
    
    double getDoubleOrDefault(final String p0);
    
    NbtCompound put(final String p0, final double p1);
    
    byte[] getByteArray(final String p0);
    
    NbtCompound put(final String p0, final byte[] p1);
    
    int[] getIntegerArray(final String p0);
    
    NbtCompound put(final String p0, final int[] p1);
    
    NbtCompound putObject(final String p0, final Object p1);
    
    Object getObject(final String p0);
    
    NbtCompound getCompound(final String p0);
    
    NbtCompound getCompoundOrDefault(final String p0);
    
    NbtCompound put(final NbtCompound p0);
    
     <T> NbtList<T> getList(final String p0);
    
     <T> NbtList<T> getListOrDefault(final String p0);
    
     <T> NbtCompound put(final NbtList<T> p0);
    
     <T> NbtCompound put(final String p0, final Collection<? extends NbtBase<T>> p1);
    
     <T> NbtBase<?> remove(final String p0);
    
    Iterator<NbtBase<?>> iterator();
}
