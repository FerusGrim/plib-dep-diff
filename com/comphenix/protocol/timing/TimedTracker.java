package com.comphenix.protocol.timing;

import java.util.Iterator;
import com.google.common.collect.Maps;
import com.comphenix.protocol.PacketType;
import java.util.Map;

public class TimedTracker
{
    private Map<PacketType, StatisticsStream> packets;
    private int observations;
    
    public TimedTracker() {
        this.packets = (Map<PacketType, StatisticsStream>)Maps.newHashMap();
    }
    
    public long beginTracking() {
        return System.nanoTime();
    }
    
    public synchronized void endTracking(final long trackingToken, final PacketType type) {
        StatisticsStream stream = this.packets.get(type);
        if (stream == null) {
            this.packets.put(type, stream = new StatisticsStream());
        }
        stream.observe((double)(System.nanoTime() - trackingToken));
        ++this.observations;
    }
    
    public int getObservations() {
        return this.observations;
    }
    
    public synchronized Map<PacketType, StatisticsStream> getStatistics() {
        final Map<PacketType, StatisticsStream> clone = (Map<PacketType, StatisticsStream>)Maps.newHashMap();
        for (final Map.Entry<PacketType, StatisticsStream> entry : this.packets.entrySet()) {
            clone.put(entry.getKey(), new StatisticsStream(entry.getValue()));
        }
        return clone;
    }
}
