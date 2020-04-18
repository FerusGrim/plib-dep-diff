package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.comphenix.net.sf.cglib.core.Signature;
import java.util.Iterator;
import java.io.IOException;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.asm.$ClassReader;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

class BridgeMethodResolver
{
    private final Map declToBridge;
    private final ClassLoader classLoader;
    
    public BridgeMethodResolver(final Map declToBridge, final ClassLoader classLoader) {
        this.declToBridge = declToBridge;
        this.classLoader = classLoader;
    }
    
    public Map resolveAll() {
        final Map resolved = new HashMap();
        for (final Map.Entry entry : this.declToBridge.entrySet()) {
            final Class owner = entry.getKey();
            final Set bridges = entry.getValue();
            try {
                new $ClassReader(this.classLoader.getResourceAsStream(owner.getName().replace('.', '/') + ".class")).accept(new BridgedFinder(bridges, resolved), 6);
            }
            catch (IOException ex) {}
        }
        return resolved;
    }
    
    private static class BridgedFinder extends $ClassVisitor
    {
        private Map resolved;
        private Set eligibleMethods;
        private Signature currentMethod;
        
        BridgedFinder(final Set eligibleMethods, final Map resolved) {
            super(327680);
            this.currentMethod = null;
            this.resolved = resolved;
            this.eligibleMethods = eligibleMethods;
        }
        
        @Override
        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        }
        
        @Override
        public $MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
            final Signature sig = new Signature(name, desc);
            if (this.eligibleMethods.remove(sig)) {
                this.currentMethod = sig;
                return new $MethodVisitor(327680) {
                    @Override
                    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
                        if (opcode == 183 && BridgedFinder.this.currentMethod != null) {
                            final Signature target = new Signature(name, desc);
                            if (!target.equals(BridgedFinder.this.currentMethod)) {
                                BridgedFinder.this.resolved.put(BridgedFinder.this.currentMethod, target);
                            }
                            BridgedFinder.this.currentMethod = null;
                        }
                    }
                };
            }
            return null;
        }
    }
}
