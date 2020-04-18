package com.comphenix.protocol;

import java.util.Locale;
import com.comphenix.protocol.injector.PlayerInjectHooks;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import com.google.common.io.Files;
import com.google.common.base.Charsets;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;

public class ProtocolConfig
{
    private static final String LAST_UPDATE_FILE = "lastupdate";
    private static final String SECTION_GLOBAL = "global";
    private static final String SECTION_AUTOUPDATER = "auto updater";
    private static final String METRICS_ENABLED = "metrics";
    private static final String IGNORE_VERSION_CHECK = "ignore version check";
    private static final String BACKGROUND_COMPILER_ENABLED = "background compiler";
    private static final String DEBUG_MODE_ENABLED = "debug";
    private static final String DETAILED_ERROR = "detailed error";
    private static final String INJECTION_METHOD = "injection method";
    private static final String SCRIPT_ENGINE_NAME = "script engine";
    private static final String SUPPRESSED_REPORTS = "suppressed reports";
    private static final String UPDATER_NOTIFY = "notify";
    private static final String UPDATER_DOWNLAD = "download";
    private static final String UPDATER_DELAY = "delay";
    private static final long DEFAULT_UPDATER_DELAY = 43200L;
    private Plugin plugin;
    private Configuration config;
    private boolean loadingSections;
    private ConfigurationSection global;
    private ConfigurationSection updater;
    private long lastUpdateTime;
    private boolean configChanged;
    private boolean valuesChanged;
    private int modCount;
    
    public ProtocolConfig(final Plugin plugin) {
        this.plugin = plugin;
        this.reloadConfig();
    }
    
    public void reloadConfig() {
        this.configChanged = false;
        this.valuesChanged = false;
        ++this.modCount;
        this.config = (Configuration)this.plugin.getConfig();
        this.lastUpdateTime = this.loadLastUpdate();
        this.loadSections(!this.loadingSections);
    }
    
    private long loadLastUpdate() {
        final File dataFile = this.getLastUpdateFile();
        if (dataFile.exists()) {
            try {
                return Long.parseLong(Files.toString(dataFile, Charsets.UTF_8));
            }
            catch (NumberFormatException e) {
                this.plugin.getLogger().warning("Cannot parse " + dataFile + " as a number.");
            }
            catch (IOException e2) {
                this.plugin.getLogger().warning("Cannot read " + dataFile);
            }
        }
        return 0L;
    }
    
    private void saveLastUpdate(final long value) {
        final File dataFile = this.getLastUpdateFile();
        dataFile.getParentFile().mkdirs();
        if (dataFile.exists()) {
            dataFile.delete();
        }
        try {
            Files.write((CharSequence)Long.toString(value), dataFile, Charsets.UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot write " + dataFile, e);
        }
    }
    
    private File getLastUpdateFile() {
        return new File(this.plugin.getDataFolder(), "lastupdate");
    }
    
    private void loadSections(final boolean copyDefaults) {
        if (this.config != null) {
            this.global = this.config.getConfigurationSection("global");
        }
        if (this.global != null) {
            this.updater = this.global.getConfigurationSection("auto updater");
            if (this.updater.getValues(true).isEmpty()) {
                this.plugin.getLogger().warning("Updater section is missing, regenerate your config!");
            }
        }
        if (copyDefaults && (!this.getFile().exists() || this.global == null || this.updater == null)) {
            this.loadingSections = true;
            if (this.config != null) {
                this.config.options().copyDefaults(true);
            }
            this.plugin.saveDefaultConfig();
            this.plugin.reloadConfig();
            this.loadingSections = false;
            this.plugin.getLogger().info("Created default configuration.");
        }
    }
    
    private void setConfig(final ConfigurationSection section, final String path, final Object value) {
        this.configChanged = true;
        section.set(path, value);
    }
    
    private <T> T getGlobalValue(final String path, final T def) {
        try {
            return (T)this.global.get(path, (Object)def);
        }
        catch (Throwable ex) {
            return def;
        }
    }
    
    private <T> T getUpdaterValue(final String path, final T def) {
        try {
            return (T)this.updater.get(path, (Object)def);
        }
        catch (Throwable ex) {
            return def;
        }
    }
    
    public File getFile() {
        return new File(this.plugin.getDataFolder(), "config.yml");
    }
    
    public boolean isDetailedErrorReporting() {
        return this.getGlobalValue("detailed error", false);
    }
    
    public void setDetailedErrorReporting(final boolean value) {
        this.global.set("detailed error", (Object)value);
    }
    
    public boolean isAutoNotify() {
        return this.getUpdaterValue("notify", true);
    }
    
    public void setAutoNotify(final boolean value) {
        this.setConfig(this.updater, "notify", value);
        ++this.modCount;
    }
    
    public boolean isAutoDownload() {
        return this.updater != null && this.getUpdaterValue("download", false);
    }
    
    public void setAutoDownload(final boolean value) {
        this.setConfig(this.updater, "download", value);
        ++this.modCount;
    }
    
    public boolean isDebug() {
        return this.getGlobalValue("debug", false);
    }
    
    public void setDebug(final boolean value) {
        this.setConfig(this.global, "debug", value);
        ++this.modCount;
    }
    
    public ImmutableList<String> getSuppressedReports() {
        return (ImmutableList<String>)ImmutableList.copyOf((Collection)this.getGlobalValue("suppressed reports", new ArrayList()));
    }
    
    public void setSuppressedReports(final List<String> reports) {
        this.global.set("suppressed reports", (Object)Lists.newArrayList((Iterable)reports));
        ++this.modCount;
    }
    
    public long getAutoDelay() {
        return Math.max(this.getUpdaterValue("delay", 0), 43200L);
    }
    
    public void setAutoDelay(long delaySeconds) {
        if (delaySeconds < 43200L) {
            delaySeconds = 43200L;
        }
        this.setConfig(this.updater, "delay", delaySeconds);
        ++this.modCount;
    }
    
    public String getIgnoreVersionCheck() {
        return this.getGlobalValue("ignore version check", "");
    }
    
    public void setIgnoreVersionCheck(final String ignoreVersion) {
        this.setConfig(this.global, "ignore version check", ignoreVersion);
        ++this.modCount;
    }
    
    public boolean isMetricsEnabled() {
        return this.getGlobalValue("metrics", true);
    }
    
    public void setMetricsEnabled(final boolean enabled) {
        this.setConfig(this.global, "metrics", enabled);
        ++this.modCount;
    }
    
    public boolean isBackgroundCompilerEnabled() {
        return this.getGlobalValue("background compiler", true);
    }
    
    public void setBackgroundCompilerEnabled(final boolean enabled) {
        this.setConfig(this.global, "background compiler", enabled);
        ++this.modCount;
    }
    
    public long getAutoLastTime() {
        return this.lastUpdateTime;
    }
    
    public void setAutoLastTime(final long lastTimeSeconds) {
        this.valuesChanged = true;
        this.lastUpdateTime = lastTimeSeconds;
    }
    
    public String getScriptEngineName() {
        return this.getGlobalValue("script engine", "JavaScript");
    }
    
    public void setScriptEngineName(final String name) {
        this.setConfig(this.global, "script engine", name);
        ++this.modCount;
    }
    
    public PlayerInjectHooks getDefaultMethod() {
        return PlayerInjectHooks.NETWORK_SERVER_OBJECT;
    }
    
    public PlayerInjectHooks getInjectionMethod() throws IllegalArgumentException {
        final String text = this.global.getString("injection method");
        PlayerInjectHooks hook = this.getDefaultMethod();
        if (text != null) {
            hook = PlayerInjectHooks.valueOf(text.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
        }
        return hook;
    }
    
    public void setInjectionMethod(final PlayerInjectHooks hook) {
        this.setConfig(this.global, "injection method", hook.name());
        ++this.modCount;
    }
    
    public int getModificationCount() {
        return this.modCount;
    }
    
    public void saveAll() {
        if (this.valuesChanged) {
            this.saveLastUpdate(this.lastUpdateTime);
        }
        if (this.configChanged) {
            this.plugin.saveConfig();
        }
        this.valuesChanged = false;
        this.configChanged = false;
    }
}
