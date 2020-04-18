package com.comphenix.net.sf.cglib.proxy;

public interface NoOp extends Callback
{
    public static final NoOp INSTANCE = new NoOp() {};
}
