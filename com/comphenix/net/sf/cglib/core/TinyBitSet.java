package com.comphenix.net.sf.cglib.core;

@Deprecated
public class TinyBitSet
{
    private static int[] T;
    private int value;
    
    public TinyBitSet() {
        this.value = 0;
    }
    
    private static int gcount(int x) {
        int c = 0;
        while (x != 0) {
            ++c;
            x &= x - 1;
        }
        return c;
    }
    
    private static int topbit(int i) {
        int j;
        for (j = 0; i != 0; i ^= j) {
            j = (i & -i);
        }
        return j;
    }
    
    private static int log2(int i) {
        int j = 0;
        j = 0;
        while (i != 0) {
            ++j;
            i >>= 1;
        }
        return j;
    }
    
    public int length() {
        return log2(topbit(this.value));
    }
    
    public int cardinality() {
        int w = this.value;
        int c = 0;
        while (w != 0) {
            c += TinyBitSet.T[w & 0xFF];
            w >>= 8;
        }
        return c;
    }
    
    public boolean get(final int index) {
        return (this.value & 1 << index) != 0x0;
    }
    
    public void set(final int index) {
        this.value |= 1 << index;
    }
    
    public void clear(final int index) {
        this.value &= ~(1 << index);
    }
    
    static {
        TinyBitSet.T = new int[256];
        for (int j = 0; j < 256; ++j) {
            TinyBitSet.T[j] = gcount(j);
        }
    }
}
