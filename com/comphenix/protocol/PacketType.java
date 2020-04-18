package com.comphenix.protocol;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Annotation;
import com.comphenix.protocol.events.ConnectionSide;
import java.util.Locale;
import com.google.common.collect.ComparisonChain;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.bukkit.Bukkit;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.Collection;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.WordUtils;
import java.util.List;
import com.google.common.collect.Iterables;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.collect.Lists;
import java.util.function.Consumer;
import com.comphenix.protocol.utility.MinecraftVersion;
import java.io.Serializable;

public class PacketType implements Serializable, Cloneable, Comparable<PacketType>
{
    private static final long serialVersionUID = 1L;
    public static final int UNKNOWN_PACKET = -1;
    private static PacketTypeLookup LOOKUP;
    private static final MinecraftVersion PROTOCOL_VERSION;
    private final Protocol protocol;
    private final Sender sender;
    private final int currentId;
    private final int legacyId;
    private final MinecraftVersion version;
    private final String[] classNames;
    String[] names;
    private String name;
    private boolean deprecated;
    private boolean forceAsync;
    private boolean dynamic;
    static Consumer<String> onDynamicCreate;
    
    private static PacketTypeLookup getLookup() {
        if (PacketType.LOOKUP == null) {
            PacketType.LOOKUP = new PacketTypeLookup().addPacketTypes(Handshake.Client.getInstance()).addPacketTypes(Handshake.Server.getInstance()).addPacketTypes(Play.Client.getInstance()).addPacketTypes(Play.Server.getInstance()).addPacketTypes(Status.Client.getInstance()).addPacketTypes(Status.Server.getInstance()).addPacketTypes(Login.Client.getInstance()).addPacketTypes(Login.Server.getInstance()).addPacketTypes(Legacy.Client.getInstance()).addPacketTypes(Legacy.Server.getInstance());
        }
        return PacketType.LOOKUP;
    }
    
    public static Iterable<PacketType> values() {
        final List<Iterable<? extends PacketType>> sources = (List<Iterable<? extends PacketType>>)Lists.newArrayList();
        sources.add(Handshake.Client.getInstance());
        sources.add(Handshake.Server.getInstance());
        sources.add(Play.Client.getInstance());
        sources.add(Play.Server.getInstance());
        sources.add(Status.Client.getInstance());
        sources.add(Status.Server.getInstance());
        sources.add(Login.Client.getInstance());
        sources.add(Login.Server.getInstance());
        if (!MinecraftReflection.isUsingNetty()) {
            sources.add(Legacy.Client.getInstance());
            sources.add(Legacy.Server.getInstance());
        }
        return (Iterable<PacketType>)Iterables.concat((Iterable)sources);
    }
    
    @Deprecated
    public static PacketType findLegacy(final int packetId) {
        final PacketType type = getLookup().getFromLegacy(packetId);
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("Cannot find legacy packet " + packetId);
    }
    
    @Deprecated
    public static PacketType findLegacy(final int packetId, final Sender preference) {
        if (preference == null) {
            return findLegacy(packetId);
        }
        final PacketType type = getLookup().getFromLegacy(packetId, preference);
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("Cannot find legacy packet " + packetId);
    }
    
    @Deprecated
    public static boolean hasLegacy(final int packetId) {
        return getLookup().getFromLegacy(packetId) != null;
    }
    
    public static PacketType findCurrent(final Protocol protocol, final Sender sender, final int packetId) {
        final PacketType type = getLookup().getFromCurrent(protocol, sender, packetId);
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("Cannot find packet " + packetId + "(Protocol: " + protocol + ", Sender: " + sender + ")");
    }
    
    public static PacketType findCurrent(final Protocol protocol, final Sender sender, String name) {
        name = formatClassName(protocol, sender, name);
        final PacketType type = getLookup().getFromCurrent(protocol, sender, name);
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("Cannot find packet " + name + "(Protocol: " + protocol + ", Sender: " + sender + ")");
    }
    
    private static String formatClassName(final Protocol protocol, final Sender sender, final String name) {
        final String base = MinecraftReflection.getMinecraftPackage() + ".Packet";
        if (name.startsWith(base)) {
            return name;
        }
        if (name.contains("$")) {
            final String[] split = name.split("\\$");
            final String parent = split[0];
            final String child = split[1];
            return base + protocol.getPacketName() + sender.getPacketName() + WordUtils.capitalize(parent) + "$Packet" + protocol.getPacketName() + sender.getPacketName() + WordUtils.capitalize(child);
        }
        return base + protocol.getPacketName() + sender.getPacketName() + WordUtils.capitalize(name);
    }
    
    public static boolean hasCurrent(final Protocol protocol, final Sender sender, final int packetId) {
        return getLookup().getFromCurrent(protocol, sender, packetId) != null;
    }
    
    @Deprecated
    public static PacketType fromLegacy(final int id, final Sender sender) {
        PacketType type = getLookup().getFromLegacy(id, sender);
        if (type == null) {
            if (sender == null) {
                throw new IllegalArgumentException("Cannot find legacy packet " + id);
            }
            type = newLegacy(sender, id);
            scheduleRegister(type, "Dynamic-" + UUID.randomUUID().toString());
        }
        return type;
    }
    
    public static PacketType fromID(final Protocol protocol, final Sender sender, final int packetId, final Class<?> packetClass) {
        PacketType type = getLookup().getFromCurrent(protocol, sender, packetId);
        if (type == null) {
            type = new PacketType(protocol, sender, packetId, -1, PacketType.PROTOCOL_VERSION, new String[] { packetClass.getName() });
            type.dynamic = true;
            scheduleRegister(type, "Dynamic-" + UUID.randomUUID().toString());
        }
        return type;
    }
    
    public static PacketType fromCurrent(final Protocol protocol, final Sender sender, final int packetId, final Class<?> packetClass) {
        final PacketTypeLookup.ClassLookup lookup = getLookup().getClassLookup();
        final Map<String, PacketType> map = lookup.getMap(protocol, sender);
        final String className = packetClass.getName();
        PacketType type = find(map, className);
        if (type == null) {
            type = new PacketType(protocol, sender, packetId, -1, PacketType.PROTOCOL_VERSION, new String[] { className });
            type.dynamic = true;
            scheduleRegister(type, "Dynamic-" + UUID.randomUUID().toString());
            PacketType.onDynamicCreate.accept(className);
        }
        return type;
    }
    
    private static PacketType find(final Map<String, PacketType> map, final String clazz) {
        final PacketType ret = map.get(clazz);
        if (ret != null) {
            return ret;
        }
        for (final PacketType check : map.values()) {
            final String[] aliases = check.getClassNames();
            if (aliases.length > 1) {
                for (final String alias : aliases) {
                    if (alias.equals(clazz)) {
                        return check;
                    }
                }
            }
        }
        return null;
    }
    
    public static PacketType fromClass(final Class<?> packetClass) {
        final PacketType type = PacketRegistry.getPacketType(packetClass);
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("Class " + packetClass + " is not a registered packet.");
    }
    
    public static Collection<PacketType> fromName(final String name) {
        return getLookup().getFromName(name);
    }
    
    public static boolean hasClass(final Class<?> packetClass) {
        return PacketRegistry.getPacketType(packetClass) != null;
    }
    
    public static Future<Boolean> scheduleRegister(final PacketType type, final String name) {
        final Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final PacketTypeEnum objEnum = PacketType.getObjectEnum(type);
                if (objEnum.registerMember(type, name)) {
                    getLookup().addPacketTypes(Arrays.asList(type));
                    return true;
                }
                return false;
            }
        };
        if (Bukkit.getServer() != null) {
            if (!Bukkit.isPrimaryThread()) {
                return (Future<Boolean>)ProtocolLibrary.getExecutorSync().submit((Callable)callable);
            }
        }
        try {
            return (Future<Boolean>)Futures.immediateFuture((Object)callable.call());
        }
        catch (Exception e) {
            return (Future<Boolean>)Futures.immediateFailedFuture((Throwable)e);
        }
        return (Future<Boolean>)ProtocolLibrary.getExecutorSync().submit((Callable)callable);
    }
    
    public static PacketTypeEnum getObjectEnum(final PacketType type) {
        switch (type.getProtocol()) {
            case HANDSHAKING: {
                return type.isClient() ? Handshake.Client.getInstance() : Handshake.Server.getInstance();
            }
            case PLAY: {
                return type.isClient() ? Play.Client.getInstance() : Play.Server.getInstance();
            }
            case STATUS: {
                return type.isClient() ? Status.Client.getInstance() : Status.Server.getInstance();
            }
            case LOGIN: {
                return type.isClient() ? Login.Client.getInstance() : Login.Server.getInstance();
            }
            case LEGACY: {
                return type.isClient() ? Legacy.Client.getInstance() : Legacy.Server.getInstance();
            }
            default: {
                throw new IllegalStateException("Unexpected protocol: " + type.getProtocol());
            }
        }
    }
    
    public PacketType(final Protocol protocol, final Sender sender, final int currentId, final int legacyId, final String... names) {
        this(protocol, sender, currentId, legacyId, PacketType.PROTOCOL_VERSION, names);
    }
    
    public PacketType(final Protocol protocol, final Sender sender, final int currentId, final int legacyId, final MinecraftVersion version, final String... names) {
        this.protocol = (Protocol)Preconditions.checkNotNull((Object)protocol, (Object)"protocol cannot be NULL");
        this.sender = (Sender)Preconditions.checkNotNull((Object)sender, (Object)"sender cannot be NULL");
        this.currentId = currentId;
        this.legacyId = legacyId;
        this.version = version;
        this.classNames = new String[names.length];
        for (int i = 0; i < this.classNames.length; ++i) {
            this.classNames[i] = formatClassName(protocol, sender, names[i]);
        }
        this.names = names;
    }
    
    public static PacketType newLegacy(final Sender sender, final int legacyId) {
        return new PacketType(Protocol.LEGACY, sender, -1, legacyId, MinecraftVersion.WORLD_UPDATE, new String[0]);
    }
    
    public boolean isSupported() {
        return PacketRegistry.isSupported(this);
    }
    
    public Protocol getProtocol() {
        return this.protocol;
    }
    
    public Sender getSender() {
        return this.sender;
    }
    
    public boolean isClient() {
        return this.sender == Sender.CLIENT;
    }
    
    public boolean isServer() {
        return this.sender == Sender.SERVER;
    }
    
    @Deprecated
    public int getCurrentId() {
        return this.currentId;
    }
    
    public String[] getClassNames() {
        return this.classNames;
    }
    
    public Class<?> getPacketClass() {
        try {
            return (Class<?>)PacketRegistry.getPacketClassFromType(this);
        }
        catch (Exception e) {
            return null;
        }
    }
    
    void setName(final String name) {
        this.name = name;
    }
    
    public String name() {
        return this.name;
    }
    
    void setDeprecated() {
        this.deprecated = true;
    }
    
    public boolean isDeprecated() {
        return this.deprecated;
    }
    
    void forceAsync() {
        this.forceAsync = true;
    }
    
    public boolean isAsyncForced() {
        return this.forceAsync;
    }
    
    public MinecraftVersion getCurrentVersion() {
        return this.version;
    }
    
    @Deprecated
    public int getLegacyId() {
        return this.legacyId;
    }
    
    public boolean isDynamic() {
        return this.dynamic;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.protocol, this.sender, this.currentId });
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PacketType) {
            final PacketType other = (PacketType)obj;
            return this.protocol == other.protocol && this.sender == other.sender && this.currentId == other.currentId;
        }
        return false;
    }
    
    @Override
    public int compareTo(final PacketType other) {
        return ComparisonChain.start().compare((Comparable)this.protocol, (Comparable)other.getProtocol()).compare((Comparable)this.sender, (Comparable)other.getSender()).compare(this.currentId, other.getCurrentId()).result();
    }
    
    @Override
    public String toString() {
        final Class<?> clazz = this.getPacketClass();
        if (clazz == null) {
            return this.name() + "[" + this.protocol + ", " + this.sender + ", " + this.currentId + ", classNames: " + Arrays.toString(this.classNames) + " (unregistered)]";
        }
        return this.name() + "[class=" + clazz.getSimpleName() + ", id=" + this.currentId + "]";
    }
    
    public PacketType clone() {
        try {
            return (PacketType)super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new Error("This shouldn't happen", ex);
        }
    }
    
    static {
        PROTOCOL_VERSION = MinecraftVersion.VILLAGE_UPDATE;
        PacketType.onDynamicCreate = (x -> {});
    }
    
    public static class Handshake
    {
        private static final Protocol PROTOCOL;
        
        public static Protocol getProtocol() {
            return Handshake.PROTOCOL;
        }
        
        static {
            PROTOCOL = Protocol.HANDSHAKING;
        }
        
        public static class Client extends PacketTypeEnum
        {
            private static final Sender SENDER;
            public static final PacketType SET_PROTOCOL;
            private static final Client INSTANCE;
            
            private Client() {
            }
            
            public static Client getInstance() {
                return Client.INSTANCE;
            }
            
            public static Sender getSender() {
                return Client.SENDER;
            }
            
            static {
                SENDER = Sender.CLIENT;
                SET_PROTOCOL = new PacketType(Handshake.PROTOCOL, Client.SENDER, 0, 0, new String[] { "SetProtocol" });
                INSTANCE = new Client();
            }
        }
        
        public static class Server extends PacketTypeEnum
        {
            private static final Sender SENDER;
            private static final Server INSTANCE;
            
            private Server() {
            }
            
            public static Server getInstance() {
                return Server.INSTANCE;
            }
            
            public static Sender getSender() {
                return Server.SENDER;
            }
            
            static {
                SENDER = Sender.CLIENT;
                INSTANCE = new Server();
            }
        }
    }
    
    public static class Play
    {
        private static final Protocol PROTOCOL;
        
        public static Protocol getProtocol() {
            return Play.PROTOCOL;
        }
        
        static {
            PROTOCOL = Protocol.PLAY;
        }
        
        public static class Server extends PacketTypeEnum
        {
            private static final Sender SENDER;
            public static final PacketType SPAWN_ENTITY;
            public static final PacketType SPAWN_ENTITY_EXPERIENCE_ORB;
            public static final PacketType SPAWN_ENTITY_WEATHER;
            public static final PacketType SPAWN_ENTITY_LIVING;
            public static final PacketType SPAWN_ENTITY_PAINTING;
            public static final PacketType NAMED_ENTITY_SPAWN;
            public static final PacketType ANIMATION;
            public static final PacketType STATISTIC;
            public static final PacketType BLOCK_BREAK;
            public static final PacketType BLOCK_BREAK_ANIMATION;
            public static final PacketType TILE_ENTITY_DATA;
            public static final PacketType BLOCK_ACTION;
            public static final PacketType BLOCK_CHANGE;
            public static final PacketType BOSS;
            public static final PacketType SERVER_DIFFICULTY;
            public static final PacketType CHAT;
            public static final PacketType MULTI_BLOCK_CHANGE;
            public static final PacketType TAB_COMPLETE;
            public static final PacketType COMMANDS;
            public static final PacketType TRANSACTION;
            public static final PacketType CLOSE_WINDOW;
            public static final PacketType WINDOW_ITEMS;
            public static final PacketType WINDOW_DATA;
            public static final PacketType SET_SLOT;
            public static final PacketType SET_COOLDOWN;
            public static final PacketType CUSTOM_PAYLOAD;
            public static final PacketType CUSTOM_SOUND_EFFECT;
            public static final PacketType KICK_DISCONNECT;
            public static final PacketType ENTITY_STATUS;
            public static final PacketType EXPLOSION;
            public static final PacketType UNLOAD_CHUNK;
            public static final PacketType GAME_STATE_CHANGE;
            public static final PacketType OPEN_WINDOW_HORSE;
            public static final PacketType KEEP_ALIVE;
            public static final PacketType MAP_CHUNK;
            public static final PacketType WORLD_EVENT;
            public static final PacketType WORLD_PARTICLES;
            public static final PacketType LIGHT_UPDATE;
            public static final PacketType LOGIN;
            public static final PacketType MAP;
            public static final PacketType OPEN_WINDOW_MERCHANT;
            public static final PacketType REL_ENTITY_MOVE;
            public static final PacketType REL_ENTITY_MOVE_LOOK;
            public static final PacketType ENTITY_LOOK;
            public static final PacketType ENTITY;
            public static final PacketType VEHICLE_MOVE;
            public static final PacketType OPEN_BOOK;
            public static final PacketType OPEN_WINDOW;
            public static final PacketType OPEN_SIGN_EDITOR;
            public static final PacketType AUTO_RECIPE;
            public static final PacketType ABILITIES;
            public static final PacketType COMBAT_EVENT;
            public static final PacketType PLAYER_INFO;
            public static final PacketType LOOK_AT;
            public static final PacketType POSITION;
            public static final PacketType RECIPES;
            public static final PacketType ENTITY_DESTROY;
            public static final PacketType REMOVE_ENTITY_EFFECT;
            public static final PacketType RESOURCE_PACK_SEND;
            public static final PacketType RESPAWN;
            public static final PacketType ENTITY_HEAD_ROTATION;
            public static final PacketType SELECT_ADVANCEMENT_TAB;
            public static final PacketType WORLD_BORDER;
            public static final PacketType CAMERA;
            public static final PacketType HELD_ITEM_SLOT;
            public static final PacketType VIEW_CENTRE;
            public static final PacketType VIEW_DISTANCE;
            public static final PacketType SCOREBOARD_DISPLAY_OBJECTIVE;
            public static final PacketType ENTITY_METADATA;
            public static final PacketType ATTACH_ENTITY;
            public static final PacketType ENTITY_VELOCITY;
            public static final PacketType ENTITY_EQUIPMENT;
            public static final PacketType EXPERIENCE;
            public static final PacketType UPDATE_HEALTH;
            public static final PacketType SCOREBOARD_OBJECTIVE;
            public static final PacketType MOUNT;
            public static final PacketType SCOREBOARD_TEAM;
            public static final PacketType SCOREBOARD_SCORE;
            public static final PacketType SPAWN_POSITION;
            public static final PacketType UPDATE_TIME;
            public static final PacketType TITLE;
            public static final PacketType ENTITY_SOUND;
            public static final PacketType NAMED_SOUND_EFFECT;
            public static final PacketType STOP_SOUND;
            public static final PacketType PLAYER_LIST_HEADER_FOOTER;
            public static final PacketType NBT_QUERY;
            public static final PacketType COLLECT;
            public static final PacketType ENTITY_TELEPORT;
            public static final PacketType ADVANCEMENTS;
            public static final PacketType UPDATE_ATTRIBUTES;
            public static final PacketType ENTITY_EFFECT;
            public static final PacketType RECIPE_UPDATE;
            public static final PacketType TAGS;
            @Deprecated
            public static final PacketType MAP_CHUNK_BULK;
            @Deprecated
            public static final PacketType SET_COMPRESSION;
            @Deprecated
            public static final PacketType UPDATE_ENTITY_NBT;
            @Deprecated
            public static final PacketType CRAFT_PROGRESS_BAR;
            @Deprecated
            public static final PacketType ENTITY_MOVE_LOOK;
            @Deprecated
            public static final PacketType STATISTICS;
            @Deprecated
            public static final PacketType OPEN_SIGN_ENTITY;
            @Deprecated
            public static final PacketType UPDATE_SIGN;
            @Deprecated
            public static final PacketType BED;
            @Deprecated
            public static final PacketType USE_BED;
            private static final Server INSTANCE;
            
            private Server() {
            }
            
            public static Sender getSender() {
                return Server.SENDER;
            }
            
            public static Server getInstance() {
                return Server.INSTANCE;
            }
            
            static {
                SENDER = Sender.SERVER;
                SPAWN_ENTITY = new PacketType(Play.PROTOCOL, Server.SENDER, 0, 255, new String[] { "SpawnEntity" });
                SPAWN_ENTITY_EXPERIENCE_ORB = new PacketType(Play.PROTOCOL, Server.SENDER, 1, 255, new String[] { "SpawnEntityExperienceOrb" });
                SPAWN_ENTITY_WEATHER = new PacketType(Play.PROTOCOL, Server.SENDER, 2, 255, new String[] { "SpawnEntityWeather" });
                SPAWN_ENTITY_LIVING = new PacketType(Play.PROTOCOL, Server.SENDER, 3, 255, new String[] { "SpawnEntityLiving" });
                SPAWN_ENTITY_PAINTING = new PacketType(Play.PROTOCOL, Server.SENDER, 4, 255, new String[] { "SpawnEntityPainting" });
                NAMED_ENTITY_SPAWN = new PacketType(Play.PROTOCOL, Server.SENDER, 5, 255, new String[] { "NamedEntitySpawn" });
                ANIMATION = new PacketType(Play.PROTOCOL, Server.SENDER, 6, 255, new String[] { "Animation" });
                STATISTIC = new PacketType(Play.PROTOCOL, Server.SENDER, 7, 255, new String[] { "Statistic" });
                BLOCK_BREAK = new PacketType(Play.PROTOCOL, Server.SENDER, 8, 255, new String[] { "BlockBreak" });
                BLOCK_BREAK_ANIMATION = new PacketType(Play.PROTOCOL, Server.SENDER, 9, 255, new String[] { "BlockBreakAnimation" });
                TILE_ENTITY_DATA = new PacketType(Play.PROTOCOL, Server.SENDER, 10, 255, new String[] { "TileEntityData" });
                BLOCK_ACTION = new PacketType(Play.PROTOCOL, Server.SENDER, 11, 255, new String[] { "BlockAction" });
                BLOCK_CHANGE = new PacketType(Play.PROTOCOL, Server.SENDER, 12, 255, new String[] { "BlockChange" });
                BOSS = new PacketType(Play.PROTOCOL, Server.SENDER, 13, 255, new String[] { "Boss" });
                SERVER_DIFFICULTY = new PacketType(Play.PROTOCOL, Server.SENDER, 14, 255, new String[] { "ServerDifficulty" });
                CHAT = new PacketType(Play.PROTOCOL, Server.SENDER, 15, 255, new String[] { "Chat" });
                MULTI_BLOCK_CHANGE = new PacketType(Play.PROTOCOL, Server.SENDER, 16, 255, new String[] { "MultiBlockChange" });
                TAB_COMPLETE = new PacketType(Play.PROTOCOL, Server.SENDER, 17, 255, new String[] { "TabComplete" });
                COMMANDS = new PacketType(Play.PROTOCOL, Server.SENDER, 18, 255, new String[] { "Commands" });
                TRANSACTION = new PacketType(Play.PROTOCOL, Server.SENDER, 19, 255, new String[] { "Transaction" });
                CLOSE_WINDOW = new PacketType(Play.PROTOCOL, Server.SENDER, 20, 255, new String[] { "CloseWindow" });
                WINDOW_ITEMS = new PacketType(Play.PROTOCOL, Server.SENDER, 21, 255, new String[] { "WindowItems" });
                WINDOW_DATA = new PacketType(Play.PROTOCOL, Server.SENDER, 22, 255, new String[] { "WindowData" });
                SET_SLOT = new PacketType(Play.PROTOCOL, Server.SENDER, 23, 255, new String[] { "SetSlot" });
                SET_COOLDOWN = new PacketType(Play.PROTOCOL, Server.SENDER, 24, 255, new String[] { "SetCooldown" });
                CUSTOM_PAYLOAD = new PacketType(Play.PROTOCOL, Server.SENDER, 25, 255, new String[] { "CustomPayload" });
                CUSTOM_SOUND_EFFECT = new PacketType(Play.PROTOCOL, Server.SENDER, 26, 255, new String[] { "CustomSoundEffect" });
                KICK_DISCONNECT = new PacketType(Play.PROTOCOL, Server.SENDER, 27, 255, new String[] { "KickDisconnect" });
                ENTITY_STATUS = new PacketType(Play.PROTOCOL, Server.SENDER, 28, 255, new String[] { "EntityStatus" });
                EXPLOSION = new PacketType(Play.PROTOCOL, Server.SENDER, 29, 255, new String[] { "Explosion" });
                UNLOAD_CHUNK = new PacketType(Play.PROTOCOL, Server.SENDER, 30, 255, new String[] { "UnloadChunk" });
                GAME_STATE_CHANGE = new PacketType(Play.PROTOCOL, Server.SENDER, 31, 255, new String[] { "GameStateChange" });
                OPEN_WINDOW_HORSE = new PacketType(Play.PROTOCOL, Server.SENDER, 32, 255, new String[] { "OpenWindowHorse" });
                KEEP_ALIVE = new PacketType(Play.PROTOCOL, Server.SENDER, 33, 255, new String[] { "KeepAlive" });
                MAP_CHUNK = new PacketType(Play.PROTOCOL, Server.SENDER, 34, 255, new String[] { "MapChunk" });
                WORLD_EVENT = new PacketType(Play.PROTOCOL, Server.SENDER, 35, 255, new String[] { "WorldEvent" });
                WORLD_PARTICLES = new PacketType(Play.PROTOCOL, Server.SENDER, 36, 255, new String[] { "WorldParticles" });
                LIGHT_UPDATE = new PacketType(Play.PROTOCOL, Server.SENDER, 37, 255, new String[] { "LightUpdate" });
                LOGIN = new PacketType(Play.PROTOCOL, Server.SENDER, 38, 255, new String[] { "Login" });
                MAP = new PacketType(Play.PROTOCOL, Server.SENDER, 39, 255, new String[] { "Map" });
                OPEN_WINDOW_MERCHANT = new PacketType(Play.PROTOCOL, Server.SENDER, 40, 255, new String[] { "OpenWindowMerchant" });
                REL_ENTITY_MOVE = new PacketType(Play.PROTOCOL, Server.SENDER, 41, 255, new String[] { "Entity$RelEntityMove" });
                REL_ENTITY_MOVE_LOOK = new PacketType(Play.PROTOCOL, Server.SENDER, 42, 255, new String[] { "Entity$RelEntityMoveLook" });
                ENTITY_LOOK = new PacketType(Play.PROTOCOL, Server.SENDER, 43, 255, new String[] { "Entity$EntityLook" });
                ENTITY = new PacketType(Play.PROTOCOL, Server.SENDER, 44, 255, new String[] { "Entity" });
                VEHICLE_MOVE = new PacketType(Play.PROTOCOL, Server.SENDER, 45, 255, new String[] { "VehicleMove" });
                OPEN_BOOK = new PacketType(Play.PROTOCOL, Server.SENDER, 46, 255, new String[] { "OpenBook" });
                OPEN_WINDOW = new PacketType(Play.PROTOCOL, Server.SENDER, 47, 255, new String[] { "OpenWindow" });
                OPEN_SIGN_EDITOR = new PacketType(Play.PROTOCOL, Server.SENDER, 48, 255, new String[] { "OpenSignEditor" });
                AUTO_RECIPE = new PacketType(Play.PROTOCOL, Server.SENDER, 49, 255, new String[] { "AutoRecipe" });
                ABILITIES = new PacketType(Play.PROTOCOL, Server.SENDER, 50, 255, new String[] { "Abilities" });
                COMBAT_EVENT = new PacketType(Play.PROTOCOL, Server.SENDER, 51, 255, new String[] { "CombatEvent" });
                PLAYER_INFO = new PacketType(Play.PROTOCOL, Server.SENDER, 52, 255, new String[] { "PlayerInfo" });
                LOOK_AT = new PacketType(Play.PROTOCOL, Server.SENDER, 53, 255, new String[] { "LookAt" });
                POSITION = new PacketType(Play.PROTOCOL, Server.SENDER, 54, 255, new String[] { "Position" });
                RECIPES = new PacketType(Play.PROTOCOL, Server.SENDER, 55, 255, new String[] { "Recipes" });
                ENTITY_DESTROY = new PacketType(Play.PROTOCOL, Server.SENDER, 56, 255, new String[] { "EntityDestroy" });
                REMOVE_ENTITY_EFFECT = new PacketType(Play.PROTOCOL, Server.SENDER, 57, 255, new String[] { "RemoveEntityEffect" });
                RESOURCE_PACK_SEND = new PacketType(Play.PROTOCOL, Server.SENDER, 58, 255, new String[] { "ResourcePackSend" });
                RESPAWN = new PacketType(Play.PROTOCOL, Server.SENDER, 59, 255, new String[] { "Respawn" });
                ENTITY_HEAD_ROTATION = new PacketType(Play.PROTOCOL, Server.SENDER, 60, 255, new String[] { "EntityHeadRotation" });
                SELECT_ADVANCEMENT_TAB = new PacketType(Play.PROTOCOL, Server.SENDER, 61, 255, new String[] { "SelectAdvancementTab" });
                WORLD_BORDER = new PacketType(Play.PROTOCOL, Server.SENDER, 62, 255, new String[] { "WorldBorder" });
                CAMERA = new PacketType(Play.PROTOCOL, Server.SENDER, 63, 255, new String[] { "Camera" });
                HELD_ITEM_SLOT = new PacketType(Play.PROTOCOL, Server.SENDER, 64, 255, new String[] { "HeldItemSlot" });
                VIEW_CENTRE = new PacketType(Play.PROTOCOL, Server.SENDER, 65, 255, new String[] { "ViewCentre" });
                VIEW_DISTANCE = new PacketType(Play.PROTOCOL, Server.SENDER, 66, 255, new String[] { "ViewDistance" });
                SCOREBOARD_DISPLAY_OBJECTIVE = new PacketType(Play.PROTOCOL, Server.SENDER, 67, 255, new String[] { "ScoreboardDisplayObjective" });
                ENTITY_METADATA = new PacketType(Play.PROTOCOL, Server.SENDER, 68, 255, new String[] { "EntityMetadata" });
                ATTACH_ENTITY = new PacketType(Play.PROTOCOL, Server.SENDER, 69, 255, new String[] { "AttachEntity" });
                ENTITY_VELOCITY = new PacketType(Play.PROTOCOL, Server.SENDER, 70, 255, new String[] { "EntityVelocity" });
                ENTITY_EQUIPMENT = new PacketType(Play.PROTOCOL, Server.SENDER, 71, 255, new String[] { "EntityEquipment" });
                EXPERIENCE = new PacketType(Play.PROTOCOL, Server.SENDER, 72, 255, new String[] { "Experience" });
                UPDATE_HEALTH = new PacketType(Play.PROTOCOL, Server.SENDER, 73, 255, new String[] { "UpdateHealth" });
                SCOREBOARD_OBJECTIVE = new PacketType(Play.PROTOCOL, Server.SENDER, 74, 255, new String[] { "ScoreboardObjective" });
                MOUNT = new PacketType(Play.PROTOCOL, Server.SENDER, 75, 255, new String[] { "Mount" });
                SCOREBOARD_TEAM = new PacketType(Play.PROTOCOL, Server.SENDER, 76, 255, new String[] { "ScoreboardTeam" });
                SCOREBOARD_SCORE = new PacketType(Play.PROTOCOL, Server.SENDER, 77, 255, new String[] { "ScoreboardScore" });
                SPAWN_POSITION = new PacketType(Play.PROTOCOL, Server.SENDER, 78, 255, new String[] { "SpawnPosition" });
                UPDATE_TIME = new PacketType(Play.PROTOCOL, Server.SENDER, 79, 255, new String[] { "UpdateTime" });
                TITLE = new PacketType(Play.PROTOCOL, Server.SENDER, 80, 255, new String[] { "Title" });
                ENTITY_SOUND = new PacketType(Play.PROTOCOL, Server.SENDER, 81, 255, new String[] { "EntitySound" });
                NAMED_SOUND_EFFECT = new PacketType(Play.PROTOCOL, Server.SENDER, 82, 255, new String[] { "NamedSoundEffect" });
                STOP_SOUND = new PacketType(Play.PROTOCOL, Server.SENDER, 83, 255, new String[] { "StopSound" });
                PLAYER_LIST_HEADER_FOOTER = new PacketType(Play.PROTOCOL, Server.SENDER, 84, 255, new String[] { "PlayerListHeaderFooter" });
                NBT_QUERY = new PacketType(Play.PROTOCOL, Server.SENDER, 85, 255, new String[] { "NBTQuery" });
                COLLECT = new PacketType(Play.PROTOCOL, Server.SENDER, 86, 255, new String[] { "Collect" });
                ENTITY_TELEPORT = new PacketType(Play.PROTOCOL, Server.SENDER, 87, 255, new String[] { "EntityTeleport" });
                ADVANCEMENTS = new PacketType(Play.PROTOCOL, Server.SENDER, 88, 255, new String[] { "Advancements" });
                UPDATE_ATTRIBUTES = new PacketType(Play.PROTOCOL, Server.SENDER, 89, 255, new String[] { "UpdateAttributes" });
                ENTITY_EFFECT = new PacketType(Play.PROTOCOL, Server.SENDER, 90, 255, new String[] { "EntityEffect" });
                RECIPE_UPDATE = new PacketType(Play.PROTOCOL, Server.SENDER, 91, 255, new String[] { "RecipeUpdate" });
                TAGS = new PacketType(Play.PROTOCOL, Server.SENDER, 92, 255, new String[] { "Tags" });
                MAP_CHUNK_BULK = new PacketType(Play.PROTOCOL, Server.SENDER, 255, 255, new String[] { "MapChunkBulk" });
                SET_COMPRESSION = new PacketType(Play.PROTOCOL, Server.SENDER, 254, 254, new String[] { "SetCompression" });
                UPDATE_ENTITY_NBT = new PacketType(Play.PROTOCOL, Server.SENDER, 253, 253, new String[] { "UpdateEntityNBT" });
                CRAFT_PROGRESS_BAR = Server.WINDOW_DATA.clone();
                ENTITY_MOVE_LOOK = Server.REL_ENTITY_MOVE_LOOK.clone();
                STATISTICS = Server.STATISTIC.clone();
                OPEN_SIGN_ENTITY = Server.OPEN_SIGN_EDITOR.clone();
                UPDATE_SIGN = (MinecraftReflection.signUpdateExists() ? new PacketType(Play.PROTOCOL, Server.SENDER, 252, 252, new String[] { "UpdateSign" }) : Server.TILE_ENTITY_DATA.clone());
                BED = new PacketType(Play.PROTOCOL, Server.SENDER, 51, 51, new String[] { "Bed" });
                USE_BED = Server.BED.clone();
                INSTANCE = new Server();
            }
        }
        
        public static class Client extends PacketTypeEnum
        {
            private static final Sender SENDER;
            public static final PacketType TELEPORT_ACCEPT;
            public static final PacketType TILE_NBT_QUERY;
            public static final PacketType DIFFICULTY_CHANGE;
            public static final PacketType CHAT;
            public static final PacketType CLIENT_COMMAND;
            public static final PacketType SETTINGS;
            public static final PacketType TAB_COMPLETE;
            public static final PacketType TRANSACTION;
            public static final PacketType ENCHANT_ITEM;
            public static final PacketType WINDOW_CLICK;
            public static final PacketType CLOSE_WINDOW;
            public static final PacketType CUSTOM_PAYLOAD;
            public static final PacketType B_EDIT;
            public static final PacketType ENTITY_NBT_QUERY;
            public static final PacketType USE_ENTITY;
            public static final PacketType KEEP_ALIVE;
            public static final PacketType DIFFICULTY_LOCK;
            public static final PacketType POSITION;
            public static final PacketType POSITION_LOOK;
            public static final PacketType LOOK;
            public static final PacketType FLYING;
            public static final PacketType VEHICLE_MOVE;
            public static final PacketType BOAT_MOVE;
            public static final PacketType PICK_ITEM;
            public static final PacketType AUTO_RECIPE;
            public static final PacketType ABILITIES;
            public static final PacketType BLOCK_DIG;
            public static final PacketType ENTITY_ACTION;
            public static final PacketType STEER_VEHICLE;
            public static final PacketType RECIPE_DISPLAYED;
            public static final PacketType ITEM_NAME;
            public static final PacketType RESOURCE_PACK_STATUS;
            public static final PacketType ADVANCEMENTS;
            public static final PacketType TR_SEL;
            public static final PacketType BEACON;
            public static final PacketType HELD_ITEM_SLOT;
            public static final PacketType SET_COMMAND_BLOCK;
            public static final PacketType SET_COMMAND_MINECART;
            public static final PacketType SET_CREATIVE_SLOT;
            public static final PacketType SET_JIGSAW;
            public static final PacketType STRUCT;
            public static final PacketType UPDATE_SIGN;
            public static final PacketType ARM_ANIMATION;
            public static final PacketType SPECTATE;
            public static final PacketType USE_ITEM;
            public static final PacketType BLOCK_PLACE;
            private static final Client INSTANCE;
            
            private Client() {
            }
            
            public static Sender getSender() {
                return Client.SENDER;
            }
            
            public static Client getInstance() {
                return Client.INSTANCE;
            }
            
            static {
                SENDER = Sender.CLIENT;
                TELEPORT_ACCEPT = new PacketType(Play.PROTOCOL, Client.SENDER, 0, 255, new String[] { "TeleportAccept" });
                TILE_NBT_QUERY = new PacketType(Play.PROTOCOL, Client.SENDER, 1, 255, new String[] { "TileNBTQuery" });
                DIFFICULTY_CHANGE = new PacketType(Play.PROTOCOL, Client.SENDER, 2, 255, new String[] { "DifficultyChange" });
                CHAT = new PacketType(Play.PROTOCOL, Client.SENDER, 3, 255, new String[] { "Chat" });
                CLIENT_COMMAND = new PacketType(Play.PROTOCOL, Client.SENDER, 4, 255, new String[] { "ClientCommand" });
                SETTINGS = new PacketType(Play.PROTOCOL, Client.SENDER, 5, 255, new String[] { "Settings" });
                TAB_COMPLETE = new PacketType(Play.PROTOCOL, Client.SENDER, 6, 255, new String[] { "TabComplete" });
                TRANSACTION = new PacketType(Play.PROTOCOL, Client.SENDER, 7, 255, new String[] { "Transaction" });
                ENCHANT_ITEM = new PacketType(Play.PROTOCOL, Client.SENDER, 8, 255, new String[] { "EnchantItem" });
                WINDOW_CLICK = new PacketType(Play.PROTOCOL, Client.SENDER, 9, 255, new String[] { "WindowClick" });
                CLOSE_WINDOW = new PacketType(Play.PROTOCOL, Client.SENDER, 10, 255, new String[] { "CloseWindow" });
                CUSTOM_PAYLOAD = new PacketType(Play.PROTOCOL, Client.SENDER, 11, 255, new String[] { "CustomPayload" });
                B_EDIT = new PacketType(Play.PROTOCOL, Client.SENDER, 12, 255, new String[] { "BEdit" });
                ENTITY_NBT_QUERY = new PacketType(Play.PROTOCOL, Client.SENDER, 13, 255, new String[] { "EntityNBTQuery" });
                USE_ENTITY = new PacketType(Play.PROTOCOL, Client.SENDER, 14, 255, new String[] { "UseEntity" });
                KEEP_ALIVE = new PacketType(Play.PROTOCOL, Client.SENDER, 15, 255, new String[] { "KeepAlive" });
                DIFFICULTY_LOCK = new PacketType(Play.PROTOCOL, Client.SENDER, 16, 255, new String[] { "DifficultyLock" });
                POSITION = new PacketType(Play.PROTOCOL, Client.SENDER, 17, 255, new String[] { "Flying$Position" });
                POSITION_LOOK = new PacketType(Play.PROTOCOL, Client.SENDER, 18, 255, new String[] { "Flying$PositionLook" });
                LOOK = new PacketType(Play.PROTOCOL, Client.SENDER, 19, 255, new String[] { "Flying$Look" });
                FLYING = new PacketType(Play.PROTOCOL, Client.SENDER, 20, 255, new String[] { "Flying" });
                VEHICLE_MOVE = new PacketType(Play.PROTOCOL, Client.SENDER, 21, 255, new String[] { "VehicleMove" });
                BOAT_MOVE = new PacketType(Play.PROTOCOL, Client.SENDER, 22, 255, new String[] { "BoatMove" });
                PICK_ITEM = new PacketType(Play.PROTOCOL, Client.SENDER, 23, 255, new String[] { "PickItem" });
                AUTO_RECIPE = new PacketType(Play.PROTOCOL, Client.SENDER, 24, 255, new String[] { "AutoRecipe" });
                ABILITIES = new PacketType(Play.PROTOCOL, Client.SENDER, 25, 255, new String[] { "Abilities" });
                BLOCK_DIG = new PacketType(Play.PROTOCOL, Client.SENDER, 26, 255, new String[] { "BlockDig" });
                ENTITY_ACTION = new PacketType(Play.PROTOCOL, Client.SENDER, 27, 255, new String[] { "EntityAction" });
                STEER_VEHICLE = new PacketType(Play.PROTOCOL, Client.SENDER, 28, 255, new String[] { "SteerVehicle" });
                RECIPE_DISPLAYED = new PacketType(Play.PROTOCOL, Client.SENDER, 29, 255, new String[] { "RecipeDisplayed" });
                ITEM_NAME = new PacketType(Play.PROTOCOL, Client.SENDER, 30, 255, new String[] { "ItemName" });
                RESOURCE_PACK_STATUS = new PacketType(Play.PROTOCOL, Client.SENDER, 31, 255, new String[] { "ResourcePackStatus" });
                ADVANCEMENTS = new PacketType(Play.PROTOCOL, Client.SENDER, 32, 255, new String[] { "Advancements" });
                TR_SEL = new PacketType(Play.PROTOCOL, Client.SENDER, 33, 255, new String[] { "TrSel" });
                BEACON = new PacketType(Play.PROTOCOL, Client.SENDER, 34, 255, new String[] { "Beacon" });
                HELD_ITEM_SLOT = new PacketType(Play.PROTOCOL, Client.SENDER, 35, 255, new String[] { "HeldItemSlot" });
                SET_COMMAND_BLOCK = new PacketType(Play.PROTOCOL, Client.SENDER, 36, 255, new String[] { "SetCommandBlock" });
                SET_COMMAND_MINECART = new PacketType(Play.PROTOCOL, Client.SENDER, 37, 255, new String[] { "SetCommandMinecart" });
                SET_CREATIVE_SLOT = new PacketType(Play.PROTOCOL, Client.SENDER, 38, 255, new String[] { "SetCreativeSlot" });
                SET_JIGSAW = new PacketType(Play.PROTOCOL, Client.SENDER, 39, 255, new String[] { "SetJigsaw" });
                STRUCT = new PacketType(Play.PROTOCOL, Client.SENDER, 40, 255, new String[] { "Struct" });
                UPDATE_SIGN = new PacketType(Play.PROTOCOL, Client.SENDER, 41, 255, new String[] { "UpdateSign" });
                ARM_ANIMATION = new PacketType(Play.PROTOCOL, Client.SENDER, 42, 255, new String[] { "ArmAnimation" });
                SPECTATE = new PacketType(Play.PROTOCOL, Client.SENDER, 43, 255, new String[] { "Spectate" });
                USE_ITEM = new PacketType(Play.PROTOCOL, Client.SENDER, 44, 255, new String[] { "UseItem" });
                BLOCK_PLACE = new PacketType(Play.PROTOCOL, Client.SENDER, 45, 255, new String[] { "BlockPlace" });
                INSTANCE = new Client();
            }
        }
    }
    
    public static class Status
    {
        private static final Protocol PROTOCOL;
        
        public static Protocol getProtocol() {
            return Status.PROTOCOL;
        }
        
        static {
            PROTOCOL = Protocol.STATUS;
        }
        
        public static class Server extends PacketTypeEnum
        {
            private static final Sender SENDER;
            @ForceAsync
            public static final PacketType SERVER_INFO;
            public static final PacketType PONG;
            @Deprecated
            @ForceAsync
            public static final PacketType OUT_SERVER_INFO;
            private static final Server INSTANCE;
            
            private Server() {
            }
            
            public static Sender getSender() {
                return Server.SENDER;
            }
            
            public static Server getInstance() {
                return Server.INSTANCE;
            }
            
            static {
                SENDER = Sender.SERVER;
                SERVER_INFO = new PacketType(Status.PROTOCOL, Server.SENDER, 0, 255, new String[] { "ServerInfo" });
                PONG = new PacketType(Status.PROTOCOL, Server.SENDER, 1, 255, new String[] { "Pong" });
                OUT_SERVER_INFO = Server.SERVER_INFO.clone();
                INSTANCE = new Server();
            }
        }
        
        public static class Client extends PacketTypeEnum
        {
            private static final Sender SENDER;
            public static final PacketType START;
            public static final PacketType PING;
            private static final Client INSTANCE;
            
            private Client() {
            }
            
            public static Sender getSender() {
                return Client.SENDER;
            }
            
            public static Client getInstance() {
                return Client.INSTANCE;
            }
            
            static {
                SENDER = Sender.CLIENT;
                START = new PacketType(Status.PROTOCOL, Client.SENDER, 0, 255, new String[] { "Start" });
                PING = new PacketType(Status.PROTOCOL, Client.SENDER, 1, 255, new String[] { "Ping" });
                INSTANCE = new Client();
            }
        }
    }
    
    public static class Login
    {
        private static final Protocol PROTOCOL;
        
        public static Protocol getProtocol() {
            return Login.PROTOCOL;
        }
        
        static {
            PROTOCOL = Protocol.LOGIN;
        }
        
        public static class Server extends PacketTypeEnum
        {
            private static final Sender SENDER;
            public static final PacketType DISCONNECT;
            public static final PacketType ENCRYPTION_BEGIN;
            public static final PacketType SUCCESS;
            public static final PacketType SET_COMPRESSION;
            public static final PacketType CUSTOM_PAYLOAD;
            private static final Server INSTANCE;
            
            private Server() {
            }
            
            public static Sender getSender() {
                return Server.SENDER;
            }
            
            public static Server getInstance() {
                return Server.INSTANCE;
            }
            
            static {
                SENDER = Sender.SERVER;
                DISCONNECT = new PacketType(Login.PROTOCOL, Server.SENDER, 0, 255, new String[] { "Disconnect" });
                ENCRYPTION_BEGIN = new PacketType(Login.PROTOCOL, Server.SENDER, 1, 255, new String[] { "EncryptionBegin" });
                SUCCESS = new PacketType(Login.PROTOCOL, Server.SENDER, 2, 255, new String[] { "Success" });
                SET_COMPRESSION = new PacketType(Login.PROTOCOL, Server.SENDER, 3, 255, new String[] { "SetCompression" });
                CUSTOM_PAYLOAD = new PacketType(Login.PROTOCOL, Server.SENDER, 4, 255, new String[] { "CustomPayload" });
                INSTANCE = new Server();
            }
        }
        
        public static class Client extends PacketTypeEnum
        {
            private static final Sender SENDER;
            public static final PacketType START;
            public static final PacketType ENCRYPTION_BEGIN;
            public static final PacketType CUSTOM_PAYLOAD;
            private static final Client INSTANCE;
            
            private Client() {
            }
            
            public static Sender getSender() {
                return Client.SENDER;
            }
            
            public static Client getInstance() {
                return Client.INSTANCE;
            }
            
            static {
                SENDER = Sender.CLIENT;
                START = new PacketType(Login.PROTOCOL, Client.SENDER, 0, 255, new String[] { "Start" });
                ENCRYPTION_BEGIN = new PacketType(Login.PROTOCOL, Client.SENDER, 1, 255, new String[] { "EncryptionBegin" });
                CUSTOM_PAYLOAD = new PacketType(Login.PROTOCOL, Client.SENDER, 2, 255, new String[] { "CustomPayload" });
                INSTANCE = new Client();
            }
        }
    }
    
    public static class Legacy
    {
        private static final Protocol PROTOCOL;
        
        public static Protocol getProtocol() {
            return Legacy.PROTOCOL;
        }
        
        static {
            PROTOCOL = Protocol.LEGACY;
        }
        
        public static class Server extends PacketTypeEnum
        {
            private static final Sender SENDER;
            public static final PacketType PLAYER_FLYING;
            public static final PacketType PLAYER_POSITION;
            public static final PacketType PLAYER_POSITON_LOOK;
            public static final PacketType PICKUP_SPAWN;
            public static final PacketType SET_CREATIVE_SLOT;
            public static final PacketType KEY_RESPONSE;
            private static final Server INSTANCE;
            
            private Server() {
            }
            
            public static Sender getSender() {
                return Server.SENDER;
            }
            
            public static Server getInstance() {
                return Server.INSTANCE;
            }
            
            static {
                SENDER = Sender.SERVER;
                PLAYER_FLYING = PacketType.newLegacy(Server.SENDER, 10);
                PLAYER_POSITION = PacketType.newLegacy(Server.SENDER, 11);
                PLAYER_POSITON_LOOK = PacketType.newLegacy(Server.SENDER, 12);
                PICKUP_SPAWN = PacketType.newLegacy(Server.SENDER, 21);
                SET_CREATIVE_SLOT = PacketType.newLegacy(Server.SENDER, 107);
                KEY_RESPONSE = PacketType.newLegacy(Server.SENDER, 252);
                INSTANCE = new Server();
            }
        }
        
        public static class Client extends PacketTypeEnum
        {
            private static final Sender SENDER;
            public static final PacketType LOGIN;
            public static final PacketType RESPAWN;
            public static final PacketType DISCONNECT;
            private static final Client INSTANCE;
            
            private Client() {
            }
            
            public static Sender getSender() {
                return Client.SENDER;
            }
            
            public static Client getInstance() {
                return Client.INSTANCE;
            }
            
            static {
                SENDER = Sender.CLIENT;
                LOGIN = PacketType.newLegacy(Client.SENDER, 1);
                RESPAWN = PacketType.newLegacy(Client.SENDER, 9);
                DISCONNECT = PacketType.newLegacy(Client.SENDER, 255);
                INSTANCE = new Client();
            }
        }
    }
    
    public enum Protocol
    {
        HANDSHAKING, 
        PLAY, 
        STATUS, 
        LOGIN, 
        LEGACY;
        
        public static Protocol fromVanilla(final Enum<?> vanilla) {
            final String name = vanilla.name();
            if ("HANDSHAKING".equals(name)) {
                return Protocol.HANDSHAKING;
            }
            if ("PLAY".equals(name)) {
                return Protocol.PLAY;
            }
            if ("STATUS".equals(name)) {
                return Protocol.STATUS;
            }
            if ("LOGIN".equals(name)) {
                return Protocol.LOGIN;
            }
            throw new IllegalArgumentException("Unrecognized vanilla enum " + vanilla);
        }
        
        public String getPacketName() {
            return WordUtils.capitalize(this.name().toLowerCase(Locale.ENGLISH));
        }
    }
    
    public enum Sender
    {
        CLIENT, 
        SERVER;
        
        public ConnectionSide toSide() {
            return (this == Sender.CLIENT) ? ConnectionSide.CLIENT_SIDE : ConnectionSide.SERVER_SIDE;
        }
        
        public String getPacketName() {
            return (this == Sender.CLIENT) ? "In" : "Out";
        }
    }
    
    @Target({ ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ForceAsync {
    }
}
