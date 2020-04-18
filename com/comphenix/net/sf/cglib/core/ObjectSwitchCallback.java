package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Label;

public interface ObjectSwitchCallback
{
    void processCase(final Object p0, final $Label p1) throws Exception;
    
    void processDefault() throws Exception;
}
