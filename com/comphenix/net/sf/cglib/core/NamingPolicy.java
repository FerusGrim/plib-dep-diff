package com.comphenix.net.sf.cglib.core;

public interface NamingPolicy
{
    String getClassName(final String p0, final String p1, final Object p2, final Predicate p3);
    
    boolean equals(final Object p0);
}
