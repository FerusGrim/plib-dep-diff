package com.comphenix.protocol.timing;

import java.util.Collection;
import com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class HistogramStream extends OnlineComputation
{
    protected List<StatisticsStream> bins;
    protected StatisticsStream current;
    protected int binWidth;
    protected int count;
    
    public HistogramStream(final int binWidth) {
        this(new ArrayList<StatisticsStream>(), new StatisticsStream(), binWidth);
    }
    
    public HistogramStream(final HistogramStream other) {
        for (final StatisticsStream stream : other.bins) {
            final StatisticsStream copy = stream.copy();
            if (stream == other.current) {
                this.current = copy;
            }
            this.bins.add(copy);
        }
        this.binWidth = other.binWidth;
    }
    
    protected HistogramStream(final List<StatisticsStream> bins, final StatisticsStream current, final int binWidth) {
        if (binWidth < 1) {
            throw new IllegalArgumentException("binWidth cannot be less than 1");
        }
        this.bins = (List<StatisticsStream>)Preconditions.checkNotNull((Object)bins, (Object)"bins cannot be NULL");
        this.current = (StatisticsStream)Preconditions.checkNotNull((Object)current, (Object)"current cannot be NULL");
        this.binWidth = binWidth;
        if (!this.bins.contains(current)) {
            this.bins.add(current);
        }
    }
    
    @Override
    public HistogramStream copy() {
        return new HistogramStream(this);
    }
    
    public ImmutableList<StatisticsStream> getBins() {
        return (ImmutableList<StatisticsStream>)ImmutableList.copyOf((Collection)this.bins);
    }
    
    @Override
    public void observe(final double value) {
        this.checkOverflow();
        ++this.count;
        this.current.observe(value);
    }
    
    protected void checkOverflow() {
        if (this.current.getCount() >= this.binWidth) {
            this.bins.add(this.current = new StatisticsStream());
        }
    }
    
    public StatisticsStream getTotal() {
        StatisticsStream sum = null;
        for (final StatisticsStream stream : this.bins) {
            sum = ((sum != null) ? stream.add(sum) : stream);
        }
        return sum;
    }
    
    @Override
    public int getCount() {
        return this.count;
    }
}
