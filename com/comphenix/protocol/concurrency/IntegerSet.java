package com.comphenix.protocol.concurrency;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;

public class IntegerSet
{
    private final boolean[] array;
    
    public IntegerSet(final int maximumCount) {
        this.array = new boolean[maximumCount];
    }
    
    public IntegerSet(final int maximumCount, final Collection<Integer> values) {
        this.array = new boolean[maximumCount];
        this.addAll(values);
    }
    
    public boolean contains(final int element) {
        return this.array[element];
    }
    
    public void add(final int element) {
        this.array[element] = true;
    }
    
    public void addAll(final Collection<Integer> packets) {
        for (final Integer id : packets) {
            this.add(id);
        }
    }
    
    public void remove(final int element) {
        if (element >= 0 && element < this.array.length) {
            this.array[element] = false;
        }
    }
    
    public void clear() {
        Arrays.fill(this.array, false);
    }
    
    public Set<Integer> toSet() {
        final Set<Integer> elements = new HashSet<Integer>();
        for (int i = 0; i < this.array.length; ++i) {
            if (this.array[i]) {
                elements.add(i);
            }
        }
        return elements;
    }
}
