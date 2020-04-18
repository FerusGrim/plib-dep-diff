package com.comphenix.protocol;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProtocolLogger
{
    private static boolean debugEnabled;
    private static Logger logger;
    
    public static void init(final ProtocolLib plugin) {
        ProtocolLogger.logger = plugin.getLogger();
        try {
            ProtocolLogger.debugEnabled = plugin.getConfig().getBoolean("global.debug", false);
        }
        catch (Throwable t) {}
    }
    
    public static void log(final Level level, final String message, final Object... args) {
        ProtocolLogger.logger.log(level, MessageFormat.format(message, args));
    }
    
    public static void log(final String message, final Object... args) {
        log(Level.INFO, message, args);
    }
    
    public static void log(final Level level, final String message, final Throwable ex) {
        ProtocolLogger.logger.log(level, message, ex);
    }
    
    public static void debug(final String message, final Object... args) {
        if (ProtocolLogger.debugEnabled) {
            log("[Debug] " + message, args);
        }
    }
    
    public static void debug(final String message, final Throwable ex) {
        if (ProtocolLogger.debugEnabled) {
            ProtocolLogger.logger.log(Level.WARNING, "[Debug] " + message, ex);
        }
    }
    
    static {
        ProtocolLogger.debugEnabled = false;
        ProtocolLogger.logger = Logger.getLogger("Minecraft");
    }
}
