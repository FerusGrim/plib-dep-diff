package com.comphenix.protocol.reflect;

import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.google.common.collect.Lists;
import com.comphenix.net.sf.cglib.asm.$ClassReader;
import java.io.IOException;
import java.util.List;
import java.lang.reflect.Method;

public class ClassAnalyser
{
    private static final ClassAnalyser DEFAULT;
    
    public static ClassAnalyser getDefault() {
        return ClassAnalyser.DEFAULT;
    }
    
    public List<AsmMethod> getMethodCalls(final Method method) throws IOException {
        return this.getMethodCalls(method.getDeclaringClass(), method);
    }
    
    private List<AsmMethod> getMethodCalls(final Class<?> clazz, final Method method) throws IOException {
        final $ClassReader reader = new $ClassReader(clazz.getCanonicalName());
        final List<AsmMethod> output = (List<AsmMethod>)Lists.newArrayList();
        final String methodName = method.getName();
        final String methodDescription = $Type.getMethodDescriptor(method);
        reader.accept(new $ClassVisitor(327680) {
            @Override
            public $MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                if (methodName.equals(name) && methodDescription.equals(desc)) {
                    return new $MethodVisitor(327680) {
                        @Override
                        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean flag) {
                            output.add(new AsmMethod(AsmMethod.AsmOpcodes.fromIntOpcode(opcode), owner, methodName, desc));
                        }
                    };
                }
                return null;
            }
        }, 8);
        return output;
    }
    
    static {
        DEFAULT = new ClassAnalyser();
    }
    
    public static class AsmMethod
    {
        private final AsmOpcodes opcode;
        private final String ownerClass;
        private final String methodName;
        private final String signature;
        
        public AsmMethod(final AsmOpcodes opcode, final String ownerClass, final String methodName, final String signature) {
            this.opcode = opcode;
            this.ownerClass = ownerClass;
            this.methodName = methodName;
            this.signature = signature;
        }
        
        public String getOwnerName() {
            return this.ownerClass;
        }
        
        public AsmOpcodes getOpcode() {
            return this.opcode;
        }
        
        public Class<?> getOwnerClass() throws ClassNotFoundException {
            return AsmMethod.class.getClassLoader().loadClass(this.getOwnerName().replace('/', '.'));
        }
        
        public String getMethodName() {
            return this.methodName;
        }
        
        public String getSignature() {
            return this.signature;
        }
        
        public enum AsmOpcodes
        {
            INVOKE_VIRTUAL, 
            INVOKE_SPECIAL, 
            INVOKE_STATIC, 
            INVOKE_INTERFACE, 
            INVOKE_DYNAMIC;
            
            public static AsmOpcodes fromIntOpcode(final int opcode) {
                switch (opcode) {
                    case 182: {
                        return AsmOpcodes.INVOKE_VIRTUAL;
                    }
                    case 183: {
                        return AsmOpcodes.INVOKE_SPECIAL;
                    }
                    case 184: {
                        return AsmOpcodes.INVOKE_STATIC;
                    }
                    case 185: {
                        return AsmOpcodes.INVOKE_INTERFACE;
                    }
                    case 186: {
                        return AsmOpcodes.INVOKE_DYNAMIC;
                    }
                    default: {
                        throw new IllegalArgumentException("Unknown opcode: " + opcode);
                    }
                }
            }
        }
    }
}
