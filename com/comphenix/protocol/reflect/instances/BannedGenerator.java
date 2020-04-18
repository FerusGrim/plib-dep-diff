package com.comphenix.protocol.reflect.instances;

import javax.annotation.Nullable;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMatchers;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;

public class BannedGenerator implements InstanceProvider
{
    private AbstractFuzzyMatcher<Class<?>> classMatcher;
    
    public BannedGenerator(final AbstractFuzzyMatcher<Class<?>> classMatcher) {
        this.classMatcher = classMatcher;
    }
    
    public BannedGenerator(final Class<?>... classes) {
        this.classMatcher = FuzzyMatchers.matchAnyOf(classes);
    }
    
    @Override
    public Object create(@Nullable final Class<?> type) {
        if (this.classMatcher.isMatch(type, null)) {
            throw new NotConstructableException();
        }
        return null;
    }
}
