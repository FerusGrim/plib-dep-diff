package com.comphenix.protocol.error;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import javax.annotation.Nullable;

public class Report
{
    private final ReportType type;
    private final Throwable exception;
    private final Object[] messageParameters;
    private final Object[] callerParameters;
    private final long rateLimit;
    
    public static ReportBuilder newBuilder(final ReportType type) {
        return new ReportBuilder().type(type);
    }
    
    protected Report(final ReportType type, @Nullable final Throwable exception, @Nullable final Object[] messageParameters, @Nullable final Object[] callerParameters) {
        this(type, exception, messageParameters, callerParameters, 0L);
    }
    
    protected Report(final ReportType type, @Nullable final Throwable exception, @Nullable final Object[] messageParameters, @Nullable final Object[] callerParameters, final long rateLimit) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be NULL.");
        }
        this.type = type;
        this.exception = exception;
        this.messageParameters = messageParameters;
        this.callerParameters = callerParameters;
        this.rateLimit = rateLimit;
    }
    
    public String getReportMessage() {
        return this.type.getMessage(this.messageParameters);
    }
    
    public Object[] getMessageParameters() {
        return this.messageParameters;
    }
    
    public Object[] getCallerParameters() {
        return this.callerParameters;
    }
    
    public ReportType getType() {
        return this.type;
    }
    
    public Throwable getException() {
        return this.exception;
    }
    
    public boolean hasMessageParameters() {
        return this.messageParameters != null && this.messageParameters.length > 0;
    }
    
    public boolean hasCallerParameters() {
        return this.callerParameters != null && this.callerParameters.length > 0;
    }
    
    public long getRateLimit() {
        return this.rateLimit;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = 31 * result + Arrays.hashCode(this.callerParameters);
        result = 31 * result + Arrays.hashCode(this.messageParameters);
        result = 31 * result + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Report) {
            final Report other = (Report)obj;
            return this.type == other.type && Arrays.equals(this.callerParameters, other.callerParameters) && Arrays.equals(this.messageParameters, other.messageParameters);
        }
        return false;
    }
    
    public static class ReportBuilder
    {
        private ReportType type;
        private Throwable exception;
        private Object[] messageParameters;
        private Object[] callerParameters;
        private long rateLimit;
        
        private ReportBuilder() {
        }
        
        public ReportBuilder type(final ReportType type) {
            if (type == null) {
                throw new IllegalArgumentException("Report type cannot be set to NULL.");
            }
            this.type = type;
            return this;
        }
        
        public ReportBuilder error(@Nullable final Throwable exception) {
            this.exception = exception;
            return this;
        }
        
        public ReportBuilder messageParam(@Nullable final Object... messageParameters) {
            this.messageParameters = messageParameters;
            return this;
        }
        
        public ReportBuilder callerParam(@Nullable final Object... callerParameters) {
            this.callerParameters = callerParameters;
            return this;
        }
        
        public ReportBuilder rateLimit(final long rateLimit) {
            if (rateLimit < 0L) {
                throw new IllegalArgumentException("Rate limit cannot be less than zero.");
            }
            this.rateLimit = rateLimit;
            return this;
        }
        
        public ReportBuilder rateLimit(final long rateLimit, final TimeUnit rateUnit) {
            return this.rateLimit(TimeUnit.NANOSECONDS.convert(rateLimit, rateUnit));
        }
        
        public Report build() {
            return new Report(this.type, this.exception, this.messageParameters, this.callerParameters, this.rateLimit);
        }
    }
}
