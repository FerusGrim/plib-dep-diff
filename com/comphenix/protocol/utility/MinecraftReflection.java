package com.comphenix.protocol.utility;

import com.google.common.collect.Maps;
import com.google.common.base.Joiner;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.net.ServerSocket;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtType;
import java.net.InetAddress;
import java.io.DataOutput;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import java.util.HashSet;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import com.comphenix.protocol.reflect.ClassAnalyser;
import com.comphenix.protocol.reflect.MethodInfo;
import java.io.DataInputStream;
import java.util.Set;
import java.util.Map;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.fuzzy.FuzzyClassContract;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.FuzzyReflection;
import javax.annotation.Nonnull;
import com.comphenix.protocol.reflect.accessors.Accessors;
import java.util.regex.Matcher;
import org.bukkit.Server;
import com.comphenix.protocol.ProtocolLogger;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMatchers;
import java.lang.reflect.Method;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import java.util.concurrent.ConcurrentMap;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import java.util.regex.Pattern;
import com.comphenix.protocol.error.ReportType;

public class MinecraftReflection
{
    public static final ReportType REPORT_CANNOT_FIND_MCPC_REMAPPER;
    public static final ReportType REPORT_CANNOT_LOAD_CPC_REMAPPER;
    public static final ReportType REPORT_NON_CRAFTBUKKIT_LIBRARY_PACKAGE;
    private static final String CANONICAL_REGEX = "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    @Deprecated
    public static final String MINECRAFT_OBJECT = "net\\.minecraft\\.(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    private static String DYNAMIC_PACKAGE_MATCHER;
    private static final String FORGE_ENTITY_PACKAGE = "net.minecraft.entity";
    private static String MINECRAFT_PREFIX_PACKAGE;
    private static final Pattern PACKAGE_VERSION_MATCHER;
    private static String MINECRAFT_FULL_PACKAGE;
    private static String CRAFTBUKKIT_PACKAGE;
    static CachedPackage minecraftPackage;
    static CachedPackage craftbukkitPackage;
    static CachedPackage libraryPackage;
    private static AbstractFuzzyMatcher<Class<?>> fuzzyMatcher;
    private static String packageVersion;
    private static Class<?> itemStackArrayClass;
    private static ConcurrentMap<Class<?>, MethodAccessor> getBukkitEntityCache;
    private static ClassSource classSource;
    private static boolean initializing;
    private static Boolean cachedNetty;
    private static Boolean cachedWatcherObject;
    private static Object itemStackAir;
    private static Method asNMSCopy;
    private static Method asCraftMirror;
    private static Boolean nullEnforced;
    private static Method isEmpty;
    
    private MinecraftReflection() {
    }
    
    public static String getMinecraftObjectRegex() {
        if (MinecraftReflection.DYNAMIC_PACKAGE_MATCHER == null) {
            getMinecraftPackage();
        }
        return MinecraftReflection.DYNAMIC_PACKAGE_MATCHER;
    }
    
    public static AbstractFuzzyMatcher<Class<?>> getMinecraftObjectMatcher() {
        if (MinecraftReflection.fuzzyMatcher == null) {
            MinecraftReflection.fuzzyMatcher = FuzzyMatchers.matchRegex(getMinecraftObjectRegex(), 50);
        }
        return MinecraftReflection.fuzzyMatcher;
    }
    
    public static String getMinecraftPackage() {
        if (MinecraftReflection.MINECRAFT_FULL_PACKAGE != null) {
            return MinecraftReflection.MINECRAFT_FULL_PACKAGE;
        }
        if (MinecraftReflection.initializing) {
            throw new IllegalStateException("Already initializing minecraft package!");
        }
        MinecraftReflection.initializing = true;
        final Server craftServer = Bukkit.getServer();
        if (craftServer != null) {
            try {
                final Class<?> craftClass = craftServer.getClass();
                MinecraftReflection.CRAFTBUKKIT_PACKAGE = getPackage(craftClass.getCanonicalName());
                final Matcher packageMatcher = MinecraftReflection.PACKAGE_VERSION_MATCHER.matcher(MinecraftReflection.CRAFTBUKKIT_PACKAGE);
                if (packageMatcher.matches()) {
                    MinecraftReflection.packageVersion = packageMatcher.group(1);
                }
                else {
                    final MinecraftVersion version = new MinecraftVersion(craftServer);
                    if (MinecraftVersion.SCARY_UPDATE.compareTo(version) <= 0) {
                        MinecraftReflection.packageVersion = "v" + version.getMajor() + "_" + version.getMinor() + "_R1";
                        ProtocolLogger.log(Level.WARNING, "Assuming package version: " + MinecraftReflection.packageVersion, new Object[0]);
                    }
                }
                handleLibigot();
                final Class<?> craftEntity = getCraftEntityClass();
                final Method getHandle = craftEntity.getMethod("getHandle", (Class<?>[])new Class[0]);
                MinecraftReflection.MINECRAFT_FULL_PACKAGE = getPackage(getHandle.getReturnType().getCanonicalName());
                if (!MinecraftReflection.MINECRAFT_FULL_PACKAGE.startsWith(MinecraftReflection.MINECRAFT_PREFIX_PACKAGE)) {
                    if (MinecraftReflection.MINECRAFT_FULL_PACKAGE.equals("net.minecraft.entity")) {
                        MinecraftReflection.MINECRAFT_FULL_PACKAGE = CachedPackage.combine(MinecraftReflection.MINECRAFT_PREFIX_PACKAGE, MinecraftReflection.packageVersion);
                    }
                    else {
                        MinecraftReflection.MINECRAFT_PREFIX_PACKAGE = MinecraftReflection.MINECRAFT_FULL_PACKAGE;
                    }
                    final String matcher = ((MinecraftReflection.MINECRAFT_PREFIX_PACKAGE.length() > 0) ? Pattern.quote(MinecraftReflection.MINECRAFT_PREFIX_PACKAGE + ".") : "") + "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
                    setDynamicPackageMatcher("(" + matcher + ")|(" + "net\\.minecraft\\.(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" + ")");
                }
                else {
                    setDynamicPackageMatcher("net\\.minecraft\\.(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");
                }
                return MinecraftReflection.MINECRAFT_FULL_PACKAGE;
            }
            catch (SecurityException e) {
                throw new RuntimeException("Security violation. Cannot get handle method.", e);
            }
            catch (NoSuchMethodException e2) {
                throw new IllegalStateException("Cannot find getHandle() method on server. Is this a modified CraftBukkit version?", e2);
            }
            finally {
                MinecraftReflection.initializing = false;
            }
        }
        MinecraftReflection.initializing = false;
        throw new IllegalStateException("Could not find Bukkit. Is it running?");
    }
    
    public static String getPackageVersion() {
        getMinecraftPackage();
        return MinecraftReflection.packageVersion;
    }
    
    private static void setDynamicPackageMatcher(final String regex) {
        MinecraftReflection.DYNAMIC_PACKAGE_MATCHER = regex;
        MinecraftReflection.fuzzyMatcher = null;
    }
    
    private static void handleLibigot() {
        try {
            getCraftEntityClass();
        }
        catch (RuntimeException e) {
            MinecraftReflection.craftbukkitPackage = null;
            MinecraftReflection.CRAFTBUKKIT_PACKAGE = "org.bukkit.craftbukkit";
            getCraftEntityClass();
        }
    }
    
    public static void setMinecraftPackage(final String minecraftPackage, final String craftBukkitPackage) {
        MinecraftReflection.MINECRAFT_FULL_PACKAGE = minecraftPackage;
        MinecraftReflection.CRAFTBUKKIT_PACKAGE = craftBukkitPackage;
        if (getMinecraftServerClass() == null) {
            throw new IllegalArgumentException("Cannot find MinecraftServer for package " + minecraftPackage);
        }
        setDynamicPackageMatcher("net\\.minecraft\\.(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");
    }
    
    public static String getCraftBukkitPackage() {
        if (MinecraftReflection.CRAFTBUKKIT_PACKAGE == null) {
            getMinecraftPackage();
        }
        return MinecraftReflection.CRAFTBUKKIT_PACKAGE;
    }
    
    private static String getPackage(final String fullName) {
        final int index = fullName.lastIndexOf(".");
        if (index > 0) {
            return fullName.substring(0, index);
        }
        return "";
    }
    
    public static Object getBukkitEntity(final Object nmsObject) {
        if (nmsObject == null) {
            return null;
        }
        try {
            final Class<?> clazz = nmsObject.getClass();
            MethodAccessor accessor = MinecraftReflection.getBukkitEntityCache.get(clazz);
            if (accessor == null) {
                final MethodAccessor created = Accessors.getMethodAccessor(clazz, "getBukkitEntity", (Class<?>[])new Class[0]);
                accessor = MinecraftReflection.getBukkitEntityCache.putIfAbsent(clazz, created);
                if (accessor == null) {
                    accessor = created;
                }
            }
            return accessor.invoke(nmsObject, new Object[0]);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Cannot get Bukkit entity from " + nmsObject, e);
        }
    }
    
    public static boolean isMinecraftObject(@Nonnull final Object obj) {
        return obj != null && obj.getClass().getName().startsWith(MinecraftReflection.MINECRAFT_PREFIX_PACKAGE);
    }
    
    public static boolean isMinecraftClass(@Nonnull final Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz cannot be NULL.");
        }
        return getMinecraftObjectMatcher().isMatch(clazz, null);
    }
    
    public static boolean isMinecraftObject(@Nonnull final Object obj, final String className) {
        if (obj == null) {
            return false;
        }
        final String javaName = obj.getClass().getName();
        return javaName.startsWith(MinecraftReflection.MINECRAFT_PREFIX_PACKAGE) && javaName.endsWith(className);
    }
    
    public static boolean is(final Class<?> clazz, final Object object) {
        return clazz != null && object != null && clazz.isAssignableFrom(object.getClass());
    }
    
    public static boolean is(final Class<?> clazz, final Class<?> test) {
        return clazz != null && test != null && clazz.isAssignableFrom(test);
    }
    
    public static boolean isChunkPosition(final Object obj) {
        return is(getChunkPositionClass(), obj);
    }
    
    public static boolean isBlockPosition(final Object obj) {
        return is(getBlockPositionClass(), obj);
    }
    
    public static boolean isChunkCoordIntPair(final Object obj) {
        return is(getChunkCoordIntPair(), obj);
    }
    
    public static boolean isChunkCoordinates(final Object obj) {
        return is(getChunkCoordinatesClass(), obj);
    }
    
    public static boolean isPacketClass(final Object obj) {
        return is(getPacketClass(), obj);
    }
    
    public static boolean isLoginHandler(final Object obj) {
        return is(getNetLoginHandlerClass(), obj);
    }
    
    public static boolean isServerHandler(final Object obj) {
        return is(getPlayerConnectionClass(), obj);
    }
    
    public static boolean isMinecraftEntity(final Object obj) {
        return is(getEntityClass(), obj);
    }
    
    public static boolean isItemStack(final Object value) {
        return is(getItemStackClass(), value);
    }
    
    public static boolean isCraftPlayer(final Object value) {
        return is(getCraftPlayerClass(), value);
    }
    
    public static boolean isMinecraftPlayer(final Object obj) {
        return is(getEntityPlayerClass(), obj);
    }
    
    public static boolean isWatchableObject(final Object obj) {
        return is(getWatchableObjectClass(), obj);
    }
    
    public static boolean isDataWatcher(final Object obj) {
        return is(getDataWatcherClass(), obj);
    }
    
    public static boolean isIntHashMap(final Object obj) {
        return is(getIntHashMapClass(), obj);
    }
    
    public static boolean isCraftItemStack(final Object obj) {
        return is(getCraftItemStackClass(), obj);
    }
    
    public static Class<?> getEntityPlayerClass() {
        try {
            return getMinecraftClass("EntityPlayer");
        }
        catch (RuntimeException e2) {
            try {
                final Method getHandle = FuzzyReflection.fromClass(getCraftBukkitClass("entity.CraftPlayer")).getMethodByName("getHandle");
                return setMinecraftClass("EntityPlayer", getHandle.getReturnType());
            }
            catch (IllegalArgumentException e1) {
                throw new RuntimeException("Could not find EntityPlayer class.", e1);
            }
        }
    }
    
    public static Class<?> getEntityHumanClass() {
        return getEntityPlayerClass().getSuperclass();
    }
    
    public static Class<?> getGameProfileClass() {
        if (!isUsingNetty()) {
            throw new IllegalStateException("GameProfile does not exist in version 1.6.4 and earlier.");
        }
        try {
            return getClass("com.mojang.authlib.GameProfile");
        }
        catch (Throwable ex) {
            try {
                return getClass("net.minecraft.util.com.mojang.authlib.GameProfile");
            }
            catch (Throwable ex2) {
                final FuzzyReflection reflection = FuzzyReflection.fromClass(PacketType.Login.Client.START.getPacketClass(), true);
                final FuzzyFieldContract contract = FuzzyFieldContract.newBuilder().banModifier(8).typeMatches(FuzzyMatchers.matchRegex("(.*)(GameProfile)", 1)).build();
                return reflection.getField(contract).getType();
            }
        }
    }
    
    public static Class<?> getEntityClass() {
        try {
            return getMinecraftClass("Entity");
        }
        catch (RuntimeException e) {
            return fallbackMethodReturn("Entity", "entity.CraftEntity", "getHandle");
        }
    }
    
    public static Class<?> getCraftChatMessage() {
        return getCraftBukkitClass("util.CraftChatMessage");
    }
    
    public static Class<?> getWorldServerClass() {
        try {
            return getMinecraftClass("WorldServer");
        }
        catch (RuntimeException e) {
            return fallbackMethodReturn("WorldServer", "CraftWorld", "getHandle");
        }
    }
    
    public static Class<?> getNmsWorldClass() {
        try {
            return getMinecraftClass("World");
        }
        catch (RuntimeException e) {
            return setMinecraftClass("World", getWorldServerClass().getSuperclass());
        }
    }
    
    private static Class<?> fallbackMethodReturn(final String nmsClass, final String craftClass, final String methodName) {
        final Class<?> result = FuzzyReflection.fromClass(getCraftBukkitClass(craftClass)).getMethodByName(methodName).getReturnType();
        return setMinecraftClass(nmsClass, result);
    }
    
    public static Class<?> getPacketClass() {
        try {
            return getMinecraftClass("Packet");
        }
        catch (RuntimeException e) {
            FuzzyClassContract paketContract = null;
            if (isUsingNetty()) {
                paketContract = FuzzyClassContract.newBuilder().method(FuzzyMethodContract.newBuilder().parameterDerivedOf(getByteBufClass()).returnTypeVoid()).method(FuzzyMethodContract.newBuilder().parameterDerivedOf(getByteBufClass(), 0).parameterExactType(byte[].class, 1).returnTypeVoid()).build();
            }
            else {
                paketContract = FuzzyClassContract.newBuilder().field(FuzzyFieldContract.newBuilder().typeDerivedOf(Map.class).requireModifier(8)).field(FuzzyFieldContract.newBuilder().typeDerivedOf(Set.class).requireModifier(8)).method(FuzzyMethodContract.newBuilder().parameterSuperOf(DataInputStream.class).returnTypeVoid()).build();
            }
            final Method selected = FuzzyReflection.fromClass(getPlayerConnectionClass()).getMethod(FuzzyMethodContract.newBuilder().parameterMatches(paketContract, 0).parameterCount(1).build());
            final Class<?> clazz = getTopmostClass(selected.getParameterTypes()[0]);
            return setMinecraftClass("Packet", clazz);
        }
    }
    
    public static Class<?> getByteBufClass() {
        try {
            return getClass("io.netty.buffer.ByteBuf");
        }
        catch (Throwable ex) {
            return getClass("net.minecraft.util.io.netty.buffer.ByteBuf");
        }
    }
    
    public static Class<?> getEnumProtocolClass() {
        try {
            return getMinecraftClass("EnumProtocol");
        }
        catch (RuntimeException e) {
            final Method protocolMethod = FuzzyReflection.fromClass(getNetworkManagerClass()).getMethod(FuzzyMethodContract.newBuilder().parameterCount(1).parameterDerivedOf(Enum.class, 0).build());
            return setMinecraftClass("EnumProtocol", protocolMethod.getParameterTypes()[0]);
        }
    }
    
    public static Class<?> getIChatBaseComponentClass() {
        try {
            return getMinecraftClass("IChatBaseComponent");
        }
        catch (RuntimeException e) {
            return setMinecraftClass("IChatBaseComponent", Accessors.getMethodAccessor(getCraftChatMessage(), "fromString", String.class).getMethod().getReturnType().getComponentType());
        }
    }
    
    public static Class<?> getIChatBaseComponentArrayClass() {
        return getArrayClass(getIChatBaseComponentClass());
    }
    
    public static Class<?> getChatComponentTextClass() {
        try {
            return getMinecraftClass("ChatComponentText");
        }
        catch (RuntimeException e) {
            try {
                final Method getScoreboardDisplayName = FuzzyReflection.fromClass(getEntityClass()).getMethodByParameters("getScoreboardDisplayName", getIChatBaseComponentClass(), new Class[0]);
                final Class<?> baseClass = getIChatBaseComponentClass();
                for (final ClassAnalyser.AsmMethod method : ClassAnalyser.getDefault().getMethodCalls(getScoreboardDisplayName)) {
                    final Class<?> owner = method.getOwnerClass();
                    if (isMinecraftClass(owner) && baseClass.isAssignableFrom(owner)) {
                        return setMinecraftClass("ChatComponentText", owner);
                    }
                }
            }
            catch (Exception e2) {
                throw new IllegalStateException("Cannot find ChatComponentText class.", e);
            }
            throw new IllegalStateException("Cannot find ChatComponentText class.");
        }
    }
    
    public static Class<?> getChatSerializerClass() {
        try {
            return getMinecraftClass("ChatSerializer", "IChatBaseComponent$ChatSerializer");
        }
        catch (RuntimeException e) {
            throw new IllegalStateException("Could not find ChatSerializer class.", e);
        }
    }
    
    public static Class<?> getServerPingClass() {
        if (!isUsingNetty()) {
            throw new IllegalStateException("ServerPing is only supported in 1.7.2.");
        }
        try {
            return getMinecraftClass("ServerPing");
        }
        catch (RuntimeException e) {
            final Class<?> statusServerInfo = PacketType.Status.Server.SERVER_INFO.getPacketClass();
            final AbstractFuzzyMatcher<Class<?>> serverPingContract = FuzzyClassContract.newBuilder().field(FuzzyFieldContract.newBuilder().typeExact(String.class).build()).field(FuzzyFieldContract.newBuilder().typeDerivedOf(getIChatBaseComponentClass()).build()).build().and(getMinecraftObjectMatcher());
            return setMinecraftClass("ServerPing", FuzzyReflection.fromClass(statusServerInfo, true).getField(FuzzyFieldContract.matchType(serverPingContract)).getType());
        }
    }
    
    public static Class<?> getServerPingServerDataClass() {
        if (!isUsingNetty()) {
            throw new IllegalStateException("ServerPingServerData is only supported in 1.7.2.");
        }
        try {
            return getMinecraftClass("ServerPingServerData", "ServerPing$ServerData");
        }
        catch (RuntimeException e) {
            final FuzzyReflection fuzzy = FuzzyReflection.fromClass(getServerPingClass(), true);
            return setMinecraftClass("ServerPingServerData", fuzzy.getFieldByType("(.*)(ServerData)(.*)").getType());
        }
    }
    
    public static Class<?> getServerPingPlayerSampleClass() {
        if (!isUsingNetty()) {
            throw new IllegalStateException("ServerPingPlayerSample is only supported in 1.7.2.");
        }
        try {
            return getMinecraftClass("ServerPingPlayerSample", "ServerPing$ServerPingPlayerSample");
        }
        catch (RuntimeException e) {
            final Class<?> serverPing = getServerPingClass();
            final AbstractFuzzyMatcher<Class<?>> serverPlayerContract = FuzzyClassContract.newBuilder().constructor(FuzzyMethodContract.newBuilder().parameterExactArray(Integer.TYPE, Integer.TYPE)).field(FuzzyFieldContract.newBuilder().typeExact(getArrayClass(getGameProfileClass()))).build().and(getMinecraftObjectMatcher());
            return setMinecraftClass("ServerPingPlayerSample", getTypeFromField(serverPing, serverPlayerContract));
        }
    }
    
    private static Class<?> getTypeFromField(final Class<?> clazz, final AbstractFuzzyMatcher<Class<?>> fieldTypeMatcher) {
        final FuzzyFieldContract fieldMatcher = FuzzyFieldContract.matchType(fieldTypeMatcher);
        return FuzzyReflection.fromClass(clazz, true).getField(fieldMatcher).getType();
    }
    
    public static boolean isUsingNetty() {
        if (MinecraftReflection.cachedNetty == null) {
            try {
                MinecraftReflection.cachedNetty = (getEnumProtocolClass() != null);
            }
            catch (RuntimeException e) {
                MinecraftReflection.cachedNetty = false;
            }
        }
        return MinecraftReflection.cachedNetty;
    }
    
    private static Class<?> getTopmostClass(Class<?> clazz) {
        while (true) {
            final Class<?> superClass = clazz.getSuperclass();
            if (superClass == Object.class || superClass == null) {
                break;
            }
            clazz = superClass;
        }
        return clazz;
    }
    
    public static Class<?> getMinecraftServerClass() {
        try {
            return getMinecraftClass("MinecraftServer");
        }
        catch (RuntimeException e) {
            useFallbackServer();
            setMinecraftClass("MinecraftServer", null);
            return getMinecraftClass("MinecraftServer");
        }
    }
    
    public static Class<?> getStatisticClass() {
        return getMinecraftClass("Statistic");
    }
    
    public static Class<?> getStatisticListClass() {
        return getMinecraftClass("StatisticList");
    }
    
    private static void useFallbackServer() {
        final Constructor<?> selected = FuzzyReflection.fromClass(getCraftBukkitClass("CraftServer")).getConstructor(FuzzyMethodContract.newBuilder().parameterMatches(getMinecraftObjectMatcher(), 0).parameterCount(2).build());
        final Class<?>[] params = selected.getParameterTypes();
        setMinecraftClass("MinecraftServer", params[0]);
        setMinecraftClass("ServerConfigurationManager", params[1]);
    }
    
    public static Class<?> getPlayerListClass() {
        try {
            return getMinecraftClass("ServerConfigurationManager", "PlayerList");
        }
        catch (RuntimeException e) {
            useFallbackServer();
            setMinecraftClass("ServerConfigurationManager", null);
            return getMinecraftClass("ServerConfigurationManager");
        }
    }
    
    public static Class<?> getNetLoginHandlerClass() {
        try {
            return getMinecraftClass("NetLoginHandler", "PendingConnection");
        }
        catch (RuntimeException e) {
            final Method selected = FuzzyReflection.fromClass(getPlayerListClass()).getMethod(FuzzyMethodContract.newBuilder().parameterMatches(FuzzyMatchers.matchExact(getEntityPlayerClass()).inverted(), 0).parameterExactType(String.class, 1).parameterExactType(String.class, 2).build());
            return setMinecraftClass("NetLoginHandler", selected.getParameterTypes()[0]);
        }
    }
    
    public static Class<?> getPlayerConnectionClass() {
        try {
            return getMinecraftClass("PlayerConnection", "NetServerHandler");
        }
        catch (RuntimeException e) {
            try {
                return setMinecraftClass("PlayerConnection", FuzzyReflection.fromClass(getEntityPlayerClass()).getFieldByType("playerConnection", getNetHandlerClass()).getType());
            }
            catch (RuntimeException e2) {
                final Class<?> playerClass = getEntityPlayerClass();
                final FuzzyClassContract playerConnection = FuzzyClassContract.newBuilder().field(FuzzyFieldContract.newBuilder().typeExact(playerClass).build()).constructor(FuzzyMethodContract.newBuilder().parameterCount(3).parameterSuperOf(getMinecraftServerClass(), 0).parameterSuperOf(getEntityPlayerClass(), 2).build()).method(FuzzyMethodContract.newBuilder().parameterCount(1).parameterExactType(String.class).build()).build();
                final Class<?> fieldType = FuzzyReflection.fromClass(getEntityPlayerClass(), true).getField(FuzzyFieldContract.newBuilder().typeMatches(playerConnection).build()).getType();
                return setMinecraftClass("PlayerConnection", fieldType);
            }
        }
    }
    
    public static Class<?> getNetworkManagerClass() {
        try {
            return getMinecraftClass("INetworkManager", "NetworkManager");
        }
        catch (RuntimeException e) {
            final Constructor<?> selected = FuzzyReflection.fromClass(getPlayerConnectionClass()).getConstructor(FuzzyMethodContract.newBuilder().parameterSuperOf(getMinecraftServerClass(), 0).parameterSuperOf(getEntityPlayerClass(), 2).build());
            return setMinecraftClass("INetworkManager", selected.getParameterTypes()[1]);
        }
    }
    
    public static Class<?> getNetHandlerClass() {
        try {
            return getMinecraftClass("NetHandler", "Connection");
        }
        catch (RuntimeException e) {
            return setMinecraftClass("NetHandler", getNetLoginHandlerClass().getSuperclass());
        }
    }
    
    public static Class<?> getItemStackClass() {
        try {
            return getMinecraftClass("ItemStack");
        }
        catch (RuntimeException e) {
            return setMinecraftClass("ItemStack", FuzzyReflection.fromClass(getCraftItemStackClass(), true).getFieldByName("handle").getType());
        }
    }
    
    public static Class<?> getBlockClass() {
        try {
            return getMinecraftClass("Block");
        }
        catch (RuntimeException e) {
            final FuzzyReflection reflect = FuzzyReflection.fromClass(getItemStackClass());
            final Set<Class<?>> candidates = new HashSet<Class<?>>();
            for (final Constructor<?> constructor : reflect.getConstructors()) {
                for (final Class<?> clazz : constructor.getParameterTypes()) {
                    if (isMinecraftClass(clazz)) {
                        candidates.add(clazz);
                    }
                }
            }
            final Method selected = reflect.getMethod(FuzzyMethodContract.newBuilder().parameterMatches(FuzzyMatchers.matchAnyOf(candidates)).returnTypeExact(Float.TYPE).build());
            return setMinecraftClass("Block", selected.getParameterTypes()[0]);
        }
    }
    
    public static Class<?> getItemClass() {
        return getNullableNMS("Item");
    }
    
    public static Class<?> getFluidTypeClass() {
        return getNullableNMS("FluidType");
    }
    
    public static Class<?> getParticleTypeClass() {
        return getNullableNMS("ParticleType");
    }
    
    public static Class<?> getWorldTypeClass() {
        try {
            return getMinecraftClass("WorldType");
        }
        catch (RuntimeException e) {
            final Method selected = FuzzyReflection.fromClass(getMinecraftServerClass(), true).getMethod(FuzzyMethodContract.newBuilder().parameterExactType(String.class, 0).parameterExactType(String.class, 1).parameterMatches(getMinecraftObjectMatcher()).parameterExactType(String.class, 4).parameterCount(5).build());
            return setMinecraftClass("WorldType", selected.getParameterTypes()[3]);
        }
    }
    
    public static Class<?> getDataWatcherClass() {
        try {
            return getMinecraftClass("DataWatcher");
        }
        catch (RuntimeException e) {
            final FuzzyClassContract dataWatcherContract = FuzzyClassContract.newBuilder().field(FuzzyFieldContract.newBuilder().requireModifier(8).typeDerivedOf(Map.class)).field(FuzzyFieldContract.newBuilder().banModifier(8).typeDerivedOf(Map.class)).method(FuzzyMethodContract.newBuilder().parameterExactType(Integer.TYPE).parameterExactType(Object.class).returnTypeVoid()).build();
            final FuzzyFieldContract fieldContract = FuzzyFieldContract.newBuilder().typeMatches(dataWatcherContract).build();
            return setMinecraftClass("DataWatcher", FuzzyReflection.fromClass(getEntityClass(), true).getField(fieldContract).getType());
        }
    }
    
    public static Class<?> getChunkPositionClass() {
        try {
            return getMinecraftClass("ChunkPosition");
        }
        catch (RuntimeException e) {
            return null;
        }
    }
    
    public static Class<?> getBlockPositionClass() {
        try {
            return getMinecraftClass("BlockPosition");
        }
        catch (RuntimeException e) {
            try {
                final Class<?> normalChunkGenerator = getCraftBukkitClass("generator.NormalChunkGenerator");
                final FuzzyMethodContract selected = FuzzyMethodContract.newBuilder().banModifier(8).parameterMatches(getMinecraftObjectMatcher(), 0).parameterExactType(String.class, 1).parameterMatches(getMinecraftObjectMatcher(), 1).build();
                return setMinecraftClass("BlockPosition", FuzzyReflection.fromClass(normalChunkGenerator).getMethod(selected).getReturnType());
            }
            catch (Throwable ex) {
                return null;
            }
        }
    }
    
    public static Class<?> getVec3DClass() {
        try {
            return getMinecraftClass("Vec3D");
        }
        catch (RuntimeException e) {
            return null;
        }
    }
    
    public static Class<?> getChunkCoordinatesClass() {
        try {
            return getMinecraftClass("ChunkCoordinates");
        }
        catch (RuntimeException e) {
            return null;
        }
    }
    
    public static Class<?> getChunkCoordIntPair() {
        if (!isUsingNetty()) {
            throw new IllegalArgumentException("Not supported on 1.6.4 and older.");
        }
        try {
            return getMinecraftClass("ChunkCoordIntPair");
        }
        catch (RuntimeException e) {
            final Class<?> packet = (Class<?>)PacketRegistry.getPacketClassFromType(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
            final AbstractFuzzyMatcher<Class<?>> chunkCoordIntContract = FuzzyClassContract.newBuilder().field(FuzzyFieldContract.newBuilder().typeDerivedOf(Integer.TYPE)).field(FuzzyFieldContract.newBuilder().typeDerivedOf(Integer.TYPE)).method(FuzzyMethodContract.newBuilder().parameterExactArray(Integer.TYPE).returnDerivedOf(getChunkPositionClass())).build().and(getMinecraftObjectMatcher());
            final Field field = FuzzyReflection.fromClass(packet, true).getField(FuzzyFieldContract.matchType(chunkCoordIntContract));
            return setMinecraftClass("ChunkCoordIntPair", field.getType());
        }
    }
    
    @Deprecated
    public static Class<?> getWatchableObjectClass() {
        return getDataWatcherItemClass();
    }
    
    public static Class<?> getDataWatcherItemClass() {
        try {
            return getMinecraftClass("DataWatcher$Item", "DataWatcher$WatchableObject", "WatchableObject");
        }
        catch (RuntimeException e) {
            final Method selected = FuzzyReflection.fromClass(getDataWatcherClass(), true).getMethod(FuzzyMethodContract.newBuilder().requireModifier(8).parameterDerivedOf(isUsingNetty() ? getPacketDataSerializerClass() : DataOutput.class, 0).parameterMatches(getMinecraftObjectMatcher(), 1).build());
            return setMinecraftClass("DataWatcher$Item", selected.getParameterTypes()[1]);
        }
    }
    
    public static Class<?> getDataWatcherObjectClass() {
        try {
            return getMinecraftClass("DataWatcherObject");
        }
        catch (RuntimeException ex) {
            return null;
        }
    }
    
    public static boolean watcherObjectExists() {
        if (MinecraftReflection.cachedWatcherObject == null) {
            try {
                return MinecraftReflection.cachedWatcherObject = (getDataWatcherObjectClass() != null);
            }
            catch (Throwable ex) {
                return MinecraftReflection.cachedWatcherObject = false;
            }
        }
        return MinecraftReflection.cachedWatcherObject;
    }
    
    public static Class<?> getDataWatcherSerializerClass() {
        return getNullableNMS("DataWatcherSerializer");
    }
    
    public static Class<?> getDataWatcherRegistryClass() {
        return getMinecraftClass("DataWatcherRegistry");
    }
    
    public static Class<?> getMinecraftKeyClass() {
        return getMinecraftClass("MinecraftKey");
    }
    
    public static Class<?> getMobEffectListClass() {
        return getMinecraftClass("MobEffectList");
    }
    
    public static Class<?> getSoundEffectClass() {
        try {
            return getMinecraftClass("SoundEffect");
        }
        catch (RuntimeException ex) {
            final FuzzyReflection fuzzy = FuzzyReflection.fromClass(PacketType.Play.Server.NAMED_SOUND_EFFECT.getPacketClass(), true);
            final Field field = fuzzy.getFieldByType("(.*)(Sound)(.*)");
            return setMinecraftClass("SoundEffect", field.getType());
        }
    }
    
    public static Class<?> getServerConnectionClass() {
        try {
            return getMinecraftClass("ServerConnection");
        }
        catch (RuntimeException e) {
            Method selected = null;
            final FuzzyClassContract.Builder serverConnectionContract = FuzzyClassContract.newBuilder().constructor(FuzzyMethodContract.newBuilder().parameterExactType(getMinecraftServerClass()).parameterCount(1));
            if (isUsingNetty()) {
                serverConnectionContract.method(FuzzyMethodContract.newBuilder().parameterDerivedOf(InetAddress.class, 0).parameterDerivedOf(Integer.TYPE, 1).parameterCount(2));
                selected = FuzzyReflection.fromClass(getMinecraftServerClass()).getMethod(FuzzyMethodContract.newBuilder().requireModifier(1).returnTypeMatches(serverConnectionContract.build()).build());
            }
            else {
                serverConnectionContract.method(FuzzyMethodContract.newBuilder().parameterExactType(getPlayerConnectionClass()));
                selected = FuzzyReflection.fromClass(getMinecraftServerClass()).getMethod(FuzzyMethodContract.newBuilder().requireModifier(1024).returnTypeMatches(serverConnectionContract.build()).build());
            }
            return setMinecraftClass("ServerConnection", selected.getReturnType());
        }
    }
    
    public static Class<?> getNBTBaseClass() {
        try {
            return getMinecraftClass("NBTBase");
        }
        catch (RuntimeException e) {
            Class<?> nbtBase = null;
            if (isUsingNetty()) {
                final FuzzyClassContract tagCompoundContract = FuzzyClassContract.newBuilder().field(FuzzyFieldContract.newBuilder().typeDerivedOf(Map.class)).method(FuzzyMethodContract.newBuilder().parameterDerivedOf(DataOutput.class).parameterCount(1)).build();
                final Method selected = FuzzyReflection.fromClass(getPacketDataSerializerClass()).getMethod(FuzzyMethodContract.newBuilder().banModifier(8).parameterCount(1).parameterMatches(tagCompoundContract).returnTypeVoid().build());
                nbtBase = selected.getParameterTypes()[0].getSuperclass();
            }
            else {
                final FuzzyClassContract tagCompoundContract = FuzzyClassContract.newBuilder().constructor(FuzzyMethodContract.newBuilder().parameterExactType(String.class).parameterCount(1)).field(FuzzyFieldContract.newBuilder().typeDerivedOf(Map.class)).build();
                final Method selected = FuzzyReflection.fromClass(getPacketClass()).getMethod(FuzzyMethodContract.newBuilder().requireModifier(8).parameterSuperOf(DataInputStream.class).parameterCount(1).returnTypeMatches(tagCompoundContract).build());
                nbtBase = selected.getReturnType().getSuperclass();
            }
            if (nbtBase == null || nbtBase.equals(Object.class)) {
                throw new IllegalStateException("Unable to find NBT base class: " + nbtBase);
            }
            return setMinecraftClass("NBTBase", nbtBase);
        }
    }
    
    public static Class<?> getNBTReadLimiterClass() {
        return getMinecraftClass("NBTReadLimiter");
    }
    
    public static Class<?> getNBTCompoundClass() {
        try {
            return getMinecraftClass("NBTTagCompound");
        }
        catch (RuntimeException e) {
            return setMinecraftClass("NBTTagCompound", NbtFactory.ofWrapper(NbtType.TAG_COMPOUND, "Test").getHandle().getClass());
        }
    }
    
    public static Class<?> getEntityTrackerClass() {
        try {
            return getMinecraftClass("EntityTracker", "PlayerChunkMap$EntityTracker");
        }
        catch (RuntimeException e) {
            final FuzzyClassContract entityTrackerContract = FuzzyClassContract.newBuilder().field(FuzzyFieldContract.newBuilder().typeDerivedOf(Set.class)).method(FuzzyMethodContract.newBuilder().parameterSuperOf(getEntityClass()).parameterCount(1).returnTypeVoid()).method(FuzzyMethodContract.newBuilder().parameterSuperOf(getEntityClass(), 0).parameterSuperOf(Integer.TYPE, 1).parameterSuperOf(Integer.TYPE, 2).parameterCount(3).returnTypeVoid()).build();
            final Field selected = FuzzyReflection.fromClass(getWorldServerClass(), true).getField(FuzzyFieldContract.newBuilder().typeMatches(entityTrackerContract).build());
            return setMinecraftClass("EntityTracker", selected.getType());
        }
    }
    
    public static Class<?> getNetworkListenThreadClass() {
        try {
            return getMinecraftClass("NetworkListenThread");
        }
        catch (RuntimeException e) {
            final FuzzyClassContract networkListenContract = FuzzyClassContract.newBuilder().field(FuzzyFieldContract.newBuilder().typeDerivedOf(ServerSocket.class)).field(FuzzyFieldContract.newBuilder().typeDerivedOf(Thread.class)).field(FuzzyFieldContract.newBuilder().typeDerivedOf(List.class)).method(FuzzyMethodContract.newBuilder().parameterExactType(getPlayerConnectionClass())).build();
            final Field selected = FuzzyReflection.fromClass(getMinecraftServerClass(), true).getField(FuzzyFieldContract.newBuilder().typeMatches(networkListenContract).build());
            return setMinecraftClass("NetworkListenThread", selected.getType());
        }
    }
    
    public static Class<?> getAttributeSnapshotClass() {
        try {
            return getMinecraftClass("AttributeSnapshot", "PacketPlayOutUpdateAttributes$AttributeSnapshot");
        }
        catch (RuntimeException ex) {
            try {
                final FuzzyReflection fuzzy = FuzzyReflection.fromClass(PacketType.Play.Server.UPDATE_ATTRIBUTES.getPacketClass(), true);
                final Field field = fuzzy.getFieldByType("attributes", Collection.class);
                final Type param = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                return setMinecraftClass("AttributeSnapshot", (Class<?>)param);
            }
            catch (Throwable ex2) {
                return getMinecraftClass("AttributeSnapshot");
            }
        }
    }
    
    public static Class<?> getIntHashMapClass() {
        try {
            return getMinecraftClass("IntHashMap");
        }
        catch (RuntimeException e) {
            final Class<?> parent = getEntityTrackerClass();
            final FuzzyClassContract intHashContract = FuzzyClassContract.newBuilder().method(FuzzyMethodContract.newBuilder().parameterCount(2).parameterExactType(Integer.TYPE, 0).parameterExactType(Object.class, 1).requirePublic()).method(FuzzyMethodContract.newBuilder().parameterCount(1).parameterExactType(Integer.TYPE).returnTypeExact(Object.class).requirePublic()).field(FuzzyFieldContract.newBuilder().typeMatches(FuzzyMatchers.matchArray(FuzzyMatchers.matchAll()))).build();
            final AbstractFuzzyMatcher<Field> intHashField = FuzzyFieldContract.newBuilder().typeMatches(getMinecraftObjectMatcher().and(intHashContract)).build();
            return setMinecraftClass("IntHashMap", FuzzyReflection.fromClass(parent).getField(intHashField).getType());
        }
    }
    
    public static Class<?> getAttributeModifierClass() {
        try {
            return getMinecraftClass("AttributeModifier");
        }
        catch (RuntimeException e) {
            getAttributeSnapshotClass();
            setMinecraftClass("AttributeModifier", null);
            return getMinecraftClass("AttributeModifier");
        }
    }
    
    public static Class<?> getMobEffectClass() {
        try {
            return getMinecraftClass("MobEffect");
        }
        catch (RuntimeException e) {
            final Class<?> packet = (Class<?>)PacketRegistry.getPacketClassFromType(PacketType.Play.Server.ENTITY_EFFECT);
            final Constructor<?> constructor = FuzzyReflection.fromClass(packet).getConstructor(FuzzyMethodContract.newBuilder().parameterCount(2).parameterExactType(Integer.TYPE, 0).parameterMatches(getMinecraftObjectMatcher(), 1).build());
            return setMinecraftClass("MobEffect", constructor.getParameterTypes()[1]);
        }
    }
    
    public static Class<?> getPacketDataSerializerClass() {
        try {
            return getMinecraftClass("PacketDataSerializer");
        }
        catch (RuntimeException e) {
            final Class<?> packet = getPacketClass();
            final Method method = FuzzyReflection.fromClass(packet).getMethod(FuzzyMethodContract.newBuilder().parameterCount(1).parameterDerivedOf(getByteBufClass()).returnTypeVoid().build());
            return setMinecraftClass("PacketDataSerializer", method.getParameterTypes()[0]);
        }
    }
    
    public static Class<?> getNbtCompressedStreamToolsClass() {
        try {
            return getMinecraftClass("NBTCompressedStreamTools");
        }
        catch (RuntimeException e2) {
            final Class<?> packetSerializer = getPacketDataSerializerClass();
            final Method writeNbt = FuzzyReflection.fromClass(packetSerializer).getMethodByParameters("writeNbt", getNBTCompoundClass());
            try {
                for (final ClassAnalyser.AsmMethod method : ClassAnalyser.getDefault().getMethodCalls(writeNbt)) {
                    final Class<?> owner = method.getOwnerClass();
                    if (!packetSerializer.equals(owner) && isMinecraftClass(owner)) {
                        return setMinecraftClass("NBTCompressedStreamTools", owner);
                    }
                }
            }
            catch (Exception e1) {
                throw new RuntimeException("Unable to analyse class.", e1);
            }
            throw new IllegalArgumentException("Unable to find NBTCompressedStreamTools.");
        }
    }
    
    public static Class<?> getTileEntityClass() {
        return getMinecraftClass("TileEntity");
    }
    
    public static Class<?> getMinecraftGsonClass() {
        try {
            return getMinecraftLibraryClass("com.google.gson.Gson");
        }
        catch (RuntimeException e) {
            final Class<?> match = FuzzyReflection.fromClass(PacketType.Status.Server.SERVER_INFO.getPacketClass(), true).getFieldByType("(.*)(google.gson.Gson)").getType();
            return setMinecraftLibraryClass("com.google.gson.Gson", match);
        }
    }
    
    public static Class<?> getItemStackArrayClass() {
        if (MinecraftReflection.itemStackArrayClass == null) {
            MinecraftReflection.itemStackArrayClass = getArrayClass(getItemStackClass());
        }
        return MinecraftReflection.itemStackArrayClass;
    }
    
    public static Class<?> getArrayClass(final Class<?> componentType) {
        return Array.newInstance(componentType, 0).getClass();
    }
    
    public static Class<?> getCraftItemStackClass() {
        return getCraftBukkitClass("inventory.CraftItemStack");
    }
    
    public static Class<?> getCraftPlayerClass() {
        return getCraftBukkitClass("entity.CraftPlayer");
    }
    
    public static Class<?> getCraftWorldClass() {
        return getCraftBukkitClass("CraftWorld");
    }
    
    public static Class<?> getCraftEntityClass() {
        return getCraftBukkitClass("entity.CraftEntity");
    }
    
    public static Class<?> getCraftMessageClass() {
        return getCraftBukkitClass("util.CraftChatMessage");
    }
    
    public static Class<?> getPlayerInfoDataClass() {
        return getMinecraftClass("PacketPlayOutPlayerInfo$PlayerInfoData", "PlayerInfoData");
    }
    
    public static boolean isPlayerInfoData(final Object obj) {
        return is(getPlayerInfoDataClass(), obj);
    }
    
    public static Class<?> getIBlockDataClass() {
        return getMinecraftClass("IBlockData");
    }
    
    public static Class<?> getMultiBlockChangeInfoClass() {
        return getMinecraftClass("MultiBlockChangeInfo", "PacketPlayOutMultiBlockChange$MultiBlockChangeInfo");
    }
    
    public static Class<?> getMultiBlockChangeInfoArrayClass() {
        return getArrayClass(getMultiBlockChangeInfoClass());
    }
    
    public static boolean signUpdateExists() {
        try {
            return getMinecraftClass("PacketPlayOutUpdateSign") != null;
        }
        catch (RuntimeException ex) {
            return false;
        }
    }
    
    public static Class<?> getNonNullListClass() {
        return getMinecraftClass("NonNullList");
    }
    
    public static Class<?> getCraftSoundClass() {
        return getCraftBukkitClass("CraftSound");
    }
    
    public static ItemStack getBukkitItemStack(final Object generic) {
        if (generic == null) {
            return new ItemStack(Material.AIR);
        }
        if (generic instanceof ItemStack) {
            final ItemStack bukkit = (ItemStack)generic;
            if (is(getCraftItemStackClass(), generic)) {
                return bukkit;
            }
            final Object nmsStack = getMinecraftItemStack((ItemStack)generic);
            return getBukkitItemStack(nmsStack);
        }
        else {
            if (!is(getItemStackClass(), generic)) {
                throw new IllegalArgumentException(generic + " is not an ItemStack!");
            }
            try {
                if (MinecraftReflection.nullEnforced == null) {
                    MinecraftReflection.isEmpty = getItemStackClass().getMethod("isEmpty", (Class<?>[])new Class[0]);
                    MinecraftReflection.nullEnforced = true;
                }
                if (MinecraftReflection.nullEnforced && (boolean)MinecraftReflection.isEmpty.invoke(generic, new Object[0])) {
                    return new ItemStack(Material.AIR);
                }
            }
            catch (ReflectiveOperationException ex) {
                MinecraftReflection.nullEnforced = false;
            }
            if (MinecraftReflection.asCraftMirror == null) {
                try {
                    MinecraftReflection.asCraftMirror = getCraftItemStackClass().getMethod("asCraftMirror", getItemStackClass());
                }
                catch (ReflectiveOperationException ex) {
                    throw new RuntimeException("Failed to obtain CraftItemStack.asCraftMirror", ex);
                }
            }
            try {
                return (ItemStack)MinecraftReflection.asCraftMirror.invoke(MinecraftReflection.nullEnforced, generic);
            }
            catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to obtain craft mirror of " + generic, ex);
            }
        }
    }
    
    public static Object getMinecraftItemStack(final ItemStack specific) {
        if (MinecraftReflection.asNMSCopy == null) {
            try {
                MinecraftReflection.asNMSCopy = getCraftItemStackClass().getMethod("asNMSCopy", ItemStack.class);
            }
            catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to obtain CraftItemStack.asNMSCopy", ex);
            }
        }
        if (is(getCraftItemStackClass(), specific)) {
            final Object unwrapped = new BukkitUnwrapper().unwrapItem(specific);
            if (unwrapped != null) {
                return unwrapped;
            }
            if (MinecraftReflection.itemStackAir == null) {
                MinecraftReflection.itemStackAir = getMinecraftItemStack(new ItemStack(Material.AIR));
            }
            return MinecraftReflection.itemStackAir;
        }
        else {
            try {
                return MinecraftReflection.asNMSCopy.invoke(null, specific);
            }
            catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to make NMS copy of " + specific, ex);
            }
        }
    }
    
    private static Class<?> getClass(final String className) {
        try {
            return MinecraftReflection.class.getClassLoader().loadClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find class " + className, e);
        }
    }
    
    public static Class<?> getCraftBukkitClass(final String className) {
        if (MinecraftReflection.craftbukkitPackage == null) {
            MinecraftReflection.craftbukkitPackage = new CachedPackage(getCraftBukkitPackage(), getClassSource());
        }
        final RuntimeException ex;
        return MinecraftReflection.craftbukkitPackage.getPackageClass(className).orElseThrow(() -> {
            new RuntimeException("Failed to find CraftBukkit class: " + className);
            return ex;
        });
    }
    
    public static Class<?> getMinecraftClass(final String className) {
        if (MinecraftReflection.minecraftPackage == null) {
            MinecraftReflection.minecraftPackage = new CachedPackage(getMinecraftPackage(), getClassSource());
        }
        final RuntimeException ex;
        return MinecraftReflection.minecraftPackage.getPackageClass(className).orElseThrow(() -> {
            new RuntimeException("Failed to find NMS class: " + className);
            return ex;
        });
    }
    
    public static Class<?> getNullableNMS(final String className) {
        if (MinecraftReflection.minecraftPackage == null) {
            MinecraftReflection.minecraftPackage = new CachedPackage(getMinecraftPackage(), getClassSource());
        }
        return MinecraftReflection.minecraftPackage.getPackageClass(className).orElse(null);
    }
    
    private static Class<?> setMinecraftClass(final String className, final Class<?> clazz) {
        if (MinecraftReflection.minecraftPackage == null) {
            MinecraftReflection.minecraftPackage = new CachedPackage(getMinecraftPackage(), getClassSource());
        }
        MinecraftReflection.minecraftPackage.setPackageClass(className, clazz);
        return clazz;
    }
    
    private static ClassSource getClassSource() {
        final ErrorReporter reporter = ProtocolLibrary.getErrorReporter();
        if (MinecraftReflection.classSource == null) {
            try {
                return MinecraftReflection.classSource = new RemappedClassSource().initialize();
            }
            catch (RemappedClassSource.RemapperUnavailableException e) {
                if (e.getReason() != RemappedClassSource.RemapperUnavailableException.Reason.MCPC_NOT_PRESENT) {
                    reporter.reportWarning(MinecraftReflection.class, Report.newBuilder(MinecraftReflection.REPORT_CANNOT_FIND_MCPC_REMAPPER));
                }
            }
            catch (Exception e2) {
                reporter.reportWarning(MinecraftReflection.class, Report.newBuilder(MinecraftReflection.REPORT_CANNOT_LOAD_CPC_REMAPPER));
            }
            MinecraftReflection.classSource = ClassSource.fromClassLoader();
        }
        return MinecraftReflection.classSource;
    }
    
    public static Class<?> getMinecraftClass(final String className, final String... aliases) {
        try {
            return getMinecraftClass(className);
        }
        catch (RuntimeException e) {
            Class<?> success = null;
            final int length = aliases.length;
            int i = 0;
            while (i < length) {
                final String alias = aliases[i];
                try {
                    success = getMinecraftClass(alias);
                }
                catch (RuntimeException ex) {
                    ++i;
                    continue;
                }
                break;
            }
            if (success != null) {
                MinecraftReflection.minecraftPackage.setPackageClass(className, success);
                return success;
            }
            throw new RuntimeException(String.format("Unable to find %s (%s)", className, Joiner.on(", ").join((Object[])aliases)));
        }
    }
    
    public static Class<?> getMinecraftLibraryClass(final String className) {
        if (MinecraftReflection.libraryPackage == null) {
            MinecraftReflection.libraryPackage = new CachedPackage("", getClassSource());
        }
        final RuntimeException ex;
        return MinecraftReflection.libraryPackage.getPackageClass(className).orElseThrow(() -> {
            new RuntimeException("Failed to find class: " + className);
            return ex;
        });
    }
    
    private static Class<?> setMinecraftLibraryClass(final String className, final Class<?> clazz) {
        if (MinecraftReflection.libraryPackage == null) {
            MinecraftReflection.libraryPackage = new CachedPackage("", getClassSource());
        }
        MinecraftReflection.libraryPackage.setPackageClass(className, clazz);
        return clazz;
    }
    
    public static String getNetworkManagerName() {
        return getNetworkManagerClass().getSimpleName();
    }
    
    public static String getNetLoginHandlerName() {
        return getNetLoginHandlerClass().getSimpleName();
    }
    
    public static Object getPacketDataSerializer(final Object buffer) {
        final Class<?> packetSerializer = getPacketDataSerializerClass();
        try {
            return packetSerializer.getConstructor(getByteBufClass()).newInstance(buffer);
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot construct packet serializer.", e);
        }
    }
    
    static {
        REPORT_CANNOT_FIND_MCPC_REMAPPER = new ReportType("Cannot find MCPC/Cauldron remapper.");
        REPORT_CANNOT_LOAD_CPC_REMAPPER = new ReportType("Unable to load MCPC/Cauldron remapper.");
        REPORT_NON_CRAFTBUKKIT_LIBRARY_PACKAGE = new ReportType("Cannot find standard Minecraft library location. Assuming MCPC/Cauldron.");
        MinecraftReflection.DYNAMIC_PACKAGE_MATCHER = null;
        MinecraftReflection.MINECRAFT_PREFIX_PACKAGE = "net.minecraft.server";
        PACKAGE_VERSION_MATCHER = Pattern.compile(".*\\.(v\\d+_\\d+_\\w*\\d+)");
        MinecraftReflection.MINECRAFT_FULL_PACKAGE = null;
        MinecraftReflection.CRAFTBUKKIT_PACKAGE = null;
        MinecraftReflection.getBukkitEntityCache = (ConcurrentMap<Class<?>, MethodAccessor>)Maps.newConcurrentMap();
        MinecraftReflection.itemStackAir = null;
        MinecraftReflection.asNMSCopy = null;
        MinecraftReflection.asCraftMirror = null;
        MinecraftReflection.nullEnforced = null;
        MinecraftReflection.isEmpty = null;
    }
}
