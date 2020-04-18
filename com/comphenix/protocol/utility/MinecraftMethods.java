package com.comphenix.protocol.utility;

import java.util.List;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.PacketType;
import io.netty.buffer.Unpooled;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Iterator;
import java.util.Map;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.lang.reflect.Method;

public class MinecraftMethods
{
    private static volatile Method sendPacketMethod;
    private static volatile Method networkManagerHandle;
    private static volatile Method networkManagerPacketRead;
    private static volatile Method packetReadByteBuf;
    private static volatile Method packetWriteByteBuf;
    
    public static Method getSendPacketMethod() {
        if (MinecraftMethods.sendPacketMethod == null) {
            final Class<?> serverHandlerClass = MinecraftReflection.getPlayerConnectionClass();
            try {
                MinecraftMethods.sendPacketMethod = FuzzyReflection.fromClass(serverHandlerClass).getMethod(FuzzyMethodContract.newBuilder().nameRegex("sendPacket.*").returnTypeVoid().parameterCount(1).build());
            }
            catch (IllegalArgumentException e) {
                if (MinecraftReflection.isUsingNetty()) {
                    return MinecraftMethods.sendPacketMethod = FuzzyReflection.fromClass(serverHandlerClass).getMethodByParameters("sendPacket", MinecraftReflection.getPacketClass());
                }
                final Map<String, Method> netServer = getMethodList(serverHandlerClass, MinecraftReflection.getPacketClass());
                final Map<String, Method> netHandler = getMethodList(MinecraftReflection.getNetHandlerClass(), MinecraftReflection.getPacketClass());
                for (final String methodName : netHandler.keySet()) {
                    netServer.remove(methodName);
                }
                if (netServer.size() != 1) {
                    throw new IllegalArgumentException("Unable to find the sendPacket method in NetServerHandler/PlayerConnection.");
                }
                final Method[] methods = netServer.values().toArray(new Method[0]);
                MinecraftMethods.sendPacketMethod = methods[0];
            }
        }
        return MinecraftMethods.sendPacketMethod;
    }
    
    public static Method getDisconnectMethod(final Class<?> playerConnection) {
        try {
            return FuzzyReflection.fromClass(playerConnection).getMethodByName("disconnect.*");
        }
        catch (IllegalArgumentException e) {
            return FuzzyReflection.fromObject(playerConnection).getMethodByParameters("disconnect", String.class);
        }
    }
    
    public static Method getNetworkManagerHandleMethod() {
        if (MinecraftMethods.networkManagerHandle == null) {
            (MinecraftMethods.networkManagerHandle = FuzzyReflection.fromClass(MinecraftReflection.getNetworkManagerClass(), true).getMethodByParameters("handle", MinecraftReflection.getPacketClass(), GenericFutureListener[].class)).setAccessible(true);
        }
        return MinecraftMethods.networkManagerHandle;
    }
    
    public static Method getNetworkManagerReadPacketMethod() {
        if (MinecraftMethods.networkManagerPacketRead == null) {
            (MinecraftMethods.networkManagerPacketRead = FuzzyReflection.fromClass(MinecraftReflection.getNetworkManagerClass(), true).getMethodByParameters("packetRead", ChannelHandlerContext.class, MinecraftReflection.getPacketClass())).setAccessible(true);
        }
        return MinecraftMethods.networkManagerPacketRead;
    }
    
    private static Map<String, Method> getMethodList(final Class<?> source, final Class<?>... params) {
        final FuzzyReflection reflect = FuzzyReflection.fromClass(source, true);
        return reflect.getMappedMethods(reflect.getMethodListByParameters(Void.TYPE, params));
    }
    
    public static Method getPacketReadByteBufMethod() {
        initializePacket();
        return MinecraftMethods.packetReadByteBuf;
    }
    
    public static Method getPacketWriteByteBufMethod() {
        initializePacket();
        return MinecraftMethods.packetWriteByteBuf;
    }
    
    private static void initializePacket() {
        if (MinecraftMethods.packetReadByteBuf == null || MinecraftMethods.packetWriteByteBuf == null) {
            final Enhancer enhancer = EnhancerFactory.getInstance().createEnhancer();
            enhancer.setSuperclass(MinecraftReflection.getPacketDataSerializerClass());
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
                    if (method.getName().contains("read")) {
                        throw new ReadMethodException();
                    }
                    if (method.getName().contains("write")) {
                        throw new WriteMethodException();
                    }
                    return proxy.invokeSuper(obj, args);
                }
            });
            final Object javaProxy = enhancer.create(new Class[] { MinecraftReflection.getByteBufClass() }, new Object[] { Unpooled.buffer() });
            final Object lookPacket = new PacketContainer(PacketType.Play.Client.CLOSE_WINDOW).getHandle();
            final List<Method> candidates = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethodListByParameters(Void.TYPE, new Class[] { MinecraftReflection.getPacketDataSerializerClass() });
            for (final Method method : candidates) {
                try {
                    method.invoke(lookPacket, javaProxy);
                }
                catch (InvocationTargetException e) {
                    if (e.getCause() instanceof ReadMethodException) {
                        MinecraftMethods.packetReadByteBuf = method;
                    }
                    else {
                        if (!(e.getCause() instanceof WriteMethodException)) {
                            continue;
                        }
                        MinecraftMethods.packetWriteByteBuf = method;
                    }
                }
                catch (Exception e2) {
                    throw new RuntimeException("Generic reflection error.", e2);
                }
            }
            if (MinecraftMethods.packetReadByteBuf == null) {
                throw new IllegalStateException("Unable to find Packet.read(PacketDataSerializer)");
            }
            if (MinecraftMethods.packetWriteByteBuf == null) {
                throw new IllegalStateException("Unable to find Packet.write(PacketDataSerializer)");
            }
        }
    }
    
    private static class ReadMethodException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        
        public ReadMethodException() {
            super("A read method was executed.");
        }
    }
    
    private static class WriteMethodException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        
        public WriteMethodException() {
            super("A write method was executed.");
        }
    }
}
