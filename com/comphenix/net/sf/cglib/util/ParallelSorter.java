package com.comphenix.net.sf.cglib.util;

import com.comphenix.net.sf.cglib.core.ReflectUtils;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ClassesKey;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;
import java.util.Comparator;

public abstract class ParallelSorter extends SorterTemplate
{
    protected Object[] a;
    private Comparer comparer;
    
    protected ParallelSorter() {
    }
    
    public abstract ParallelSorter newInstance(final Object[] p0);
    
    public static ParallelSorter create(final Object[] arrays) {
        final Generator gen = new Generator();
        gen.setArrays(arrays);
        return gen.create();
    }
    
    private int len() {
        return ((Object[])this.a[0]).length;
    }
    
    public void quickSort(final int index) {
        this.quickSort(index, 0, this.len(), null);
    }
    
    public void quickSort(final int index, final int lo, final int hi) {
        this.quickSort(index, lo, hi, null);
    }
    
    public void quickSort(final int index, final Comparator cmp) {
        this.quickSort(index, 0, this.len(), cmp);
    }
    
    public void quickSort(final int index, final int lo, final int hi, final Comparator cmp) {
        this.chooseComparer(index, cmp);
        super.quickSort(lo, hi - 1);
    }
    
    public void mergeSort(final int index) {
        this.mergeSort(index, 0, this.len(), null);
    }
    
    public void mergeSort(final int index, final int lo, final int hi) {
        this.mergeSort(index, lo, hi, null);
    }
    
    public void mergeSort(final int index, final Comparator cmp) {
        this.mergeSort(index, 0, this.len(), cmp);
    }
    
    public void mergeSort(final int index, final int lo, final int hi, final Comparator cmp) {
        this.chooseComparer(index, cmp);
        super.mergeSort(lo, hi - 1);
    }
    
    private void chooseComparer(final int index, final Comparator cmp) {
        final Object array = this.a[index];
        final Class type = array.getClass().getComponentType();
        if (type.equals(Integer.TYPE)) {
            this.comparer = new IntComparer((int[])array);
        }
        else if (type.equals(Long.TYPE)) {
            this.comparer = new LongComparer((long[])array);
        }
        else if (type.equals(Double.TYPE)) {
            this.comparer = new DoubleComparer((double[])array);
        }
        else if (type.equals(Float.TYPE)) {
            this.comparer = new FloatComparer((float[])array);
        }
        else if (type.equals(Short.TYPE)) {
            this.comparer = new ShortComparer((short[])array);
        }
        else if (type.equals(Byte.TYPE)) {
            this.comparer = new ByteComparer((byte[])array);
        }
        else if (cmp != null) {
            this.comparer = new ComparatorComparer((Object[])array, cmp);
        }
        else {
            this.comparer = new ObjectComparer((Object[])array);
        }
    }
    
    @Override
    protected int compare(final int i, final int j) {
        return this.comparer.compare(i, j);
    }
    
    static class ComparatorComparer implements Comparer
    {
        private Object[] a;
        private Comparator cmp;
        
        public ComparatorComparer(final Object[] a, final Comparator cmp) {
            this.a = a;
            this.cmp = cmp;
        }
        
        public int compare(final int i, final int j) {
            return this.cmp.compare(this.a[i], this.a[j]);
        }
    }
    
    static class ObjectComparer implements Comparer
    {
        private Object[] a;
        
        public ObjectComparer(final Object[] a) {
            this.a = a;
        }
        
        public int compare(final int i, final int j) {
            return ((Comparable)this.a[i]).compareTo(this.a[j]);
        }
    }
    
    static class IntComparer implements Comparer
    {
        private int[] a;
        
        public IntComparer(final int[] a) {
            this.a = a;
        }
        
        public int compare(final int i, final int j) {
            return this.a[i] - this.a[j];
        }
    }
    
    static class LongComparer implements Comparer
    {
        private long[] a;
        
        public LongComparer(final long[] a) {
            this.a = a;
        }
        
        public int compare(final int i, final int j) {
            final long vi = this.a[i];
            final long vj = this.a[j];
            return (vi == vj) ? 0 : ((vi > vj) ? 1 : -1);
        }
    }
    
    static class FloatComparer implements Comparer
    {
        private float[] a;
        
        public FloatComparer(final float[] a) {
            this.a = a;
        }
        
        public int compare(final int i, final int j) {
            final float vi = this.a[i];
            final float vj = this.a[j];
            return (vi == vj) ? 0 : ((vi > vj) ? 1 : -1);
        }
    }
    
    static class DoubleComparer implements Comparer
    {
        private double[] a;
        
        public DoubleComparer(final double[] a) {
            this.a = a;
        }
        
        public int compare(final int i, final int j) {
            final double vi = this.a[i];
            final double vj = this.a[j];
            return (vi == vj) ? 0 : ((vi > vj) ? 1 : -1);
        }
    }
    
    static class ShortComparer implements Comparer
    {
        private short[] a;
        
        public ShortComparer(final short[] a) {
            this.a = a;
        }
        
        public int compare(final int i, final int j) {
            return this.a[i] - this.a[j];
        }
    }
    
    static class ByteComparer implements Comparer
    {
        private byte[] a;
        
        public ByteComparer(final byte[] a) {
            this.a = a;
        }
        
        public int compare(final int i, final int j) {
            return this.a[i] - this.a[j];
        }
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private Object[] arrays;
        
        public Generator() {
            super(Generator.SOURCE);
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return null;
        }
        
        public void setArrays(final Object[] arrays) {
            this.arrays = arrays;
        }
        
        public ParallelSorter create() {
            return (ParallelSorter)super.create(ClassesKey.create(this.arrays));
        }
        
        public void generateClass(final $ClassVisitor v) throws Exception {
            if (this.arrays.length == 0) {
                throw new IllegalArgumentException("No arrays specified to sort");
            }
            for (int i = 0; i < this.arrays.length; ++i) {
                if (!this.arrays[i].getClass().isArray()) {
                    throw new IllegalArgumentException(this.arrays[i].getClass() + " is not an array");
                }
            }
            new ParallelSorterEmitter(v, this.getClassName(), this.arrays);
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            return ((ParallelSorter)ReflectUtils.newInstance(type)).newInstance(this.arrays);
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return ((ParallelSorter)instance).newInstance(this.arrays);
        }
        
        static {
            SOURCE = new Source(ParallelSorter.class.getName());
        }
    }
    
    interface Comparer
    {
        int compare(final int p0, final int p1);
    }
}
