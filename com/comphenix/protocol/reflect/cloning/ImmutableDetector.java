package com.comphenix.protocol.reflect.cloning;

import com.comphenix.protocol.utility.MinecraftVersion;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.net.Inet6Address;
import java.net.Inet4Address;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.Locale;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.Iterator;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.primitives.Primitives;
import java.util.function.Supplier;
import java.util.Set;

public class ImmutableDetector implements Cloner
{
    private static final Set<Class<?>> immutableClasses;
    private static final Set<Class<?>> immutableNMS;
    
    private static void add(final Supplier<Class<?>> getClass) {
        try {
            final Class<?> clazz = getClass.get();
            if (clazz != null) {
                ImmutableDetector.immutableNMS.add(clazz);
            }
        }
        catch (RuntimeException ex) {}
    }
    
    @Override
    public boolean canClone(final Object source) {
        return source != null && isImmutable(source.getClass());
    }
    
    public static boolean isImmutable(final Class<?> type) {
        if (type.isArray()) {
            return false;
        }
        if (Primitives.isWrapperType((Class)type) || String.class.equals(type)) {
            return true;
        }
        if (isEnumWorkaround(type)) {
            return true;
        }
        if (type.getName().contains("$$Lambda$")) {
            return true;
        }
        if (ImmutableDetector.immutableClasses.contains(type)) {
            return true;
        }
        for (final Class<?> clazz : ImmutableDetector.immutableNMS) {
            if (MinecraftReflection.is(clazz, type)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isEnumWorkaround(Class<?> enumClass) {
        while (enumClass != null) {
            if (enumClass.isEnum()) {
                return true;
            }
            enumClass = enumClass.getSuperclass();
        }
        return false;
    }
    
    @Override
    public Object clone(final Object source) {
        return source;
    }
    
    static {
        immutableClasses = (Set)ImmutableSet.of((Object)StackTraceElement.class, (Object)BigDecimal.class, (Object)BigInteger.class, (Object)Locale.class, (Object)UUID.class, (Object)URL.class, (Object[])new Class[] { URI.class, Inet4Address.class, Inet6Address.class, InetSocketAddress.class, SecretKey.class, PublicKey.class });
        immutableNMS = Sets.newConcurrentHashSet();
        add(MinecraftReflection::getGameProfileClass);
        add(MinecraftReflection::getDataWatcherSerializerClass);
        add(() -> MinecraftReflection.getMinecraftClass("SoundEffect"));
        add(MinecraftReflection::getBlockClass);
        add(MinecraftReflection::getItemClass);
        if (MinecraftVersion.atOrAbove(MinecraftVersion.AQUATIC_UPDATE)) {
            add(() -> MinecraftReflection.getMinecraftClass("Particle"));
            add(MinecraftReflection::getFluidTypeClass);
            add(MinecraftReflection::getParticleTypeClass);
        }
    }
}
