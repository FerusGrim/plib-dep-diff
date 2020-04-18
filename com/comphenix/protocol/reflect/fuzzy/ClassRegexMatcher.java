package com.comphenix.protocol.reflect.fuzzy;

import com.google.common.base.Objects;
import java.util.regex.Pattern;

class ClassRegexMatcher extends AbstractFuzzyMatcher<Class<?>>
{
    private final Pattern regex;
    private final int priority;
    
    public ClassRegexMatcher(final Pattern regex, final int priority) {
        if (regex == null) {
            throw new IllegalArgumentException("Regular expression pattern cannot be NULL.");
        }
        this.regex = regex;
        this.priority = priority;
    }
    
    @Override
    public boolean isMatch(final Class<?> value, final Object parent) {
        return value != null && this.regex.matcher(value.getCanonicalName()).matches();
    }
    
    @Override
    protected int calculateRoundNumber() {
        return -this.priority;
    }
    
    @Override
    public String toString() {
        return "class name of " + this.regex.toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.regex, this.priority });
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ClassRegexMatcher) {
            final ClassRegexMatcher other = (ClassRegexMatcher)obj;
            return this.priority == other.priority && FuzzyMatchers.checkPattern(this.regex, other.regex);
        }
        return false;
    }
}
