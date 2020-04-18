package com.comphenix.protocol.reflect.fuzzy;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;
import java.lang.reflect.Member;
import com.google.common.base.Objects;
import java.util.Map;
import java.lang.reflect.Field;

public class FuzzyFieldContract extends AbstractFuzzyMember<Field>
{
    private AbstractFuzzyMatcher<Class<?>> typeMatcher;
    
    public static FuzzyFieldContract matchType(final AbstractFuzzyMatcher<Class<?>> matcher) {
        return newBuilder().typeMatches(matcher).build();
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    private FuzzyFieldContract() {
        this.typeMatcher = ClassExactMatcher.MATCH_ALL;
    }
    
    public AbstractFuzzyMatcher<Class<?>> getTypeMatcher() {
        return this.typeMatcher;
    }
    
    private FuzzyFieldContract(final FuzzyFieldContract other) {
        super(other);
        this.typeMatcher = ClassExactMatcher.MATCH_ALL;
        this.typeMatcher = other.typeMatcher;
    }
    
    @Override
    public boolean isMatch(final Field value, final Object parent) {
        return super.isMatch(value, parent) && this.typeMatcher.isMatch(value.getType(), value);
    }
    
    @Override
    protected int calculateRoundNumber() {
        return this.combineRounds(super.calculateRoundNumber(), this.typeMatcher.calculateRoundNumber());
    }
    
    @Override
    protected Map<String, Object> getKeyValueView() {
        final Map<String, Object> member = super.getKeyValueView();
        if (this.typeMatcher != ClassExactMatcher.MATCH_ALL) {
            member.put("type", this.typeMatcher);
        }
        return member;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.typeMatcher, super.hashCode() });
    }
    
    @Override
    public boolean equals(final Object obj) {
        return this == obj || !(obj instanceof FuzzyFieldContract) || !super.equals(obj) || Objects.equal((Object)this.typeMatcher, (Object)((FuzzyFieldContract)obj).typeMatcher);
    }
    
    public static class Builder extends AbstractFuzzyMember.Builder<FuzzyFieldContract>
    {
        @Override
        public Builder requireModifier(final int modifier) {
            super.requireModifier(modifier);
            return this;
        }
        
        @Override
        public Builder banModifier(final int modifier) {
            super.banModifier(modifier);
            return this;
        }
        
        @Override
        public Builder requirePublic() {
            super.requirePublic();
            return this;
        }
        
        @Override
        public Builder nameRegex(final String regex) {
            super.nameRegex(regex);
            return this;
        }
        
        @Override
        public Builder nameRegex(final Pattern pattern) {
            super.nameRegex(pattern);
            return this;
        }
        
        @Override
        public Builder nameExact(final String name) {
            super.nameExact(name);
            return this;
        }
        
        @Override
        public Builder declaringClassExactType(final Class<?> declaringClass) {
            super.declaringClassExactType(declaringClass);
            return this;
        }
        
        @Override
        public Builder declaringClassSuperOf(final Class<?> declaringClass) {
            super.declaringClassSuperOf(declaringClass);
            return this;
        }
        
        @Override
        public Builder declaringClassDerivedOf(final Class<?> declaringClass) {
            super.declaringClassDerivedOf(declaringClass);
            return this;
        }
        
        @Override
        public Builder declaringClassMatching(final AbstractFuzzyMatcher<Class<?>> classMatcher) {
            super.declaringClassMatching(classMatcher);
            return this;
        }
        
        @Nonnull
        @Override
        protected FuzzyFieldContract initialMember() {
            return new FuzzyFieldContract((FuzzyFieldContract$1)null);
        }
        
        public Builder typeExact(final Class<?> type) {
            ((FuzzyFieldContract)this.member).typeMatcher = FuzzyMatchers.matchExact(type);
            return this;
        }
        
        public Builder typeSuperOf(final Class<?> type) {
            ((FuzzyFieldContract)this.member).typeMatcher = FuzzyMatchers.matchSuper(type);
            return this;
        }
        
        public Builder typeDerivedOf(final Class<?> type) {
            ((FuzzyFieldContract)this.member).typeMatcher = FuzzyMatchers.matchDerived(type);
            return this;
        }
        
        public Builder typeMatches(final AbstractFuzzyMatcher<Class<?>> matcher) {
            ((FuzzyFieldContract)this.member).typeMatcher = matcher;
            return this;
        }
        
        @Override
        public FuzzyFieldContract build() {
            ((FuzzyFieldContract)this.member).prepareBuild();
            return new FuzzyFieldContract((FuzzyFieldContract)this.member, null);
        }
    }
}
