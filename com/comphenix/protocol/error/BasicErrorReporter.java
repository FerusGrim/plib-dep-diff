package com.comphenix.protocol.error;

import com.comphenix.protocol.reflect.PrettyPrinter;
import org.bukkit.plugin.Plugin;
import java.io.PrintStream;

public class BasicErrorReporter implements ErrorReporter
{
    private final PrintStream output;
    
    public BasicErrorReporter() {
        this(System.err);
    }
    
    public BasicErrorReporter(final PrintStream output) {
        this.output = output;
    }
    
    @Override
    public void reportMinimal(final Plugin sender, final String methodName, final Throwable error) {
        this.output.println("Unhandled exception occured in " + methodName + " for " + sender.getName());
        error.printStackTrace(this.output);
    }
    
    @Override
    public void reportMinimal(final Plugin sender, final String methodName, final Throwable error, final Object... parameters) {
        this.reportMinimal(sender, methodName, error);
        this.printParameters(parameters);
    }
    
    @Override
    public void reportDebug(final Object sender, final Report report) {
    }
    
    @Override
    public void reportDebug(final Object sender, final Report.ReportBuilder builder) {
    }
    
    @Override
    public void reportWarning(final Object sender, final Report report) {
        this.output.println("[" + sender.getClass().getSimpleName() + "] " + report.getReportMessage());
        if (report.getException() != null) {
            report.getException().printStackTrace(this.output);
        }
        this.printParameters(report.getCallerParameters());
    }
    
    @Override
    public void reportWarning(final Object sender, final Report.ReportBuilder reportBuilder) {
        this.reportWarning(sender, reportBuilder.build());
    }
    
    @Override
    public void reportDetailed(final Object sender, final Report report) {
        this.reportWarning(sender, report);
    }
    
    @Override
    public void reportDetailed(final Object sender, final Report.ReportBuilder reportBuilder) {
        this.reportWarning(sender, reportBuilder);
    }
    
    private void printParameters(final Object[] parameters) {
        if (parameters != null && parameters.length > 0) {
            this.output.println("Parameters: ");
            try {
                for (final Object parameter : parameters) {
                    if (parameter == null) {
                        this.output.println("[NULL]");
                    }
                    else {
                        this.output.println(PrettyPrinter.printObject(parameter));
                    }
                }
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
