package com.comphenix.protocol.injector.server;

public class TemporaryPlayer
{
    private volatile SocketInjector injector;
    
    SocketInjector getInjector() {
        return this.injector;
    }
    
    void setInjector(final SocketInjector injector) {
        if (injector == null) {
            throw new IllegalArgumentException("Injector cannot be NULL.");
        }
        this.injector = injector;
    }
}
