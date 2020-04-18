package com.comphenix.protocol.async;

public interface AsyncRunnable extends Runnable
{
    int getID();
    
    boolean stop() throws InterruptedException;
    
    boolean isRunning();
    
    boolean isFinished();
}
