package com.comphenix.protocol;

import com.comphenix.protocol.error.BasicErrorReporter;
import org.bukkit.command.CommandSender;
import com.comphenix.protocol.async.AsyncFilterManager;
import org.bukkit.command.PluginCommand;
import java.util.regex.Matcher;
import java.io.File;
import java.util.regex.Pattern;
import java.util.Iterator;
import org.bukkit.plugin.PluginManager;
import org.bukkit.Server;
import java.io.IOException;
import org.bukkit.command.CommandExecutor;
import com.comphenix.protocol.utility.ChatExtensions;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import com.google.common.collect.Iterables;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import java.util.Set;
import com.comphenix.protocol.error.DelegatedErrorReporter;
import com.comphenix.protocol.injector.PlayerInjectHooks;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.injector.PacketFilterManager;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.error.DetailedErrorReporter;
import org.bukkit.plugin.Plugin;
import com.comphenix.executors.BukkitExecutors;
import com.comphenix.protocol.utility.EnhancerFactory;
import java.util.logging.Handler;
import java.util.logging.Logger;
import com.comphenix.protocol.updater.Updater;
import com.comphenix.protocol.injector.DelayedSingleTask;
import com.comphenix.protocol.reflect.compiler.BackgroundCompiler;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.comphenix.protocol.metrics.Statistics;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.injector.InternalManager;
import com.comphenix.protocol.error.ReportType;
import org.bukkit.plugin.java.JavaPlugin;

public class ProtocolLib extends JavaPlugin
{
    public static final ReportType REPORT_CANNOT_LOAD_CONFIG;
    public static final ReportType REPORT_CANNOT_DELETE_CONFIG;
    public static final ReportType REPORT_CANNOT_PARSE_INJECTION_METHOD;
    public static final ReportType REPORT_PLUGIN_LOAD_ERROR;
    public static final ReportType REPORT_PLUGIN_ENABLE_ERROR;
    public static final ReportType REPORT_METRICS_IO_ERROR;
    public static final ReportType REPORT_METRICS_GENERIC_ERROR;
    public static final ReportType REPORT_CANNOT_PARSE_MINECRAFT_VERSION;
    public static final ReportType REPORT_CANNOT_DETECT_CONFLICTING_PLUGINS;
    public static final ReportType REPORT_CANNOT_REGISTER_COMMAND;
    public static final ReportType REPORT_CANNOT_CREATE_TIMEOUT_TASK;
    public static final ReportType REPORT_CANNOT_UPDATE_PLUGIN;
    static final String BUKKIT_DEV_SLUG = "protocollib";
    static final int BUKKIT_DEV_ID = 45564;
    static final long MILLI_PER_SECOND = 1000L;
    private static final String PERMISSION_INFO = "protocol.info";
    private static InternalManager protocolManager;
    private static ErrorReporter reporter;
    private static ProtocolConfig config;
    private Statistics statistics;
    private static ListeningScheduledExecutorService executorAsync;
    private static ListeningScheduledExecutorService executorSync;
    private BackgroundCompiler backgroundCompiler;
    private int packetTask;
    private int tickCounter;
    private static final int ASYNC_MANAGER_DELAY = 1;
    private DelayedSingleTask unhookTask;
    private int configExpectedMod;
    private Updater updater;
    public static boolean UPDATES_DISABLED;
    private static Logger logger;
    private Handler redirectHandler;
    private CommandProtocol commandProtocol;
    private CommandPacket commandPacket;
    private CommandFilter commandFilter;
    private PacketLogging packetLogging;
    private boolean skipDisable;
    
    public ProtocolLib() {
        this.packetTask = -1;
        this.tickCounter = 0;
        this.configExpectedMod = -1;
    }
    
    public void onLoad() {
        ProtocolLib.logger = this.getLogger();
        ProtocolLogger.init(this);
        EnhancerFactory.getInstance().setClassLoader(this.getClassLoader());
        ProtocolLib.executorAsync = (ListeningScheduledExecutorService)BukkitExecutors.newAsynchronous((Plugin)this);
        ProtocolLib.executorSync = (ListeningScheduledExecutorService)BukkitExecutors.newSynchronous((Plugin)this);
        final DetailedErrorReporter detailedReporter = new DetailedErrorReporter((Plugin)this);
        ProtocolLib.reporter = this.getFilteredReporter(detailedReporter);
        this.saveDefaultConfig();
        this.reloadConfig();
        try {
            ProtocolLib.config = new ProtocolConfig((Plugin)this);
        }
        catch (Exception e) {
            ProtocolLib.reporter.reportWarning(this, Report.newBuilder(ProtocolLib.REPORT_CANNOT_LOAD_CONFIG).error(e));
            if (this.deleteConfig()) {
                ProtocolLib.config = new ProtocolConfig((Plugin)this);
            }
            else {
                ProtocolLib.reporter.reportWarning(this, Report.newBuilder(ProtocolLib.REPORT_CANNOT_DELETE_CONFIG));
            }
        }
        if (ProtocolLib.config.isDebug()) {
            ProtocolLib.logger.warning("Debug mode is enabled!");
        }
        if (ProtocolLib.config.isDetailedErrorReporting()) {
            detailedReporter.setDetailedReporting(true);
            ProtocolLib.logger.warning("Detailed error reporting enabled!");
        }
        try {
            this.checkConflictingVersions();
            final MinecraftVersion version = this.verifyMinecraftVersion();
            this.updater = Updater.create(this, 45564, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, true);
            this.unhookTask = new DelayedSingleTask((Plugin)this);
            ProtocolLib.protocolManager = PacketFilterManager.newBuilder().classLoader(this.getClassLoader()).server(this.getServer()).library((Plugin)this).minecraftVersion(version).unhookTask(this.unhookTask).reporter(ProtocolLib.reporter).build();
            ProtocolLibrary.init((Plugin)this, ProtocolLib.config, ProtocolLib.protocolManager, ProtocolLib.reporter, ProtocolLib.executorAsync, ProtocolLib.executorSync);
            detailedReporter.addGlobalParameter("manager", ProtocolLib.protocolManager);
            try {
                final PlayerInjectHooks hook = ProtocolLib.config.getInjectionMethod();
                if (!ProtocolLib.protocolManager.getPlayerHook().equals(hook)) {
                    ProtocolLib.logger.info("Changing player hook from " + ProtocolLib.protocolManager.getPlayerHook() + " to " + hook);
                    ProtocolLib.protocolManager.setPlayerHook(hook);
                }
            }
            catch (IllegalArgumentException e2) {
                ProtocolLib.reporter.reportWarning(ProtocolLib.config, Report.newBuilder(ProtocolLib.REPORT_CANNOT_PARSE_INJECTION_METHOD).error(e2));
            }
            this.initializeCommands();
            this.setupBroadcastUsers("protocol.info");
        }
        catch (OutOfMemoryError e3) {
            throw e3;
        }
        catch (Throwable e4) {
            ProtocolLib.reporter.reportDetailed(this, Report.newBuilder(ProtocolLib.REPORT_PLUGIN_LOAD_ERROR).error(e4).callerParam(ProtocolLib.protocolManager));
            this.disablePlugin();
        }
    }
    
    private void initializeCommands() {
        for (final ProtocolCommand command : ProtocolCommand.values()) {
            try {
                switch (command) {
                    case PROTOCOL: {
                        this.commandProtocol = new CommandProtocol(ProtocolLib.reporter, (Plugin)this, this.updater, ProtocolLib.config);
                        break;
                    }
                    case FILTER: {
                        this.commandFilter = new CommandFilter(ProtocolLib.reporter, (Plugin)this, ProtocolLib.config);
                        break;
                    }
                    case PACKET: {
                        this.commandPacket = new CommandPacket(ProtocolLib.reporter, (Plugin)this, ProtocolLib.logger, this.commandFilter, ProtocolLib.protocolManager);
                        break;
                    }
                    case LOGGING: {
                        this.packetLogging = new PacketLogging((Plugin)this, ProtocolLib.protocolManager);
                        break;
                    }
                }
            }
            catch (OutOfMemoryError e) {
                throw e;
            }
            catch (LinkageError e2) {
                ProtocolLib.logger.warning("Failed to register command " + command.name() + ": " + e2);
            }
            catch (Throwable e3) {
                ProtocolLib.reporter.reportWarning(this, Report.newBuilder(ProtocolLib.REPORT_CANNOT_REGISTER_COMMAND).messageParam(command.name(), e3.getMessage()).error(e3));
            }
        }
    }
    
    private ErrorReporter getFilteredReporter(final ErrorReporter reporter) {
        return new DelegatedErrorReporter(reporter) {
            private int lastModCount = -1;
            private Set<String> reports = Sets.newHashSet();
            
            @Override
            protected Report filterReport(final Object sender, final Report report, final boolean detailed) {
                try {
                    final String canonicalName = ReportType.getReportName(sender, report.getType());
                    final String reportName = ((String)Iterables.getLast(Splitter.on("#").split((CharSequence)canonicalName))).toUpperCase();
                    if (ProtocolLib.config != null && ProtocolLib.config.getModificationCount() != this.lastModCount) {
                        this.reports = (Set<String>)Sets.newHashSet((Iterable)ProtocolLib.config.getSuppressedReports());
                        this.lastModCount = ProtocolLib.config.getModificationCount();
                    }
                    if (this.reports.contains(canonicalName) || this.reports.contains(reportName)) {
                        return null;
                    }
                }
                catch (Exception e) {
                    ProtocolLib.logger.warning("Error filtering reports: " + e.toString());
                }
                return report;
            }
        };
    }
    
    private boolean deleteConfig() {
        return ProtocolLib.config.getFile().delete();
    }
    
    public void reloadConfig() {
        super.reloadConfig();
        if (ProtocolLib.config != null) {
            ProtocolLib.config.reloadConfig();
        }
    }
    
    private void setupBroadcastUsers(final String permission) {
        if (this.redirectHandler != null) {
            return;
        }
        this.redirectHandler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    ProtocolLib.this.commandPacket.broadcastMessageSilently(record.getMessage(), permission);
                }
            }
            
            @Override
            public void flush() {
            }
            
            @Override
            public void close() throws SecurityException {
            }
        };
        ProtocolLib.logger.addHandler(this.redirectHandler);
    }
    
    public void onEnable() {
        try {
            final Server server = this.getServer();
            final PluginManager manager = server.getPluginManager();
            if (manager == null) {
                return;
            }
            if (ProtocolLib.protocolManager == null) {
                final Logger directLogging = Logger.getLogger("Minecraft");
                final String[] message = { " ProtocolLib does not support plugin reloaders! ", " Please use the built-in reload command! " };
                for (final String line : ChatExtensions.toFlowerBox(message, "*", 3, 1)) {
                    directLogging.severe(line);
                }
                this.disablePlugin();
                return;
            }
            this.checkForIncompatibility(manager);
            if (this.backgroundCompiler == null && ProtocolLib.config.isBackgroundCompilerEnabled()) {
                BackgroundCompiler.setInstance(this.backgroundCompiler = new BackgroundCompiler(this.getClassLoader(), ProtocolLib.reporter));
                ProtocolLib.logger.info("Started structure compiler thread.");
            }
            else {
                ProtocolLib.logger.info("Structure compiler thread has been disabled.");
            }
            this.registerCommand("protocol", (CommandExecutor)this.commandProtocol);
            this.registerCommand("packet", (CommandExecutor)this.commandPacket);
            this.registerCommand("filter", (CommandExecutor)this.commandFilter);
            this.registerCommand("packetlog", (CommandExecutor)this.packetLogging);
            ProtocolLib.protocolManager.registerEvents(manager, (Plugin)this);
            this.createPacketTask(server);
        }
        catch (OutOfMemoryError e) {
            throw e;
        }
        catch (Throwable e2) {
            ProtocolLib.reporter.reportDetailed(this, Report.newBuilder(ProtocolLib.REPORT_PLUGIN_ENABLE_ERROR).error(e2));
            this.disablePlugin();
            return;
        }
        try {
            if (ProtocolLib.config.isMetricsEnabled()) {
                this.statistics = new Statistics(this);
            }
        }
        catch (OutOfMemoryError e) {
            throw e;
        }
        catch (IOException e3) {
            ProtocolLib.reporter.reportDetailed(this, Report.newBuilder(ProtocolLib.REPORT_METRICS_IO_ERROR).error(e3).callerParam(this.statistics));
        }
        catch (Throwable e2) {
            ProtocolLib.reporter.reportDetailed(this, Report.newBuilder(ProtocolLib.REPORT_METRICS_GENERIC_ERROR).error(e2).callerParam(this.statistics));
        }
    }
    
    private void checkForIncompatibility(final PluginManager manager) {
        for (final String plugin : ProtocolLibrary.INCOMPATIBLE) {
            if (manager.getPlugin(plugin) != null) {
                if (plugin.equals("TagAPI")) {
                    final Plugin iTag = manager.getPlugin("iTag");
                    if (iTag != null && !iTag.getDescription().getVersion().startsWith("1.0")) {
                        continue;
                    }
                    ProtocolLib.logger.severe("Detected incompatible plugin: TagAPI");
                }
                else {
                    ProtocolLib.logger.severe("Detected incompatible plugin: " + plugin);
                }
            }
        }
    }
    
    private MinecraftVersion verifyMinecraftVersion() {
        final MinecraftVersion minimum = new MinecraftVersion("1.8");
        final MinecraftVersion maximum = new MinecraftVersion("1.15.1");
        try {
            final MinecraftVersion current = new MinecraftVersion(this.getServer());
            if (!ProtocolLib.config.getIgnoreVersionCheck().equals(current.getVersion())) {
                if (current.compareTo(minimum) < 0) {
                    ProtocolLib.logger.warning("Version " + current + " is lower than the minimum " + minimum);
                }
                if (current.compareTo(maximum) > 0) {
                    ProtocolLib.logger.warning("Version " + current + " has not yet been tested! Proceed with caution.");
                }
            }
            return current;
        }
        catch (Exception e) {
            ProtocolLib.reporter.reportWarning(this, Report.newBuilder(ProtocolLib.REPORT_CANNOT_PARSE_MINECRAFT_VERSION).error(e).messageParam(maximum));
            return maximum;
        }
    }
    
    private void checkConflictingVersions() {
        final Pattern ourPlugin = Pattern.compile("ProtocolLib-(.*)\\.jar");
        final MinecraftVersion currentVersion = new MinecraftVersion(this.getDescription().getVersion());
        MinecraftVersion newestVersion = null;
        final File loadedFile = this.getFile();
        try {
            final File pluginFolder = this.getDataFolder().getParentFile();
            final File[] candidates = pluginFolder.listFiles();
            if (candidates != null) {
                for (final File candidate : candidates) {
                    if (candidate.isFile() && !candidate.equals(loadedFile)) {
                        final Matcher match = ourPlugin.matcher(candidate.getName());
                        if (match.matches()) {
                            final MinecraftVersion version = new MinecraftVersion(match.group(1));
                            if (candidate.length() == 0L) {
                                ProtocolLib.logger.info((candidate.delete() ? "Deleted " : "Could not delete ") + candidate);
                            }
                            else if (newestVersion == null || newestVersion.compareTo(version) < 0) {
                                newestVersion = version;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            ProtocolLib.reporter.reportWarning(this, Report.newBuilder(ProtocolLib.REPORT_CANNOT_DETECT_CONFLICTING_PLUGINS).error(e));
        }
        if (newestVersion != null && currentVersion.compareTo(newestVersion) < 0) {
            this.skipDisable = true;
            throw new IllegalStateException(String.format("Detected a newer version of ProtocolLib (%s) in plugin folder than the current (%s). Disabling.", newestVersion.getVersion(), currentVersion.getVersion()));
        }
    }
    
    private void registerCommand(final String name, final CommandExecutor executor) {
        try {
            if (executor == null) {
                return;
            }
            final PluginCommand command = this.getCommand(name);
            if (command == null) {
                throw new RuntimeException("plugin.yml might be corrupt.");
            }
            command.setExecutor(executor);
        }
        catch (RuntimeException e) {
            ProtocolLib.reporter.reportWarning(this, Report.newBuilder(ProtocolLib.REPORT_CANNOT_REGISTER_COMMAND).messageParam(name, e.getMessage()).error(e));
        }
    }
    
    private void disablePlugin() {
        this.getServer().getPluginManager().disablePlugin((Plugin)this);
    }
    
    private void createPacketTask(final Server server) {
        try {
            if (this.packetTask >= 0) {
                throw new IllegalStateException("Packet task has already been created");
            }
            this.packetTask = server.getScheduler().scheduleSyncRepeatingTask((Plugin)this, (Runnable)new Runnable() {
                @Override
                public void run() {
                    final AsyncFilterManager manager = (AsyncFilterManager)ProtocolLib.protocolManager.getAsynchronousManager();
                    manager.sendProcessedPackets(ProtocolLib.this.tickCounter++, true);
                    ProtocolLib.this.updateConfiguration();
                    if (!ProtocolLib.UPDATES_DISABLED && ProtocolLib.this.tickCounter % 20 == 0) {
                        ProtocolLib.this.checkUpdates();
                    }
                }
            }, 1L, 1L);
        }
        catch (OutOfMemoryError e) {
            throw e;
        }
        catch (Throwable e2) {
            if (this.packetTask == -1) {
                ProtocolLib.reporter.reportDetailed(this, Report.newBuilder(ProtocolLib.REPORT_CANNOT_CREATE_TIMEOUT_TASK).error(e2));
            }
        }
    }
    
    private void updateConfiguration() {
        if (ProtocolLib.config != null && ProtocolLib.config.getModificationCount() != this.configExpectedMod) {
            this.configExpectedMod = ProtocolLib.config.getModificationCount();
            ProtocolLib.protocolManager.setDebug(ProtocolLib.config.isDebug());
        }
    }
    
    private void checkUpdates() {
        final long currentTime = System.currentTimeMillis() / 1000L;
        try {
            final long updateTime = ProtocolLib.config.getAutoLastTime() + ProtocolLib.config.getAutoDelay();
            if (currentTime > updateTime && !this.updater.isChecking()) {
                if (ProtocolLib.config.isAutoDownload()) {
                    this.commandProtocol.updateVersion((CommandSender)this.getServer().getConsoleSender(), false);
                }
                else if (ProtocolLib.config.isAutoNotify()) {
                    this.commandProtocol.checkVersion((CommandSender)this.getServer().getConsoleSender(), false);
                }
                else {
                    this.commandProtocol.updateFinished();
                }
            }
        }
        catch (Exception e) {
            ProtocolLib.reporter.reportDetailed(this, Report.newBuilder(ProtocolLib.REPORT_CANNOT_UPDATE_PLUGIN).error(e));
            ProtocolLib.UPDATES_DISABLED = true;
        }
    }
    
    public void onDisable() {
        if (this.skipDisable) {
            return;
        }
        if (this.backgroundCompiler != null) {
            this.backgroundCompiler.shutdownAll();
            BackgroundCompiler.setInstance(this.backgroundCompiler = null);
        }
        if (this.packetTask >= 0) {
            this.getServer().getScheduler().cancelTask(this.packetTask);
            this.packetTask = -1;
        }
        if (this.redirectHandler != null) {
            ProtocolLib.logger.removeHandler(this.redirectHandler);
        }
        if (ProtocolLib.protocolManager != null) {
            ProtocolLib.protocolManager.close();
            if (this.unhookTask != null) {
                this.unhookTask.close();
            }
            ProtocolLib.protocolManager = null;
            this.statistics = null;
            ProtocolLib.reporter = new BasicErrorReporter();
        }
    }
    
    public Statistics getStatistics() {
        return this.statistics;
    }
    
    public ProtocolConfig getProtocolConfig() {
        return ProtocolLib.config;
    }
    
    static {
        REPORT_CANNOT_LOAD_CONFIG = new ReportType("Cannot load configuration");
        REPORT_CANNOT_DELETE_CONFIG = new ReportType("Cannot delete old ProtocolLib configuration.");
        REPORT_CANNOT_PARSE_INJECTION_METHOD = new ReportType("Cannot parse injection method. Using default.");
        REPORT_PLUGIN_LOAD_ERROR = new ReportType("Cannot load ProtocolLib.");
        REPORT_PLUGIN_ENABLE_ERROR = new ReportType("Cannot enable ProtocolLib.");
        REPORT_METRICS_IO_ERROR = new ReportType("Unable to enable metrics due to network problems.");
        REPORT_METRICS_GENERIC_ERROR = new ReportType("Unable to enable metrics due to network problems.");
        REPORT_CANNOT_PARSE_MINECRAFT_VERSION = new ReportType("Unable to retrieve current Minecraft version. Assuming %s");
        REPORT_CANNOT_DETECT_CONFLICTING_PLUGINS = new ReportType("Unable to detect conflicting plugin versions.");
        REPORT_CANNOT_REGISTER_COMMAND = new ReportType("Cannot register command %s: %s");
        REPORT_CANNOT_CREATE_TIMEOUT_TASK = new ReportType("Unable to create packet timeout task.");
        REPORT_CANNOT_UPDATE_PLUGIN = new ReportType("Cannot perform automatic updates.");
        ProtocolLib.reporter = new BasicErrorReporter();
    }
    
    private enum ProtocolCommand
    {
        FILTER, 
        PACKET, 
        PROTOCOL, 
        LOGGING;
    }
}
