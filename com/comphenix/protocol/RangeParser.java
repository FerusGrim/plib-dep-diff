package com.comphenix.protocol;

import com.google.common.collect.UnmodifiableIterator;
import java.util.Iterator;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import com.google.common.collect.Range;

class RangeParser
{
    public static List<Range<Integer>> getRanges(final String text, final Range<Integer> legalRange) {
        return getRanges(new ArrayDeque<String>(Arrays.asList(text)), legalRange);
    }
    
    public static List<Range<Integer>> getRanges(final Deque<String> input, final Range<Integer> legalRange) {
        final List<String> tokens = tokenizeInput(input);
        final List<Range<Integer>> ranges = new ArrayList<Range<Integer>>();
        for (int i = 0; i < tokens.size(); ++i) {
            final String current = tokens.get(i);
            final String next = (i + 1 < tokens.size()) ? tokens.get(i + 1) : null;
            if ("-".equals(current)) {
                throw new IllegalArgumentException("A hyphen must appear between two numbers.");
            }
            Range<Integer> range;
            if ("-".equals(next)) {
                if (i + 2 >= tokens.size()) {
                    throw new IllegalArgumentException("Cannot form a range without a upper limit.");
                }
                range = (Range<Integer>)Range.closed((Comparable)Integer.parseInt(current), (Comparable)Integer.parseInt(tokens.get(i + 2)));
                ranges.add(range);
                i += 2;
            }
            else {
                range = (Range<Integer>)Range.singleton((Comparable)Integer.parseInt(current));
                ranges.add(range);
            }
            if (!legalRange.encloses((Range)range)) {
                throw new IllegalArgumentException(range + " is not in the range " + range.toString());
            }
        }
        return simplify(ranges, (int)legalRange.upperEndpoint());
    }
    
    private static List<Range<Integer>> simplify(final List<Range<Integer>> ranges, final int maximum) {
        final List<Range<Integer>> result = new ArrayList<Range<Integer>>();
        final boolean[] set = new boolean[maximum + 1];
        int start = -1;
        for (final Range<Integer> range : ranges) {
            for (final int id : ContiguousSet.create((Range)range, DiscreteDomain.integers())) {
                set[id] = true;
            }
        }
        for (int i = 0; i <= set.length; ++i) {
            if (i < set.length && set[i]) {
                if (start < 0) {
                    start = i;
                }
            }
            else if (start >= 0) {
                result.add((Range<Integer>)Range.closed((Comparable)start, (Comparable)(i - 1)));
                start = -1;
            }
        }
        return result;
    }
    
    private static List<String> tokenizeInput(final Deque<String> input) {
        final List<String> tokens = new ArrayList<String>();
        while (!input.isEmpty()) {
            final StringBuilder number = new StringBuilder();
            final String text = input.peek();
            for (int j = 0; j < text.length(); ++j) {
                final char current = text.charAt(j);
                if (Character.isDigit(current)) {
                    number.append(current);
                }
                else if (!Character.isWhitespace(current)) {
                    if (current != '-') {
                        return tokens;
                    }
                    if (number.length() > 0) {
                        tokens.add(number.toString());
                        number.setLength(0);
                    }
                    tokens.add(Character.toString(current));
                }
            }
            if (number.length() > 0) {
                tokens.add(number.toString());
            }
            input.poll();
        }
        return tokens;
    }
}
