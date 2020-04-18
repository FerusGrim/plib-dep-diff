package com.comphenix.protocol.error;

import java.util.Set;
import com.google.common.primitives.Primitives;
import com.comphenix.protocol.reflect.PrettyPrinter;
import com.comphenix.protocol.ProtocolLogger;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import java.util.Iterator;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.events.PacketAdapter;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import com.comphenix.protocol.collections.ExpireHashMap;
import java.util.Map;
import org.bukkit.plugin.Plugin;
import java.lang.ref.WeakReference;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentMap;

public class DetailedErrorReporter implements ErrorReporter
{
    public static final ReportType REPORT_EXCEPTION_COUNT;
    public static final String SECOND_LEVEL_PREFIX = "  ";
    public static final String DEFAULT_PREFIX = "  ";
    public static final String DEFAULT_SUPPORT_URL = "https://github.com/dmulloy2/ProtocolLib/issues";
    public static final String ERROR_PERMISSION = "protocol.info";
    public static final int DEFAULT_MAX_ERROR_COUNT = 20;
    private ConcurrentMap<String, AtomicInteger> warningCount;
    protected String prefix;
    protected String supportURL;
    protected AtomicInteger internalErrorCount;
    protected int maxErrorCount;
    protected Logger logger;
    protected WeakReference<Plugin> pluginReference;
    protected String pluginName;
    protected static boolean apacheCommonsMissing;
    protected boolean detailedReporting;
    protected Map<String, Object> globalParameters;
    private ExpireHashMap<Report, Boolean> rateLimited;
    private Object rateLock;
    
    public DetailedErrorReporter(final Plugin plugin) {
        this(plugin, "  ", "https://github.com/dmulloy2/ProtocolLib/issues");
    }
    
    public DetailedErrorReporter(final Plugin plugin, final String prefix, final String supportURL) {
        this(plugin, prefix, supportURL, 20, getBukkitLogger());
    }
    
    public DetailedErrorReporter(final Plugin plugin, final String prefix, final String supportURL, final int maxErrorCount, final Logger logger) {
        this.warningCount = new ConcurrentHashMap<String, AtomicInteger>();
        this.internalErrorCount = new AtomicInteger();
        this.globalParameters = new HashMap<String, Object>();
        this.rateLimited = new ExpireHashMap<Report, Boolean>();
        this.rateLock = new Object();
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be NULL.");
        }
        this.pluginReference = new WeakReference<Plugin>(plugin);
        this.pluginName = this.getNameSafely(plugin);
        this.prefix = prefix;
        this.supportURL = supportURL;
        this.maxErrorCount = maxErrorCount;
        this.logger = logger;
    }
    
    private String getNameSafely(final Plugin plugin) {
        try {
            return plugin.getName();
        }
        catch (LinkageError e) {
            return "ProtocolLib";
        }
    }
    
    private static Logger getBukkitLogger() {
        try {
            return Bukkit.getLogger();
        }
        catch (LinkageError e) {
            return Logger.getLogger("Minecraft");
        }
    }
    
    public boolean isDetailedReporting() {
        return this.detailedReporting;
    }
    
    public void setDetailedReporting(final boolean detailedReporting) {
        this.detailedReporting = detailedReporting;
    }
    
    @Override
    public void reportMinimal(final Plugin sender, final String methodName, final Throwable error, final Object... parameters) {
        if (this.reportMinimalNoSpam(sender, methodName, error) && parameters != null && parameters.length > 0) {
            this.logger.log(Level.SEVERE, this.printParameters(parameters));
        }
    }
    
    @Override
    public void reportMinimal(final Plugin sender, final String methodName, final Throwable error) {
        this.reportMinimalNoSpam(sender, methodName, error);
    }
    
    public boolean reportMinimalNoSpam(final Plugin sender, final String methodName, final Throwable error) {
        final String pluginName = PacketAdapter.getPluginName(sender);
        AtomicInteger counter = this.warningCount.get(pluginName);
        if (counter == null) {
            final AtomicInteger created = new AtomicInteger();
            counter = this.warningCount.putIfAbsent(pluginName, created);
            if (counter == null) {
                counter = created;
            }
        }
        final int errorCount = counter.incrementAndGet();
        if (errorCount < this.getMaxErrorCount()) {
            this.logger.log(Level.SEVERE, "[" + pluginName + "] Unhandled exception occured in " + methodName + " for " + pluginName, error);
            return true;
        }
        if (this.isPowerOfTwo(errorCount)) {
            this.logger.log(Level.SEVERE, "[" + pluginName + "] Unhandled exception number " + errorCount + " occured in " + methodName + " for " + pluginName, error);
        }
        return false;
    }
    
    private boolean isPowerOfTwo(final int number) {
        return (number & number - 1) == 0x0;
    }
    
    @Override
    public void reportDebug(final Object sender, final Report.ReportBuilder builder) {
        this.reportDebug(sender, ((Report.ReportBuilder)Preconditions.checkNotNull((Object)builder, (Object)"builder cannot be NULL")).build());
    }
    
    @Override
    public void reportDebug(final Object sender, final Report report) {
        if (this.logger.isLoggable(Level.FINE) && this.canReport(report)) {
            this.reportLevel(Level.FINE, sender, report);
        }
    }
    
    @Override
    public void reportWarning(final Object sender, final Report.ReportBuilder reportBuilder) {
        if (reportBuilder == null) {
            throw new IllegalArgumentException("reportBuilder cannot be NULL.");
        }
        this.reportWarning(sender, reportBuilder.build());
    }
    
    @Override
    public void reportWarning(final Object sender, final Report report) {
        if (this.logger.isLoggable(Level.WARNING) && this.canReport(report)) {
            this.reportLevel(Level.WARNING, sender, report);
        }
    }
    
    protected boolean canReport(final Report report) {
        final long rateLimit = report.getRateLimit();
        if (rateLimit > 0L) {
            synchronized (this.rateLock) {
                if (this.rateLimited.containsKey(report)) {
                    return false;
                }
                this.rateLimited.put(report, true, rateLimit, TimeUnit.NANOSECONDS);
            }
        }
        return true;
    }
    
    private void reportLevel(final Level level, final Object sender, final Report report) {
        final String message = "[" + this.pluginName + "] [" + this.getSenderName(sender) + "] " + report.getReportMessage();
        if (report.getException() != null) {
            this.logger.log(level, message, report.getException());
        }
        else {
            this.logger.log(level, message);
            if (this.detailedReporting) {
                this.printCallStack(level, this.logger);
            }
        }
        if (report.hasCallerParameters()) {
            this.logger.log(level, this.printParameters(report.getCallerParameters()));
        }
    }
    
    private String getSenderName(final Object sender) {
        if (sender != null) {
            return ReportType.getSenderClass(sender).getSimpleName();
        }
        return "NULL";
    }
    
    @Override
    public void reportDetailed(final Object sender, final Report.ReportBuilder reportBuilder) {
        this.reportDetailed(sender, reportBuilder.build());
    }
    
    @Override
    public void reportDetailed(final Object sender, final Report report) {
        final Plugin plugin = this.pluginReference.get();
        final int errorCount = this.internalErrorCount.incrementAndGet();
        if (errorCount > this.getMaxErrorCount()) {
            if (!this.isPowerOfTwo(errorCount)) {
                return;
            }
            this.reportWarning(this, Report.newBuilder(DetailedErrorReporter.REPORT_EXCEPTION_COUNT).messageParam(errorCount).build());
        }
        if (!this.canReport(report)) {
            return;
        }
        final StringWriter text = new StringWriter();
        final PrintWriter writer = new PrintWriter(text);
        writer.println("[" + this.pluginName + "] INTERNAL ERROR: " + report.getReportMessage());
        writer.println("If this problem hasn't already been reported, please open a ticket");
        writer.println("at " + this.supportURL + " with the following data:");
        writer.println("Stack Trace:");
        if (report.getException() != null) {
            report.getException().printStackTrace(writer);
        }
        else if (this.detailedReporting) {
            this.printCallStack(writer);
        }
        writer.println("Dump:");
        if (report.hasCallerParameters()) {
            this.printParameters(writer, report.getCallerParameters());
        }
        for (final String param : this.globalParameters()) {
            writer.println("  " + param + ":");
            writer.println(this.addPrefix(getStringDescription(this.getGlobalParameter(param)), "    "));
        }
        writer.println("Sender:");
        writer.println(this.addPrefix(getStringDescription(sender), "  "));
        if (plugin != null) {
            writer.println("Version:");
            writer.println(this.addPrefix(plugin.toString(), "  "));
        }
        writer.println("Java Version:");
        writer.println(this.addPrefix(System.getProperty("java.version"), "  "));
        if (Bukkit.getServer() != null) {
            writer.println("Server:");
            writer.println(this.addPrefix(Bukkit.getServer().getVersion(), "  "));
            if ("protocol.info" != null) {
                Bukkit.getServer().broadcast(String.format("Error %s (%s) occured in %s.", report.getReportMessage(), report.getException(), sender), "protocol.info");
            }
        }
        this.logger.severe(this.addPrefix(text.toString(), this.prefix));
    }
    
    private void printCallStack(final Level level, final Logger logger) {
        final StringWriter text = new StringWriter();
        this.printCallStack(new PrintWriter(text));
        logger.log(level, text.toString());
    }
    
    private void printCallStack(final PrintWriter writer) {
        final Exception current = new Exception("Not an error! This is the call stack.");
        current.printStackTrace(writer);
    }
    
    private String printParameters(final Object... parameters) {
        final StringWriter writer = new StringWriter();
        this.printParameters(new PrintWriter(writer), parameters);
        return writer.toString();
    }
    
    private void printParameters(final PrintWriter writer, final Object[] parameters) {
        writer.println("Parameters: ");
        for (final Object param : parameters) {
            writer.println(this.addPrefix(getStringDescription(param), "  "));
        }
    }
    
    protected String addPrefix(final String text, final String prefix) {
        return text.replaceAll("(?m)^", prefix);
    }
    
    public static String getStringDescription(final Object value) {
        if (value == null) {
            return "[NULL]";
        }
        if (isSimpleType(value) || value instanceof Class) {
            return value.toString();
        }
        try {
            if (!DetailedErrorReporter.apacheCommonsMissing) {
                return ToStringBuilder.reflectionToString(value, ToStringStyle.MULTI_LINE_STYLE, false, (Class)null);
            }
        }
        catch (LinkageError ex2) {
            DetailedErrorReporter.apacheCommonsMissing = true;
        }
        catch (ThreadDeath | OutOfMemoryError threadDeath) {
            final Error error;
            final Error e = error;
            throw e;
        }
        catch (Throwable ex) {
            ProtocolLogger.log(Level.WARNING, "Cannot convert to a String with Apache: " + ex.getMessage(), new Object[0]);
        }
        try {
            return PrettyPrinter.printObject(value, value.getClass(), Object.class);
        }
        catch (IllegalAccessException e2) {
            return "[Error: " + e2.getMessage() + "]";
        }
    }
    
    protected static boolean isSimpleType(final Object test) {
        return test instanceof String || Primitives.isWrapperType((Class)test.getClass());
    }
    
    public int getErrorCount() {
        return this.internalErrorCount.get();
    }
    
    public void setErrorCount(final int errorCount) {
        this.internalErrorCount.set(errorCount);
    }
    
    public int getMaxErrorCount() {
        return this.maxErrorCount;
    }
    
    public void setMaxErrorCount(final int maxErrorCount) {
        this.maxErrorCount = maxErrorCount;
    }
    
    public void addGlobalParameter(final String key, final Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be NULL.");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be NULL.");
        }
        this.globalParameters.put(key, value);
    }
    
    public Object getGlobalParameter(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be NULL.");
        }
        return this.globalParameters.get(key);
    }
    
    public void clearGlobalParameters() {
        this.globalParameters.clear();
    }
    
    public Set<String> globalParameters() {
        return this.globalParameters.keySet();
    }
    
    public String getSupportURL() {
        return this.supportURL;
    }
    
    public void setSupportURL(final String supportURL) {
        this.supportURL = supportURL;
    }
    
    public String getPrefix() {
        return this.prefix;
    }
    
    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }
    
    public Logger getLogger() {
        return this.logger;
    }
    
    public void setLogger(final Logger logger) {
        this.logger = logger;
    }
    
    static {
        REPORT_EXCEPTION_COUNT = new ReportType("Internal exception count: %s!");
    }
}
