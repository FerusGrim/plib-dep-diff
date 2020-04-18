package com.comphenix.protocol.reflect.fuzzy;

import com.google.common.primitives.Ints;

public abstract class AbstractFuzzyMatcher<T> implements Comparable<AbstractFuzzyMatcher<T>>
{
    private Integer roundNumber;
    
    public abstract boolean isMatch(final T p0, final Object p1);
    
    protected abstract int calculateRoundNumber();
    
    public final int getRoundNumber() {
        if (this.roundNumber == null) {
            final Integer value = this.calculateRoundNumber();
            this.roundNumber = value;
            return value;
        }
        return this.roundNumber;
    }
    
    protected final int combineRounds(final int roundA, final int roundB) {
        if (roundA == 0) {
            return roundB;
        }
        if (roundB == 0) {
            return roundA;
        }
        return Math.max(roundA, roundB);
    }
    
    protected final int combineRounds(final Integer... rounds) {
        if (rounds.length < 2) {
            throw new IllegalArgumentException("Must supply at least two arguments.");
        }
        int reduced = this.combineRounds((int)rounds[0], rounds[1]);
        for (int i = 2; i < rounds.length; ++i) {
            reduced = this.combineRounds(reduced, rounds[i]);
        }
        return reduced;
    }
    
    @Override
    public int compareTo(final AbstractFuzzyMatcher<T> obj) {
        return Ints.compare(this.getRoundNumber(), obj.getRoundNumber());
    }
    
    public AbstractFuzzyMatcher<T> inverted() {
        return new AbstractFuzzyMatcher<T>() {
            @Override
            public boolean isMatch(final T value, final Object parent) {
                return !AbstractFuzzyMatcher.this.isMatch(value, parent);
            }
            
            @Override
            protected int calculateRoundNumber() {
                return -2;
            }
        };
    }
    
    public AbstractFuzzyMatcher<T> and(final AbstractFuzzyMatcher<T> other) {
        return new AbstractFuzzyMatcher<T>() {
            @Override
            public boolean isMatch(final T value, final Object parent) {
                return AbstractFuzzyMatcher.this.isMatch(value, parent) && other.isMatch(value, parent);
            }
            
            @Override
            protected int calculateRoundNumber() {
                return this.combineRounds(AbstractFuzzyMatcher.this.getRoundNumber(), other.getRoundNumber());
            }
        };
    }
    
    public AbstractFuzzyMatcher<T> or(final AbstractFuzzyMatcher<T> other) {
        return new AbstractFuzzyMatcher<T>() {
            @Override
            public boolean isMatch(final T value, final Object parent) {
                return AbstractFuzzyMatcher.this.isMatch(value, parent) || other.isMatch(value, parent);
            }
            
            @Override
            protected int calculateRoundNumber() {
                return this.combineRounds(AbstractFuzzyMatcher.this.getRoundNumber(), other.getRoundNumber());
            }
        };
    }
}
