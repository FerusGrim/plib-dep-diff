package com.comphenix.protocol;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import com.google.common.collect.Sets;
import java.util.Set;

public class PacketTypeEnum implements Iterable<PacketType>
{
    protected Set<PacketType> members;
    
    public PacketTypeEnum() {
        this.members = (Set<PacketType>)Sets.newHashSet();
        this.registerAll();
    }
    
    protected void registerAll() {
        try {
            for (final Field entry : this.getClass().getFields()) {
                if (Modifier.isStatic(entry.getModifiers()) && PacketType.class.isAssignableFrom(entry.getType())) {
                    final PacketType value = (PacketType)entry.get(null);
                    if (value == null) {
                        throw new IllegalArgumentException("Field " + entry.getName() + " was null!");
                    }
                    value.setName(entry.getName());
                    if (entry.getAnnotation(PacketType.ForceAsync.class) != null) {
                        value.forceAsync();
                    }
                    final boolean deprecated = entry.getAnnotation(Deprecated.class) != null;
                    if (deprecated) {
                        value.setDeprecated();
                    }
                    if (this.members.contains(value)) {
                        if (!deprecated) {
                            this.members.remove(value);
                            this.members.add(value);
                        }
                    }
                    else {
                        this.members.add(value);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public boolean registerMember(final PacketType instance, final String name) {
        instance.setName(name);
        if (!this.members.contains(instance)) {
            this.members.add(instance);
            return true;
        }
        return false;
    }
    
    public boolean hasMember(final PacketType member) {
        return this.members.contains(member);
    }
    
    @Deprecated
    public PacketType valueOf(final String name) {
        for (final PacketType member : this.members) {
            if (member.name().equals(name)) {
                return member;
            }
        }
        return null;
    }
    
    public Set<PacketType> values() {
        return new HashSet<PacketType>(this.members);
    }
    
    @Override
    public Iterator<PacketType> iterator() {
        return this.members.iterator();
    }
}
