package com.comphenix.protocol.reflect;

public class FieldAccessException extends RuntimeException
{
    private static final long serialVersionUID = 1911011681494034617L;
    
    public FieldAccessException() {
    }
    
    public FieldAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    public FieldAccessException(final String message) {
        super(message);
    }
    
    public FieldAccessException(final Throwable cause) {
        super(cause);
    }
    
    public static FieldAccessException fromFormat(final String message, final Object... params) {
        return new FieldAccessException(String.format(message, params));
    }
    
    @Override
    public String toString() {
        final String message = this.getMessage();
        return "FieldAccessException" + ((message != null) ? (": " + message) : "");
    }
}
