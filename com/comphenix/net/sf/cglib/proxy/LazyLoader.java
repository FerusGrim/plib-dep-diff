package com.comphenix.net.sf.cglib.proxy;

public interface LazyLoader extends Callback
{
    Object loadObject() throws Exception;
}
