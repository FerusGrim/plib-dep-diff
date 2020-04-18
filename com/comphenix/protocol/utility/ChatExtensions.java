package com.comphenix.protocol.utility;

import java.util.List;
import com.google.common.collect.Iterables;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.google.common.base.Strings;
import java.util.Iterator;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMatchers;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import java.lang.reflect.Constructor;
import com.comphenix.protocol.injector.PacketConstructor;
import com.comphenix.protocol.ProtocolManager;

public class ChatExtensions
{
    private ProtocolManager manager;
    private static volatile PacketConstructor chatConstructor;
    private static volatile Constructor<?> jsonConstructor;
    private static volatile MethodAccessor messageFactory;
    
    public ChatExtensions(final ProtocolManager manager) {
        this.manager = manager;
    }
    
    public void sendMessageSilently(final CommandSender receiver, final String message) throws InvocationTargetException {
        if (receiver == null) {
            throw new IllegalArgumentException("receiver cannot be NULL.");
        }
        if (message == null) {
            throw new IllegalArgumentException("message cannot be NULL.");
        }
        if (receiver instanceof Player) {
            this.sendMessageSilently((Player)receiver, message);
        }
        else {
            receiver.sendMessage(message);
        }
    }
    
    private void sendMessageSilently(final Player player, final String message) throws InvocationTargetException {
        try {
            for (final PacketContainer packet : createChatPackets(message)) {
                this.manager.sendServerPacket(player, packet, false);
            }
        }
        catch (FieldAccessException e) {
            throw new InvocationTargetException(e);
        }
    }
    
    public static PacketContainer[] createChatPackets(final String message) {
        if (ChatExtensions.jsonConstructor == null) {
            if (ChatExtensions.chatConstructor == null) {
                ChatExtensions.chatConstructor = PacketConstructor.DEFAULT.withPacket(PacketType.Play.Server.CHAT, new Object[] { message });
            }
            return new PacketContainer[] { ChatExtensions.chatConstructor.createPacket(message) };
        }
        if (ChatExtensions.chatConstructor == null) {
            final Class<?> messageClass = ChatExtensions.jsonConstructor.getParameterTypes()[0];
            ChatExtensions.chatConstructor = PacketConstructor.DEFAULT.withPacket(PacketType.Play.Server.CHAT, new Object[] { messageClass });
            if (MinecraftReflection.isUsingNetty()) {
                ChatExtensions.messageFactory = Accessors.getMethodAccessor(MinecraftReflection.getCraftMessageClass(), "fromString", String.class);
            }
            else {
                ChatExtensions.messageFactory = Accessors.getMethodAccessor(FuzzyReflection.fromClass(messageClass).getMethod(FuzzyMethodContract.newBuilder().requireModifier(8).parameterCount(1).parameterExactType(String.class).returnTypeMatches(FuzzyMatchers.matchParent()).build()));
            }
        }
        if (MinecraftReflection.isUsingNetty()) {
            final Object[] components = (Object[])ChatExtensions.messageFactory.invoke(null, message);
            final PacketContainer[] packets = new PacketContainer[components.length];
            for (int i = 0; i < components.length; ++i) {
                packets[i] = ChatExtensions.chatConstructor.createPacket(components[i]);
            }
            return packets;
        }
        return new PacketContainer[] { ChatExtensions.chatConstructor.createPacket(ChatExtensions.messageFactory.invoke(null, message)) };
    }
    
    public void broadcastMessageSilently(final String message, final String permission) throws InvocationTargetException {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be NULL.");
        }
        for (final Player player : Util.getOnlinePlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                this.sendMessageSilently(player, message);
            }
        }
    }
    
    public static String[] toFlowerBox(final String[] message, final String marginChar, final int marginWidth, final int marginHeight) {
        final String[] output = new String[message.length + marginHeight * 2];
        final int width = getMaximumLength(message);
        final String topButtomMargin = Strings.repeat(marginChar, width + marginWidth * 2);
        final String leftRightMargin = Strings.repeat(marginChar, marginWidth);
        for (int i = 0; i < message.length; ++i) {
            output[i + marginHeight] = leftRightMargin + Strings.padEnd(message[i], width, ' ') + leftRightMargin;
        }
        for (int i = 0; i < marginHeight; ++i) {
            output[i] = topButtomMargin;
            output[output.length - i - 1] = topButtomMargin;
        }
        return output;
    }
    
    private static int getMaximumLength(final String[] lines) {
        int current = 0;
        for (int i = 0; i < lines.length; ++i) {
            if (current < lines[i].length()) {
                current = lines[i].length();
            }
        }
        return current;
    }
    
    static Constructor<?> getJsonFormatConstructor() {
        final Class<?> chatPacket = (Class<?>)PacketRegistry.getPacketClassFromType(PacketType.Play.Server.CHAT, true);
        final List<Constructor<?>> list = FuzzyReflection.fromClass(chatPacket).getConstructorList(FuzzyMethodContract.newBuilder().parameterCount(1).parameterMatches(MinecraftReflection.getMinecraftObjectMatcher()).build());
        return (Constructor<?>)Iterables.getFirst((Iterable)list, (Object)null);
    }
    
    static {
        ChatExtensions.jsonConstructor = getJsonFormatConstructor();
    }
}
