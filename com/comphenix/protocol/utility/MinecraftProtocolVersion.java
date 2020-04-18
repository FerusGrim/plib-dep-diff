package com.comphenix.protocol.utility;

import java.util.Map;
import java.util.TreeMap;
import com.google.common.collect.Maps;
import java.util.NavigableMap;

public class MinecraftProtocolVersion
{
    private static final NavigableMap<MinecraftVersion, Integer> lookup;
    
    private static NavigableMap<MinecraftVersion, Integer> createLookup() {
        final TreeMap<MinecraftVersion, Integer> map = (TreeMap<MinecraftVersion, Integer>)Maps.newTreeMap();
        map.put(new MinecraftVersion(1, 0, 0), 22);
        map.put(new MinecraftVersion(1, 1, 0), 23);
        map.put(new MinecraftVersion(1, 2, 2), 28);
        map.put(new MinecraftVersion(1, 2, 4), 29);
        map.put(new MinecraftVersion(1, 3, 1), 39);
        map.put(new MinecraftVersion(1, 4, 2), 47);
        map.put(new MinecraftVersion(1, 4, 3), 48);
        map.put(new MinecraftVersion(1, 4, 4), 49);
        map.put(new MinecraftVersion(1, 4, 6), 51);
        map.put(new MinecraftVersion(1, 5, 0), 60);
        map.put(new MinecraftVersion(1, 5, 2), 61);
        map.put(new MinecraftVersion(1, 6, 0), 72);
        map.put(new MinecraftVersion(1, 6, 1), 73);
        map.put(new MinecraftVersion(1, 6, 2), 74);
        map.put(new MinecraftVersion(1, 6, 4), 78);
        map.put(new MinecraftVersion(1, 7, 1), 4);
        map.put(new MinecraftVersion(1, 7, 6), 5);
        map.put(new MinecraftVersion(1, 8, 0), 47);
        map.put(new MinecraftVersion(1, 9, 0), 107);
        map.put(new MinecraftVersion(1, 9, 2), 109);
        map.put(new MinecraftVersion(1, 9, 4), 110);
        map.put(new MinecraftVersion(1, 10, 0), 210);
        map.put(new MinecraftVersion(1, 11, 0), 315);
        map.put(new MinecraftVersion(1, 11, 1), 316);
        map.put(new MinecraftVersion(1, 12, 0), 335);
        map.put(new MinecraftVersion(1, 12, 1), 338);
        map.put(new MinecraftVersion(1, 12, 2), 340);
        map.put(new MinecraftVersion(1, 13, 0), 393);
        map.put(new MinecraftVersion(1, 13, 1), 401);
        return map;
    }
    
    public static int getCurrentVersion() {
        return getVersion(MinecraftVersion.getCurrentVersion());
    }
    
    public static int getVersion(final MinecraftVersion version) {
        final Map.Entry<MinecraftVersion, Integer> result = MinecraftProtocolVersion.lookup.floorEntry(version);
        return (result != null) ? result.getValue() : Integer.MIN_VALUE;
    }
    
    static {
        lookup = createLookup();
    }
}
