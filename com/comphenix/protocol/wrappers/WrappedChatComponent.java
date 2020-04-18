package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.io.StringReader;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

public class WrappedChatComponent extends AbstractWrapper implements ClonableWrapper
{
    private static final Class<?> SERIALIZER;
    private static final Class<?> COMPONENT;
    private static final Class<?> GSON_CLASS;
    private static Object GSON;
    private static MethodAccessor DESERIALIZE;
    private static MethodAccessor SERIALIZE_COMPONENT;
    private static MethodAccessor CONSTRUCT_COMPONENT;
    private static ConstructorAccessor CONSTRUCT_TEXT_COMPONENT;
    private transient String cache;
    
    private static Object deserialize(final String json) {
        if (WrappedChatComponent.DESERIALIZE != null) {
            return WrappedChatComponent.DESERIALIZE.invoke(null, WrappedChatComponent.GSON, json, WrappedChatComponent.COMPONENT, true);
        }
        final StringReader str = new StringReader(json);
        return ComponentParser.deserialize(WrappedChatComponent.GSON, WrappedChatComponent.COMPONENT, str);
    }
    
    private WrappedChatComponent(final Object handle, final String cache) {
        super(MinecraftReflection.getIChatBaseComponentClass());
        this.setHandle(handle);
        this.cache = cache;
    }
    
    public static WrappedChatComponent fromHandle(final Object handle) {
        return new WrappedChatComponent(handle, null);
    }
    
    public static WrappedChatComponent fromJson(final String json) {
        return new WrappedChatComponent(deserialize(json), json);
    }
    
    public static WrappedChatComponent fromText(final String text) {
        Preconditions.checkNotNull((Object)text, (Object)"text cannot be NULL.");
        return fromHandle(WrappedChatComponent.CONSTRUCT_TEXT_COMPONENT.invoke(text));
    }
    
    public static WrappedChatComponent[] fromChatMessage(final String message) {
        final Object[] components = (Object[])WrappedChatComponent.CONSTRUCT_COMPONENT.invoke(null, message);
        final WrappedChatComponent[] result = new WrappedChatComponent[components.length];
        for (int i = 0; i < components.length; ++i) {
            result[i] = fromHandle(components[i]);
        }
        return result;
    }
    
    public String getJson() {
        if (this.cache == null) {
            this.cache = (String)WrappedChatComponent.SERIALIZE_COMPONENT.invoke(null, this.handle);
        }
        return this.cache;
    }
    
    public void setJson(final String obj) {
        this.handle = deserialize(obj);
        this.cache = obj;
    }
    
    @Override
    public WrappedChatComponent deepClone() {
        return fromJson(this.getJson());
    }
    
    @Override
    public String toString() {
        return "WrappedChatComponent[json=" + this.getJson() + "]";
    }
    
    static {
        SERIALIZER = MinecraftReflection.getChatSerializerClass();
        COMPONENT = MinecraftReflection.getIChatBaseComponentClass();
        GSON_CLASS = MinecraftReflection.getMinecraftGsonClass();
        WrappedChatComponent.GSON = null;
        WrappedChatComponent.DESERIALIZE = null;
        WrappedChatComponent.SERIALIZE_COMPONENT = null;
        WrappedChatComponent.CONSTRUCT_COMPONENT = null;
        WrappedChatComponent.CONSTRUCT_TEXT_COMPONENT = null;
        final FuzzyReflection fuzzy = FuzzyReflection.fromClass(WrappedChatComponent.SERIALIZER, true);
        WrappedChatComponent.SERIALIZE_COMPONENT = Accessors.getMethodAccessor(fuzzy.getMethodByParameters("serialize", String.class, new Class[] { WrappedChatComponent.COMPONENT }));
        try {
            WrappedChatComponent.GSON = FieldUtils.readStaticField(fuzzy.getFieldByType("gson", WrappedChatComponent.GSON_CLASS), true);
        }
        catch (IllegalAccessException ex) {
            throw new RuntimeException("Failed to obtain GSON field", ex);
        }
        try {
            WrappedChatComponent.DESERIALIZE = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getMinecraftClass("ChatDeserializer"), true).getMethodByParameters("deserialize", Object.class, new Class[] { WrappedChatComponent.GSON_CLASS, String.class, Class.class, Boolean.TYPE }));
        }
        catch (IllegalArgumentException ex2) {
            WrappedChatComponent.DESERIALIZE = null;
        }
        WrappedChatComponent.CONSTRUCT_COMPONENT = Accessors.getMethodAccessor(MinecraftReflection.getCraftChatMessage(), "fromString", String.class);
        WrappedChatComponent.CONSTRUCT_TEXT_COMPONENT = Accessors.getConstructorAccessor(MinecraftReflection.getChatComponentTextClass(), String.class);
    }
}
