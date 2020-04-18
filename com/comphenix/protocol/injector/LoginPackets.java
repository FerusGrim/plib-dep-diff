package com.comphenix.protocol.injector;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ConnectionSide;
import org.bukkit.Bukkit;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.concurrency.IntegerSet;

class LoginPackets
{
    private IntegerSet clientSide;
    private IntegerSet serverSide;
    
    public LoginPackets(final MinecraftVersion version) {
        this.clientSide = new IntegerSet(256);
        this.serverSide = new IntegerSet(256);
        this.clientSide.add(2);
        this.serverSide.add(253);
        this.clientSide.add(252);
        this.serverSide.add(252);
        this.clientSide.add(205);
        this.serverSide.add(1);
        this.clientSide.add(254);
        if (version.isAtLeast(MinecraftVersion.HORSE_UPDATE) || isCauldronOrMCPC()) {
            this.clientSide.add(250);
        }
        if (isCauldronOrMCPC()) {
            this.serverSide.add(250);
        }
        this.serverSide.add(255);
    }
    
    private static boolean isCauldronOrMCPC() {
        final String version = Bukkit.getServer().getVersion();
        return version.contains("MCPC") || version.contains("Cauldron");
    }
    
    @Deprecated
    public boolean isLoginPacket(final int packetId, final ConnectionSide side) {
        switch (side) {
            case CLIENT_SIDE: {
                return this.clientSide.contains(packetId);
            }
            case SERVER_SIDE: {
                return this.serverSide.contains(packetId);
            }
            case BOTH: {
                return this.clientSide.contains(packetId) || this.serverSide.contains(packetId);
            }
            default: {
                throw new IllegalArgumentException("Unknown connection side: " + side);
            }
        }
    }
    
    public boolean isLoginPacket(final PacketType type) {
        if (!MinecraftReflection.isUsingNetty()) {
            return this.isLoginPacket(type.getLegacyId(), type.getSender().toSide());
        }
        return PacketType.Login.Client.getInstance().hasMember(type) || PacketType.Login.Server.getInstance().hasMember(type) || PacketType.Status.Client.getInstance().hasMember(type) || PacketType.Status.Server.getInstance().hasMember(type);
    }
}
