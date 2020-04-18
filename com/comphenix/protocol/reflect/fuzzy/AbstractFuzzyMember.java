package com.comphenix.protocol.reflect.fuzzy;

import javax.annotation.Nonnull;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.regex.Pattern;
import java.lang.reflect.Member;

public abstract class AbstractFuzzyMember<T extends Member> extends AbstractFuzzyMatcher<T>
{
    protected int modifiersRequired;
    protected int modifiersBanned;
    protected Pattern nameRegex;
    protected AbstractFuzzyMatcher<Class<?>> declaringMatcher;
    protected transient boolean sealed;
    
    protected AbstractFuzzyMember() {
        this.declaringMatcher = ClassExactMatcher.MATCH_ALL;
    }
    
    protected void prepareBuild() {
    }
    
    protected AbstractFuzzyMember(final AbstractFuzzyMember<T> other) {
        this.declaringMatcher = ClassExactMatcher.MATCH_ALL;
        this.modifiersRequired = other.modifiersRequired;
        this.modifiersBanned = other.modifiersBanned;
        this.nameRegex = other.nameRegex;
        this.declaringMatcher = other.declaringMatcher;
        this.sealed = true;
    }
    
    public int getModifiersRequired() {
        return this.modifiersRequired;
    }
    
    public int getModifiersBanned() {
        return this.modifiersBanned;
    }
    
    public Pattern getNameRegex() {
        return this.nameRegex;
    }
    
    public AbstractFuzzyMatcher<Class<?>> getDeclaringMatcher() {
        return this.declaringMatcher;
    }
    
    @Override
    public boolean isMatch(final T value, final Object parent) {
        final int mods = value.getModifiers();
        return (mods & this.modifiersRequired) == this.modifiersRequired && (mods & this.modifiersBanned) == 0x0 && this.declaringMatcher.isMatch(value.getDeclaringClass(), value) && this.isNameMatch(value.getName());
    }
    
    private boolean isNameMatch(final String name) {
        return this.nameRegex == null || this.nameRegex.matcher(name).matches();
    }
    
    @Override
    protected int calculateRoundNumber() {
        if (!this.sealed) {
            throw new IllegalStateException("Cannot calculate round number during construction.");
        }
        return this.declaringMatcher.getRoundNumber();
    }
    
    @Override
    public String toString() {
        return this.getKeyValueView().toString();
    }
    
    protected Map<String, Object> getKeyValueView() {
        final Map<String, Object> map = (Map<String, Object>)Maps.newLinkedHashMap();
        if (this.modifiersRequired != Integer.MAX_VALUE || this.modifiersBanned != 0) {
            map.put("modifiers", String.format("[required: %s, banned: %s]", getBitView(this.modifiersRequired, 16), getBitView(this.modifiersBanned, 16)));
        }
        if (this.nameRegex != null) {
            map.put("name", this.nameRegex.pattern());
        }
        if (this.declaringMatcher != ClassExactMatcher.MATCH_ALL) {
            map.put("declaring", this.declaringMatcher);
        }
        return map;
    }
    
    private static String getBitView(final int value, final int bits) {
        if (bits < 0 || bits > 31) {
            throw new IllegalArgumentException("Bits must be a value between 0 and 32");
        }
        final int snipped = value & (1 << bits) - 1;
        return Integer.toBinaryString(snipped);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AbstractFuzzyMember) {
            final AbstractFuzzyMember<T> other = (AbstractFuzzyMember<T>)obj;
            return this.modifiersBanned == other.modifiersBanned && this.modifiersRequired == other.modifiersRequired && FuzzyMatchers.checkPattern(this.nameRegex, other.nameRegex) && Objects.equal((Object)this.declaringMatcher, (Object)other.declaringMatcher);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.modifiersBanned, this.modifiersRequired, (this.nameRegex != null) ? this.nameRegex.pattern() : null, this.declaringMatcher });
    }
    
    public abstract static class Builder<T extends AbstractFuzzyMember<?>>
    {
        protected T member;
        
        public Builder() {
            this.member = this.initialMember();
        }
        
        public Builder<T> requireModifier(final int modifier) {
            final AbstractFuzzyMember<?> member = this.member;
            member.modifiersRequired |= modifier;
            return this;
        }
        
        public Builder<T> requirePublic() {
            return this.requireModifier(1);
        }
        
        public Builder<T> banModifier(final int modifier) {
            final AbstractFuzzyMember<?> member = this.member;
            member.modifiersBanned |= modifier;
            return this;
        }
        
        public Builder<T> nameRegex(final String regex) {
            this.member.nameRegex = Pattern.compile(regex);
            return this;
        }
        
        public Builder<T> nameRegex(final Pattern pattern) {
            this.member.nameRegex = pattern;
            return this;
        }
        
        public Builder<T> nameExact(final String name) {
            return this.nameRegex(Pattern.quote(name));
        }
        
        public Builder<T> declaringClassExactType(final Class<?> declaringClass) {
            this.member.declaringMatcher = FuzzyMatchers.matchExact(declaringClass);
            return this;
        }
        
        public Builder<T> declaringClassSuperOf(final Class<?> declaringClass) {
            this.member.declaringMatcher = FuzzyMatchers.matchSuper(declaringClass);
            return this;
        }
        
        public Builder<T> declaringClassDerivedOf(final Class<?> declaringClass) {
            this.member.declaringMatcher = FuzzyMatchers.matchDerived(declaringClass);
            return this;
        }
        
        public Builder<T> declaringClassMatching(final AbstractFuzzyMatcher<Class<?>> classMatcher) {
            this.member.declaringMatcher = classMatcher;
            return this;
        }
        
        @Nonnull
        protected abstract T initialMember();
        
        public abstract T build();
    }
}
