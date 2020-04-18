package com.comphenix.net.sf.cglib.core;

public class CodeGenerationException extends RuntimeException
{
    private Throwable cause;
    
    public CodeGenerationException(final Throwable cause) {
        super(cause.getClass().getName() + "-->" + cause.getMessage());
        this.cause = cause;
    }
    
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
