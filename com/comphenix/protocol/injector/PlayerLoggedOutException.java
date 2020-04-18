package com.comphenix.protocol.injector;

public class PlayerLoggedOutException extends RuntimeException
{
    private static final long serialVersionUID = 4889257862160145234L;
    
    public PlayerLoggedOutException() {
        super("Cannot inject a player that has already logged out.");
    }
    
    public PlayerLoggedOutException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    public PlayerLoggedOutException(final String message) {
        super(message);
    }
    
    public PlayerLoggedOutException(final Throwable cause) {
        super(cause);
    }
    
    public static PlayerLoggedOutException fromFormat(final String message, final Object... params) {
        return new PlayerLoggedOutException(String.format(message, params));
    }
}
