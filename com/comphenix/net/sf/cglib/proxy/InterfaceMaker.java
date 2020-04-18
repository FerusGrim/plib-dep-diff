package com.comphenix.net.sf.cglib.proxy;

import java.util.Iterator;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.Signature;
import java.util.HashMap;
import java.util.Map;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;

public class InterfaceMaker extends AbstractClassGenerator
{
    private static final Source SOURCE;
    private Map signatures;
    
    public InterfaceMaker() {
        super(InterfaceMaker.SOURCE);
        this.signatures = new HashMap();
    }
    
    public void add(final Signature sig, final $Type[] exceptions) {
        this.signatures.put(sig, exceptions);
    }
    
    public void add(final Method method) {
        this.add(ReflectUtils.getSignature(method), ReflectUtils.getExceptionTypes(method));
    }
    
    public void add(final Class clazz) {
        final Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; ++i) {
            final Method m = methods[i];
            if (!m.getDeclaringClass().getName().equals("java.lang.Object")) {
                this.add(m);
            }
        }
    }
    
    public Class create() {
        this.setUseCache(false);
        return (Class)super.create(this);
    }
    
    @Override
    protected ClassLoader getDefaultClassLoader() {
        return null;
    }
    
    @Override
    protected Object firstInstance(final Class type) {
        return type;
    }
    
    @Override
    protected Object nextInstance(final Object instance) {
        throw new IllegalStateException("InterfaceMaker does not cache");
    }
    
    public void generateClass(final $ClassVisitor v) throws Exception {
        final ClassEmitter ce = new ClassEmitter(v);
        ce.begin_class(46, 513, this.getClassName(), null, null, "<generated>");
        for (final Signature sig : this.signatures.keySet()) {
            final $Type[] exceptions = this.signatures.get(sig);
            ce.begin_method(1025, sig, exceptions).end_method();
        }
        ce.end_class();
    }
    
    static {
        SOURCE = new Source(InterfaceMaker.class.getName());
    }
}
