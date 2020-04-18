package com.comphenix.protocol.updater;

import com.comphenix.protocol.utility.Util;
import java.io.File;
import com.comphenix.protocol.ProtocolLib;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.utility.MinecraftVersion;
import java.util.concurrent.CopyOnWriteArrayList;
import com.comphenix.protocol.error.ReportType;
import java.util.List;
import org.bukkit.plugin.Plugin;

public abstract class Updater
{
    protected Plugin plugin;
    protected String versionName;
    protected String versionLink;
    protected String versionType;
    protected String versionGameVersion;
    protected String versionFileName;
    protected UpdateType type;
    protected boolean announce;
    protected Thread thread;
    protected UpdateResult result;
    protected List<Runnable> listeners;
    public static final ReportType REPORT_CANNOT_UPDATE_PLUGIN;
    
    protected Updater(final Plugin plugin, final UpdateType type, final boolean announce) {
        this.result = UpdateResult.SUCCESS;
        this.listeners = new CopyOnWriteArrayList<Runnable>();
        this.plugin = plugin;
        this.type = type;
        this.announce = announce;
    }
    
    public boolean versionCheck(final String title) {
        if (this.type != UpdateType.NO_VERSION_CHECK) {
            String version = this.plugin.getDescription().getVersion();
            final String[] split = title.split(" ");
            String remote = "Unknown";
            if (split.length == 2) {
                remote = split[1];
            }
            else {
                if (!(this instanceof SpigotUpdater)) {
                    final String authorInfo = (this.plugin.getDescription().getAuthors().size() == 0) ? "" : (" (" + this.plugin.getDescription().getAuthors().get(0) + ")");
                    this.plugin.getLogger().warning("The author of this plugin " + authorInfo + " has misconfigured their Auto Update system");
                    this.plugin.getLogger().warning("File versions should follow the format 'PluginName VERSION[-SNAPSHOT]'");
                    this.plugin.getLogger().warning("Please notify the author of this error.");
                    this.result = UpdateResult.FAIL_NOVERSION;
                    return false;
                }
                remote = split[0];
            }
            boolean devBuild = false;
            if (version.contains("-SNAPSHOT") || version.contains("-BETA")) {
                devBuild = true;
                version = version.substring(0, version.indexOf("-"));
            }
            if (remote.startsWith("v")) {
                remote = remote.substring(1);
            }
            if (version.contains("-b")) {
                version = version.substring(0, version.lastIndexOf("-"));
            }
            final MinecraftVersion parsedRemote = new MinecraftVersion(remote);
            final MinecraftVersion parsedCurrent = new MinecraftVersion(version);
            if (devBuild && parsedRemote.equals(parsedCurrent)) {
                return !remote.contains("-BETA") && !remote.contains("-SNAPSHOT");
            }
            if (parsedRemote.compareTo(parsedCurrent) <= 0) {
                this.result = UpdateResult.NO_UPDATE;
                return false;
            }
        }
        return true;
    }
    
    public void addListener(final Runnable listener) {
        this.listeners.add((Runnable)Preconditions.checkNotNull((Object)listener, (Object)"listener cannot be NULL"));
    }
    
    public boolean removeListener(final Runnable listener) {
        return this.listeners.remove(listener);
    }
    
    public String getResult() {
        this.waitForThread();
        return this.result.toString();
    }
    
    public String getLatestType() {
        this.waitForThread();
        return this.versionType;
    }
    
    public String getLatestGameVersion() {
        this.waitForThread();
        return this.versionGameVersion;
    }
    
    public String getLatestName() {
        this.waitForThread();
        return this.versionName;
    }
    
    public String getLatestFileLink() {
        this.waitForThread();
        return this.versionLink;
    }
    
    protected void waitForThread() {
        if (this.thread != null && this.thread.isAlive()) {
            try {
                this.thread.join();
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public boolean isChecking() {
        return this.thread != null && this.thread.isAlive();
    }
    
    public static Updater create(final ProtocolLib protocolLib, final int id, final File file, final UpdateType type, final boolean announce) {
        if (Util.isUsingSpigot()) {
            return new SpigotUpdater((Plugin)protocolLib, type, announce);
        }
        return new BukkitUpdater((Plugin)protocolLib, id, file, type, announce);
    }
    
    public abstract void start(final UpdateType p0);
    
    public boolean shouldNotify() {
        switch (this.result) {
            case SPIGOT_UPDATE_AVAILABLE:
            case SUCCESS:
            case UPDATE_AVAILABLE: {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public abstract String getRemoteVersion();
    
    static {
        REPORT_CANNOT_UPDATE_PLUGIN = new ReportType("Cannot update ProtocolLib.");
    }
    
    public enum UpdateType
    {
        DEFAULT, 
        NO_VERSION_CHECK, 
        NO_DOWNLOAD;
    }
    
    public enum UpdateResult
    {
        SUCCESS("The updater found an update, and has readied it to be loaded the next time the server restarts/reloads."), 
        NO_UPDATE("The updater did not find an update, and nothing was downloaded."), 
        DISABLED("The server administrator has disabled the updating system"), 
        FAIL_DOWNLOAD("The updater found an update, but was unable to download it."), 
        FAIL_DBO("For some reason, the updater was unable to contact dev.bukkit.org to download the file."), 
        FAIL_NOVERSION("When running the version check, the file on DBO did not contain the a version in the format 'vVersion' such as 'v1.0'."), 
        FAIL_BADID("The id provided by the plugin running the updater was invalid and doesn't exist on DBO."), 
        FAIL_APIKEY("The server administrator has improperly configured their API key in the configuration"), 
        UPDATE_AVAILABLE("The updater found an update, but because of the UpdateType being set to NO_DOWNLOAD, it wasn't downloaded."), 
        SPIGOT_UPDATE_AVAILABLE("The updater found an update: %s (Running %s). Download at %s");
        
        private final String description;
        
        private UpdateResult(final String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return this.description;
        }
    }
}
