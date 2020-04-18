package com.comphenix.protocol.injector.server;

import com.comphenix.protocol.events.NetworkMarker;

class QueuedSendPacket
{
    private final Object packet;
    private final NetworkMarker marker;
    private final boolean filtered;
    
    public QueuedSendPacket(final Object packet, final NetworkMarker marker, final boolean filtered) {
        this.packet = packet;
        this.marker = marker;
        this.filtered = filtered;
    }
    
    public NetworkMarker getMarker() {
        return this.marker;
    }
    
    public Object getPacket() {
        return this.packet;
    }
    
    public boolean isFiltered() {
        return this.filtered;
    }
}
