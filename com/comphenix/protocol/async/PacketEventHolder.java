package com.comphenix.protocol.async;

import com.google.common.primitives.Longs;
import com.google.common.collect.ComparisonChain;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.events.PacketEvent;

class PacketEventHolder implements Comparable<PacketEventHolder>
{
    private PacketEvent event;
    private long sendingIndex;
    
    public PacketEventHolder(final PacketEvent event) {
        this.sendingIndex = 0L;
        this.event = (PacketEvent)Preconditions.checkNotNull((Object)event, (Object)"Event must be non-null");
        if (event.getAsyncMarker() != null) {
            this.sendingIndex = event.getAsyncMarker().getNewSendingIndex();
        }
    }
    
    public PacketEvent getEvent() {
        return this.event;
    }
    
    @Override
    public int compareTo(final PacketEventHolder other) {
        return ComparisonChain.start().compare(this.sendingIndex, other.sendingIndex).result();
    }
    
    @Override
    public boolean equals(final Object other) {
        return other == this || (other instanceof PacketEventHolder && this.sendingIndex == ((PacketEventHolder)other).sendingIndex);
    }
    
    @Override
    public int hashCode() {
        return Longs.hashCode(this.sendingIndex);
    }
}
