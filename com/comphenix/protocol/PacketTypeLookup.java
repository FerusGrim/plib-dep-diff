package com.comphenix.protocol;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.comphenix.protocol.collections.IntegerMap;

class PacketTypeLookup
{
    private final IntegerMap<PacketType> legacyLookup;
    private final IntegerMap<PacketType> serverLookup;
    private final IntegerMap<PacketType> clientLookup;
    private final ProtocolSenderLookup idLookup;
    private final ClassLookup classLookup;
    private final Multimap<String, PacketType> nameLookup;
    
    PacketTypeLookup() {
        this.legacyLookup = new IntegerMap<PacketType>();
        this.serverLookup = new IntegerMap<PacketType>();
        this.clientLookup = new IntegerMap<PacketType>();
        this.idLookup = new ProtocolSenderLookup();
        this.classLookup = new ClassLookup();
        this.nameLookup = (Multimap<String, PacketType>)HashMultimap.create();
    }
    
    public PacketTypeLookup addPacketTypes(final Iterable<? extends PacketType> types) {
        Preconditions.checkNotNull((Object)types, (Object)"types cannot be NULL");
        for (final PacketType type : types) {
            final int legacy = type.getLegacyId();
            if (legacy != -1) {
                if (type.isServer()) {
                    this.serverLookup.put(type.getLegacyId(), type);
                }
                if (type.isClient()) {
                    this.clientLookup.put(type.getLegacyId(), type);
                }
                this.legacyLookup.put(type.getLegacyId(), type);
            }
            if (type.getCurrentId() != -1) {
                this.idLookup.getMap(type.getProtocol(), type.getSender()).put(type.getCurrentId(), type);
                this.classLookup.getMap(type.getProtocol(), type.getSender()).put(type.getClassNames()[0], type);
            }
            this.nameLookup.put((Object)type.name(), (Object)type);
        }
        return this;
    }
    
    public PacketType getFromLegacy(final int packetId) {
        return this.legacyLookup.get(packetId);
    }
    
    public Collection<PacketType> getFromName(final String name) {
        return Collections.unmodifiableCollection((Collection<? extends PacketType>)this.nameLookup.get((Object)name));
    }
    
    public PacketType getFromLegacy(final int packetId, final PacketType.Sender preference) {
        if (preference == PacketType.Sender.CLIENT) {
            return this.getFirst(packetId, this.clientLookup, this.serverLookup);
        }
        return this.getFirst(packetId, this.serverLookup, this.clientLookup);
    }
    
    private <T> T getFirst(final int packetId, final IntegerMap<T> first, final IntegerMap<T> second) {
        if (first.containsKey(packetId)) {
            return first.get(packetId);
        }
        return second.get(packetId);
    }
    
    @Deprecated
    public PacketType getFromCurrent(final PacketType.Protocol protocol, final PacketType.Sender sender, final int packetId) {
        return this.idLookup.getMap(protocol, sender).get(packetId);
    }
    
    public PacketType getFromCurrent(final PacketType.Protocol protocol, final PacketType.Sender sender, final String name) {
        return this.classLookup.getMap(protocol, sender).get(name);
    }
    
    public ClassLookup getClassLookup() {
        return this.classLookup;
    }
    
    public static class ProtocolSenderLookup
    {
        public final IntegerMap<PacketType> HANDSHAKE_CLIENT;
        public final IntegerMap<PacketType> HANDSHAKE_SERVER;
        public final IntegerMap<PacketType> GAME_CLIENT;
        public final IntegerMap<PacketType> GAME_SERVER;
        public final IntegerMap<PacketType> STATUS_CLIENT;
        public final IntegerMap<PacketType> STATUS_SERVER;
        public final IntegerMap<PacketType> LOGIN_CLIENT;
        public final IntegerMap<PacketType> LOGIN_SERVER;
        
        public ProtocolSenderLookup() {
            this.HANDSHAKE_CLIENT = IntegerMap.newMap();
            this.HANDSHAKE_SERVER = IntegerMap.newMap();
            this.GAME_CLIENT = IntegerMap.newMap();
            this.GAME_SERVER = IntegerMap.newMap();
            this.STATUS_CLIENT = IntegerMap.newMap();
            this.STATUS_SERVER = IntegerMap.newMap();
            this.LOGIN_CLIENT = IntegerMap.newMap();
            this.LOGIN_SERVER = IntegerMap.newMap();
        }
        
        public IntegerMap<PacketType> getMap(final PacketType.Protocol protocol, final PacketType.Sender sender) {
            switch (protocol) {
                case HANDSHAKING: {
                    return (sender == PacketType.Sender.CLIENT) ? this.HANDSHAKE_CLIENT : this.HANDSHAKE_SERVER;
                }
                case PLAY: {
                    return (sender == PacketType.Sender.CLIENT) ? this.GAME_CLIENT : this.GAME_SERVER;
                }
                case STATUS: {
                    return (sender == PacketType.Sender.CLIENT) ? this.STATUS_CLIENT : this.STATUS_SERVER;
                }
                case LOGIN: {
                    return (sender == PacketType.Sender.CLIENT) ? this.LOGIN_CLIENT : this.LOGIN_SERVER;
                }
                default: {
                    throw new IllegalArgumentException("Unable to find protocol " + protocol);
                }
            }
        }
    }
    
    public static class ClassLookup
    {
        public final Map<String, PacketType> HANDSHAKE_CLIENT;
        public final Map<String, PacketType> HANDSHAKE_SERVER;
        public final Map<String, PacketType> GAME_CLIENT;
        public final Map<String, PacketType> GAME_SERVER;
        public final Map<String, PacketType> STATUS_CLIENT;
        public final Map<String, PacketType> STATUS_SERVER;
        public final Map<String, PacketType> LOGIN_CLIENT;
        public final Map<String, PacketType> LOGIN_SERVER;
        
        public ClassLookup() {
            this.HANDSHAKE_CLIENT = new ConcurrentHashMap<String, PacketType>();
            this.HANDSHAKE_SERVER = new ConcurrentHashMap<String, PacketType>();
            this.GAME_CLIENT = new ConcurrentHashMap<String, PacketType>();
            this.GAME_SERVER = new ConcurrentHashMap<String, PacketType>();
            this.STATUS_CLIENT = new ConcurrentHashMap<String, PacketType>();
            this.STATUS_SERVER = new ConcurrentHashMap<String, PacketType>();
            this.LOGIN_CLIENT = new ConcurrentHashMap<String, PacketType>();
            this.LOGIN_SERVER = new ConcurrentHashMap<String, PacketType>();
        }
        
        public Map<String, PacketType> getMap(final PacketType.Protocol protocol, final PacketType.Sender sender) {
            switch (protocol) {
                case HANDSHAKING: {
                    return (sender == PacketType.Sender.CLIENT) ? this.HANDSHAKE_CLIENT : this.HANDSHAKE_SERVER;
                }
                case PLAY: {
                    return (sender == PacketType.Sender.CLIENT) ? this.GAME_CLIENT : this.GAME_SERVER;
                }
                case STATUS: {
                    return (sender == PacketType.Sender.CLIENT) ? this.STATUS_CLIENT : this.STATUS_SERVER;
                }
                case LOGIN: {
                    return (sender == PacketType.Sender.CLIENT) ? this.LOGIN_CLIENT : this.LOGIN_SERVER;
                }
                default: {
                    throw new IllegalArgumentException("Unable to find protocol " + protocol);
                }
            }
        }
    }
}
