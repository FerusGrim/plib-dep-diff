package com.comphenix.protocol.utility;

import com.comphenix.net.sf.cglib.proxy.Enhancer;

public class EnhancerFactory
{
    private static EnhancerFactory INSTANCE;
    private ClassLoader loader;
    
    public EnhancerFactory() {
        this.loader = EnhancerFactory.class.getClassLoader();
    }
    
    public static EnhancerFactory getInstance() {
        return EnhancerFactory.INSTANCE;
    }
    
    public Enhancer createEnhancer() {
        final Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(this.loader);
        return enhancer;
    }
    
    public void setClassLoader(final ClassLoader loader) {
        this.loader = loader;
    }
    
    public ClassLoader getClassLoader() {
        return this.loader;
    }
    
    static {
        EnhancerFactory.INSTANCE = new EnhancerFactory();
    }
}
