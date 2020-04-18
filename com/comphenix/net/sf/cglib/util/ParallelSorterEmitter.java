package com.comphenix.net.sf.cglib.util;

import com.comphenix.net.sf.cglib.core.Local;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.ClassEmitter;

class ParallelSorterEmitter extends ClassEmitter
{
    private static final $Type PARALLEL_SORTER;
    private static final Signature CSTRUCT_OBJECT_ARRAY;
    private static final Signature NEW_INSTANCE;
    private static final Signature SWAP;
    
    public ParallelSorterEmitter(final $ClassVisitor v, final String className, final Object[] arrays) {
        super(v);
        this.begin_class(46, 1, className, ParallelSorterEmitter.PARALLEL_SORTER, null, "<generated>");
        EmitUtils.null_constructor(this);
        EmitUtils.factory_method(this, ParallelSorterEmitter.NEW_INSTANCE);
        this.generateConstructor(arrays);
        this.generateSwap(arrays);
        this.end_class();
    }
    
    private String getFieldName(final int index) {
        return "FIELD_" + index;
    }
    
    private void generateConstructor(final Object[] arrays) {
        final CodeEmitter e = this.begin_method(1, ParallelSorterEmitter.CSTRUCT_OBJECT_ARRAY, null);
        e.load_this();
        e.super_invoke_constructor();
        e.load_this();
        e.load_arg(0);
        e.super_putfield("a", Constants.TYPE_OBJECT_ARRAY);
        for (int i = 0; i < arrays.length; ++i) {
            final $Type type = $Type.getType(arrays[i].getClass());
            this.declare_field(2, this.getFieldName(i), type, null);
            e.load_this();
            e.load_arg(0);
            e.push(i);
            e.aaload();
            e.checkcast(type);
            e.putfield(this.getFieldName(i));
        }
        e.return_value();
        e.end_method();
    }
    
    private void generateSwap(final Object[] arrays) {
        final CodeEmitter e = this.begin_method(1, ParallelSorterEmitter.SWAP, null);
        for (int i = 0; i < arrays.length; ++i) {
            final $Type type = $Type.getType(arrays[i].getClass());
            final $Type component = TypeUtils.getComponentType(type);
            final Local T = e.make_local(type);
            e.load_this();
            e.getfield(this.getFieldName(i));
            e.store_local(T);
            e.load_local(T);
            e.load_arg(0);
            e.load_local(T);
            e.load_arg(1);
            e.array_load(component);
            e.load_local(T);
            e.load_arg(1);
            e.load_local(T);
            e.load_arg(0);
            e.array_load(component);
            e.array_store(component);
            e.array_store(component);
        }
        e.return_value();
        e.end_method();
    }
    
    static {
        PARALLEL_SORTER = TypeUtils.parseType("com.comphenix.net.sf.cglib.util.ParallelSorter");
        CSTRUCT_OBJECT_ARRAY = TypeUtils.parseConstructor("Object[]");
        NEW_INSTANCE = new Signature("newInstance", ParallelSorterEmitter.PARALLEL_SORTER, new $Type[] { Constants.TYPE_OBJECT_ARRAY });
        SWAP = TypeUtils.parseSignature("void swap(int, int)");
    }
}
