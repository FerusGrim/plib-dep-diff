package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.util.Iterator;
import java.util.Set;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import java.util.HashSet;
import java.util.List;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.Signature;

class LazyLoaderGenerator implements CallbackGenerator
{
    public static final LazyLoaderGenerator INSTANCE;
    private static final Signature LOAD_OBJECT;
    private static final $Type LAZY_LOADER;
    
    public void generate(final ClassEmitter ce, final Context context, final List methods) {
        final Set indexes = new HashSet();
        for (final MethodInfo method : methods) {
            if (TypeUtils.isProtected(method.getModifiers())) {
                continue;
            }
            final int index = context.getIndex(method);
            indexes.add(new Integer(index));
            final CodeEmitter e = context.beginMethod(ce, method);
            e.load_this();
            e.dup();
            e.invoke_virtual_this(this.loadMethod(index));
            e.checkcast(method.getClassInfo().getType());
            e.load_args();
            e.invoke(method);
            e.return_value();
            e.end_method();
        }
        for (final int index2 : indexes) {
            final String delegate = "CGLIB$LAZY_LOADER_" + index2;
            ce.declare_field(2, delegate, Constants.TYPE_OBJECT, null);
            final CodeEmitter e = ce.begin_method(50, this.loadMethod(index2), null);
            e.load_this();
            e.getfield(delegate);
            e.dup();
            final $Label end = e.make_label();
            e.ifnonnull(end);
            e.pop();
            e.load_this();
            context.emitCallback(e, index2);
            e.invoke_interface(LazyLoaderGenerator.LAZY_LOADER, LazyLoaderGenerator.LOAD_OBJECT);
            e.dup_x1();
            e.putfield(delegate);
            e.mark(end);
            e.return_value();
            e.end_method();
        }
    }
    
    private Signature loadMethod(final int index) {
        return new Signature("CGLIB$LOAD_PRIVATE_" + index, Constants.TYPE_OBJECT, Constants.TYPES_EMPTY);
    }
    
    public void generateStatic(final CodeEmitter e, final Context context, final List methods) {
    }
    
    static {
        INSTANCE = new LazyLoaderGenerator();
        LOAD_OBJECT = TypeUtils.parseSignature("Object loadObject()");
        LAZY_LOADER = TypeUtils.parseType("com.comphenix.net.sf.cglib.proxy.LazyLoader");
    }
}
