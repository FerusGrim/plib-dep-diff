package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.core.MethodInfo;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import java.util.List;
import com.comphenix.net.sf.cglib.core.ClassEmitter;

interface CallbackGenerator
{
    void generate(final ClassEmitter p0, final Context p1, final List p2) throws Exception;
    
    void generateStatic(final CodeEmitter p0, final Context p1, final List p2) throws Exception;
    
    public interface Context
    {
        ClassLoader getClassLoader();
        
        CodeEmitter beginMethod(final ClassEmitter p0, final MethodInfo p1);
        
        int getOriginalModifiers(final MethodInfo p0);
        
        int getIndex(final MethodInfo p0);
        
        void emitCallback(final CodeEmitter p0, final int p1);
        
        Signature getImplSignature(final MethodInfo p0);
        
        void emitLoadArgsAndInvoke(final CodeEmitter p0, final MethodInfo p1);
    }
}
