package com.comphenix.net.sf.cglib.transform.impl;

import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.core.ReflectUtils;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.CodeGenerationException;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.transform.ClassEmitterTransformer;

public class AddDelegateTransformer extends ClassEmitterTransformer
{
    private static final String DELEGATE = "$CGLIB_DELEGATE";
    private static final Signature CSTRUCT_OBJECT;
    private Class[] delegateIf;
    private Class delegateImpl;
    private $Type delegateType;
    
    public AddDelegateTransformer(final Class[] delegateIf, final Class delegateImpl) {
        try {
            delegateImpl.getConstructor(Object.class);
            this.delegateIf = delegateIf;
            this.delegateImpl = delegateImpl;
            this.delegateType = $Type.getType(delegateImpl);
        }
        catch (NoSuchMethodException e) {
            throw new CodeGenerationException(e);
        }
    }
    
    @Override
    public void begin_class(final int version, final int access, final String className, final $Type superType, final $Type[] interfaces, final String sourceFile) {
        if (!TypeUtils.isInterface(access)) {
            final $Type[] all = TypeUtils.add(interfaces, TypeUtils.getTypes(this.delegateIf));
            super.begin_class(version, access, className, superType, all, sourceFile);
            this.declare_field(130, "$CGLIB_DELEGATE", this.delegateType, null);
            for (int i = 0; i < this.delegateIf.length; ++i) {
                final Method[] methods = this.delegateIf[i].getMethods();
                for (int j = 0; j < methods.length; ++j) {
                    if (Modifier.isAbstract(methods[j].getModifiers())) {
                        this.addDelegate(methods[j]);
                    }
                }
            }
        }
        else {
            super.begin_class(version, access, className, superType, interfaces, sourceFile);
        }
    }
    
    @Override
    public CodeEmitter begin_method(final int access, final Signature sig, final $Type[] exceptions) {
        final CodeEmitter e = super.begin_method(access, sig, exceptions);
        if (sig.getName().equals("<init>")) {
            return new CodeEmitter(e) {
                private boolean transformInit = true;
                
                @Override
                public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    if (this.transformInit && opcode == 183) {
                        this.load_this();
                        this.new_instance(AddDelegateTransformer.this.delegateType);
                        this.dup();
                        this.load_this();
                        this.invoke_constructor(AddDelegateTransformer.this.delegateType, AddDelegateTransformer.CSTRUCT_OBJECT);
                        this.putfield("$CGLIB_DELEGATE");
                        this.transformInit = false;
                    }
                }
            };
        }
        return e;
    }
    
    private void addDelegate(final Method m) {
        try {
            final Method delegate = this.delegateImpl.getMethod(m.getName(), (Class[])m.getParameterTypes());
            if (!delegate.getReturnType().getName().equals(m.getReturnType().getName())) {
                throw new IllegalArgumentException("Invalid delegate signature " + delegate);
            }
        }
        catch (NoSuchMethodException e) {
            throw new CodeGenerationException(e);
        }
        final Signature sig = ReflectUtils.getSignature(m);
        final $Type[] exceptions = TypeUtils.getTypes(m.getExceptionTypes());
        final CodeEmitter e2 = super.begin_method(1, sig, exceptions);
        e2.load_this();
        e2.getfield("$CGLIB_DELEGATE");
        e2.load_args();
        e2.invoke_virtual(this.delegateType, sig);
        e2.return_value();
        e2.end_method();
    }
    
    static {
        CSTRUCT_OBJECT = TypeUtils.parseSignature("void <init>(Object)");
    }
}
