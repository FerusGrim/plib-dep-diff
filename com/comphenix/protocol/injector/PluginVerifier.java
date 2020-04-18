package com.comphenix.protocol.injector;

import java.util.Iterator;
import java.util.Collections;
import com.google.common.collect.Sets;
import org.bukkit.plugin.PluginLoadOrder;
import java.util.logging.Level;
import java.util.List;
import java.util.HashSet;
import org.bukkit.plugin.Plugin;
import java.util.Set;

class PluginVerifier
{
    private static final Set<String> DYNAMIC_DEPENDENCY;
    private final Set<String> loadedAfter;
    private final Plugin dependency;
    
    public PluginVerifier(final Plugin dependency) {
        this.loadedAfter = new HashSet<String>();
        if (dependency == null) {
            throw new IllegalArgumentException("dependency cannot be NULL.");
        }
        try {
            if (this.safeConversion(dependency.getDescription().getLoadBefore()).size() > 0) {
                throw new IllegalArgumentException("dependency cannot have a load directives.");
            }
        }
        catch (LinkageError e) {
            dependency.getLogger().log(Level.WARNING, "Failed to determine loadBefore: " + e);
        }
        this.dependency = dependency;
    }
    
    private Plugin getPlugin(final String pluginName) {
        final Plugin plugin = this.getPluginOrDefault(pluginName);
        if (plugin != null) {
            return plugin;
        }
        throw new PluginNotFoundException("Cannot find plugin " + pluginName);
    }
    
    private Plugin getPluginOrDefault(final String pluginName) {
        return this.dependency.getServer().getPluginManager().getPlugin(pluginName);
    }
    
    public VerificationResult verify(final String pluginName) {
        if (pluginName == null) {
            throw new IllegalArgumentException("pluginName cannot be NULL.");
        }
        return this.verify(this.getPlugin(pluginName));
    }
    
    public VerificationResult verify(final Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be NULL.");
        }
        final String name = plugin.getName();
        if (!this.dependency.equals(plugin) && !this.loadedAfter.contains(name) && !PluginVerifier.DYNAMIC_DEPENDENCY.contains(name)) {
            if (!this.verifyLoadOrder(this.dependency, plugin)) {
                return VerificationResult.NO_DEPEND;
            }
            this.loadedAfter.add(plugin.getName());
        }
        return VerificationResult.VALID;
    }
    
    private boolean verifyLoadOrder(final Plugin beforePlugin, final Plugin afterPlugin) {
        return this.hasDependency(afterPlugin, beforePlugin) || (beforePlugin.getDescription().getLoad() == PluginLoadOrder.STARTUP && afterPlugin.getDescription().getLoad() == PluginLoadOrder.POSTWORLD);
    }
    
    private boolean hasDependency(final Plugin plugin, final Plugin dependency) {
        return this.hasDependency(plugin, dependency, Sets.newHashSet());
    }
    
    private Set<String> safeConversion(final List<String> list) {
        if (list == null) {
            return Collections.emptySet();
        }
        return (Set<String>)Sets.newHashSet((Iterable)list);
    }
    
    private boolean hasDependency(final Plugin plugin, final Plugin dependency, final Set<String> checking) {
        final Set<String> childNames = (Set<String>)Sets.union((Set)this.safeConversion(plugin.getDescription().getDepend()), (Set)this.safeConversion(plugin.getDescription().getSoftDepend()));
        if (!checking.add(plugin.getName())) {
            throw new IllegalStateException("Cycle detected in dependency graph: " + plugin);
        }
        if (childNames.contains(dependency.getName())) {
            return true;
        }
        for (final String childName : childNames) {
            final Plugin childPlugin = this.getPluginOrDefault(childName);
            if (childPlugin != null && this.hasDependency(childPlugin, dependency, checking)) {
                return true;
            }
        }
        checking.remove(plugin.getName());
        return false;
    }
    
    static {
        DYNAMIC_DEPENDENCY = Sets.newHashSet((Object[])new String[] { "mcore", "MassiveCore" });
    }
    
    public static class PluginNotFoundException extends RuntimeException
    {
        private static final long serialVersionUID = 8956699101336877611L;
        
        public PluginNotFoundException() {
        }
        
        public PluginNotFoundException(final String message) {
            super(message);
        }
    }
    
    public enum VerificationResult
    {
        VALID, 
        NO_DEPEND;
        
        public boolean isValid() {
            return this == VerificationResult.VALID;
        }
    }
}
