package com.comphenix.protocol.error;

import org.bukkit.plugin.Plugin;

public class DelegatedErrorReporter implements ErrorReporter
{
    private final ErrorReporter delegated;
    
    public DelegatedErrorReporter(final ErrorReporter delegated) {
        this.delegated = delegated;
    }
    
    public ErrorReporter getDelegated() {
        return this.delegated;
    }
    
    @Override
    public void reportMinimal(final Plugin sender, final String methodName, final Throwable error) {
        this.delegated.reportMinimal(sender, methodName, error);
    }
    
    @Override
    public void reportMinimal(final Plugin sender, final String methodName, final Throwable error, final Object... parameters) {
        this.delegated.reportMinimal(sender, methodName, error, parameters);
    }
    
    @Override
    public void reportDebug(final Object sender, final Report report) {
        final Report transformed = this.filterReport(sender, report, false);
        if (transformed != null) {
            this.delegated.reportDebug(sender, transformed);
        }
    }
    
    @Override
    public void reportWarning(final Object sender, final Report report) {
        final Report transformed = this.filterReport(sender, report, false);
        if (transformed != null) {
            this.delegated.reportWarning(sender, transformed);
        }
    }
    
    @Override
    public void reportDetailed(final Object sender, final Report report) {
        final Report transformed = this.filterReport(sender, report, true);
        if (transformed != null) {
            this.delegated.reportDetailed(sender, transformed);
        }
    }
    
    protected Report filterReport(final Object sender, final Report report, final boolean detailed) {
        return report;
    }
    
    @Override
    public void reportWarning(final Object sender, final Report.ReportBuilder reportBuilder) {
        this.reportWarning(sender, reportBuilder.build());
    }
    
    @Override
    public void reportDetailed(final Object sender, final Report.ReportBuilder reportBuilder) {
        this.reportDetailed(sender, reportBuilder.build());
    }
    
    @Override
    public void reportDebug(final Object sender, final Report.ReportBuilder builder) {
        this.reportDebug(sender, builder.build());
    }
}
