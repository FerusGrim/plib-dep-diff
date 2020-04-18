package com.comphenix.protocol.wrappers;

import com.google.common.base.Objects;
import com.comphenix.protocol.wrappers.collection.ConvertedMultimap;
import com.google.common.base.Charsets;
import com.comphenix.protocol.error.PluginContext;
import java.util.concurrent.TimeUnit;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.OfflinePlayer;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.entity.Player;
import java.util.UUID;
import com.google.common.collect.Multimap;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.error.ReportType;

public class WrappedGameProfile extends AbstractWrapper
{
    public static final ReportType REPORT_INVALID_UUID;
    private static final Class<?> GAME_PROFILE;
    private static final ConstructorAccessor CREATE_STRING_STRING;
    private static final ConstructorAccessor CREATE_UUID_STRING;
    private static final FieldAccessor GET_UUID_STRING;
    private static final MethodAccessor GET_ID;
    private static final MethodAccessor GET_NAME;
    private static final MethodAccessor GET_PROPERTIES;
    private static final MethodAccessor IS_COMPLETE;
    private static FieldAccessor PLAYER_PROFILE;
    private static FieldAccessor OFFLINE_PROFILE;
    private Multimap<String, WrappedSignedProperty> propertyMap;
    private volatile UUID parsedUUID;
    
    private WrappedGameProfile(final Object profile) {
        super(WrappedGameProfile.GAME_PROFILE);
        this.setHandle(profile);
    }
    
    public static WrappedGameProfile fromPlayer(final Player player) {
        FieldAccessor accessor = WrappedGameProfile.PLAYER_PROFILE;
        if (accessor == null) {
            accessor = (WrappedGameProfile.PLAYER_PROFILE = Accessors.getFieldAccessor(MinecraftReflection.getEntityHumanClass(), WrappedGameProfile.GAME_PROFILE, true));
        }
        final Object nmsPlayer = BukkitUnwrapper.getInstance().unwrapItem(player);
        return fromHandle(WrappedGameProfile.PLAYER_PROFILE.get(nmsPlayer));
    }
    
    public static WrappedGameProfile fromOfflinePlayer(final OfflinePlayer player) {
        FieldAccessor accessor = WrappedGameProfile.OFFLINE_PROFILE;
        if (accessor == null) {
            accessor = (WrappedGameProfile.OFFLINE_PROFILE = Accessors.getFieldAccessor(player.getClass(), WrappedGameProfile.GAME_PROFILE, true));
        }
        return fromHandle(WrappedGameProfile.OFFLINE_PROFILE.get(player));
    }
    
    @Deprecated
    public WrappedGameProfile(final String id, final String name) {
        super(WrappedGameProfile.GAME_PROFILE);
        if (WrappedGameProfile.CREATE_STRING_STRING != null) {
            this.setHandle(WrappedGameProfile.CREATE_STRING_STRING.invoke(id, name));
        }
        else {
            if (WrappedGameProfile.CREATE_UUID_STRING == null) {
                throw new IllegalArgumentException("Unsupported GameProfile constructor.");
            }
            this.setHandle(WrappedGameProfile.CREATE_UUID_STRING.invoke(parseUUID(id), name));
        }
    }
    
    public WrappedGameProfile(final UUID uuid, final String name) {
        super(WrappedGameProfile.GAME_PROFILE);
        if (WrappedGameProfile.CREATE_STRING_STRING != null) {
            this.setHandle(WrappedGameProfile.CREATE_STRING_STRING.invoke((uuid != null) ? uuid.toString() : null, name));
        }
        else {
            if (WrappedGameProfile.CREATE_UUID_STRING == null) {
                throw new IllegalArgumentException("Unsupported GameProfile constructor.");
            }
            this.setHandle(WrappedGameProfile.CREATE_UUID_STRING.invoke(uuid, name));
        }
    }
    
    public static WrappedGameProfile fromHandle(final Object handle) {
        if (handle == null) {
            return null;
        }
        return new WrappedGameProfile(handle);
    }
    
    private static UUID parseUUID(final String id) {
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id);
        }
        catch (IllegalArgumentException e) {
            ProtocolLibrary.getErrorReporter().reportWarning(WrappedGameProfile.class, Report.newBuilder(WrappedGameProfile.REPORT_INVALID_UUID).rateLimit(1L, TimeUnit.HOURS).messageParam(PluginContext.getPluginCaller(new Exception()), id));
            return UUID.nameUUIDFromBytes(id.getBytes(Charsets.UTF_8));
        }
    }
    
    public UUID getUUID() {
        UUID uuid = this.parsedUUID;
        if (uuid == null) {
            try {
                if (WrappedGameProfile.GET_UUID_STRING != null) {
                    uuid = parseUUID(this.getId());
                }
                else {
                    if (WrappedGameProfile.GET_ID == null) {
                        throw new IllegalStateException("Unsupported getId() method");
                    }
                    uuid = (UUID)WrappedGameProfile.GET_ID.invoke(this.handle, new Object[0]);
                }
                this.parsedUUID = uuid;
            }
            catch (IllegalArgumentException e) {
                throw new IllegalStateException("Cannot parse ID " + this.getId() + " as an UUID in player profile " + this.getName(), e);
            }
        }
        return uuid;
    }
    
    public String getId() {
        if (WrappedGameProfile.GET_UUID_STRING != null) {
            return (String)WrappedGameProfile.GET_UUID_STRING.get(this.handle);
        }
        if (WrappedGameProfile.GET_ID != null) {
            final UUID uuid = (UUID)WrappedGameProfile.GET_ID.invoke(this.handle, new Object[0]);
            return (uuid != null) ? uuid.toString() : null;
        }
        throw new IllegalStateException("Unsupported getId() method");
    }
    
    public String getName() {
        if (WrappedGameProfile.GET_NAME != null) {
            return (String)WrappedGameProfile.GET_NAME.invoke(this.handle, new Object[0]);
        }
        throw new IllegalStateException("Unsupported getName() method");
    }
    
    public Multimap<String, WrappedSignedProperty> getProperties() {
        Multimap<String, WrappedSignedProperty> result = this.propertyMap;
        if (result == null) {
            final Multimap properties = (Multimap)WrappedGameProfile.GET_PROPERTIES.invoke(this.handle, new Object[0]);
            result = (Multimap<String, WrappedSignedProperty>)new ConvertedMultimap<String, Object, WrappedSignedProperty>(GuavaWrappers.getBukkitMultimap((com.google.common.collect.Multimap<Object, Object>)properties)) {
                protected Object toInner(final WrappedSignedProperty outer) {
                    return outer.handle;
                }
                
                @Override
                protected Object toInnerObject(final Object outer) {
                    if (outer instanceof WrappedSignedProperty) {
                        return this.toInner((WrappedSignedProperty)outer);
                    }
                    return outer;
                }
                
                @Override
                protected WrappedSignedProperty toOuter(final Object inner) {
                    return WrappedSignedProperty.fromHandle(inner);
                }
            };
            this.propertyMap = result;
        }
        return result;
    }
    
    public WrappedGameProfile withName(final String name) {
        return new WrappedGameProfile(this.getId(), name);
    }
    
    public WrappedGameProfile withId(final String id) {
        return new WrappedGameProfile(id, this.getName());
    }
    
    public boolean isComplete() {
        return (boolean)WrappedGameProfile.IS_COMPLETE.invoke(this.handle, new Object[0]);
    }
    
    @Override
    public String toString() {
        return String.valueOf(this.getHandle());
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.getId(), this.getName() });
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof WrappedGameProfile) {
            final WrappedGameProfile other = (WrappedGameProfile)obj;
            return Objects.equal(this.getHandle(), other.getHandle());
        }
        return false;
    }
    
    static {
        REPORT_INVALID_UUID = new ReportType("Plugin %s created a profile with '%s' as an UUID.");
        GAME_PROFILE = MinecraftReflection.getGameProfileClass();
        CREATE_STRING_STRING = Accessors.getConstructorAccessorOrNull(WrappedGameProfile.GAME_PROFILE, String.class, String.class);
        CREATE_UUID_STRING = Accessors.getConstructorAccessorOrNull(WrappedGameProfile.GAME_PROFILE, UUID.class, String.class);
        GET_UUID_STRING = Accessors.getFieldAcccessorOrNull(WrappedGameProfile.GAME_PROFILE, "id", String.class);
        GET_ID = Accessors.getMethodAcccessorOrNull(WrappedGameProfile.GAME_PROFILE, "getId");
        GET_NAME = Accessors.getMethodAcccessorOrNull(WrappedGameProfile.GAME_PROFILE, "getName");
        GET_PROPERTIES = Accessors.getMethodAcccessorOrNull(WrappedGameProfile.GAME_PROFILE, "getProperties");
        IS_COMPLETE = Accessors.getMethodAcccessorOrNull(WrappedGameProfile.GAME_PROFILE, "isComplete");
    }
}
