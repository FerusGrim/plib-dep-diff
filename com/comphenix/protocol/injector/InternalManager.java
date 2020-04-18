package com.comphenix.protocol.injector;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import com.comphenix.protocol.ProtocolManager;

public interface InternalManager extends ProtocolManager
{
    PlayerInjectHooks getPlayerHook();
    
    void setPlayerHook(final PlayerInjectHooks p0);
    
    void registerEvents(final PluginManager p0, final Plugin p1);
    
    void close();
    
    boolean isDebug();
    
    void setDebug(final boolean p0);
}
