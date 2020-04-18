package com.comphenix.protocol.events;

import com.comphenix.protocol.PacketType;

public enum ConnectionSide
{
    SERVER_SIDE, 
    CLIENT_SIDE, 
    BOTH;
    
    public boolean isForClient() {
        return this == ConnectionSide.CLIENT_SIDE || this == ConnectionSide.BOTH;
    }
    
    public boolean isForServer() {
        return this == ConnectionSide.SERVER_SIDE || this == ConnectionSide.BOTH;
    }
    
    public PacketType.Sender getSender() {
        if (this == ConnectionSide.SERVER_SIDE) {
            return PacketType.Sender.SERVER;
        }
        if (this == ConnectionSide.CLIENT_SIDE) {
            return PacketType.Sender.CLIENT;
        }
        return null;
    }
    
    public static ConnectionSide add(final ConnectionSide a, final ConnectionSide b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        final boolean client = a.isForClient() || b.isForClient();
        final boolean server = a.isForServer() || b.isForServer();
        if (client && server) {
            return ConnectionSide.BOTH;
        }
        if (client) {
            return ConnectionSide.CLIENT_SIDE;
        }
        return ConnectionSide.SERVER_SIDE;
    }
}
