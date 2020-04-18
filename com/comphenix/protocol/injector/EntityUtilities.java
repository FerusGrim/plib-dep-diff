package com.comphenix.protocol.injector;

import com.comphenix.protocol.utility.MinecraftVersion;
import com.google.common.collect.Lists;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.WrappedIntHashMap;
import org.bukkit.World;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import org.apache.commons.lang.Validate;
import java.util.Iterator;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.ArrayList;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import java.util.function.Function;
import java.util.Collection;
import org.bukkit.entity.Player;
import java.util.List;
import org.bukkit.entity.Entity;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import java.util.Map;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;

class EntityUtilities
{
    private static final boolean NEW_TRACKER;
    private static final EntityUtilities INSTANCE;
    private FieldAccessor entityTrackerField;
    private FieldAccessor trackedEntitiesField;
    private FieldAccessor trackedPlayersField;
    private Map<Class<?>, MethodAccessor> scanPlayersMethods;
    private MethodAccessor getChunkProvider;
    private FieldAccessor chunkMapField;
    private Map<Class<?>, FieldAccessor> trackerFields;
    private MethodAccessor getEntityFromId;
    
    public static EntityUtilities getInstance() {
        return EntityUtilities.INSTANCE;
    }
    
    private EntityUtilities() {
        this.scanPlayersMethods = new HashMap<Class<?>, MethodAccessor>();
        this.trackerFields = new ConcurrentHashMap<Class<?>, FieldAccessor>();
    }
    
    public void updateEntity(final Entity entity, final List<Player> observers) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        final Collection<?> trackedPlayers = this.getTrackedPlayers(entity);
        final List<Object> nmsPlayers = this.unwrapBukkit(observers);
        trackedPlayers.removeAll(nmsPlayers);
        final Object trackerEntry = this.getEntityTrackerEntry(entity.getWorld(), entity.getEntityId());
        this.scanPlayersMethods.computeIfAbsent(trackerEntry.getClass(), this::findScanPlayers).invoke(trackerEntry, nmsPlayers);
    }
    
    private MethodAccessor findScanPlayers(final Class<?> trackerClass) {
        final MethodAccessor candidate = Accessors.getMethodAcccessorOrNull(trackerClass, "scanPlayers");
        if (candidate != null) {
            return candidate;
        }
        final FuzzyReflection fuzzy = FuzzyReflection.fromClass(trackerClass, true);
        return Accessors.getMethodAccessor(fuzzy.getMethod(FuzzyMethodContract.newBuilder().returnTypeVoid().parameterExactArray(List.class).build()));
    }
    
    public List<Player> getEntityTrackers(final Entity entity) {
        if (entity == null || !entity.isValid()) {
            return new ArrayList<Player>();
        }
        final List<Player> result = new ArrayList<Player>();
        final Collection<?> trackedPlayers = this.getTrackedPlayers(entity);
        for (final Object tracker : trackedPlayers) {
            if (MinecraftReflection.isMinecraftPlayer(tracker)) {
                result.add((Player)MinecraftReflection.getBukkitEntity(tracker));
            }
        }
        return result;
    }
    
    private Collection<?> getTrackedPlayers(final Entity entity) {
        Validate.notNull((Object)entity, "entity cannot be null");
        final Object trackerEntry = this.getEntityTrackerEntry(entity.getWorld(), entity.getEntityId());
        Validate.notNull(trackerEntry, "Could not find entity trackers for " + entity);
        if (this.trackedPlayersField == null) {
            this.trackedPlayersField = Accessors.getFieldAccessor(FuzzyReflection.fromObject(trackerEntry).getFieldByType("java\\.util\\..*"));
        }
        Validate.notNull((Object)this.trackedPlayersField, "Could not find trackedPlayers field");
        final Object value = this.trackedPlayersField.get(trackerEntry);
        if (value instanceof Collection) {
            return (Collection<?>)value;
        }
        if (value instanceof Map) {
            return (Collection<?>)((Map)value).keySet();
        }
        throw new IllegalStateException("trackedPlayers field was an unknown type: expected Collection or Map, but got " + value.getClass());
    }
    
    private Object getNewEntityTracker(final Object worldServer, final int entityId) {
        if (this.getChunkProvider == null) {
            final Class<?> chunkProviderClass = MinecraftReflection.getMinecraftClass("ChunkProviderServer");
            this.getChunkProvider = Accessors.getMethodAccessor(FuzzyReflection.fromClass(worldServer.getClass(), false).getMethod(FuzzyMethodContract.newBuilder().parameterCount(0).returnTypeExact(chunkProviderClass).build()));
        }
        final Object chunkProvider = this.getChunkProvider.invoke(worldServer, new Object[0]);
        if (this.chunkMapField == null) {
            final Class<?> chunkMapClass = MinecraftReflection.getMinecraftClass("PlayerChunkMap");
            this.chunkMapField = Accessors.getFieldAccessor(FuzzyReflection.fromClass(chunkProvider.getClass(), false).getField(FuzzyFieldContract.newBuilder().typeExact(chunkMapClass).build()));
        }
        final Object playerChunkMap = this.chunkMapField.get(chunkProvider);
        if (this.trackedEntitiesField == null) {
            this.trackedEntitiesField = Accessors.getFieldAccessor(FuzzyReflection.fromClass(playerChunkMap.getClass(), false).getField(FuzzyFieldContract.newBuilder().typeDerivedOf(Map.class).nameExact("trackedEntities").build()));
        }
        final Map<Integer, Object> trackedEntities = (Map<Integer, Object>)this.trackedEntitiesField.get(playerChunkMap);
        return trackedEntities.get(entityId);
    }
    
    private Object getEntityTrackerEntry(final World world, final int entityID) {
        final BukkitUnwrapper unwrapper = new BukkitUnwrapper();
        final Object worldServer = unwrapper.unwrapItem(world);
        if (EntityUtilities.NEW_TRACKER) {
            return this.getNewEntityTracker(worldServer, entityID);
        }
        if (this.entityTrackerField == null) {
            this.entityTrackerField = Accessors.getFieldAccessor(FuzzyReflection.fromObject(worldServer).getFieldByType("tracker", MinecraftReflection.getEntityTrackerClass()));
        }
        final Object tracker = this.entityTrackerField.get(worldServer);
        if (this.trackedEntitiesField == null) {
            this.trackedEntitiesField = Accessors.getFieldAccessor(FuzzyReflection.fromObject(tracker, false).getFieldByType("trackedEntities", MinecraftReflection.getIntHashMapClass()));
        }
        final Object trackedEntities = this.trackedEntitiesField.get(tracker);
        return WrappedIntHashMap.fromHandle(trackedEntities).get(entityID);
    }
    
    public Entity getEntityFromID(final World world, final int entityID) {
        Validate.notNull((Object)world, "world cannot be null");
        Validate.isTrue(entityID >= 0, "entityID cannot be negative");
        try {
            if (EntityUtilities.NEW_TRACKER) {
                final Object worldServer = BukkitUnwrapper.getInstance().unwrapItem(world);
                if (this.getEntityFromId == null) {
                    final FuzzyReflection fuzzy = FuzzyReflection.fromClass(worldServer.getClass(), false);
                    this.getEntityFromId = Accessors.getMethodAccessor(fuzzy.getMethod(FuzzyMethodContract.newBuilder().parameterExactArray(Integer.TYPE).returnTypeExact(MinecraftReflection.getEntityClass()).build()));
                }
                final Object entity = this.getEntityFromId.invoke(worldServer, entityID);
                if (entity != null) {
                    return (Entity)MinecraftReflection.getBukkitEntity(entity);
                }
            }
            final Object trackerEntry = this.getEntityTrackerEntry(world, entityID);
            Object tracker = null;
            if (trackerEntry != null) {
                final Object reference;
                final FieldAccessor trackerField = this.trackerFields.computeIfAbsent(trackerEntry.getClass(), x -> {
                    try {
                        return Accessors.getFieldAccessor(reference.getClass(), "tracker", true);
                    }
                    catch (Exception e2) {
                        return Accessors.getFieldAccessor(FuzzyReflection.fromObject(reference, true).getFieldByType("tracker", MinecraftReflection.getEntityClass()));
                    }
                });
                tracker = trackerField.get(trackerEntry);
            }
            return (tracker != null) ? ((Entity)MinecraftReflection.getBukkitEntity(tracker)) : null;
        }
        catch (Exception e) {
            throw new FieldAccessException("Cannot find entity from ID " + entityID + ".", e);
        }
    }
    
    private List<Object> unwrapBukkit(final List<Player> players) {
        final List<Object> output = (List<Object>)Lists.newArrayList();
        final BukkitUnwrapper unwrapper = new BukkitUnwrapper();
        for (final Player player : players) {
            final Object result = unwrapper.unwrapItem(player);
            if (result == null) {
                throw new IllegalArgumentException("Cannot unwrap item " + player);
            }
            output.add(result);
        }
        return output;
    }
    
    static {
        NEW_TRACKER = MinecraftVersion.VILLAGE_UPDATE.atOrAbove();
        INSTANCE = new EntityUtilities();
    }
}
