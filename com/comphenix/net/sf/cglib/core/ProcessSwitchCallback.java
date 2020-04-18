package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Label;

public interface ProcessSwitchCallback
{
    void processCase(final int p0, final $Label p1) throws Exception;
    
    void processDefault() throws Exception;
}
