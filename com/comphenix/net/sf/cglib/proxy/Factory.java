package com.comphenix.net.sf.cglib.proxy;

public interface Factory
{
    Object newInstance(final Callback p0);
    
    Object newInstance(final Callback[] p0);
    
    Object newInstance(final Class[] p0, final Object[] p1, final Callback[] p2);
    
    Callback getCallback(final int p0);
    
    void setCallback(final int p0, final Callback p1);
    
    void setCallbacks(final Callback[] p0);
    
    Callback[] getCallbacks();
}
