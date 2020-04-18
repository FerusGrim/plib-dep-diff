package com.comphenix.protocol.wrappers;

import com.google.common.base.Objects;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.EquivalentConverter;
import java.lang.reflect.Constructor;

public class PlayerInfoData
{
    private static Constructor<?> constructor;
    private final int latency;
    private final EnumWrappers.NativeGameMode gameMode;
    private final WrappedGameProfile profile;
    private final WrappedChatComponent displayName;
    
    public PlayerInfoData(final WrappedGameProfile profile, final int latency, final EnumWrappers.NativeGameMode gameMode, final WrappedChatComponent displayName) {
        this.profile = profile;
        this.latency = latency;
        this.gameMode = gameMode;
        this.displayName = displayName;
    }
    
    public WrappedGameProfile getProfile() {
        return this.profile;
    }
    
    @Deprecated
    public int getPing() {
        return this.latency;
    }
    
    public int getLatency() {
        return this.latency;
    }
    
    public EnumWrappers.NativeGameMode getGameMode() {
        return this.gameMode;
    }
    
    public WrappedChatComponent getDisplayName() {
        return this.displayName;
    }
    
    public static EquivalentConverter<PlayerInfoData> getConverter() {
        return new EquivalentConverter<PlayerInfoData>() {
            @Override
            public Object getGeneric(final PlayerInfoData specific) {
                if (PlayerInfoData.constructor == null) {
                    try {
                        PlayerInfoData.constructor = MinecraftReflection.getPlayerInfoDataClass().getConstructor(MinecraftReflection.getMinecraftClass("PacketPlayOutPlayerInfo"), MinecraftReflection.getGameProfileClass(), Integer.TYPE, EnumWrappers.getGameModeClass(), MinecraftReflection.getIChatBaseComponentClass());
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Cannot find PlayerInfoData constructor.", e);
                    }
                }
                try {
                    final Object result = PlayerInfoData.constructor.newInstance(null, specific.profile.handle, specific.latency, EnumWrappers.getGameModeConverter().getGeneric(specific.gameMode), (specific.displayName != null) ? specific.displayName.handle : null);
                    return result;
                }
                catch (Exception e) {
                    throw new RuntimeException("Failed to construct PlayerInfoData.", e);
                }
            }
            
            @Override
            public PlayerInfoData getSpecific(final Object generic) {
                if (MinecraftReflection.isPlayerInfoData(generic)) {
                    final StructureModifier<Object> modifier = new StructureModifier<Object>(generic.getClass(), null, false).withTarget(generic);
                    final StructureModifier<WrappedGameProfile> gameProfiles = modifier.withType(MinecraftReflection.getGameProfileClass(), BukkitConverters.getWrappedGameProfileConverter());
                    final WrappedGameProfile gameProfile = gameProfiles.read(0);
                    final StructureModifier<Integer> ints = modifier.withType(Integer.TYPE);
                    final int latency = ints.read(0);
                    final StructureModifier<EnumWrappers.NativeGameMode> gameModes = modifier.withType(EnumWrappers.getGameModeClass(), EnumWrappers.getGameModeConverter());
                    final EnumWrappers.NativeGameMode gameMode = gameModes.read(0);
                    final StructureModifier<WrappedChatComponent> displayNames = modifier.withType(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter());
                    final WrappedChatComponent displayName = displayNames.read(0);
                    return new PlayerInfoData(gameProfile, latency, gameMode, displayName);
                }
                return null;
            }
            
            @Override
            public Class<PlayerInfoData> getSpecificType() {
                return PlayerInfoData.class;
            }
        };
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof PlayerInfoData) {
            final PlayerInfoData other = (PlayerInfoData)obj;
            return this.profile.equals(other.profile) && this.latency == other.latency && this.gameMode == other.gameMode && this.displayName.equals(other.displayName);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.latency, this.gameMode, this.profile, this.displayName });
    }
    
    @Override
    public String toString() {
        return String.format("PlayerInfoData[latency=%s, gameMode=%s, profile=%s, displayName=%s", this.latency, this.gameMode, this.profile, this.displayName);
    }
}
