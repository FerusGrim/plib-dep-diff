package com.comphenix.protocol.wrappers.collection;

import java.util.Collection;
import java.util.Set;

public abstract class ConvertedSet<VInner, VOuter> extends ConvertedCollection<VInner, VOuter> implements Set<VOuter>
{
    public ConvertedSet(final Collection<VInner> inner) {
        super(inner);
    }
}
