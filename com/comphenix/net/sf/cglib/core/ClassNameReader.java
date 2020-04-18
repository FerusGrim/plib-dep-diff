package com.comphenix.net.sf.cglib.core;

import java.util.List;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import java.util.ArrayList;
import com.comphenix.net.sf.cglib.asm.$ClassReader;

public class ClassNameReader
{
    private static final EarlyExitException EARLY_EXIT;
    
    private ClassNameReader() {
    }
    
    public static String getClassName(final $ClassReader r) {
        return getClassInfo(r)[0];
    }
    
    public static String[] getClassInfo(final $ClassReader r) {
        final List array = new ArrayList();
        try {
            r.accept(new $ClassVisitor(327680, null) {
                @Override
                public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
                    array.add(name.replace('/', '.'));
                    if (superName != null) {
                        array.add(superName.replace('/', '.'));
                    }
                    for (int i = 0; i < interfaces.length; ++i) {
                        array.add(interfaces[i].replace('/', '.'));
                    }
                    throw ClassNameReader.EARLY_EXIT;
                }
            }, 6);
        }
        catch (EarlyExitException ex) {}
        return array.toArray(new String[0]);
    }
    
    static {
        EARLY_EXIT = new EarlyExitException();
    }
    
    private static class EarlyExitException extends RuntimeException
    {
    }
}
