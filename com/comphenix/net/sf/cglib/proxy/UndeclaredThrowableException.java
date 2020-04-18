package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.core.CodeGenerationException;

public class UndeclaredThrowableException extends CodeGenerationException
{
    public UndeclaredThrowableException(final Throwable t) {
        super(t);
    }
    
    public Throwable getUndeclaredThrowable() {
        return this.getCause();
    }
}
