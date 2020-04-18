package com.comphenix.protocol.wrappers;

import java.util.HashMap;
import java.util.Locale;
import org.bukkit.GameMode;
import com.google.common.collect.Maps;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.lang.reflect.Field;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.EquivalentConverter;
import java.util.Map;

public abstract class EnumWrappers
{
    private static Class<?> PROTOCOL_CLASS;
    private static Class<?> CLIENT_COMMAND_CLASS;
    private static Class<?> CHAT_VISIBILITY_CLASS;
    private static Class<?> DIFFICULTY_CLASS;
    private static Class<?> ENTITY_USE_ACTION_CLASS;
    private static Class<?> GAMEMODE_CLASS;
    private static Class<?> RESOURCE_PACK_STATUS_CLASS;
    private static Class<?> PLAYER_INFO_ACTION_CLASS;
    private static Class<?> TITLE_ACTION_CLASS;
    private static Class<?> WORLD_BORDER_ACTION_CLASS;
    private static Class<?> COMBAT_EVENT_TYPE_CLASS;
    private static Class<?> PLAYER_DIG_TYPE_CLASS;
    private static Class<?> PLAYER_ACTION_CLASS;
    private static Class<?> SCOREBOARD_ACTION_CLASS;
    private static Class<?> PARTICLE_CLASS;
    private static Class<?> SOUND_CATEGORY_CLASS;
    private static Class<?> ITEM_SLOT_CLASS;
    private static Class<?> HAND_CLASS;
    private static Class<?> DIRECTION_CLASS;
    private static Class<?> CHAT_TYPE_CLASS;
    private static boolean INITIALIZED;
    private static Map<Class<?>, EquivalentConverter<?>> FROM_NATIVE;
    private static Map<Class<?>, EquivalentConverter<?>> FROM_WRAPPER;
    
    private static void initialize() {
        if (!MinecraftReflection.isUsingNetty()) {
            throw new IllegalArgumentException("Not supported on 1.6.4 and earlier.");
        }
        if (EnumWrappers.INITIALIZED) {
            return;
        }
        EnumWrappers.INITIALIZED = true;
        EnumWrappers.PROTOCOL_CLASS = getEnum(PacketType.Handshake.Client.SET_PROTOCOL.getPacketClass(), 0);
        EnumWrappers.CLIENT_COMMAND_CLASS = getEnum(PacketType.Play.Client.CLIENT_COMMAND.getPacketClass(), 0);
        EnumWrappers.CHAT_VISIBILITY_CLASS = getEnum(PacketType.Play.Client.SETTINGS.getPacketClass(), 0);
        EnumWrappers.DIFFICULTY_CLASS = getEnum(PacketType.Play.Server.LOGIN.getPacketClass(), 1);
        EnumWrappers.ENTITY_USE_ACTION_CLASS = getEnum(PacketType.Play.Client.USE_ENTITY.getPacketClass(), 0);
        EnumWrappers.GAMEMODE_CLASS = getEnum(PacketType.Play.Server.LOGIN.getPacketClass(), 0);
        EnumWrappers.RESOURCE_PACK_STATUS_CLASS = getEnum(PacketType.Play.Client.RESOURCE_PACK_STATUS.getPacketClass(), 0);
        EnumWrappers.PLAYER_INFO_ACTION_CLASS = getEnum(PacketType.Play.Server.PLAYER_INFO.getPacketClass(), 0);
        EnumWrappers.TITLE_ACTION_CLASS = getEnum(PacketType.Play.Server.TITLE.getPacketClass(), 0);
        EnumWrappers.WORLD_BORDER_ACTION_CLASS = getEnum(PacketType.Play.Server.WORLD_BORDER.getPacketClass(), 0);
        EnumWrappers.COMBAT_EVENT_TYPE_CLASS = getEnum(PacketType.Play.Server.COMBAT_EVENT.getPacketClass(), 0);
        EnumWrappers.PLAYER_DIG_TYPE_CLASS = getEnum(PacketType.Play.Client.BLOCK_DIG.getPacketClass(), 1);
        EnumWrappers.PLAYER_ACTION_CLASS = getEnum(PacketType.Play.Client.ENTITY_ACTION.getPacketClass(), 0);
        EnumWrappers.SCOREBOARD_ACTION_CLASS = getEnum(PacketType.Play.Server.SCOREBOARD_SCORE.getPacketClass(), 0);
        EnumWrappers.PARTICLE_CLASS = getEnum(PacketType.Play.Server.WORLD_PARTICLES.getPacketClass(), 0);
        EnumWrappers.SOUND_CATEGORY_CLASS = getEnum(PacketType.Play.Server.CUSTOM_SOUND_EFFECT.getPacketClass(), 0);
        EnumWrappers.ITEM_SLOT_CLASS = getEnum(PacketType.Play.Server.ENTITY_EQUIPMENT.getPacketClass(), 0);
        EnumWrappers.HAND_CLASS = getEnum(PacketType.Play.Client.USE_ENTITY.getPacketClass(), 1);
        EnumWrappers.DIRECTION_CLASS = getEnum(PacketType.Play.Client.USE_ITEM.getPacketClass(), 0);
        EnumWrappers.CHAT_TYPE_CLASS = getEnum(PacketType.Play.Server.CHAT.getPacketClass(), 0);
        associate(EnumWrappers.PROTOCOL_CLASS, PacketType.Protocol.class, getClientCommandConverter());
        associate(EnumWrappers.CLIENT_COMMAND_CLASS, ClientCommand.class, getClientCommandConverter());
        associate(EnumWrappers.CHAT_VISIBILITY_CLASS, ChatVisibility.class, getChatVisibilityConverter());
        associate(EnumWrappers.DIFFICULTY_CLASS, Difficulty.class, getDifficultyConverter());
        associate(EnumWrappers.ENTITY_USE_ACTION_CLASS, EntityUseAction.class, getEntityUseActionConverter());
        associate(EnumWrappers.GAMEMODE_CLASS, NativeGameMode.class, getGameModeConverter());
        associate(EnumWrappers.RESOURCE_PACK_STATUS_CLASS, ResourcePackStatus.class, getResourcePackStatusConverter());
        associate(EnumWrappers.PLAYER_INFO_ACTION_CLASS, PlayerInfoAction.class, getPlayerInfoActionConverter());
        associate(EnumWrappers.TITLE_ACTION_CLASS, TitleAction.class, getTitleActionConverter());
        associate(EnumWrappers.WORLD_BORDER_ACTION_CLASS, WorldBorderAction.class, getWorldBorderActionConverter());
        associate(EnumWrappers.COMBAT_EVENT_TYPE_CLASS, CombatEventType.class, getCombatEventTypeConverter());
        associate(EnumWrappers.PLAYER_DIG_TYPE_CLASS, PlayerDigType.class, getPlayerDiggingActionConverter());
        associate(EnumWrappers.PLAYER_ACTION_CLASS, PlayerAction.class, getEntityActionConverter());
        associate(EnumWrappers.SCOREBOARD_ACTION_CLASS, ScoreboardAction.class, getUpdateScoreActionConverter());
        associate(EnumWrappers.PARTICLE_CLASS, Particle.class, getParticleConverter());
        associate(EnumWrappers.SOUND_CATEGORY_CLASS, SoundCategory.class, getSoundCategoryConverter());
        associate(EnumWrappers.ITEM_SLOT_CLASS, ItemSlot.class, getItemSlotConverter());
        associate(EnumWrappers.HAND_CLASS, Hand.class, getHandConverter());
        associate(EnumWrappers.DIRECTION_CLASS, Direction.class, getDirectionConverter());
        associate(EnumWrappers.CHAT_TYPE_CLASS, ChatType.class, getChatTypeConverter());
        EnumWrappers.INITIALIZED = true;
    }
    
    private static void associate(final Class<?> nativeClass, final Class<?> wrapperClass, final EquivalentConverter<?> converter) {
        if (nativeClass != null) {
            EnumWrappers.FROM_NATIVE.put(nativeClass, converter);
            EnumWrappers.FROM_WRAPPER.put(wrapperClass, converter);
        }
    }
    
    private static Class<?> getEnum(final Class<?> clazz, final int index) {
        try {
            return FuzzyReflection.fromClass(clazz, true).getFieldListByType(Enum.class).get(index).getType();
        }
        catch (Throwable ex) {
            return null;
        }
    }
    
    public static Map<Class<?>, EquivalentConverter<?>> getFromNativeMap() {
        return EnumWrappers.FROM_NATIVE;
    }
    
    public static Map<Class<?>, EquivalentConverter<?>> getFromWrapperMap() {
        return EnumWrappers.FROM_WRAPPER;
    }
    
    public static Class<?> getProtocolClass() {
        initialize();
        return EnumWrappers.PROTOCOL_CLASS;
    }
    
    public static Class<?> getClientCommandClass() {
        initialize();
        return EnumWrappers.CLIENT_COMMAND_CLASS;
    }
    
    public static Class<?> getChatVisibilityClass() {
        initialize();
        return EnumWrappers.CHAT_VISIBILITY_CLASS;
    }
    
    public static Class<?> getDifficultyClass() {
        initialize();
        return EnumWrappers.DIFFICULTY_CLASS;
    }
    
    public static Class<?> getEntityUseActionClass() {
        initialize();
        return EnumWrappers.ENTITY_USE_ACTION_CLASS;
    }
    
    public static Class<?> getGameModeClass() {
        initialize();
        return EnumWrappers.GAMEMODE_CLASS;
    }
    
    public static Class<?> getResourcePackStatusClass() {
        initialize();
        return EnumWrappers.RESOURCE_PACK_STATUS_CLASS;
    }
    
    public static Class<?> getPlayerInfoActionClass() {
        initialize();
        return EnumWrappers.PLAYER_INFO_ACTION_CLASS;
    }
    
    public static Class<?> getTitleActionClass() {
        initialize();
        return EnumWrappers.TITLE_ACTION_CLASS;
    }
    
    public static Class<?> getWorldBorderActionClass() {
        initialize();
        return EnumWrappers.WORLD_BORDER_ACTION_CLASS;
    }
    
    public static Class<?> getCombatEventTypeClass() {
        initialize();
        return EnumWrappers.COMBAT_EVENT_TYPE_CLASS;
    }
    
    public static Class<?> getPlayerDigTypeClass() {
        initialize();
        return EnumWrappers.PLAYER_DIG_TYPE_CLASS;
    }
    
    public static Class<?> getPlayerActionClass() {
        initialize();
        return EnumWrappers.PLAYER_ACTION_CLASS;
    }
    
    public static Class<?> getScoreboardActionClass() {
        initialize();
        return EnumWrappers.SCOREBOARD_ACTION_CLASS;
    }
    
    public static Class<?> getParticleClass() {
        initialize();
        return EnumWrappers.PARTICLE_CLASS;
    }
    
    public static Class<?> getSoundCategoryClass() {
        initialize();
        return EnumWrappers.SOUND_CATEGORY_CLASS;
    }
    
    public static Class<?> getItemSlotClass() {
        initialize();
        return EnumWrappers.ITEM_SLOT_CLASS;
    }
    
    public static Class<?> getHandClass() {
        initialize();
        return EnumWrappers.HAND_CLASS;
    }
    
    public static Class<?> getDirectionClass() {
        initialize();
        return EnumWrappers.DIRECTION_CLASS;
    }
    
    public static Class<?> getChatTypeClass() {
        initialize();
        return EnumWrappers.CHAT_TYPE_CLASS;
    }
    
    public static EquivalentConverter<PacketType.Protocol> getProtocolConverter() {
        return new EnumConverter<PacketType.Protocol>(getProtocolClass(), PacketType.Protocol.class);
    }
    
    public static EquivalentConverter<ClientCommand> getClientCommandConverter() {
        return new EnumConverter<ClientCommand>(getClientCommandClass(), ClientCommand.class);
    }
    
    public static EquivalentConverter<ChatVisibility> getChatVisibilityConverter() {
        return new EnumConverter<ChatVisibility>(getChatVisibilityClass(), ChatVisibility.class);
    }
    
    public static EquivalentConverter<Difficulty> getDifficultyConverter() {
        return new EnumConverter<Difficulty>(getDifficultyClass(), Difficulty.class);
    }
    
    public static EquivalentConverter<EntityUseAction> getEntityUseActionConverter() {
        return new EnumConverter<EntityUseAction>(getEntityUseActionClass(), EntityUseAction.class);
    }
    
    public static EquivalentConverter<NativeGameMode> getGameModeConverter() {
        return new EnumConverter<NativeGameMode>(getGameModeClass(), NativeGameMode.class);
    }
    
    public static EquivalentConverter<ResourcePackStatus> getResourcePackStatusConverter() {
        return new EnumConverter<ResourcePackStatus>(getResourcePackStatusClass(), ResourcePackStatus.class);
    }
    
    public static EquivalentConverter<PlayerInfoAction> getPlayerInfoActionConverter() {
        return new EnumConverter<PlayerInfoAction>(getPlayerInfoActionClass(), PlayerInfoAction.class);
    }
    
    public static EquivalentConverter<TitleAction> getTitleActionConverter() {
        return new EnumConverter<TitleAction>(getTitleActionClass(), TitleAction.class);
    }
    
    public static EquivalentConverter<WorldBorderAction> getWorldBorderActionConverter() {
        return new EnumConverter<WorldBorderAction>(getWorldBorderActionClass(), WorldBorderAction.class);
    }
    
    public static EquivalentConverter<CombatEventType> getCombatEventTypeConverter() {
        return new EnumConverter<CombatEventType>(getCombatEventTypeClass(), CombatEventType.class);
    }
    
    public static EquivalentConverter<PlayerDigType> getPlayerDiggingActionConverter() {
        return new EnumConverter<PlayerDigType>(getPlayerDigTypeClass(), PlayerDigType.class);
    }
    
    public static EquivalentConverter<PlayerAction> getEntityActionConverter() {
        return new EnumConverter<PlayerAction>(getPlayerActionClass(), PlayerAction.class);
    }
    
    public static EquivalentConverter<ScoreboardAction> getUpdateScoreActionConverter() {
        return new EnumConverter<ScoreboardAction>(getScoreboardActionClass(), ScoreboardAction.class);
    }
    
    public static EquivalentConverter<Particle> getParticleConverter() {
        return new EnumConverter<Particle>(getParticleClass(), Particle.class);
    }
    
    public static EquivalentConverter<SoundCategory> getSoundCategoryConverter() {
        return new EnumConverter<SoundCategory>(getSoundCategoryClass(), SoundCategory.class);
    }
    
    public static EquivalentConverter<ItemSlot> getItemSlotConverter() {
        return new EnumConverter<ItemSlot>(getItemSlotClass(), ItemSlot.class);
    }
    
    public static EquivalentConverter<Hand> getHandConverter() {
        return new EnumConverter<Hand>(getHandClass(), Hand.class);
    }
    
    public static EquivalentConverter<Direction> getDirectionConverter() {
        return new EnumConverter<Direction>(getDirectionClass(), Direction.class);
    }
    
    public static EquivalentConverter<ChatType> getChatTypeConverter() {
        return new EnumConverter<ChatType>(getChatTypeClass(), ChatType.class);
    }
    
    public static <T extends Enum<T>> EquivalentConverter<T> getGenericConverter(final Class<?> genericClass, final Class<T> specificType) {
        return new EnumConverter<T>(genericClass, specificType);
    }
    
    @Deprecated
    public static <T extends Enum<T>> EquivalentConverter<T> getGenericConverter(final Class<T> specificType) {
        return new EnumConverter<T>(null, specificType);
    }
    
    static {
        EnumWrappers.PROTOCOL_CLASS = null;
        EnumWrappers.CLIENT_COMMAND_CLASS = null;
        EnumWrappers.CHAT_VISIBILITY_CLASS = null;
        EnumWrappers.DIFFICULTY_CLASS = null;
        EnumWrappers.ENTITY_USE_ACTION_CLASS = null;
        EnumWrappers.GAMEMODE_CLASS = null;
        EnumWrappers.RESOURCE_PACK_STATUS_CLASS = null;
        EnumWrappers.PLAYER_INFO_ACTION_CLASS = null;
        EnumWrappers.TITLE_ACTION_CLASS = null;
        EnumWrappers.WORLD_BORDER_ACTION_CLASS = null;
        EnumWrappers.COMBAT_EVENT_TYPE_CLASS = null;
        EnumWrappers.PLAYER_DIG_TYPE_CLASS = null;
        EnumWrappers.PLAYER_ACTION_CLASS = null;
        EnumWrappers.SCOREBOARD_ACTION_CLASS = null;
        EnumWrappers.PARTICLE_CLASS = null;
        EnumWrappers.SOUND_CATEGORY_CLASS = null;
        EnumWrappers.ITEM_SLOT_CLASS = null;
        EnumWrappers.HAND_CLASS = null;
        EnumWrappers.DIRECTION_CLASS = null;
        EnumWrappers.CHAT_TYPE_CLASS = null;
        EnumWrappers.INITIALIZED = false;
        EnumWrappers.FROM_NATIVE = (Map<Class<?>, EquivalentConverter<?>>)Maps.newHashMap();
        EnumWrappers.FROM_WRAPPER = (Map<Class<?>, EquivalentConverter<?>>)Maps.newHashMap();
    }
    
    public enum ClientCommand
    {
        PERFORM_RESPAWN, 
        REQUEST_STATS, 
        OPEN_INVENTORY_ACHIEVEMENT;
    }
    
    public enum ChatVisibility
    {
        FULL, 
        SYSTEM, 
        HIDDEN;
    }
    
    public enum Difficulty
    {
        PEACEFUL, 
        EASY, 
        NORMAL, 
        HARD;
    }
    
    public enum EntityUseAction
    {
        INTERACT, 
        ATTACK, 
        INTERACT_AT;
    }
    
    public enum NativeGameMode
    {
        NOT_SET, 
        SURVIVAL, 
        CREATIVE, 
        ADVENTURE, 
        SPECTATOR, 
        @Deprecated
        NONE;
        
        public GameMode toBukkit() {
            switch (this) {
                case ADVENTURE: {
                    return GameMode.ADVENTURE;
                }
                case CREATIVE: {
                    return GameMode.CREATIVE;
                }
                case SPECTATOR: {
                    return GameMode.SPECTATOR;
                }
                case SURVIVAL: {
                    return GameMode.SURVIVAL;
                }
                default: {
                    return null;
                }
            }
        }
        
        public static NativeGameMode fromBukkit(final GameMode mode) {
            switch (mode) {
                case ADVENTURE: {
                    return NativeGameMode.ADVENTURE;
                }
                case CREATIVE: {
                    return NativeGameMode.CREATIVE;
                }
                case SPECTATOR: {
                    return NativeGameMode.SPECTATOR;
                }
                case SURVIVAL: {
                    return NativeGameMode.SURVIVAL;
                }
                default: {
                    return null;
                }
            }
        }
    }
    
    public enum ResourcePackStatus
    {
        SUCCESSFULLY_LOADED, 
        DECLINED, 
        FAILED_DOWNLOAD, 
        ACCEPTED;
    }
    
    public enum PlayerInfoAction
    {
        ADD_PLAYER, 
        UPDATE_GAME_MODE, 
        UPDATE_LATENCY, 
        UPDATE_DISPLAY_NAME, 
        REMOVE_PLAYER;
    }
    
    public enum TitleAction
    {
        TITLE, 
        SUBTITLE, 
        TIMES, 
        CLEAR, 
        RESET;
    }
    
    public enum WorldBorderAction
    {
        SET_SIZE, 
        LERP_SIZE, 
        SET_CENTER, 
        INITIALIZE, 
        SET_WARNING_TIME, 
        SET_WARNING_BLOCKS;
    }
    
    public enum CombatEventType
    {
        ENTER_COMBAT, 
        END_COMBAT, 
        ENTITY_DIED;
    }
    
    public enum PlayerDigType
    {
        START_DESTROY_BLOCK, 
        ABORT_DESTROY_BLOCK, 
        STOP_DESTROY_BLOCK, 
        DROP_ALL_ITEMS, 
        DROP_ITEM, 
        RELEASE_USE_ITEM, 
        SWAP_HELD_ITEMS;
    }
    
    public enum PlayerAction
    {
        START_SNEAKING, 
        STOP_SNEAKING, 
        STOP_SLEEPING, 
        START_SPRINTING, 
        STOP_SPRINTING, 
        START_RIDING_JUMP, 
        STOP_RIDING_JUMP, 
        OPEN_INVENTORY, 
        START_FALL_FLYING;
    }
    
    public enum ScoreboardAction
    {
        CHANGE, 
        REMOVE;
    }
    
    public enum Particle
    {
        EXPLOSION_NORMAL("explode", 0, true), 
        EXPLOSION_LARGE("largeexplode", 1, true), 
        EXPLOSION_HUGE("hugeexplosion", 2, true), 
        FIREWORKS_SPARK("fireworksSpark", 3, false), 
        WATER_BUBBLE("bubble", 4, false), 
        WATER_SPLASH("splash", 5, false), 
        WATER_WAKE("wake", 6, false), 
        SUSPENDED("suspended", 7, false), 
        SUSPENDED_DEPTH("depthsuspend", 8, false), 
        CRIT("crit", 9, false), 
        CRIT_MAGIC("magicCrit", 10, false), 
        SMOKE_NORMAL("smoke", 11, false), 
        SMOKE_LARGE("largesmoke", 12, false), 
        SPELL("spell", 13, false), 
        SPELL_INSTANT("instantSpell", 14, false), 
        SPELL_MOB("mobSpell", 15, false), 
        SPELL_MOB_AMBIENT("mobSpellAmbient", 16, false), 
        SPELL_WITCH("witchMagic", 17, false), 
        DRIP_WATER("dripWater", 18, false), 
        DRIP_LAVA("dripLava", 19, false), 
        VILLAGER_ANGRY("angryVillager", 20, false), 
        VILLAGER_HAPPY("happyVillager", 21, false), 
        TOWN_AURA("townaura", 22, false), 
        NOTE("note", 23, false), 
        PORTAL("portal", 24, false), 
        ENCHANTMENT_TABLE("enchantmenttable", 25, false), 
        FLAME("flame", 26, false), 
        LAVA("lava", 27, false), 
        FOOTSTEP("footstep", 28, false), 
        CLOUD("cloud", 29, false), 
        REDSTONE("reddust", 30, false), 
        SNOWBALL("snowballpoof", 31, false), 
        SNOW_SHOVEL("snowshovel", 32, false), 
        SLIME("slime", 33, false), 
        HEART("heart", 34, false), 
        BARRIER("barrier", 35, false), 
        ITEM_CRACK("iconcrack", 36, false, 2), 
        BLOCK_CRACK("blockcrack", 37, false, 1), 
        BLOCK_DUST("blockdust", 38, false, 1), 
        WATER_DROP("droplet", 39, false), 
        ITEM_TAKE("take", 40, false), 
        MOB_APPEARANCE("mobappearance", 41, true), 
        DRAGON_BREATH("dragonbreath", 42, false), 
        END_ROD("endRod", 43, false), 
        DAMAGE_INDICATOR("damageIndicator", 44, true), 
        SWEEP_ATTACK("sweepAttack", 45, true), 
        FALLING_DUST("fallingdust", 46, false, 1), 
        TOTEM("totem", 47, false), 
        SPIT("spit", 48, true);
        
        private static final Map<String, Particle> BY_NAME;
        private static final Map<Integer, Particle> BY_ID;
        private final String name;
        private final int id;
        private final boolean longDistance;
        private final int dataLength;
        
        private Particle(final String name, final int id, final boolean longDistance) {
            this(name, id, longDistance, 0);
        }
        
        private Particle(final String name, final int id, final boolean longDistance, final int dataLength) {
            this.name = name;
            this.id = id;
            this.longDistance = longDistance;
            this.dataLength = dataLength;
        }
        
        public String getName() {
            return this.name;
        }
        
        public int getId() {
            return this.id;
        }
        
        public boolean isLongDistance() {
            return this.longDistance;
        }
        
        public int getDataLength() {
            return this.dataLength;
        }
        
        public static Particle getByName(final String name) {
            return Particle.BY_NAME.get(name.toLowerCase(Locale.ENGLISH));
        }
        
        public static Particle getById(final int id) {
            return Particle.BY_ID.get(id);
        }
        
        static {
            BY_ID = new HashMap<Integer, Particle>();
            BY_NAME = new HashMap<String, Particle>();
            for (final Particle particle : values()) {
                Particle.BY_NAME.put(particle.getName().toLowerCase(Locale.ENGLISH), particle);
                Particle.BY_ID.put(particle.getId(), particle);
            }
        }
    }
    
    public enum SoundCategory
    {
        MASTER("master"), 
        MUSIC("music"), 
        RECORDS("record"), 
        WEATHER("weather"), 
        BLOCKS("block"), 
        HOSTILE("hostile"), 
        NEUTRAL("neutral"), 
        PLAYERS("player"), 
        AMBIENT("ambient"), 
        VOICE("voice");
        
        private static final Map<String, SoundCategory> LOOKUP;
        private final String key;
        
        private SoundCategory(final String key) {
            this.key = key;
        }
        
        public String getKey() {
            return this.key;
        }
        
        public static SoundCategory getByKey(final String key) {
            return SoundCategory.LOOKUP.get(key.toLowerCase(Locale.ENGLISH));
        }
        
        static {
            LOOKUP = new HashMap<String, SoundCategory>();
            for (final SoundCategory category : values()) {
                SoundCategory.LOOKUP.put(category.key, category);
            }
        }
    }
    
    public enum ItemSlot
    {
        MAINHAND, 
        OFFHAND, 
        FEET, 
        LEGS, 
        CHEST, 
        HEAD;
    }
    
    public enum Hand
    {
        MAIN_HAND, 
        OFF_HAND;
    }
    
    public enum Direction
    {
        DOWN, 
        UP, 
        NORTH, 
        SOUTH, 
        WEST, 
        EAST;
    }
    
    public enum ChatType
    {
        CHAT(0), 
        SYSTEM(1), 
        GAME_INFO(2);
        
        private byte id;
        
        private ChatType(final int id) {
            this.id = (byte)id;
        }
        
        public byte getId() {
            return this.id;
        }
    }
    
    public static class EnumConverter<T extends Enum<T>> implements EquivalentConverter<T>
    {
        private Class<?> genericType;
        private Class<T> specificType;
        
        public EnumConverter(final Class<?> genericType, final Class<T> specificType) {
            this.genericType = genericType;
            this.specificType = specificType;
        }
        
        @Override
        public T getSpecific(final Object generic) {
            return Enum.valueOf(this.specificType, ((Enum)generic).name());
        }
        
        @Override
        public Object getGeneric(final T specific) {
            return Enum.valueOf(this.genericType, specific.name());
        }
        
        @Override
        public Class<T> getSpecificType() {
            return this.specificType;
        }
        
        void setGenericType(final Class<?> genericType) {
            this.genericType = genericType;
        }
    }
}
