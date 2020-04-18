package com.comphenix.net.sf.cglib.core;

public interface GeneratorStrategy
{
    byte[] generate(final ClassGenerator p0) throws Exception;
    
    boolean equals(final Object p0);
}
