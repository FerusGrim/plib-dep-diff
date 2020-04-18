package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.asm.$Attribute;
import com.comphenix.net.sf.cglib.asm.$ClassReader;
import com.comphenix.net.sf.cglib.core.ClassGenerator;

public class ClassReaderGenerator implements ClassGenerator
{
    private final $ClassReader r;
    private final $Attribute[] attrs;
    private final int flags;
    
    public ClassReaderGenerator(final $ClassReader r, final int flags) {
        this(r, null, flags);
    }
    
    public ClassReaderGenerator(final $ClassReader r, final $Attribute[] attrs, final int flags) {
        this.r = r;
        this.attrs = ((attrs != null) ? attrs : new $Attribute[0]);
        this.flags = flags;
    }
    
    public void generateClass(final $ClassVisitor v) {
        this.r.accept(v, this.attrs, this.flags);
    }
}
