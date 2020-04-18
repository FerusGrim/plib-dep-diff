package com.comphenix.net.sf.cglib.transform;

import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public class ClassTransformerChain extends AbstractClassTransformer
{
    private ClassTransformer[] chain;
    
    public ClassTransformerChain(final ClassTransformer[] chain) {
        this.chain = chain.clone();
    }
    
    @Override
    public void setTarget(final $ClassVisitor v) {
        super.setTarget(this.chain[0]);
        $ClassVisitor next = v;
        for (int i = this.chain.length - 1; i >= 0; --i) {
            this.chain[i].setTarget(next);
            next = this.chain[i];
        }
    }
    
    @Override
    public $MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        return this.cv.visitMethod(access, name, desc, signature, exceptions);
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("ClassTransformerChain{");
        for (int i = 0; i < this.chain.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.chain[i].toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
