package com.comphenix.net.sf.cglib.core;

import java.security.AccessController;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import com.comphenix.net.sf.cglib.asm.$ClassReader;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.security.PrivilegedAction;
import com.comphenix.net.sf.cglib.asm.$ClassWriter;
import java.lang.reflect.Constructor;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;

public class DebuggingClassWriter extends $ClassVisitor
{
    public static final String DEBUG_LOCATION_PROPERTY = "cglib.debugLocation";
    private static String debugLocation;
    private static Constructor traceCtor;
    private String className;
    private String superName;
    
    public DebuggingClassWriter(final int flags) {
        super(327680, new $ClassWriter(flags));
    }
    
    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.className = name.replace('/', '.');
        this.superName = superName.replace('/', '.');
        super.visit(version, access, name, signature, superName, interfaces);
    }
    
    public String getClassName() {
        return this.className;
    }
    
    public String getSuperName() {
        return this.superName;
    }
    
    public byte[] toByteArray() {
        return AccessController.doPrivileged((PrivilegedAction<byte[]>)new PrivilegedAction() {
            public Object run() {
                final byte[] b = (($ClassWriter)DebuggingClassWriter.this.cv).toByteArray();
                if (DebuggingClassWriter.debugLocation != null) {
                    final String dirs = DebuggingClassWriter.this.className.replace('.', File.separatorChar);
                    try {
                        new File(DebuggingClassWriter.debugLocation + File.separatorChar + dirs).getParentFile().mkdirs();
                        File file = new File(new File(DebuggingClassWriter.debugLocation), dirs + ".class");
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                        try {
                            out.write(b);
                        }
                        finally {
                            out.close();
                        }
                        if (DebuggingClassWriter.traceCtor != null) {
                            file = new File(new File(DebuggingClassWriter.debugLocation), dirs + ".asm");
                            out = new BufferedOutputStream(new FileOutputStream(file));
                            try {
                                final $ClassReader cr = new $ClassReader(b);
                                final PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
                                final $ClassVisitor tcv = DebuggingClassWriter.traceCtor.newInstance(null, pw);
                                cr.accept(tcv, 0);
                                pw.flush();
                            }
                            finally {
                                out.close();
                            }
                        }
                    }
                    catch (Exception e) {
                        throw new CodeGenerationException(e);
                    }
                }
                return b;
            }
        });
    }
    
    static {
        DebuggingClassWriter.debugLocation = System.getProperty("cglib.debugLocation");
        if (DebuggingClassWriter.debugLocation != null) {
            System.err.println("CGLIB debugging enabled, writing to '" + DebuggingClassWriter.debugLocation + "'");
            try {
                final Class clazz = Class.forName("com.comphenix.net.sf.cglib.asm.util.$TraceClassVisitor");
                DebuggingClassWriter.traceCtor = clazz.getConstructor($ClassVisitor.class, PrintWriter.class);
            }
            catch (Throwable t) {}
        }
    }
}
