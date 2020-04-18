package com.comphenix.net.sf.cglib.transform.impl;

import com.comphenix.net.sf.cglib.asm.$Type;

public interface InterceptFieldFilter
{
    boolean acceptRead(final $Type p0, final String p1);
    
    boolean acceptWrite(final $Type p0, final String p1);
}
