package com.comphenix.net.sf.cglib.transform.impl;

import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.Block;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.Signature;
import java.lang.reflect.Constructor;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.transform.ClassEmitterTransformer;

public class UndeclaredThrowableTransformer extends ClassEmitterTransformer
{
    private $Type wrapper;
    
    public UndeclaredThrowableTransformer(final Class wrapper) {
        this.wrapper = $Type.getType(wrapper);
        boolean found = false;
        final Constructor[] cstructs = wrapper.getConstructors();
        for (int i = 0; i < cstructs.length; ++i) {
            final Class[] types = cstructs[i].getParameterTypes();
            if (types.length == 1 && types[0].equals(Throwable.class)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException(wrapper + " does not have a single-arg constructor that takes a Throwable");
        }
    }
    
    @Override
    public CodeEmitter begin_method(final int access, final Signature sig, final $Type[] exceptions) {
        final CodeEmitter e = super.begin_method(access, sig, exceptions);
        if (TypeUtils.isAbstract(access) || sig.equals(Constants.SIG_STATIC)) {
            return e;
        }
        return new CodeEmitter(e) {
            private Block handler = this.begin_block();
            
            @Override
            public void visitMaxs(final int maxStack, final int maxLocals) {
                this.handler.end();
                EmitUtils.wrap_undeclared_throwable(this, this.handler, exceptions, UndeclaredThrowableTransformer.this.wrapper);
                super.visitMaxs(maxStack, maxLocals);
            }
        };
    }
}
