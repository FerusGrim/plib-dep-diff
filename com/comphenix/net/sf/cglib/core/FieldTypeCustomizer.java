package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Type;

public interface FieldTypeCustomizer extends KeyFactoryCustomizer
{
    void customize(final CodeEmitter p0, final int p1, final $Type p2);
    
    $Type getOutType(final int p0, final $Type p1);
}
