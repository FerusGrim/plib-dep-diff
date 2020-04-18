package com.comphenix.protocol.injector;

public enum GamePhase
{
    LOGIN, 
    PLAYING, 
    BOTH;
    
    public boolean hasLogin() {
        return this == GamePhase.LOGIN || this == GamePhase.BOTH;
    }
    
    public boolean hasPlaying() {
        return this == GamePhase.PLAYING || this == GamePhase.BOTH;
    }
}
