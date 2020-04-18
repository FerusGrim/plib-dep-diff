package com.comphenix.protocol.wrappers.collection;

import java.util.Set;

public class CachedSet<T> extends CachedCollection<T> implements Set<T>
{
    public CachedSet(final Set<T> delegate) {
        super(delegate);
    }
}
