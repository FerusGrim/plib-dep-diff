package com.comphenix.net.sf.cglib.core;

import java.lang.ref.WeakReference;

public class WeakCacheKey<T> extends WeakReference<T>
{
    private final int hash;
    
    public WeakCacheKey(final T referent) {
        super(referent);
        this.hash = referent.hashCode();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof WeakCacheKey)) {
            return false;
        }
        final Object ours = this.get();
        final Object theirs = ((WeakCacheKey)obj).get();
        return ours != null && theirs != null && ours.equals(theirs);
    }
    
    @Override
    public int hashCode() {
        return this.hash;
    }
    
    @Override
    public String toString() {
        final T t = this.get();
        return (t == null) ? ("Clean WeakIdentityKey, hash: " + this.hash) : t.toString();
    }
}
