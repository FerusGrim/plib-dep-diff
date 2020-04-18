package com.comphenix.protocol.timing;

public abstract class OnlineComputation
{
    public abstract int getCount();
    
    public abstract void observe(final double p0);
    
    public abstract OnlineComputation copy();
    
    public static OnlineComputation synchronizedComputation(final OnlineComputation computation) {
        return new OnlineComputation() {
            @Override
            public synchronized void observe(final double value) {
                computation.observe(value);
            }
            
            @Override
            public synchronized int getCount() {
                return computation.getCount();
            }
            
            @Override
            public synchronized OnlineComputation copy() {
                return computation.copy();
            }
        };
    }
}
