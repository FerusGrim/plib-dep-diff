package com.comphenix.protocol.timing;

public class StatisticsStream extends OnlineComputation
{
    private int count;
    private double mean;
    private double m2;
    private double minimum;
    private double maximum;
    
    public StatisticsStream() {
        this.count = 0;
        this.mean = 0.0;
        this.m2 = 0.0;
        this.minimum = Double.MAX_VALUE;
        this.maximum = 0.0;
    }
    
    public StatisticsStream(final StatisticsStream other) {
        this.count = 0;
        this.mean = 0.0;
        this.m2 = 0.0;
        this.minimum = Double.MAX_VALUE;
        this.maximum = 0.0;
        this.count = other.count;
        this.mean = other.mean;
        this.m2 = other.m2;
        this.minimum = other.minimum;
        this.maximum = other.maximum;
    }
    
    @Override
    public StatisticsStream copy() {
        return new StatisticsStream(this);
    }
    
    @Override
    public void observe(final double value) {
        final double delta = value - this.mean;
        ++this.count;
        this.mean += delta / this.count;
        this.m2 += delta * (value - this.mean);
        if (value < this.minimum) {
            this.minimum = value;
        }
        if (value > this.maximum) {
            this.maximum = value;
        }
    }
    
    public double getMean() {
        this.checkCount();
        return this.mean;
    }
    
    public double getVariance() {
        this.checkCount();
        return this.m2 / (this.count - 1);
    }
    
    public double getStandardDeviation() {
        return Math.sqrt(this.getVariance());
    }
    
    public double getMinimum() {
        this.checkCount();
        return this.minimum;
    }
    
    public double getMaximum() {
        this.checkCount();
        return this.maximum;
    }
    
    public StatisticsStream add(final StatisticsStream other) {
        if (this.count == 0) {
            return other;
        }
        if (other.count == 0) {
            return this;
        }
        final StatisticsStream stream = new StatisticsStream();
        final double delta = other.mean - this.mean;
        final double n = this.count + other.count;
        stream.count = (int)n;
        stream.mean = this.mean + delta * (other.count / n);
        stream.m2 = this.m2 + other.m2 + delta * delta * (this.count * other.count) / n;
        stream.minimum = Math.min(this.minimum, other.minimum);
        stream.maximum = Math.max(this.maximum, other.maximum);
        return stream;
    }
    
    @Override
    public int getCount() {
        return this.count;
    }
    
    private void checkCount() {
        if (this.count == 0) {
            throw new IllegalStateException("No observations in stream.");
        }
    }
    
    @Override
    public String toString() {
        if (this.count == 0) {
            return "StatisticsStream [Nothing recorded]";
        }
        return String.format("StatisticsStream [Average: %.3f, SD: %.3f, Min: %.3f, Max: %.3f, Count: %s]", this.getMean(), this.getStandardDeviation(), this.getMinimum(), this.getMaximum(), this.getCount());
    }
}
