package com.comphenix.protocol.reflect.fuzzy;

import com.google.common.base.Objects;
import java.util.Iterator;
import java.util.Set;

class ClassSetMatcher extends AbstractFuzzyMatcher<Class<?>>
{
    private final Set<Class<?>> classes;
    
    public ClassSetMatcher(final Set<Class<?>> classes) {
        if (classes == null) {
            throw new IllegalArgumentException("Set of classes cannot be NULL.");
        }
        this.classes = classes;
    }
    
    @Override
    public boolean isMatch(final Class<?> value, final Object parent) {
        return this.classes.contains(value);
    }
    
    @Override
    protected int calculateRoundNumber() {
        int roundNumber = 0;
        for (final Class<?> clazz : this.classes) {
            roundNumber = this.combineRounds(roundNumber, -ClassExactMatcher.getClassNumber(clazz));
        }
        return roundNumber;
    }
    
    @Override
    public String toString() {
        return "match any: " + this.classes;
    }
    
    @Override
    public int hashCode() {
        return this.classes.hashCode();
    }
    
    @Override
    public boolean equals(final Object obj) {
        return this == obj || !(obj instanceof ClassSetMatcher) || Objects.equal((Object)this.classes, (Object)((ClassSetMatcher)obj).classes);
    }
}
