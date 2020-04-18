package com.comphenix.protocol.reflect.instances;

public class NotConstructableException extends IllegalArgumentException
{
    private static final long serialVersionUID = -1144171604744845463L;
    
    public NotConstructableException() {
        super("This object should never be constructed.");
    }
    
    public NotConstructableException(final String message) {
        super(message);
    }
    
    public NotConstructableException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    public NotConstructableException(final Throwable cause) {
        super(cause);
    }
}
