package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.core.ClassGenerator;
import com.comphenix.net.sf.cglib.asm.$ClassReader;

public class TransformingClassLoader extends AbstractClassLoader
{
    private ClassTransformerFactory t;
    
    public TransformingClassLoader(final ClassLoader parent, final ClassFilter filter, final ClassTransformerFactory t) {
        super(parent, parent, filter);
        this.t = t;
    }
    
    @Override
    protected ClassGenerator getGenerator(final $ClassReader r) {
        final ClassTransformer t2 = this.t.newInstance();
        return new TransformingClassGenerator(super.getGenerator(r), t2);
    }
}
