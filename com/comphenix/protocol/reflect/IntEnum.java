package com.comphenix.protocol.reflect;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Field;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.BiMap;

public class IntEnum
{
    protected BiMap<Integer, String> members;
    
    public IntEnum() {
        this.members = (BiMap<Integer, String>)HashBiMap.create();
        this.registerAll();
    }
    
    protected void registerAll() {
        try {
            for (final Field entry : this.getClass().getFields()) {
                if (entry.getType().equals(Integer.TYPE)) {
                    this.registerMember(entry.getInt(this), entry.getName());
                }
            }
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e2) {
            e2.printStackTrace();
        }
    }
    
    protected void registerMember(final int id, final String name) {
        this.members.put((Object)id, (Object)name);
    }
    
    public boolean hasMember(final int id) {
        return this.members.containsKey((Object)id);
    }
    
    public Integer valueOf(final String name) {
        return (Integer)this.members.inverse().get((Object)name);
    }
    
    public String getDeclaredName(final Integer id) {
        return (String)this.members.get((Object)id);
    }
    
    public Set<Integer> values() {
        return new HashSet<Integer>(this.members.keySet());
    }
}
