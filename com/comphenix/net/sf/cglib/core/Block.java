package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Label;

public class Block
{
    private CodeEmitter e;
    private $Label start;
    private $Label end;
    
    public Block(final CodeEmitter e) {
        this.e = e;
        this.start = e.mark();
    }
    
    public CodeEmitter getCodeEmitter() {
        return this.e;
    }
    
    public void end() {
        if (this.end != null) {
            throw new IllegalStateException("end of label already set");
        }
        this.end = this.e.mark();
    }
    
    public $Label getStart() {
        return this.start;
    }
    
    public $Label getEnd() {
        return this.end;
    }
}
