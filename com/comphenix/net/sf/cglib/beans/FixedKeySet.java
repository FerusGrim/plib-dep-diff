package com.comphenix.net.sf.cglib.beans;

import java.util.Iterator;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;
import java.util.AbstractSet;

public class FixedKeySet extends AbstractSet
{
    private Set set;
    private int size;
    
    public FixedKeySet(final String[] keys) {
        this.size = keys.length;
        this.set = Collections.unmodifiableSet((Set<?>)new HashSet<Object>(Arrays.asList(keys)));
    }
    
    @Override
    public Iterator iterator() {
        return this.set.iterator();
    }
    
    @Override
    public int size() {
        return this.size;
    }
}
