package com.comphenix.protocol.reflect.fuzzy;

import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import java.lang.reflect.Member;
import com.google.common.base.Objects;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import java.util.Collection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import com.comphenix.protocol.reflect.MethodInfo;

public class FuzzyMethodContract extends AbstractFuzzyMember<MethodInfo>
{
    private AbstractFuzzyMatcher<Class<?>> returnMatcher;
    private List<ParameterClassMatcher> paramMatchers;
    private List<ParameterClassMatcher> exceptionMatchers;
    private Integer paramCount;
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    private FuzzyMethodContract() {
        this.returnMatcher = ClassExactMatcher.MATCH_ALL;
        this.paramMatchers = (List<ParameterClassMatcher>)Lists.newArrayList();
        this.exceptionMatchers = (List<ParameterClassMatcher>)Lists.newArrayList();
    }
    
    private FuzzyMethodContract(final FuzzyMethodContract other) {
        super(other);
        this.returnMatcher = ClassExactMatcher.MATCH_ALL;
        this.returnMatcher = other.returnMatcher;
        this.paramMatchers = other.paramMatchers;
        this.exceptionMatchers = other.exceptionMatchers;
        this.paramCount = other.paramCount;
    }
    
    private static FuzzyMethodContract immutableCopy(final FuzzyMethodContract other) {
        final FuzzyMethodContract copy = new FuzzyMethodContract(other);
        copy.paramMatchers = (List<ParameterClassMatcher>)ImmutableList.copyOf((Collection)copy.paramMatchers);
        copy.exceptionMatchers = (List<ParameterClassMatcher>)ImmutableList.copyOf((Collection)copy.exceptionMatchers);
        return copy;
    }
    
    public AbstractFuzzyMatcher<Class<?>> getReturnMatcher() {
        return this.returnMatcher;
    }
    
    public ImmutableList<ParameterClassMatcher> getParamMatchers() {
        if (this.paramMatchers instanceof ImmutableList) {
            return (ImmutableList<ParameterClassMatcher>)this.paramMatchers;
        }
        throw new IllegalStateException("Lists haven't been sealed yet.");
    }
    
    public List<ParameterClassMatcher> getExceptionMatchers() {
        if (this.exceptionMatchers instanceof ImmutableList) {
            return this.exceptionMatchers;
        }
        throw new IllegalStateException("Lists haven't been sealed yet.");
    }
    
    public Integer getParamCount() {
        return this.paramCount;
    }
    
    @Override
    protected void prepareBuild() {
        super.prepareBuild();
        Collections.sort(this.paramMatchers);
        Collections.sort(this.exceptionMatchers);
    }
    
    @Override
    public boolean isMatch(final MethodInfo value, final Object parent) {
        if (super.isMatch(value, parent)) {
            final Class<?>[] params = value.getParameterTypes();
            final Class<?>[] exceptions = value.getExceptionTypes();
            return this.returnMatcher.isMatch(value.getReturnType(), value) && (this.paramCount == null || this.paramCount == value.getParameterTypes().length) && this.matchParameters(params, value, this.paramMatchers) && this.matchParameters(exceptions, value, this.exceptionMatchers);
        }
        return false;
    }
    
    private boolean matchParameters(final Class<?>[] types, final MethodInfo parent, final List<ParameterClassMatcher> matchers) {
        final boolean[] accepted = new boolean[matchers.size()];
        int count = accepted.length;
        for (int i = 0; i < types.length; ++i) {
            final int matcherIndex = this.processValue(types[i], parent, i, accepted, matchers);
            if (matcherIndex >= 0) {
                accepted[matcherIndex] = true;
                --count;
            }
            if (count == 0) {
                return true;
            }
        }
        return count == 0;
    }
    
    private int processValue(final Class<?> value, final MethodInfo parent, final int index, final boolean[] accepted, final List<ParameterClassMatcher> matchers) {
        for (int i = 0; i < matchers.size(); ++i) {
            if (!accepted[i] && matchers.get(i).isParameterMatch(value, parent, index)) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    protected int calculateRoundNumber() {
        int current = 0;
        current = this.returnMatcher.getRoundNumber();
        for (final ParameterClassMatcher matcher : this.paramMatchers) {
            current = this.combineRounds(current, matcher.calculateRoundNumber());
        }
        for (final ParameterClassMatcher matcher : this.exceptionMatchers) {
            current = this.combineRounds(current, matcher.calculateRoundNumber());
        }
        return this.combineRounds(super.calculateRoundNumber(), current);
    }
    
    @Override
    protected Map<String, Object> getKeyValueView() {
        final Map<String, Object> member = super.getKeyValueView();
        if (this.returnMatcher != ClassExactMatcher.MATCH_ALL) {
            member.put("return", this.returnMatcher);
        }
        if (this.paramMatchers.size() > 0) {
            member.put("params", this.paramMatchers);
        }
        if (this.exceptionMatchers.size() > 0) {
            member.put("exceptions", this.exceptionMatchers);
        }
        if (this.paramCount != null) {
            member.put("paramCount", this.paramCount);
        }
        return member;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.returnMatcher, this.paramMatchers, this.exceptionMatchers, this.paramCount, super.hashCode() });
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FuzzyMethodContract && super.equals(obj)) {
            final FuzzyMethodContract other = (FuzzyMethodContract)obj;
            return Objects.equal((Object)this.paramCount, (Object)other.paramCount) && Objects.equal((Object)this.returnMatcher, (Object)other.returnMatcher) && Objects.equal((Object)this.paramMatchers, (Object)other.paramMatchers) && Objects.equal((Object)this.exceptionMatchers, (Object)other.exceptionMatchers);
        }
        return true;
    }
    
    private static class ParameterClassMatcher extends AbstractFuzzyMatcher<Class<?>[]>
    {
        private final AbstractFuzzyMatcher<Class<?>> typeMatcher;
        private final Integer indexMatch;
        
        public ParameterClassMatcher(@Nonnull final AbstractFuzzyMatcher<Class<?>> typeMatcher) {
            this(typeMatcher, null);
        }
        
        public ParameterClassMatcher(@Nonnull final AbstractFuzzyMatcher<Class<?>> typeMatcher, final Integer indexMatch) {
            if (typeMatcher == null) {
                throw new IllegalArgumentException("Type matcher cannot be NULL.");
            }
            this.typeMatcher = typeMatcher;
            this.indexMatch = indexMatch;
        }
        
        public boolean isParameterMatch(final Class<?> param, final MethodInfo parent, final int index) {
            return (this.indexMatch == null || this.indexMatch == index) && this.typeMatcher.isMatch(param, parent);
        }
        
        @Override
        public boolean isMatch(final Class<?>[] value, final Object parent) {
            throw new UnsupportedOperationException("Use the parameter match instead.");
        }
        
        @Override
        protected int calculateRoundNumber() {
            return this.typeMatcher.getRoundNumber();
        }
        
        @Override
        public String toString() {
            return String.format("{Type: %s, Index: %s}", this.typeMatcher, this.indexMatch);
        }
    }
    
    public static class Builder extends AbstractFuzzyMember.Builder<FuzzyMethodContract>
    {
        @Override
        public Builder requireModifier(final int modifier) {
            super.requireModifier(modifier);
            return this;
        }
        
        @Override
        public Builder requirePublic() {
            super.requirePublic();
            return this;
        }
        
        @Override
        public Builder banModifier(final int modifier) {
            super.banModifier(modifier);
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
        
        public Builder parameterExactType(final Class<?> type) {
            ((FuzzyMethodContract)this.member).paramMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchExact(type)));
            return this;
        }
        
        public Builder parameterSuperOf(final Class<?> type) {
            ((FuzzyMethodContract)this.member).paramMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchSuper(type)));
            return this;
        }
        
        public Builder parameterDerivedOf(final Class<?> type) {
            ((FuzzyMethodContract)this.member).paramMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchDerived(type)));
            return this;
        }
        
        public Builder parameterMatches(final AbstractFuzzyMatcher<Class<?>> classMatcher) {
            ((FuzzyMethodContract)this.member).paramMatchers.add(new ParameterClassMatcher(classMatcher));
            return this;
        }
        
        public Builder parameterExactType(final Class<?> type, final int index) {
            ((FuzzyMethodContract)this.member).paramMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchExact(type), index));
            return this;
        }
        
        public Builder parameterExactArray(final Class<?>... types) {
            this.parameterCount(types.length);
            for (int i = 0; i < types.length; ++i) {
                this.parameterExactType(types[i], i);
            }
            return this;
        }
        
        public Builder parameterSuperOf(final Class<?> type, final int index) {
            ((FuzzyMethodContract)this.member).paramMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchSuper(type), index));
            return this;
        }
        
        public Builder parameterDerivedOf(final Class<?> type, final int index) {
            ((FuzzyMethodContract)this.member).paramMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchDerived(type), index));
            return this;
        }
        
        public Builder parameterMatches(final AbstractFuzzyMatcher<Class<?>> classMatcher, final int index) {
            ((FuzzyMethodContract)this.member).paramMatchers.add(new ParameterClassMatcher(classMatcher, index));
            return this;
        }
        
        public Builder parameterCount(final int expectedCount) {
            ((FuzzyMethodContract)this.member).paramCount = expectedCount;
            return this;
        }
        
        public Builder returnTypeVoid() {
            return this.returnTypeExact(Void.TYPE);
        }
        
        public Builder returnTypeExact(final Class<?> type) {
            ((FuzzyMethodContract)this.member).returnMatcher = FuzzyMatchers.matchExact(type);
            return this;
        }
        
        public Builder returnDerivedOf(final Class<?> type) {
            ((FuzzyMethodContract)this.member).returnMatcher = FuzzyMatchers.matchDerived(type);
            return this;
        }
        
        public Builder returnTypeMatches(final AbstractFuzzyMatcher<Class<?>> classMatcher) {
            ((FuzzyMethodContract)this.member).returnMatcher = classMatcher;
            return this;
        }
        
        public Builder exceptionExactType(final Class<?> type) {
            ((FuzzyMethodContract)this.member).exceptionMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchExact(type)));
            return this;
        }
        
        public Builder exceptionSuperOf(final Class<?> type) {
            ((FuzzyMethodContract)this.member).exceptionMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchSuper(type)));
            return this;
        }
        
        public Builder exceptionMatches(final AbstractFuzzyMatcher<Class<?>> classMatcher) {
            ((FuzzyMethodContract)this.member).exceptionMatchers.add(new ParameterClassMatcher(classMatcher));
            return this;
        }
        
        public Builder exceptionExactType(final Class<?> type, final int index) {
            ((FuzzyMethodContract)this.member).exceptionMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchExact(type), index));
            return this;
        }
        
        public Builder exceptionSuperOf(final Class<?> type, final int index) {
            ((FuzzyMethodContract)this.member).exceptionMatchers.add(new ParameterClassMatcher(FuzzyMatchers.matchSuper(type), index));
            return this;
        }
        
        public Builder exceptionMatches(final AbstractFuzzyMatcher<Class<?>> classMatcher, final int index) {
            ((FuzzyMethodContract)this.member).exceptionMatchers.add(new ParameterClassMatcher(classMatcher, index));
            return this;
        }
        
        @Nonnull
        @Override
        protected FuzzyMethodContract initialMember() {
            return new FuzzyMethodContract((FuzzyMethodContract$1)null);
        }
        
        @Override
        public FuzzyMethodContract build() {
            ((FuzzyMethodContract)this.member).prepareBuild();
            return immutableCopy((FuzzyMethodContract)this.member);
        }
    }
}
