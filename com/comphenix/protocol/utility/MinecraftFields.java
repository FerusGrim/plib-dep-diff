package com.comphenix.protocol.utility;

import com.google.common.base.Preconditions;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import org.bukkit.entity.Player;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;

public class MinecraftFields
{
    private static volatile FieldAccessor CONNECTION_ACCESSOR;
    private static volatile FieldAccessor NETWORK_ACCESSOR;
    
    private MinecraftFields() {
    }
    
    public static Object getNetworkManager(final Player player) {
        final Object nmsPlayer = BukkitUnwrapper.getInstance().unwrapItem(player);
        if (MinecraftFields.NETWORK_ACCESSOR == null) {
            final Class<?> networkClass = MinecraftReflection.getNetworkManagerClass();
            final Class<?> connectionClass = MinecraftReflection.getPlayerConnectionClass();
            MinecraftFields.NETWORK_ACCESSOR = Accessors.getFieldAccessor(connectionClass, networkClass, true);
        }
        final Object playerConnection = getPlayerConnection(nmsPlayer);
        if (playerConnection != null) {
            return MinecraftFields.NETWORK_ACCESSOR.get(playerConnection);
        }
        return null;
    }
    
    public static Object getPlayerConnection(final Player player) {
        Preconditions.checkNotNull((Object)player, (Object)"player cannot be null!");
        return getPlayerConnection(BukkitUnwrapper.getInstance().unwrapItem(player));
    }
    
    private static Object getPlayerConnection(final Object nmsPlayer) {
        Preconditions.checkNotNull(nmsPlayer, (Object)"nmsPlayer cannot be null!");
        if (MinecraftFields.CONNECTION_ACCESSOR == null) {
            final Class<?> connectionClass = MinecraftReflection.getPlayerConnectionClass();
            MinecraftFields.CONNECTION_ACCESSOR = Accessors.getFieldAccessor(nmsPlayer.getClass(), connectionClass, true);
        }
        return MinecraftFields.CONNECTION_ACCESSOR.get(nmsPlayer);
    }
}
