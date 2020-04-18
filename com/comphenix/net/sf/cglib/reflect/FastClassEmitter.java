package com.comphenix.net.sf.cglib.reflect;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import com.comphenix.net.sf.cglib.core.Block;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import com.comphenix.net.sf.cglib.core.ProcessSwitchCallback;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.core.Constants;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.ObjectSwitchCallback;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.Transformer;
import com.comphenix.net.sf.cglib.core.MethodInfoTransformer;
import java.util.Arrays;
import com.comphenix.net.sf.cglib.core.DuplicatesPredicate;
import com.comphenix.net.sf.cglib.core.Predicate;
import java.util.Collection;
import com.comphenix.net.sf.cglib.core.CollectionUtils;
import java.util.List;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.util.ArrayList;
import com.comphenix.net.sf.cglib.core.VisibilityPredicate;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.core.ClassEmitter;

class FastClassEmitter extends ClassEmitter
{
    private static final Signature CSTRUCT_CLASS;
    private static final Signature METHOD_GET_INDEX;
    private static final Signature SIGNATURE_GET_INDEX;
    private static final Signature TO_STRING;
    private static final Signature CONSTRUCTOR_GET_INDEX;
    private static final Signature INVOKE;
    private static final Signature NEW_INSTANCE;
    private static final Signature GET_MAX_INDEX;
    private static final Signature GET_SIGNATURE_WITHOUT_RETURN_TYPE;
    private static final $Type FAST_CLASS;
    private static final $Type ILLEGAL_ARGUMENT_EXCEPTION;
    private static final $Type INVOCATION_TARGET_EXCEPTION;
    private static final $Type[] INVOCATION_TARGET_EXCEPTION_ARRAY;
    private static final int TOO_MANY_METHODS = 100;
    
    public FastClassEmitter(final $ClassVisitor v, final String className, final Class type) {
        super(v);
        final $Type base = $Type.getType(type);
        this.begin_class(46, 1, className, FastClassEmitter.FAST_CLASS, null, "<generated>");
        CodeEmitter e = this.begin_method(1, FastClassEmitter.CSTRUCT_CLASS, null);
        e.load_this();
        e.load_args();
        e.super_invoke_constructor(FastClassEmitter.CSTRUCT_CLASS);
        e.return_value();
        e.end_method();
        final VisibilityPredicate vp = new VisibilityPredicate(type, false);
        final List methods = ReflectUtils.addAllMethods(type, new ArrayList());
        CollectionUtils.filter(methods, vp);
        CollectionUtils.filter(methods, new DuplicatesPredicate());
        final List constructors = new ArrayList(Arrays.asList(type.getDeclaredConstructors()));
        CollectionUtils.filter(constructors, vp);
        this.emitIndexBySignature(methods);
        this.emitIndexByClassArray(methods);
        e = this.begin_method(1, FastClassEmitter.CONSTRUCTOR_GET_INDEX, null);
        e.load_args();
        final List info = CollectionUtils.transform(constructors, MethodInfoTransformer.getInstance());
        EmitUtils.constructor_switch(e, info, new GetIndexCallback(e, info));
        e.end_method();
        e = this.begin_method(1, FastClassEmitter.INVOKE, FastClassEmitter.INVOCATION_TARGET_EXCEPTION_ARRAY);
        e.load_arg(1);
        e.checkcast(base);
        e.load_arg(0);
        invokeSwitchHelper(e, methods, 2, base);
        e.end_method();
        e = this.begin_method(1, FastClassEmitter.NEW_INSTANCE, FastClassEmitter.INVOCATION_TARGET_EXCEPTION_ARRAY);
        e.new_instance(base);
        e.dup();
        e.load_arg(0);
        invokeSwitchHelper(e, constructors, 1, base);
        e.end_method();
        e = this.begin_method(1, FastClassEmitter.GET_MAX_INDEX, null);
        e.push(methods.size() - 1);
        e.return_value();
        e.end_method();
        this.end_class();
    }
    
    private void emitIndexBySignature(final List methods) {
        final CodeEmitter e = this.begin_method(1, FastClassEmitter.SIGNATURE_GET_INDEX, null);
        final List signatures = CollectionUtils.transform(methods, new Transformer() {
            public Object transform(final Object obj) {
                return ReflectUtils.getSignature((Member)obj).toString();
            }
        });
        e.load_arg(0);
        e.invoke_virtual(Constants.TYPE_OBJECT, FastClassEmitter.TO_STRING);
        this.signatureSwitchHelper(e, signatures);
        e.end_method();
    }
    
    private void emitIndexByClassArray(final List methods) {
        final CodeEmitter e = this.begin_method(1, FastClassEmitter.METHOD_GET_INDEX, null);
        if (methods.size() > 100) {
            final List signatures = CollectionUtils.transform(methods, new Transformer() {
                public Object transform(final Object obj) {
                    final String s = ReflectUtils.getSignature((Member)obj).toString();
                    return s.substring(0, s.lastIndexOf(41) + 1);
                }
            });
            e.load_args();
            e.invoke_static(FastClassEmitter.FAST_CLASS, FastClassEmitter.GET_SIGNATURE_WITHOUT_RETURN_TYPE);
            this.signatureSwitchHelper(e, signatures);
        }
        else {
            e.load_args();
            final List info = CollectionUtils.transform(methods, MethodInfoTransformer.getInstance());
            EmitUtils.method_switch(e, info, new GetIndexCallback(e, info));
        }
        e.end_method();
    }
    
    private void signatureSwitchHelper(final CodeEmitter e, final List signatures) {
        final ObjectSwitchCallback callback = new ObjectSwitchCallback() {
            public void processCase(final Object key, final $Label end) {
                e.push(signatures.indexOf(key));
                e.return_value();
            }
            
            public void processDefault() {
                e.push(-1);
                e.return_value();
            }
        };
        EmitUtils.string_switch(e, signatures.toArray(new String[signatures.size()]), 1, callback);
    }
    
    private static void invokeSwitchHelper(final CodeEmitter e, final List members, final int arg, final $Type base) {
        final List info = CollectionUtils.transform(members, MethodInfoTransformer.getInstance());
        final $Label illegalArg = e.make_label();
        final Block block = e.begin_block();
        e.process_switch(getIntRange(info.size()), new ProcessSwitchCallback() {
            public void processCase(final int key, final $Label end) {
                final MethodInfo method = info.get(key);
                final $Type[] types = method.getSignature().getArgumentTypes();
                for (int i = 0; i < types.length; ++i) {
                    e.load_arg(arg);
                    e.aaload(i);
                    e.unbox(types[i]);
                }
                e.invoke(method, base);
                if (!TypeUtils.isConstructor(method)) {
                    e.box(method.getSignature().getReturnType());
                }
                e.return_value();
            }
            
            public void processDefault() {
                e.goTo(illegalArg);
            }
        });
        block.end();
        EmitUtils.wrap_throwable(block, FastClassEmitter.INVOCATION_TARGET_EXCEPTION);
        e.mark(illegalArg);
        e.throw_exception(FastClassEmitter.ILLEGAL_ARGUMENT_EXCEPTION, "Cannot find matching method/constructor");
    }
    
    private static int[] getIntRange(final int length) {
        final int[] range = new int[length];
        for (int i = 0; i < length; ++i) {
            range[i] = i;
        }
        return range;
    }
    
    static {
        CSTRUCT_CLASS = TypeUtils.parseConstructor("Class");
        METHOD_GET_INDEX = TypeUtils.parseSignature("int getIndex(String, Class[])");
        SIGNATURE_GET_INDEX = new Signature("getIndex", $Type.INT_TYPE, new $Type[] { Constants.TYPE_SIGNATURE });
        TO_STRING = TypeUtils.parseSignature("String toString()");
        CONSTRUCTOR_GET_INDEX = TypeUtils.parseSignature("int getIndex(Class[])");
        INVOKE = TypeUtils.parseSignature("Object invoke(int, Object, Object[])");
        NEW_INSTANCE = TypeUtils.parseSignature("Object newInstance(int, Object[])");
        GET_MAX_INDEX = TypeUtils.parseSignature("int getMaxIndex()");
        GET_SIGNATURE_WITHOUT_RETURN_TYPE = TypeUtils.parseSignature("String getSignatureWithoutReturnType(String, Class[])");
        FAST_CLASS = TypeUtils.parseType("com.comphenix.net.sf.cglib.reflect.FastClass");
        ILLEGAL_ARGUMENT_EXCEPTION = TypeUtils.parseType("IllegalArgumentException");
        INVOCATION_TARGET_EXCEPTION = TypeUtils.parseType("java.lang.reflect.InvocationTargetException");
        INVOCATION_TARGET_EXCEPTION_ARRAY = new $Type[] { FastClassEmitter.INVOCATION_TARGET_EXCEPTION };
    }
    
    private static class GetIndexCallback implements ObjectSwitchCallback
    {
        private CodeEmitter e;
        private Map indexes;
        
        public GetIndexCallback(final CodeEmitter e, final List methods) {
            this.indexes = new HashMap();
            this.e = e;
            int index = 0;
            final Iterator it = methods.iterator();
            while (it.hasNext()) {
                this.indexes.put(it.next(), new Integer(index++));
            }
        }
        
        public void processCase(final Object key, final $Label end) {
            this.e.push(this.indexes.get(key));
            this.e.return_value();
        }
        
        public void processDefault() {
            this.e.push(-1);
            this.e.return_value();
        }
    }
}
