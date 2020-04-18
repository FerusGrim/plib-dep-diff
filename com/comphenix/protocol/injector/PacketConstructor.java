package com.comphenix.protocol.injector;

import com.google.common.primitives.Primitives;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.events.PacketContainer;
import java.util.Iterator;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.google.common.collect.Lists;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.error.RethrowErrorReporter;
import java.util.List;
import com.comphenix.protocol.PacketType;
import java.lang.reflect.Constructor;

public class PacketConstructor
{
    public static PacketConstructor DEFAULT;
    private Constructor<?> constructorMethod;
    private PacketType type;
    private List<Unwrapper> unwrappers;
    private Unwrapper[] paramUnwrapper;
    
    private PacketConstructor(final Constructor<?> constructorMethod) {
        this.constructorMethod = constructorMethod;
        (this.unwrappers = (List<Unwrapper>)Lists.newArrayList((Object[])new Unwrapper[] { new BukkitUnwrapper(new RethrowErrorReporter()) })).addAll(BukkitConverters.getUnwrappers());
    }
    
    private PacketConstructor(final PacketType type, final Constructor<?> constructorMethod, final List<Unwrapper> unwrappers, final Unwrapper[] paramUnwrapper) {
        this.type = type;
        this.constructorMethod = constructorMethod;
        this.unwrappers = unwrappers;
        this.paramUnwrapper = paramUnwrapper;
    }
    
    public ImmutableList<Unwrapper> getUnwrappers() {
        return (ImmutableList<Unwrapper>)ImmutableList.copyOf((Collection)this.unwrappers);
    }
    
    @Deprecated
    public int getPacketID() {
        return this.type.getLegacyId();
    }
    
    public PacketType getType() {
        return this.type;
    }
    
    public PacketConstructor withUnwrappers(final List<Unwrapper> unwrappers) {
        return new PacketConstructor(this.type, this.constructorMethod, unwrappers, this.paramUnwrapper);
    }
    
    @Deprecated
    public PacketConstructor withPacket(final int id, final Object[] values) {
        return this.withPacket(PacketType.findLegacy(id), values);
    }
    
    public PacketConstructor withPacket(final PacketType type, final Object[] values) {
        final Class<?>[] types = (Class<?>[])new Class[values.length];
        Throwable lastException = null;
        final Unwrapper[] paramUnwrapper = new Unwrapper[values.length];
        for (int i = 0; i < types.length; ++i) {
            if (values[i] != null) {
                types[i] = getClass(values[i]);
                for (final Unwrapper unwrapper : this.unwrappers) {
                    Object result = null;
                    try {
                        result = unwrapper.unwrapItem(values[i]);
                    }
                    catch (OutOfMemoryError e) {
                        throw e;
                    }
                    catch (ThreadDeath e2) {
                        throw e2;
                    }
                    catch (Throwable e3) {
                        lastException = e3;
                    }
                    if (result != null) {
                        types[i] = getClass(result);
                        paramUnwrapper[i] = unwrapper;
                        break;
                    }
                }
            }
            else {
                types[i] = Object.class;
            }
        }
        final Class<?> packetType = (Class<?>)PacketRegistry.getPacketClassFromType(type, true);
        if (packetType == null) {
            throw new IllegalArgumentException("Could not find a packet by the type " + type);
        }
        for (final Constructor<?> constructor : packetType.getConstructors()) {
            final Class<?>[] params = constructor.getParameterTypes();
            if (isCompatible(types, params)) {
                return new PacketConstructor(type, constructor, this.unwrappers, paramUnwrapper);
            }
        }
        throw new IllegalArgumentException("No suitable constructor could be found.", lastException);
    }
    
    public PacketContainer createPacket(final Object... values) throws FieldAccessException {
        try {
            for (int i = 0; i < values.length; ++i) {
                if (this.paramUnwrapper[i] != null) {
                    values[i] = this.paramUnwrapper[i].unwrapItem(values[i]);
                }
            }
            final Object nmsPacket = this.constructorMethod.newInstance(values);
            return new PacketContainer(this.type, nmsPacket);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (InstantiationException e2) {
            throw new FieldAccessException("Cannot construct an abstract packet.", e2);
        }
        catch (IllegalAccessException e3) {
            throw new FieldAccessException("Cannot construct packet due to a security limitation.", e3);
        }
        catch (InvocationTargetException e4) {
            throw new RuntimeException("Minecraft error.", e4);
        }
    }
    
    private static boolean isCompatible(final Class<?>[] types, final Class<?>[] params) {
        if (params.length == types.length) {
            for (int i = 0; i < params.length; ++i) {
                final Class<?> inputType = types[i];
                Class<?> paramType = params[i];
                if (!inputType.isPrimitive() && paramType.isPrimitive()) {
                    paramType = (Class<?>)Primitives.wrap((Class)paramType);
                }
                if (!paramType.isAssignableFrom(inputType)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    public static Class<?> getClass(final Object obj) {
        if (obj instanceof Class) {
            return (Class<?>)obj;
        }
        return obj.getClass();
    }
    
    static {
        PacketConstructor.DEFAULT = new PacketConstructor(null);
    }
    
    public interface Unwrapper
    {
        Object unwrapItem(final Object p0);
    }
}
