package com.comphenix.protocol.concurrency;

import com.google.common.base.Objects;
import com.google.common.collect.Range;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.NavigableMap;

public abstract class AbstractIntervalTree<TKey extends Comparable<TKey>, TValue>
{
    protected NavigableMap<TKey, EndPoint> bounds;
    
    public AbstractIntervalTree() {
        this.bounds = new TreeMap<TKey, EndPoint>();
    }
    
    public Set<Entry> remove(final TKey lowerBound, final TKey upperBound) {
        return this.remove(lowerBound, upperBound, false);
    }
    
    public Set<Entry> remove(final TKey lowerBound, final TKey upperBound, final boolean preserveDifference) {
        this.checkBounds(lowerBound, upperBound);
        final NavigableMap<TKey, EndPoint> range = this.bounds.subMap(lowerBound, true, upperBound, true);
        final EndPoint first = this.getNextEndPoint(lowerBound, true);
        final EndPoint last = this.getPreviousEndPoint(upperBound, true);
        EndPoint previous = null;
        EndPoint next = null;
        final Set<Entry> resized = new HashSet<Entry>();
        final Set<Entry> removed = new HashSet<Entry>();
        if (first != null && first.state == State.CLOSE) {
            previous = this.getPreviousEndPoint(first.key, false);
            if (previous != null) {
                removed.add(this.getEntry(previous, first));
            }
        }
        if (last != null && last.state == State.OPEN) {
            next = this.getNextEndPoint(last.key, false);
            if (next != null) {
                removed.add(this.getEntry(last, next));
            }
        }
        this.removeEntrySafely(previous, first);
        this.removeEntrySafely(last, next);
        if (preserveDifference) {
            if (previous != null) {
                resized.add(this.putUnsafe(previous.key, this.decrementKey(lowerBound), previous.value));
            }
            if (next != null) {
                resized.add(this.putUnsafe(this.incrementKey(upperBound), next.key, next.value));
            }
        }
        this.getEntries(removed, range);
        this.invokeEntryRemoved(removed);
        if (preserveDifference) {
            this.invokeEntryAdded(resized);
        }
        range.clear();
        return removed;
    }
    
    protected Entry getEntry(final EndPoint left, final EndPoint right) {
        if (left == null) {
            throw new IllegalArgumentException("left endpoint cannot be NULL.");
        }
        if (right == null) {
            throw new IllegalArgumentException("right endpoint cannot be NULL.");
        }
        if (right.key.compareTo(left.key) < 0) {
            return this.getEntry(right, left);
        }
        return new Entry(left, right);
    }
    
    private void removeEntrySafely(final EndPoint left, final EndPoint right) {
        if (left != null && right != null) {
            this.bounds.remove(left.key);
            this.bounds.remove(right.key);
        }
    }
    
    protected EndPoint addEndPoint(final TKey key, final TValue value, final State state) {
        EndPoint endPoint = this.bounds.get(key);
        if (endPoint != null) {
            endPoint.state = State.BOTH;
        }
        else {
            endPoint = new EndPoint(state, key, value);
            this.bounds.put(key, endPoint);
        }
        return endPoint;
    }
    
    public void put(final TKey lowerBound, final TKey upperBound, final TValue value) {
        this.remove(lowerBound, upperBound, true);
        this.invokeEntryAdded(this.putUnsafe(lowerBound, upperBound, value));
    }
    
    private Entry putUnsafe(final TKey lowerBound, final TKey upperBound, final TValue value) {
        if (value != null) {
            final EndPoint left = this.addEndPoint(lowerBound, value, State.OPEN);
            final EndPoint right = this.addEndPoint(upperBound, value, State.CLOSE);
            return new Entry(left, right);
        }
        return null;
    }
    
    private void checkBounds(final TKey lowerBound, final TKey upperBound) {
        if (lowerBound == null) {
            throw new IllegalAccessError("lowerbound cannot be NULL.");
        }
        if (upperBound == null) {
            throw new IllegalAccessError("upperBound cannot be NULL.");
        }
        if (upperBound.compareTo(lowerBound) < 0) {
            throw new IllegalArgumentException("upperBound cannot be less than lowerBound.");
        }
    }
    
    public boolean containsKey(final TKey key) {
        return this.getEndPoint(key) != null;
    }
    
    public Set<Entry> entrySet() {
        final Set<Entry> result = new HashSet<Entry>();
        this.getEntries(result, this.bounds);
        return result;
    }
    
    public void clear() {
        if (!this.bounds.isEmpty()) {
            this.remove(this.bounds.firstKey(), this.bounds.lastKey());
        }
    }
    
    private void getEntries(final Set<Entry> destination, final NavigableMap<TKey, EndPoint> map) {
        Map.Entry<TKey, EndPoint> last = null;
        for (final Map.Entry<TKey, EndPoint> entry : map.entrySet()) {
            switch (entry.getValue().state) {
                case BOTH: {
                    final EndPoint point = entry.getValue();
                    destination.add(new Entry(point, point));
                    continue;
                }
                case CLOSE: {
                    if (last != null) {
                        destination.add(new Entry(last.getValue(), entry.getValue()));
                        continue;
                    }
                    continue;
                }
                case OPEN: {
                    last = entry;
                    continue;
                }
                default: {
                    throw new IllegalStateException("Illegal open/close state detected.");
                }
            }
        }
    }
    
    public void putAll(final AbstractIntervalTree<TKey, TValue> other) {
        for (final Entry entry : other.entrySet()) {
            this.put(entry.left.key, entry.right.key, entry.getValue());
        }
    }
    
    public TValue get(final TKey key) {
        final EndPoint point = this.getEndPoint(key);
        if (point != null) {
            return point.value;
        }
        return null;
    }
    
    protected EndPoint getEndPoint(final TKey key) {
        final EndPoint ends = this.bounds.get(key);
        if (ends != null) {
            if (ends.state == State.CLOSE) {
                final Map.Entry<TKey, EndPoint> left = this.bounds.floorEntry(this.decrementKey(key));
                return (left != null) ? left.getValue() : null;
            }
            return ends;
        }
        else {
            final Map.Entry<TKey, EndPoint> left = this.bounds.floorEntry(key);
            if (left != null && left.getValue().state == State.OPEN) {
                return left.getValue();
            }
            return null;
        }
    }
    
    protected EndPoint getPreviousEndPoint(final TKey point, final boolean inclusive) {
        // 
        // This method could not be decompiled.
        // 
        // Original Bytecode:
        // 
        //     1: ifnull          41
        //     4: aload_0         /* this */
        //     5: getfield        com/comphenix/protocol/concurrency/AbstractIntervalTree.bounds:Ljava/util/NavigableMap;
        //     8: iload_2         /* inclusive */
        //     9: ifeq            16
        //    12: aload_1         /* point */
        //    13: goto            21
        //    16: aload_0         /* this */
        //    17: aload_1         /* point */
        //    18: invokevirtual   com/comphenix/protocol/concurrency/AbstractIntervalTree.decrementKey:(Ljava/lang/Comparable;)Ljava/lang/Comparable;
        //    21: invokeinterface java/util/NavigableMap.floorEntry:(Ljava/lang/Object;)Ljava/util/Map$Entry;
        //    26: astore_3        /* previous */
        //    27: aload_3         /* previous */
        //    28: ifnull          41
        //    31: aload_3         /* previous */
        //    32: invokeinterface java/util/Map$Entry.getValue:()Ljava/lang/Object;
        //    37: checkcast       Lcom/comphenix/protocol/concurrency/AbstractIntervalTree$EndPoint;
        //    40: areturn        
        //    41: aconst_null    
        //    42: areturn        
        //    Signature:
        //  (TTKey;Z)Lcom/comphenix/protocol/concurrency/AbstractIntervalTree$EndPoint; [from metadata: (TTKey;Z)Lcom/comphenix/protocol/concurrency/AbstractIntervalTree<TTKey;TTValue;>.EndPoint;]
        //  
        //    StackMapTable: 00 03 50 07 00 33 FF 00 04 00 03 07 00 02 07 00 57 01 00 02 07 00 33 07 00 57 13
        // 
        // The error that occurred was:
        // 
        // java.lang.UnsupportedOperationException: The requested operation is not supported.
        //     at com.strobel.util.ContractUtils.unsupported(ContractUtils.java:27)
        //     at com.strobel.assembler.metadata.TypeReference.getRawType(TypeReference.java:276)
        //     at com.strobel.assembler.metadata.TypeReference.getRawType(TypeReference.java:271)
        //     at com.strobel.assembler.metadata.TypeReference.makeGenericType(TypeReference.java:150)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:187)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:25)
        //     at com.strobel.assembler.metadata.ParameterizedType.accept(ParameterizedType.java:103)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visit(TypeSubstitutionVisitor.java:39)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:173)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:25)
        //     at com.strobel.assembler.metadata.ParameterizedType.accept(ParameterizedType.java:103)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visit(TypeSubstitutionVisitor.java:39)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:173)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:25)
        //     at com.strobel.assembler.metadata.ParameterizedType.accept(ParameterizedType.java:103)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visit(TypeSubstitutionVisitor.java:39)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitMethod(TypeSubstitutionVisitor.java:276)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferCall(TypeAnalysis.java:2591)
        //     at com.strobel.decompiler.ast.TypeAnalysis.doInferTypeForExpression(TypeAnalysis.java:1029)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferTypeForExpression(TypeAnalysis.java:803)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferTypeForExpression(TypeAnalysis.java:770)
        //     at com.strobel.decompiler.ast.TypeAnalysis.doInferTypeForExpression(TypeAnalysis.java:881)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferTypeForExpression(TypeAnalysis.java:803)
        //     at com.strobel.decompiler.ast.TypeAnalysis.runInference(TypeAnalysis.java:672)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferTypesForVariables(TypeAnalysis.java:586)
        //     at com.strobel.decompiler.ast.TypeAnalysis.runInference(TypeAnalysis.java:397)
        //     at com.strobel.decompiler.ast.TypeAnalysis.run(TypeAnalysis.java:96)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:109)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:42)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:214)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:99)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethodBody(AstBuilder.java:782)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethod(AstBuilder.java:675)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addTypeMembers(AstBuilder.java:552)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeCore(AstBuilder.java:519)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeNoCache(AstBuilder.java:161)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createType(AstBuilder.java:150)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addType(AstBuilder.java:125)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.buildAst(JavaLanguage.java:71)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.decompileType(JavaLanguage.java:59)
        //     at us.deathmarine.luyten.FileSaver.doSaveJarDecompiled(FileSaver.java:192)
        //     at us.deathmarine.luyten.FileSaver.access$300(FileSaver.java:45)
        //     at us.deathmarine.luyten.FileSaver$4.run(FileSaver.java:112)
        //     at java.lang.Thread.run(Unknown Source)
        // 
        throw new IllegalStateException("An error occurred while decompiling this method.");
    }
    
    protected EndPoint getNextEndPoint(final TKey point, final boolean inclusive) {
        // 
        // This method could not be decompiled.
        // 
        // Original Bytecode:
        // 
        //     1: ifnull          41
        //     4: aload_0         /* this */
        //     5: getfield        com/comphenix/protocol/concurrency/AbstractIntervalTree.bounds:Ljava/util/NavigableMap;
        //     8: iload_2         /* inclusive */
        //     9: ifeq            16
        //    12: aload_1         /* point */
        //    13: goto            21
        //    16: aload_0         /* this */
        //    17: aload_1         /* point */
        //    18: invokevirtual   com/comphenix/protocol/concurrency/AbstractIntervalTree.incrementKey:(Ljava/lang/Comparable;)Ljava/lang/Comparable;
        //    21: invokeinterface java/util/NavigableMap.ceilingEntry:(Ljava/lang/Object;)Ljava/util/Map$Entry;
        //    26: astore_3        /* next */
        //    27: aload_3         /* next */
        //    28: ifnull          41
        //    31: aload_3         /* next */
        //    32: invokeinterface java/util/Map$Entry.getValue:()Ljava/lang/Object;
        //    37: checkcast       Lcom/comphenix/protocol/concurrency/AbstractIntervalTree$EndPoint;
        //    40: areturn        
        //    41: aconst_null    
        //    42: areturn        
        //    Signature:
        //  (TTKey;Z)Lcom/comphenix/protocol/concurrency/AbstractIntervalTree$EndPoint; [from metadata: (TTKey;Z)Lcom/comphenix/protocol/concurrency/AbstractIntervalTree<TTKey;TTValue;>.EndPoint;]
        //  
        //    StackMapTable: 00 03 50 07 00 33 FF 00 04 00 03 07 00 02 07 00 57 01 00 02 07 00 33 07 00 57 13
        // 
        // The error that occurred was:
        // 
        // java.lang.UnsupportedOperationException: The requested operation is not supported.
        //     at com.strobel.util.ContractUtils.unsupported(ContractUtils.java:27)
        //     at com.strobel.assembler.metadata.TypeReference.getRawType(TypeReference.java:276)
        //     at com.strobel.assembler.metadata.TypeReference.getRawType(TypeReference.java:271)
        //     at com.strobel.assembler.metadata.TypeReference.makeGenericType(TypeReference.java:150)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:187)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:25)
        //     at com.strobel.assembler.metadata.ParameterizedType.accept(ParameterizedType.java:103)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visit(TypeSubstitutionVisitor.java:39)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:173)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:25)
        //     at com.strobel.assembler.metadata.ParameterizedType.accept(ParameterizedType.java:103)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visit(TypeSubstitutionVisitor.java:39)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:173)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitParameterizedType(TypeSubstitutionVisitor.java:25)
        //     at com.strobel.assembler.metadata.ParameterizedType.accept(ParameterizedType.java:103)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visit(TypeSubstitutionVisitor.java:39)
        //     at com.strobel.assembler.metadata.TypeSubstitutionVisitor.visitMethod(TypeSubstitutionVisitor.java:276)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferCall(TypeAnalysis.java:2591)
        //     at com.strobel.decompiler.ast.TypeAnalysis.doInferTypeForExpression(TypeAnalysis.java:1029)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferTypeForExpression(TypeAnalysis.java:803)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferTypeForExpression(TypeAnalysis.java:770)
        //     at com.strobel.decompiler.ast.TypeAnalysis.doInferTypeForExpression(TypeAnalysis.java:881)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferTypeForExpression(TypeAnalysis.java:803)
        //     at com.strobel.decompiler.ast.TypeAnalysis.runInference(TypeAnalysis.java:672)
        //     at com.strobel.decompiler.ast.TypeAnalysis.inferTypesForVariables(TypeAnalysis.java:586)
        //     at com.strobel.decompiler.ast.TypeAnalysis.runInference(TypeAnalysis.java:397)
        //     at com.strobel.decompiler.ast.TypeAnalysis.run(TypeAnalysis.java:96)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:109)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:42)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:214)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:99)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethodBody(AstBuilder.java:782)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethod(AstBuilder.java:675)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addTypeMembers(AstBuilder.java:552)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeCore(AstBuilder.java:519)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeNoCache(AstBuilder.java:161)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createType(AstBuilder.java:150)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addType(AstBuilder.java:125)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.buildAst(JavaLanguage.java:71)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.decompileType(JavaLanguage.java:59)
        //     at us.deathmarine.luyten.FileSaver.doSaveJarDecompiled(FileSaver.java:192)
        //     at us.deathmarine.luyten.FileSaver.access$300(FileSaver.java:45)
        //     at us.deathmarine.luyten.FileSaver$4.run(FileSaver.java:112)
        //     at java.lang.Thread.run(Unknown Source)
        // 
        throw new IllegalStateException("An error occurred while decompiling this method.");
    }
    
    private void invokeEntryAdded(final Entry added) {
        if (added != null) {
            this.onEntryAdded(added);
        }
    }
    
    private void invokeEntryAdded(final Set<Entry> added) {
        for (final Entry entry : added) {
            this.onEntryAdded(entry);
        }
    }
    
    private void invokeEntryRemoved(final Set<Entry> removed) {
        for (final Entry entry : removed) {
            this.onEntryRemoved(entry);
        }
    }
    
    protected void onEntryAdded(final Entry added) {
    }
    
    protected void onEntryRemoved(final Entry removed) {
    }
    
    protected abstract TKey decrementKey(final TKey p0);
    
    protected abstract TKey incrementKey(final TKey p0);
    
    protected enum State
    {
        OPEN, 
        CLOSE, 
        BOTH;
    }
    
    public class Entry implements Map.Entry<Range<TKey>, TValue>
    {
        private EndPoint left;
        private EndPoint right;
        
        Entry(final EndPoint left, final EndPoint right) {
            if (left == null) {
                throw new IllegalAccessError("left cannot be NUll");
            }
            if (right == null) {
                throw new IllegalAccessError("right cannot be NUll");
            }
            if (left.key.compareTo(right.key) > 0) {
                throw new IllegalArgumentException("Left key (" + left.key + ") cannot be greater than the right key (" + right.key + ")");
            }
            this.left = left;
            this.right = right;
        }
        
        @Override
        public Range<TKey> getKey() {
            return (Range<TKey>)Range.closed((Comparable)this.left.key, (Comparable)this.right.key);
        }
        
        @Override
        public TValue getValue() {
            return this.left.value;
        }
        
        @Override
        public TValue setValue(final TValue value) {
            final TValue old = this.left.value;
            this.left.value = value;
            this.right.value = value;
            return old;
        }
        
        @Override
        public boolean equals(final Object obj) {
            return obj == this || (obj instanceof Entry && Objects.equal((Object)this.left.key, (Object)((Entry)obj).left.key) && Objects.equal((Object)this.right.key, (Object)((Entry)obj).right.key) && Objects.equal((Object)this.left.value, (Object)((Entry)obj).left.value));
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(new Object[] { this.left.key, this.right.key, this.left.value });
        }
        
        @Override
        public String toString() {
            return String.format("Value %s at [%s, %s]", this.left.value, this.left.key, this.right.key);
        }
    }
    
    protected class EndPoint
    {
        public State state;
        public TValue value;
        public TKey key;
        
        public EndPoint(final State state, final TKey key, final TValue value) {
            this.state = state;
            this.key = key;
            this.value = value;
        }
    }
}
