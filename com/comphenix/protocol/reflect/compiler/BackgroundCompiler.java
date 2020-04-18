package com.comphenix.protocol.reflect.compiler;

import java.util.concurrent.TimeUnit;
import java.lang.management.MemoryUsage;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.RejectedExecutionException;
import java.util.Iterator;
import com.comphenix.protocol.error.Report;
import java.util.concurrent.Callable;
import com.google.common.collect.Lists;
import com.comphenix.protocol.reflect.StructureModifier;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.collect.Maps;
import com.comphenix.protocol.error.ErrorReporter;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.Map;
import com.comphenix.protocol.error.ReportType;

public class BackgroundCompiler
{
    public static final ReportType REPORT_CANNOT_COMPILE_STRUCTURE_MODIFIER;
    public static final ReportType REPORT_CANNOT_SCHEDULE_COMPILATION;
    public static final String THREAD_FORMAT = "ProtocolLib-StructureCompiler %s";
    public static final int SHUTDOWN_DELAY_MS = 2000;
    public static final double DEFAULT_DISABLE_AT_PERM_GEN = 0.65;
    private static BackgroundCompiler backgroundCompiler;
    private Map<StructureCompiler.StructureKey, List<CompileListener<?>>> listeners;
    private Object listenerLock;
    private StructureCompiler compiler;
    private boolean enabled;
    private boolean shuttingDown;
    private ExecutorService executor;
    private ErrorReporter reporter;
    private double disablePermGenFraction;
    
    public static BackgroundCompiler getInstance() {
        return BackgroundCompiler.backgroundCompiler;
    }
    
    public static void setInstance(final BackgroundCompiler backgroundCompiler) {
        BackgroundCompiler.backgroundCompiler = backgroundCompiler;
    }
    
    public BackgroundCompiler(final ClassLoader loader, final ErrorReporter reporter) {
        this.listeners = (Map<StructureCompiler.StructureKey, List<CompileListener<?>>>)Maps.newHashMap();
        this.listenerLock = new Object();
        this.disablePermGenFraction = 0.65;
        final ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ProtocolLib-StructureCompiler %s").build();
        this.initializeCompiler(loader, reporter, Executors.newSingleThreadExecutor(factory));
    }
    
    public BackgroundCompiler(final ClassLoader loader, final ErrorReporter reporter, final ExecutorService executor) {
        this.listeners = (Map<StructureCompiler.StructureKey, List<CompileListener<?>>>)Maps.newHashMap();
        this.listenerLock = new Object();
        this.disablePermGenFraction = 0.65;
        this.initializeCompiler(loader, reporter, executor);
    }
    
    private void initializeCompiler(final ClassLoader loader, final ErrorReporter reporter, final ExecutorService executor) {
        if (loader == null) {
            throw new IllegalArgumentException("loader cannot be NULL");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor cannot be NULL");
        }
        if (reporter == null) {
            throw new IllegalArgumentException("reporter cannot be NULL.");
        }
        this.compiler = new StructureCompiler(loader);
        this.reporter = reporter;
        this.executor = executor;
        this.enabled = true;
    }
    
    public void scheduleCompilation(final Map<Class, StructureModifier> cache, final Class key) {
        final StructureModifier<Object> uncompiled = cache.get(key);
        if (uncompiled != null) {
            this.scheduleCompilation(uncompiled, new CompileListener<Object>() {
                @Override
                public void onCompiled(final StructureModifier<Object> compiledModifier) {
                    cache.put(key, compiledModifier);
                }
            });
        }
    }
    
    public <TKey> void scheduleCompilation(final StructureModifier<TKey> uncompiled, final CompileListener<TKey> listener) {
        if (this.enabled && !this.shuttingDown) {
            if (this.getPermGenUsage() > this.disablePermGenFraction) {
                return;
            }
            if (this.executor == null || this.executor.isShutdown()) {
                return;
            }
            final StructureCompiler.StructureKey key = new StructureCompiler.StructureKey(uncompiled);
            synchronized (this.listenerLock) {
                final List list = this.listeners.get(key);
                if (this.listeners.containsKey(key)) {
                    list.add(listener);
                    return;
                }
                this.listeners.put(key, Lists.newArrayList((Object[])new CompileListener[] { listener }));
            }
            final Callable<?> worker = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    StructureModifier<TKey> modifier = uncompiled;
                    List list = null;
                    try {
                        modifier = BackgroundCompiler.this.compiler.compile(modifier);
                        synchronized (BackgroundCompiler.this.listenerLock) {
                            list = BackgroundCompiler.this.listeners.get(key);
                            if (list != null) {
                                list = Lists.newArrayList((Iterable)list);
                            }
                        }
                        if (list != null) {
                            for (final Object compileListener : list) {
                                ((CompileListener)compileListener).onCompiled(modifier);
                            }
                            synchronized (BackgroundCompiler.this.listenerLock) {
                                list = BackgroundCompiler.this.listeners.remove(key);
                            }
                        }
                    }
                    catch (OutOfMemoryError e) {
                        BackgroundCompiler.this.setEnabled(false);
                        throw e;
                    }
                    catch (ThreadDeath e2) {
                        BackgroundCompiler.this.setEnabled(false);
                        throw e2;
                    }
                    catch (Throwable e3) {
                        BackgroundCompiler.this.setEnabled(false);
                        BackgroundCompiler.this.reporter.reportDetailed(BackgroundCompiler.this, Report.newBuilder(BackgroundCompiler.REPORT_CANNOT_COMPILE_STRUCTURE_MODIFIER).callerParam(uncompiled).error(e3));
                    }
                    return modifier;
                }
            };
            try {
                if (this.compiler.lookupClassLoader(uncompiled)) {
                    try {
                        worker.call();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    this.executor.submit(worker);
                }
            }
            catch (RejectedExecutionException e2) {
                this.reporter.reportWarning(this, Report.newBuilder(BackgroundCompiler.REPORT_CANNOT_SCHEDULE_COMPILATION).error(e2));
            }
        }
    }
    
    public <TKey> void addListener(final StructureModifier<TKey> uncompiled, final CompileListener<TKey> listener) {
        synchronized (this.listenerLock) {
            final StructureCompiler.StructureKey key = new StructureCompiler.StructureKey(uncompiled);
            final List list = this.listeners.get(key);
            if (list != null) {
                list.add(listener);
            }
        }
    }
    
    private double getPermGenUsage() {
        for (final MemoryPoolMXBean item : ManagementFactory.getMemoryPoolMXBeans()) {
            if (item.getName().contains("Perm Gen")) {
                final MemoryUsage usage = item.getUsage();
                return usage.getUsed() / (double)usage.getCommitted();
            }
        }
        return 0.0;
    }
    
    public void shutdownAll() {
        this.shutdownAll(2000L, TimeUnit.MILLISECONDS);
    }
    
    public void shutdownAll(final long timeout, final TimeUnit unit) {
        this.setEnabled(false);
        this.shuttingDown = true;
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(timeout, unit);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
    
    public double getDisablePermGenFraction() {
        return this.disablePermGenFraction;
    }
    
    public void setDisablePermGenFraction(final double fraction) {
        this.disablePermGenFraction = fraction;
    }
    
    public StructureCompiler getCompiler() {
        return this.compiler;
    }
    
    static {
        REPORT_CANNOT_COMPILE_STRUCTURE_MODIFIER = new ReportType("Cannot compile structure. Disabing compiler.");
        REPORT_CANNOT_SCHEDULE_COMPILATION = new ReportType("Unable to schedule compilation task.");
    }
}
