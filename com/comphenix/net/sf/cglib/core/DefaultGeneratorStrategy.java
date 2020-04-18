package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$ClassWriter;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public class DefaultGeneratorStrategy implements GeneratorStrategy
{
    public static final DefaultGeneratorStrategy INSTANCE;
    
    public byte[] generate(final ClassGenerator cg) throws Exception {
        final DebuggingClassWriter cw = this.getClassVisitor();
        this.transform(cg).generateClass(cw);
        return this.transform(cw.toByteArray());
    }
    
    protected DebuggingClassWriter getClassVisitor() throws Exception {
        return new DebuggingClassWriter(2);
    }
    
    protected final $ClassWriter getClassWriter() {
        throw new UnsupportedOperationException("You are calling getClassWriter, which no longer exists in this cglib version.");
    }
    
    protected byte[] transform(final byte[] b) throws Exception {
        return b;
    }
    
    protected ClassGenerator transform(final ClassGenerator cg) throws Exception {
        return cg;
    }
    
    static {
        INSTANCE = new DefaultGeneratorStrategy();
    }
}
