package com.comphenix.protocol.error;

import org.bukkit.plugin.Plugin;

public interface ErrorReporter
{
    void reportMinimal(final Plugin p0, final String p1, final Throwable p2);
    
    void reportMinimal(final Plugin p0, final String p1, final Throwable p2, final Object... p3);
    
    void reportDebug(final Object p0, final Report p1);
    
    void reportDebug(final Object p0, final Report.ReportBuilder p1);
    
    void reportWarning(final Object p0, final Report p1);
    
    void reportWarning(final Object p0, final Report.ReportBuilder p1);
    
    void reportDetailed(final Object p0, final Report p1);
    
    void reportDetailed(final Object p0, final Report.ReportBuilder p1);
}
