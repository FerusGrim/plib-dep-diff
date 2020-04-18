package com.comphenix.protocol.events;

import org.bukkit.plugin.Plugin;

public interface PacketListener
{
    void onPacketSending(final PacketEvent p0);
    
    void onPacketReceiving(final PacketEvent p0);
    
    ListeningWhitelist getSendingWhitelist();
    
    ListeningWhitelist getReceivingWhitelist();
    
    Plugin getPlugin();
}
