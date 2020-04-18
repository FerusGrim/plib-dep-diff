package com.comphenix.protocol.reflect.fuzzy;

import java.util.Collections;
import com.google.common.collect.Lists;
import java.util.Map;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.lang.reflect.Method;
import java.util.List;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.util.Iterator;
import java.util.Collection;
import com.comphenix.protocol.reflect.MethodInfo;
import java.lang.reflect.Field;
import com.google.common.collect.ImmutableList;

public class FuzzyClassContract extends AbstractFuzzyMatcher<Class<?>>
{
    private final ImmutableList<AbstractFuzzyMatcher<Field>> fieldContracts;
    private final ImmutableList<AbstractFuzzyMatcher<MethodInfo>> methodContracts;
    private final ImmutableList<AbstractFuzzyMatcher<MethodInfo>> constructorContracts;
    private final ImmutableList<AbstractFuzzyMatcher<Class<?>>> baseclassContracts;
    private final ImmutableList<AbstractFuzzyMatcher<Class<?>>> interfaceContracts;
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    private FuzzyClassContract(final Builder builder) {
        this.fieldContracts = (ImmutableList<AbstractFuzzyMatcher<Field>>)ImmutableList.copyOf((Collection)builder.fieldContracts);
        this.methodContracts = (ImmutableList<AbstractFuzzyMatcher<MethodInfo>>)ImmutableList.copyOf((Collection)builder.methodContracts);
        this.constructorContracts = (ImmutableList<AbstractFuzzyMatcher<MethodInfo>>)ImmutableList.copyOf((Collection)builder.constructorContracts);
        this.baseclassContracts = (ImmutableList<AbstractFuzzyMatcher<Class<?>>>)ImmutableList.copyOf((Collection)builder.baseclassContracts);
        this.interfaceContracts = (ImmutableList<AbstractFuzzyMatcher<Class<?>>>)ImmutableList.copyOf((Collection)builder.interfaceContracts);
    }
    
    public ImmutableList<AbstractFuzzyMatcher<Field>> getFieldContracts() {
        return this.fieldContracts;
    }
    
    public ImmutableList<AbstractFuzzyMatcher<MethodInfo>> getMethodContracts() {
        return this.methodContracts;
    }
    
    public ImmutableList<AbstractFuzzyMatcher<MethodInfo>> getConstructorContracts() {
        return this.constructorContracts;
    }
    
    public ImmutableList<AbstractFuzzyMatcher<Class<?>>> getBaseclassContracts() {
        return this.baseclassContracts;
    }
    
    public ImmutableList<AbstractFuzzyMatcher<Class<?>>> getInterfaceContracts() {
        return this.interfaceContracts;
    }
    
    @Override
    protected int calculateRoundNumber() {
        return this.combineRounds(this.findHighestRound((Collection<AbstractFuzzyMatcher<Object>>)this.fieldContracts), this.findHighestRound((Collection<AbstractFuzzyMatcher<Object>>)this.methodContracts), this.findHighestRound((Collection<AbstractFuzzyMatcher<Object>>)this.constructorContracts), this.findHighestRound((Collection<AbstractFuzzyMatcher<Object>>)this.interfaceContracts), this.findHighestRound((Collection<AbstractFuzzyMatcher<Object>>)this.baseclassContracts));
    }
    
    private <T> int findHighestRound(final Collection<AbstractFuzzyMatcher<T>> list) {
        int highest = 0;
        for (final AbstractFuzzyMatcher<T> matcher : list) {
            highest = this.combineRounds(highest, matcher.getRoundNumber());
        }
        return highest;
    }
    
    @Override
    public boolean isMatch(final Class<?> value, final Object parent) {
        final FuzzyReflection reflection = FuzzyReflection.fromClass(value, true);
        return (this.fieldContracts.size() == 0 || this.processContracts(reflection.getFields(), value, (List<AbstractFuzzyMatcher<Field>>)this.fieldContracts)) && (this.methodContracts.size() == 0 || this.processContracts(MethodInfo.fromMethods(reflection.getMethods()), value, (List<AbstractFuzzyMatcher<MethodInfo>>)this.methodContracts)) && (this.constructorContracts.size() == 0 || this.processContracts(MethodInfo.fromConstructors(value.getDeclaredConstructors()), value, (List<AbstractFuzzyMatcher<MethodInfo>>)this.constructorContracts)) && (this.baseclassContracts.size() == 0 || this.processValue(value.getSuperclass(), parent, (List<AbstractFuzzyMatcher<Class<?>>>)this.baseclassContracts)) && (this.interfaceContracts.size() == 0 || this.processContracts(Arrays.asList(value.getInterfaces()), parent, (List<AbstractFuzzyMatcher<Class<?>>>)this.interfaceContracts));
    }
    
    private <T> boolean processContracts(final Collection<T> values, final Object parent, final List<AbstractFuzzyMatcher<T>> matchers) {
        final boolean[] accepted = new boolean[matchers.size()];
        int count = accepted.length;
        for (final T value : values) {
            final int index = this.processValue(value, parent, accepted, matchers);
            if (index >= 0) {
                accepted[index] = true;
                --count;
            }
            if (count == 0) {
                return true;
            }
        }
        return count == 0;
    }
    
    private <T> boolean processValue(final T value, final Object parent, final List<AbstractFuzzyMatcher<T>> matchers) {
        for (int i = 0; i < matchers.size(); ++i) {
            if (matchers.get(i).isMatch(value, parent)) {
                return true;
            }
        }
        return false;
    }
    
    private <T> int processValue(final T value, final Object parent, final boolean[] accepted, final List<AbstractFuzzyMatcher<T>> matchers) {
        for (int i = 0; i < matchers.size(); ++i) {
            if (!accepted[i]) {
                final AbstractFuzzyMatcher<T> matcher = matchers.get(i);
                if (matcher.isMatch(value, parent)) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    @Override
    public String toString() {
        final Map<String, Object> params = (Map<String, Object>)Maps.newLinkedHashMap();
        if (this.fieldContracts.size() > 0) {
            params.put("fields", this.fieldContracts);
        }
        if (this.methodContracts.size() > 0) {
            params.put("methods", this.methodContracts);
        }
        if (this.constructorContracts.size() > 0) {
            params.put("constructors", this.constructorContracts);
        }
        if (this.baseclassContracts.size() > 0) {
            params.put("baseclasses", this.baseclassContracts);
        }
        if (this.interfaceContracts.size() > 0) {
            params.put("interfaces", this.interfaceContracts);
        }
        return "{\n  " + Joiner.on(", \n  ").join((Iterable)params.entrySet()) + "\n}";
    }
    
    public static class Builder
    {
        private List<AbstractFuzzyMatcher<Field>> fieldContracts;
        private List<AbstractFuzzyMatcher<MethodInfo>> methodContracts;
        private List<AbstractFuzzyMatcher<MethodInfo>> constructorContracts;
        private List<AbstractFuzzyMatcher<Class<?>>> baseclassContracts;
        private List<AbstractFuzzyMatcher<Class<?>>> interfaceContracts;
        
        public Builder() {
            this.fieldContracts = (List<AbstractFuzzyMatcher<Field>>)Lists.newArrayList();
            this.methodContracts = (List<AbstractFuzzyMatcher<MethodInfo>>)Lists.newArrayList();
            this.constructorContracts = (List<AbstractFuzzyMatcher<MethodInfo>>)Lists.newArrayList();
            this.baseclassContracts = (List<AbstractFuzzyMatcher<Class<?>>>)Lists.newArrayList();
            this.interfaceContracts = (List<AbstractFuzzyMatcher<Class<?>>>)Lists.newArrayList();
        }
        
        public Builder field(final AbstractFuzzyMatcher<Field> matcher) {
            this.fieldContracts.add(matcher);
            return this;
        }
        
        public Builder field(final FuzzyFieldContract.Builder builder) {
            return this.field(builder.build());
        }
        
        public Builder method(final AbstractFuzzyMatcher<MethodInfo> matcher) {
            this.methodContracts.add(matcher);
            return this;
        }
        
        public Builder method(final FuzzyMethodContract.Builder builder) {
            return this.method(builder.build());
        }
        
        public Builder constructor(final AbstractFuzzyMatcher<MethodInfo> matcher) {
            this.constructorContracts.add(matcher);
            return this;
        }
        
        public Builder constructor(final FuzzyMethodContract.Builder builder) {
            return this.constructor(builder.build());
        }
        
        public Builder baseclass(final AbstractFuzzyMatcher<Class<?>> matcher) {
            this.baseclassContracts.add(matcher);
            return this;
        }
        
        public Builder baseclass(final Builder builder) {
            return this.baseclass(builder.build());
        }
        
        public Builder interfaces(final AbstractFuzzyMatcher<Class<?>> matcher) {
            this.interfaceContracts.add(matcher);
            return this;
        }
        
        public Builder interfaces(final Builder builder) {
            return this.interfaces(builder.build());
        }
        
        public FuzzyClassContract build() {
            Collections.sort(this.fieldContracts);
            Collections.sort(this.methodContracts);
            Collections.sort(this.constructorContracts);
            Collections.sort(this.baseclassContracts);
            Collections.sort(this.interfaceContracts);
            return new FuzzyClassContract(this, null);
        }
    }
}
