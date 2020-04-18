package com.comphenix.net.sf.cglib.transform;

import java.security.AccessController;
import java.security.PrivilegedAction;
import com.comphenix.net.sf.cglib.asm.$Attribute;
import com.comphenix.net.sf.cglib.core.ClassGenerator;
import java.io.InputStream;
import com.comphenix.net.sf.cglib.core.CodeGenerationException;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.DebuggingClassWriter;
import java.io.IOException;
import com.comphenix.net.sf.cglib.asm.$ClassReader;
import java.security.ProtectionDomain;

public abstract class AbstractClassLoader extends ClassLoader
{
    private ClassFilter filter;
    private ClassLoader classPath;
    private static ProtectionDomain DOMAIN;
    
    protected AbstractClassLoader(final ClassLoader parent, final ClassLoader classPath, final ClassFilter filter) {
        super(parent);
        this.filter = filter;
        this.classPath = classPath;
    }
    
    @Override
    public Class loadClass(final String name) throws ClassNotFoundException {
        final Class loaded = this.findLoadedClass(name);
        if (loaded != null && loaded.getClassLoader() == this) {
            return loaded;
        }
        if (!this.filter.accept(name)) {
            return super.loadClass(name);
        }
        $ClassReader r;
        try {
            final InputStream is = this.classPath.getResourceAsStream(name.replace('.', '/') + ".class");
            if (is == null) {
                throw new ClassNotFoundException(name);
            }
            try {
                r = new $ClassReader(is);
            }
            finally {
                is.close();
            }
        }
        catch (IOException e) {
            throw new ClassNotFoundException(name + ":" + e.getMessage());
        }
        try {
            final DebuggingClassWriter w = new DebuggingClassWriter(2);
            this.getGenerator(r).generateClass(w);
            final byte[] b = w.toByteArray();
            final Class c = super.defineClass(name, b, 0, b.length, AbstractClassLoader.DOMAIN);
            this.postProcess(c);
            return c;
        }
        catch (RuntimeException e2) {
            throw e2;
        }
        catch (Error e3) {
            throw e3;
        }
        catch (Exception e4) {
            throw new CodeGenerationException(e4);
        }
    }
    
    protected ClassGenerator getGenerator(final $ClassReader r) {
        return new ClassReaderGenerator(r, this.attributes(), this.getFlags());
    }
    
    protected int getFlags() {
        return 0;
    }
    
    protected $Attribute[] attributes() {
        return null;
    }
    
    protected void postProcess(final Class c) {
    }
    
    static {
        AbstractClassLoader.DOMAIN = AccessController.doPrivileged((PrivilegedAction<ProtectionDomain>)new PrivilegedAction() {
            public Object run() {
                return AbstractClassLoader.class.getProtectionDomain();
            }
        });
    }
}
