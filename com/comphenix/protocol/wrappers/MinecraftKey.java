package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.EquivalentConverter;
import java.util.Locale;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Constructor;

public class MinecraftKey
{
    private final String prefix;
    private final String key;
    private static Constructor<?> constructor;
    
    public MinecraftKey(final String prefix, final String key) {
        this.prefix = prefix;
        this.key = key;
    }
    
    public MinecraftKey(final String key) {
        this("minecraft", key);
    }
    
    public static MinecraftKey fromHandle(final Object handle) {
        final StructureModifier<String> modifier = (StructureModifier<String>)new StructureModifier(handle.getClass()).withTarget(handle).withType(String.class);
        return new MinecraftKey(modifier.read(0), modifier.read(1));
    }
    
    @Deprecated
    public static MinecraftKey fromEnum(final Enum<?> value) {
        return new MinecraftKey(value.name().toLowerCase(Locale.ENGLISH).replace("_", "."));
    }
    
    public String getPrefix() {
        return this.prefix;
    }
    
    public String getKey() {
        return this.key;
    }
    
    public String getFullKey() {
        return this.prefix + ":" + this.key;
    }
    
    @Deprecated
    public String getEnumFormat() {
        return this.key.toUpperCase(Locale.ENGLISH).replace(".", "_");
    }
    
    public static EquivalentConverter<MinecraftKey> getConverter() {
        return new EquivalentConverter<MinecraftKey>() {
            @Override
            public MinecraftKey getSpecific(final Object generic) {
                return MinecraftKey.fromHandle(generic);
            }
            
            @Override
            public Object getGeneric(final MinecraftKey specific) {
                if (MinecraftKey.constructor == null) {
                    try {
                        MinecraftKey.constructor = MinecraftReflection.getMinecraftKeyClass().getConstructor(String.class, String.class);
                    }
                    catch (ReflectiveOperationException e) {
                        throw new RuntimeException("Failed to obtain MinecraftKey constructor", e);
                    }
                }
                try {
                    return MinecraftKey.constructor.newInstance(specific.getPrefix(), specific.getKey());
                }
                catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to create new MinecraftKey", e);
                }
            }
            
            @Override
            public Class<MinecraftKey> getSpecificType() {
                return MinecraftKey.class;
            }
        };
    }
    
    static {
        MinecraftKey.constructor = null;
    }
}
