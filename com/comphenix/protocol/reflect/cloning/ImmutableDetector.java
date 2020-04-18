package com.comphenix.protocol.reflect.cloning;

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
import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.primitives.Primitives;

public class ImmutableDetector implements Cloner
{
    private static final Class<?>[] immutableClasses;
    
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
        for (final Class<?> clazz : ImmutableDetector.immutableClasses) {
            if (clazz.equals(type)) {
                return true;
            }
        }
        return (MinecraftReflection.isUsingNetty() && type.equals(MinecraftReflection.getGameProfileClass())) || (MinecraftReflection.watcherObjectExists() && (type.equals(MinecraftReflection.getDataWatcherSerializerClass()) || type.equals(MinecraftReflection.getMinecraftClass("SoundEffect"))));
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
        immutableClasses = new Class[] { StackTraceElement.class, BigDecimal.class, BigInteger.class, Locale.class, UUID.class, URL.class, URI.class, Inet4Address.class, Inet6Address.class, InetSocketAddress.class, SecretKey.class, PublicKey.class };
    }
}
