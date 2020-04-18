package com.comphenix.protocol.reflect.fuzzy;

import com.google.common.base.Objects;

class ClassExactMatcher extends AbstractFuzzyMatcher<Class<?>>
{
    public static final ClassExactMatcher MATCH_ALL;
    private final Class<?> matcher;
    private final Options option;
    
    ClassExactMatcher(final Class<?> matcher, final Options option) {
        this.matcher = matcher;
        this.option = option;
    }
    
    @Override
    public boolean isMatch(final Class<?> input, final Object parent) {
        if (input == null) {
            throw new IllegalArgumentException("Input class cannot be NULL.");
        }
        if (this.matcher == null) {
            return this.option != Options.MATCH_EXACT;
        }
        if (this.option == Options.MATCH_SUPER) {
            return input.isAssignableFrom(this.matcher);
        }
        if (this.option == Options.MATCH_DERIVED) {
            return this.matcher.isAssignableFrom(input);
        }
        return input.equals(this.matcher);
    }
    
    @Override
    protected int calculateRoundNumber() {
        return -getClassNumber(this.matcher);
    }
    
    public static int getClassNumber(Class<?> clazz) {
        int count = 0;
        while (clazz != null) {
            ++count;
            clazz = clazz.getSuperclass();
        }
        return count;
    }
    
    public Class<?> getMatcher() {
        return this.matcher;
    }
    
    public Options getOptions() {
        return this.option;
    }
    
    @Override
    public String toString() {
        if (this.option == Options.MATCH_SUPER) {
            return this.matcher + " instanceof input";
        }
        if (this.option == Options.MATCH_DERIVED) {
            return "input instanceof " + this.matcher;
        }
        return "Exact " + this.matcher;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.matcher, this.option });
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ClassExactMatcher) {
            final ClassExactMatcher other = (ClassExactMatcher)obj;
            return Objects.equal((Object)this.matcher, (Object)other.matcher) && Objects.equal((Object)this.option, (Object)other.option);
        }
        return false;
    }
    
    static {
        MATCH_ALL = new ClassExactMatcher(null, Options.MATCH_SUPER);
    }
    
    enum Options
    {
        MATCH_EXACT, 
        MATCH_SUPER, 
        MATCH_DERIVED;
    }
}
