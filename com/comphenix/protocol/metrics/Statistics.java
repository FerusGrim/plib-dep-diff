package com.comphenix.protocol.metrics;

import com.google.common.collect.UnmodifiableIterator;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketListener;
import java.util.HashMap;
import java.util.Map;
import com.comphenix.protocol.ProtocolLibrary;
import org.apache.commons.lang3.tuple.Pair;
import java.io.IOException;
import org.bukkit.plugin.Plugin;
import com.comphenix.protocol.ProtocolLib;

public class Statistics
{
    private Metrics metrics;
    
    public Statistics(final ProtocolLib plugin) throws IOException {
        (this.metrics = new Metrics((Plugin)plugin)).logFailedRequests(plugin.getProtocolConfig().isDebug());
        this.addPluginUserGraph(this.metrics);
    }
    
    private void addPluginUserGraph(final Metrics metrics) {
        metrics.addCustomChart(new Metrics.AdvancedPie("Plugin Users", this::getPluginUsers));
        metrics.addCustomChart(new Metrics.SimplePie("buildVersion", () -> (String)splitVersion().getRight()));
    }
    
    public static Pair<String, String> splitVersion() {
        final String version = ProtocolLibrary.getPlugin().getDescription().getVersion();
        if (version.contains("-b")) {
            final String[] split = version.split("-b");
            return (Pair<String, String>)Pair.of((Object)split[0], (Object)split[1]);
        }
        return (Pair<String, String>)Pair.of((Object)version, (Object)"Unknown");
    }
    
    private Map<String, Integer> getPluginUsers() {
        final Map<String, Integer> users = new HashMap<String, Integer>();
        for (final PacketListener listener : ProtocolLibrary.getProtocolManager().getPacketListeners()) {
            final String name = PacketAdapter.getPluginName(listener);
            if (!users.containsKey(name)) {
                users.put(name, 1);
            }
            else {
                users.put(name, users.get(name) + 1);
            }
        }
        return users;
    }
}
