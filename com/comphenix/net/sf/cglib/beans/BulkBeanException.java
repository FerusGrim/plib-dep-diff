package com.comphenix.net.sf.cglib.beans;

public class BulkBeanException extends RuntimeException
{
    private int index;
    private Throwable cause;
    
    public BulkBeanException(final String message, final int index) {
        super(message);
        this.index = index;
    }
    
    public BulkBeanException(final Throwable cause, final int index) {
        super(cause.getMessage());
        this.index = index;
        this.cause = cause;
    }
    
    public int getIndex() {
        return this.index;
    }
    
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
