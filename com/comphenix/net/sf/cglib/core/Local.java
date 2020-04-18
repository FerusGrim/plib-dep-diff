package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Type;

public class Local
{
    private $Type type;
    private int index;
    
    public Local(final int index, final $Type type) {
        this.type = type;
        this.index = index;
    }
    
    public int getIndex() {
        return this.index;
    }
    
    public $Type getType() {
        return this.type;
    }
}
