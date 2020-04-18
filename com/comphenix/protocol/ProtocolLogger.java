package com.comphenix.protocol;

import java.text.MessageFormat;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public class ProtocolLogger
{
    private static Plugin plugin;
    
    protected static void init(final Plugin plugin) {
        ProtocolLogger.plugin = plugin;
    }
    
    private static boolean isDebugEnabled() {
        try {
            return ProtocolLogger.plugin.getConfig().getBoolean("global.debug", false);
        }
        catch (Throwable ex) {
            return false;
        }
    }
    
    public static void log(final Level level, final String message, final Object... args) {
        ProtocolLogger.plugin.getLogger().log(level, MessageFormat.format(message, args));
    }
    
    public static void log(final String message, final Object... args) {
        log(Level.INFO, message, args);
    }
    
    public static void log(final Level level, final String message, final Throwable ex) {
        ProtocolLogger.plugin.getLogger().log(level, message, ex);
    }
    
    public static void debug(final String message, final Object... args) {
        if (isDebugEnabled()) {
            log("[Debug] " + message, args);
        }
    }
    
    public static void debug(final String message, final Throwable ex) {
        if (isDebugEnabled()) {
            ProtocolLogger.plugin.getLogger().log(Level.WARNING, "[Debug] " + message, ex);
        }
    }
}
