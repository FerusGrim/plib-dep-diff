package com.comphenix.protocol;

import com.comphenix.protocol.error.BasicErrorReporter;
import java.util.Arrays;
import org.apache.commons.lang.Validate;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.comphenix.protocol.error.ErrorReporter;
import org.bukkit.plugin.Plugin;
import java.util.List;

public class ProtocolLibrary
{
    public static final String MINIMUM_MINECRAFT_VERSION = "1.8";
    public static final String MAXIMUM_MINECRAFT_VERSION = "1.12.2";
    public static final String MINECRAFT_LAST_RELEASE_DATE = "2017-09-18";
    public static final List<String> INCOMPATIBLE;
    private static Plugin plugin;
    private static ProtocolConfig config;
    private static ProtocolManager manager;
    private static ErrorReporter reporter;
    private static ListeningScheduledExecutorService executorAsync;
    private static ListeningScheduledExecutorService executorSync;
    private static boolean updatesDisabled;
    private static boolean initialized;
    
    protected static void init(final Plugin plugin, final ProtocolConfig config, final ProtocolManager manager, final ErrorReporter reporter, final ListeningScheduledExecutorService executorAsync, final ListeningScheduledExecutorService executorSync) {
        Validate.isTrue(!ProtocolLibrary.initialized, "ProtocolLib has already been initialized.");
        ProtocolLibrary.plugin = plugin;
        ProtocolLibrary.config = config;
        ProtocolLibrary.manager = manager;
        ProtocolLibrary.reporter = reporter;
        ProtocolLibrary.executorAsync = executorAsync;
        ProtocolLibrary.executorSync = executorSync;
        ProtocolLibrary.initialized = true;
    }
    
    public static Plugin getPlugin() {
        return ProtocolLibrary.plugin;
    }
    
    public static ProtocolConfig getConfig() {
        return ProtocolLibrary.config;
    }
    
    public static ProtocolManager getProtocolManager() {
        return ProtocolLibrary.manager;
    }
    
    public static ErrorReporter getErrorReporter() {
        return ProtocolLibrary.reporter;
    }
    
    public static ListeningScheduledExecutorService getExecutorAsync() {
        return ProtocolLibrary.executorAsync;
    }
    
    public static ListeningScheduledExecutorService getExecutorSync() {
        return ProtocolLibrary.executorSync;
    }
    
    public static void disableUpdates() {
        ProtocolLibrary.updatesDisabled = true;
    }
    
    public static boolean updatesDisabled() {
        return ProtocolLibrary.updatesDisabled;
    }
    
    static {
        INCOMPATIBLE = Arrays.asList("TagAPI");
        ProtocolLibrary.reporter = new BasicErrorReporter();
    }
}
