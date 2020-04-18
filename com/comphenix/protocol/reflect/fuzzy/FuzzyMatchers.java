package com.comphenix.protocol.reflect.fuzzy;

import java.lang.reflect.Member;
import java.util.regex.Pattern;
import java.util.Set;
import com.google.common.collect.Sets;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

public class FuzzyMatchers
{
    private static AbstractFuzzyMatcher<Class<?>> MATCH_ALL;
    
    private FuzzyMatchers() {
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchArray(@Nonnull final AbstractFuzzyMatcher<Class<?>> componentMatcher) {
        Preconditions.checkNotNull((Object)componentMatcher, (Object)"componentMatcher cannot be NULL.");
        return new AbstractFuzzyMatcher<Class<?>>() {
            @Override
            public boolean isMatch(final Class<?> value, final Object parent) {
                return value.isArray() && componentMatcher.isMatch(value.getComponentType(), parent);
            }
            
            @Override
            protected int calculateRoundNumber() {
                return -1;
            }
        };
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchAll() {
        return FuzzyMatchers.MATCH_ALL;
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchExact(final Class<?> matcher) {
        return new ClassExactMatcher(matcher, ClassExactMatcher.Options.MATCH_EXACT);
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchAnyOf(final Class<?>... classes) {
        return matchAnyOf(Sets.newHashSet((Object[])classes));
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchAnyOf(final Set<Class<?>> classes) {
        return new ClassSetMatcher(classes);
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchSuper(final Class<?> matcher) {
        return new ClassExactMatcher(matcher, ClassExactMatcher.Options.MATCH_SUPER);
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchDerived(final Class<?> matcher) {
        return new ClassExactMatcher(matcher, ClassExactMatcher.Options.MATCH_DERIVED);
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchRegex(final Pattern regex, final int priority) {
        return new ClassRegexMatcher(regex, priority);
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchRegex(final String regex, final int priority) {
        return matchRegex(Pattern.compile(regex), priority);
    }
    
    public static AbstractFuzzyMatcher<Class<?>> matchParent() {
        return new AbstractFuzzyMatcher<Class<?>>() {
            @Override
            public boolean isMatch(final Class<?> value, final Object parent) {
                if (parent instanceof Member) {
                    return ((Member)parent).getDeclaringClass().equals(value);
                }
                return parent instanceof Class && parent.equals(value);
            }
            
            @Override
            protected int calculateRoundNumber() {
                return -100;
            }
            
            @Override
            public String toString() {
                return "match parent class";
            }
            
            @Override
            public int hashCode() {
                return 0;
            }
            
            @Override
            public boolean equals(final Object obj) {
                return obj != null && obj.getClass() == this.getClass();
            }
        };
    }
    
    static boolean checkPattern(final Pattern a, final Pattern b) {
        if (a == null) {
            return b == null;
        }
        return b != null && a.pattern().equals(b.pattern());
    }
    
    static {
        FuzzyMatchers.MATCH_ALL = new AbstractFuzzyMatcher<Class<?>>() {
            @Override
            public boolean isMatch(final Class<?> value, final Object parent) {
                return true;
            }
            
            @Override
            protected int calculateRoundNumber() {
                return 0;
            }
        };
    }
}
