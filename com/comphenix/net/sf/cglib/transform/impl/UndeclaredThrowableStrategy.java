package com.comphenix.net.sf.cglib.transform.impl;

import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.transform.ClassTransformer;
import com.comphenix.net.sf.cglib.transform.TransformingClassGenerator;
import com.comphenix.net.sf.cglib.transform.MethodFilterTransformer;
import com.comphenix.net.sf.cglib.core.ClassGenerator;
import com.comphenix.net.sf.cglib.transform.MethodFilter;
import com.comphenix.net.sf.cglib.core.DefaultGeneratorStrategy;

public class UndeclaredThrowableStrategy extends DefaultGeneratorStrategy
{
    private Class wrapper;
    private static final MethodFilter TRANSFORM_FILTER;
    
    public UndeclaredThrowableStrategy(final Class wrapper) {
        this.wrapper = wrapper;
    }
    
    @Override
    protected ClassGenerator transform(final ClassGenerator cg) throws Exception {
        ClassTransformer tr = new UndeclaredThrowableTransformer(this.wrapper);
        tr = new MethodFilterTransformer(UndeclaredThrowableStrategy.TRANSFORM_FILTER, tr);
        return new TransformingClassGenerator(cg, tr);
    }
    
    static {
        TRANSFORM_FILTER = new MethodFilter() {
            public boolean accept(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                return !TypeUtils.isPrivate(access) && name.indexOf(36) < 0;
            }
        };
    }
}
