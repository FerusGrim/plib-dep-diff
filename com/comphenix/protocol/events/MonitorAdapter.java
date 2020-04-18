package com.comphenix.protocol.events;

import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.logging.Level;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.Packets;
import com.comphenix.protocol.PacketType;
import java.util.Collection;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

public abstract class MonitorAdapter implements PacketListener
{
    private Plugin plugin;
    private ListeningWhitelist sending;
    private ListeningWhitelist receiving;
    
    public MonitorAdapter(final Plugin plugin, final ConnectionSide side) {
        this.sending = ListeningWhitelist.EMPTY_WHITELIST;
        this.receiving = ListeningWhitelist.EMPTY_WHITELIST;
        this.initialize(plugin, side, this.getLogger(plugin));
    }
    
    public MonitorAdapter(final Plugin plugin, final ConnectionSide side, final Logger logger) {
        this.sending = ListeningWhitelist.EMPTY_WHITELIST;
        this.receiving = ListeningWhitelist.EMPTY_WHITELIST;
        this.initialize(plugin, side, logger);
    }
    
    private void initialize(final Plugin plugin, final ConnectionSide side, final Logger logger) {
        this.plugin = plugin;
        try {
            if (side.isForServer()) {
                this.sending = ListeningWhitelist.newBuilder().monitor().types(PacketRegistry.getServerPacketTypes()).gamePhaseBoth().build();
            }
            if (side.isForClient()) {
                this.receiving = ListeningWhitelist.newBuilder().monitor().types(PacketRegistry.getClientPacketTypes()).gamePhaseBoth().build();
            }
        }
        catch (FieldAccessException e) {
            if (side.isForServer()) {
                this.sending = new ListeningWhitelist(ListenerPriority.MONITOR, Packets.Server.getRegistry().values(), GamePhase.BOTH);
            }
            if (side.isForClient()) {
                this.receiving = new ListeningWhitelist(ListenerPriority.MONITOR, Packets.Client.getRegistry().values(), GamePhase.BOTH);
            }
            logger.log(Level.WARNING, "Defaulting to 1.3 packets.", e);
        }
    }
    
    private Logger getLogger(final Plugin plugin) {
        try {
            return plugin.getLogger();
        }
        catch (NoSuchMethodError e) {
            return Logger.getLogger("Minecraft");
        }
    }
    
    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return this.sending;
    }
    
    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return this.receiving;
    }
    
    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }
}
