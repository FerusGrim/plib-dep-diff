package com.comphenix.protocol.error;

import org.bukkit.plugin.Plugin;
import com.google.common.base.Preconditions;
import java.security.CodeSource;
import java.net.URLDecoder;
import org.bukkit.Bukkit;
import java.io.File;

public final class PluginContext
{
    private static File pluginFolder;
    
    private PluginContext() {
    }
    
    public static String getPluginCaller(final Exception ex) {
        final StackTraceElement[] elements = ex.getStackTrace();
        final String current = getPluginName(elements[0]);
        for (int i = 1; i < elements.length; ++i) {
            final String caller = getPluginName(elements[i]);
            if (caller != null && !caller.equals(current)) {
                return caller;
            }
        }
        return null;
    }
    
    public static String getPluginName(final StackTraceElement element) {
        try {
            if (Bukkit.getServer() == null) {
                return null;
            }
            final CodeSource codeSource = Class.forName(element.getClassName()).getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                final String encoding = codeSource.getLocation().getPath();
                final File path = new File(URLDecoder.decode(encoding, "UTF-8"));
                final File plugins = getPluginFolder();
                if (plugins != null && folderContains(plugins, path)) {
                    return path.getName().replaceAll(".jar", "");
                }
            }
        }
        catch (Throwable t) {}
        return null;
    }
    
    private static boolean folderContains(File folder, File file) {
        Preconditions.checkNotNull((Object)folder, (Object)"folder cannot be NULL");
        Preconditions.checkNotNull((Object)file, (Object)"file cannot be NULL");
        folder = folder.getAbsoluteFile();
        for (file = file.getAbsoluteFile(); file != null; file = file.getParentFile()) {
            if (folder.equals(file)) {
                return true;
            }
        }
        return false;
    }
    
    private static File getPluginFolder() {
        File folder = PluginContext.pluginFolder;
        if (folder == null && Bukkit.getServer() != null) {
            final Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
            if (plugins.length > 0) {
                folder = (PluginContext.pluginFolder = plugins[0].getDataFolder().getParentFile());
            }
        }
        return folder;
    }
}
