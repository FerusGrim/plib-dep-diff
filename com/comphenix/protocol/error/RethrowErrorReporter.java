package com.comphenix.protocol.error;

import com.google.common.base.Joiner;
import org.bukkit.plugin.Plugin;

public class RethrowErrorReporter implements ErrorReporter
{
    @Override
    public void reportMinimal(final Plugin sender, final String methodName, final Throwable error) {
        throw new RuntimeException("Minimal error by " + sender + " in " + methodName, error);
    }
    
    @Override
    public void reportMinimal(final Plugin sender, final String methodName, final Throwable error, final Object... parameters) {
        throw new RuntimeException("Minimal error by " + sender + " in " + methodName + " with " + Joiner.on(",").join(parameters), error);
    }
    
    @Override
    public void reportDebug(final Object sender, final Report report) {
    }
    
    @Override
    public void reportDebug(final Object sender, final Report.ReportBuilder builder) {
    }
    
    @Override
    public void reportWarning(final Object sender, final Report.ReportBuilder reportBuilder) {
        this.reportWarning(sender, reportBuilder.build());
    }
    
    @Override
    public void reportWarning(final Object sender, final Report report) {
        throw new RuntimeException("Warning by " + sender + ": " + report);
    }
    
    @Override
    public void reportDetailed(final Object sender, final Report.ReportBuilder reportBuilder) {
        this.reportDetailed(sender, reportBuilder.build());
    }
    
    @Override
    public void reportDetailed(final Object sender, final Report report) {
        throw new RuntimeException("Detailed error " + sender + ": " + report, report.getException());
    }
}
