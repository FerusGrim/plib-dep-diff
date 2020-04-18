package com.comphenix.protocol.injector;

import com.google.common.collect.Lists;
import com.comphenix.protocol.wrappers.WrappedIntHashMap;
import org.bukkit.World;
import java.util.Map;
import com.comphenix.protocol.reflect.FieldUtils;
import org.apache.commons.lang.Validate;
import java.util.Iterator;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.Collection;
import com.comphenix.protocol.reflect.FuzzyReflection;
import org.bukkit.entity.Player;
import java.util.List;
import org.bukkit.entity.Entity;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

class EntityUtilities
{
    private static Field entityTrackerField;
    private static Field trackedEntitiesField;
    private static Field trackedPlayersField;
    private static Field trackerField;
    private static Method scanPlayersMethod;
    
    public static void updateEntity(final Entity entity, final List<Player> observers) throws FieldAccessException {
        if (entity == null || !entity.isValid()) {
            return;
        }
        try {
            final Object trackerEntry = getEntityTrackerEntry(entity.getWorld(), entity.getEntityId());
            if (trackerEntry == null) {
                throw new IllegalArgumentException("Cannot find entity trackers for " + entity + ".");
            }
            if (EntityUtilities.trackedPlayersField == null) {
                EntityUtilities.trackedPlayersField = FuzzyReflection.fromObject(trackerEntry).getFieldByType("java\\.util\\..*");
            }
            final Collection<?> trackedPlayers = getTrackedPlayers(EntityUtilities.trackedPlayersField, trackerEntry);
            final List<Object> nmsPlayers = unwrapBukkit(observers);
            trackedPlayers.removeAll(nmsPlayers);
            if (EntityUtilities.scanPlayersMethod == null) {
                EntityUtilities.scanPlayersMethod = trackerEntry.getClass().getMethod("scanPlayers", List.class);
            }
            EntityUtilities.scanPlayersMethod.invoke(trackerEntry, nmsPlayers);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (IllegalAccessException e2) {
            throw new FieldAccessException("Security limitation prevents access to 'get' method in IntHashMap", e2);
        }
        catch (InvocationTargetException e3) {
            throw new RuntimeException("Exception occurred in Minecraft.", e3);
        }
        catch (SecurityException e4) {
            throw new FieldAccessException("Security limitation prevents access to 'scanPlayers' method in trackerEntry.", e4);
        }
        catch (NoSuchMethodException e5) {
            throw new FieldAccessException("Cannot find 'scanPlayers' method. Is ProtocolLib up to date?", e5);
        }
    }
    
    public static List<Player> getEntityTrackers(final Entity entity) {
        if (entity == null || !entity.isValid()) {
            return new ArrayList<Player>();
        }
        try {
            final List<Player> result = new ArrayList<Player>();
            final Object trackerEntry = getEntityTrackerEntry(entity.getWorld(), entity.getEntityId());
            if (trackerEntry == null) {
                throw new IllegalArgumentException("Cannot find entity trackers for " + entity + ".");
            }
            if (EntityUtilities.trackedPlayersField == null) {
                EntityUtilities.trackedPlayersField = FuzzyReflection.fromObject(trackerEntry).getFieldByType("java\\.util\\..*");
            }
            final Collection<?> trackedPlayers = getTrackedPlayers(EntityUtilities.trackedPlayersField, trackerEntry);
            for (final Object tracker : trackedPlayers) {
                if (MinecraftReflection.isMinecraftPlayer(tracker)) {
                    result.add((Player)MinecraftReflection.getBukkitEntity(tracker));
                }
            }
            return result;
        }
        catch (IllegalAccessException e) {
            throw new FieldAccessException("Security limitation prevented access to the list of tracked players.", e);
        }
    }
    
    private static Collection<?> getTrackedPlayers(final Field field, final Object entry) throws IllegalAccessException {
        Validate.notNull((Object)field, "Cannot find 'trackedPlayers' field.");
        Validate.notNull(entry, "entry cannot be null!");
        final Object value = FieldUtils.readField(field, entry, false);
        if (value instanceof Collection) {
            return (Collection<?>)value;
        }
        if (value instanceof Map) {
            return (Collection<?>)((Map)value).keySet();
        }
        throw new IllegalStateException("trackedPlayers field was an unknown type: expected Collection or Map, but got " + value.getClass());
    }
    
    private static Object getEntityTrackerEntry(final World world, final int entityID) throws FieldAccessException, IllegalArgumentException {
        final BukkitUnwrapper unwrapper = new BukkitUnwrapper();
        final Object worldServer = unwrapper.unwrapItem(world);
        if (EntityUtilities.entityTrackerField == null) {
            EntityUtilities.entityTrackerField = FuzzyReflection.fromObject(worldServer).getFieldByType("tracker", MinecraftReflection.getEntityTrackerClass());
        }
        Object tracker = null;
        try {
            tracker = FieldUtils.readField(EntityUtilities.entityTrackerField, worldServer, false);
        }
        catch (IllegalAccessException e) {
            throw new FieldAccessException("Cannot access 'tracker' field due to security limitations.", e);
        }
        if (EntityUtilities.trackedEntitiesField == null) {
            EntityUtilities.trackedEntitiesField = FuzzyReflection.fromObject(tracker, false).getFieldByType("trackedEntities", MinecraftReflection.getIntHashMapClass());
        }
        Object trackedEntities = null;
        try {
            trackedEntities = FieldUtils.readField(EntityUtilities.trackedEntitiesField, tracker, false);
        }
        catch (IllegalAccessException e2) {
            throw new FieldAccessException("Cannot access 'trackedEntities' field due to security limitations.", e2);
        }
        return WrappedIntHashMap.fromHandle(trackedEntities).get(entityID);
    }
    
    public static Entity getEntityFromID(final World world, final int entityID) throws FieldAccessException {
        try {
            final Object trackerEntry = getEntityTrackerEntry(world, entityID);
            Object tracker = null;
            if (trackerEntry != null) {
                if (EntityUtilities.trackerField == null) {
                    try {
                        final Class<?> entryClass = MinecraftReflection.getMinecraftClass("EntityTrackerEntry");
                        EntityUtilities.trackerField = entryClass.getDeclaredField("tracker");
                    }
                    catch (NoSuchFieldException e2) {
                        EntityUtilities.trackerField = FuzzyReflection.fromObject(trackerEntry, true).getFieldByType("tracker", MinecraftReflection.getEntityClass());
                    }
                }
                tracker = FieldUtils.readField(EntityUtilities.trackerField, trackerEntry, true);
            }
            if (tracker != null) {
                return (Entity)MinecraftReflection.getBukkitEntity(tracker);
            }
            return null;
        }
        catch (Exception e) {
            throw new FieldAccessException("Cannot find entity from ID " + entityID + ".", e);
        }
    }
    
    private static List<Object> unwrapBukkit(final List<Player> players) {
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
}
