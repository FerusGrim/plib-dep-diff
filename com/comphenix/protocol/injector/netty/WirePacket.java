package com.comphenix.protocol.injector.netty;

import com.comphenix.protocol.utility.MinecraftReflection;
import java.lang.reflect.Method;
import com.comphenix.protocol.utility.MinecraftMethods;
import com.comphenix.protocol.events.PacketContainer;
import java.util.Arrays;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.PacketType;

public class WirePacket
{
    private final int id;
    private final byte[] bytes;
    
    public WirePacket(final PacketType type, final byte[] bytes) {
        this.id = ((PacketType)Preconditions.checkNotNull((Object)type, (Object)"type cannot be null")).getCurrentId();
        this.bytes = bytes;
    }
    
    public WirePacket(final int id, final byte[] bytes) {
        this.id = id;
        this.bytes = bytes;
    }
    
    public int getId() {
        return this.id;
    }
    
    public byte[] getBytes() {
        return this.bytes;
    }
    
    public void writeId(final ByteBuf output) {
        writeVarInt(output, this.id);
    }
    
    public void writeBytes(final ByteBuf output) {
        Preconditions.checkNotNull((Object)output, (Object)"output cannot be null!");
        output.writeBytes(this.bytes);
    }
    
    public void writeFully(final ByteBuf output) {
        this.writeId(output);
        this.writeBytes(output);
    }
    
    public ByteBuf serialize() {
        final ByteBuf buffer = Unpooled.buffer();
        this.writeFully(buffer);
        return buffer;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof WirePacket) {
            final WirePacket that = (WirePacket)obj;
            return this.id == that.id && Arrays.equals(this.bytes, that.bytes);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = 31 * result + Arrays.hashCode(this.bytes);
        result = 31 * result + this.id;
        return result;
    }
    
    @Override
    public String toString() {
        return "WirePacket[id=" + this.id + ", bytes=" + Arrays.toString(this.bytes) + "]";
    }
    
    private static byte[] getBytes(final ByteBuf buffer) {
        final byte[] array = new byte[buffer.readableBytes()];
        buffer.readBytes(array);
        return array;
    }
    
    public static WirePacket fromPacket(final PacketContainer packet) {
        final int id = packet.getType().getCurrentId();
        return new WirePacket(id, bytesFromPacket(packet));
    }
    
    public static byte[] bytesFromPacket(final PacketContainer packet) {
        Preconditions.checkNotNull((Object)packet, (Object)"packet cannot be null!");
        final ByteBuf buffer = PacketContainer.createPacketBuffer();
        final ByteBuf store = PacketContainer.createPacketBuffer();
        final Method write = MinecraftMethods.getPacketWriteByteBufMethod();
        try {
            write.invoke(packet.getHandle(), buffer);
        }
        catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to read packet contents.", ex);
        }
        final byte[] bytes = getBytes(buffer);
        buffer.release();
        if (packet.getType() == PacketType.Play.Server.CUSTOM_PAYLOAD || packet.getType() == PacketType.Play.Client.CUSTOM_PAYLOAD) {
            final byte[] ret = Arrays.copyOf(bytes, bytes.length);
            store.writeBytes(bytes);
            final Method read = MinecraftMethods.getPacketReadByteBufMethod();
            try {
                read.invoke(packet.getHandle(), store);
            }
            catch (ReflectiveOperationException ex2) {
                throw new RuntimeException("Failed to rewrite packet contents.", ex2);
            }
            return ret;
        }
        store.release();
        return bytes;
    }
    
    public static WirePacket fromPacket(final Object packet) {
        Preconditions.checkNotNull(packet, (Object)"packet cannot be null!");
        Preconditions.checkArgument(MinecraftReflection.isPacketClass(packet), (Object)"packet must be a Minecraft packet");
        final PacketType type = PacketType.fromClass(packet.getClass());
        final int id = type.getCurrentId();
        final ByteBuf buffer = PacketContainer.createPacketBuffer();
        final Method write = MinecraftMethods.getPacketWriteByteBufMethod();
        try {
            write.invoke(packet, buffer);
        }
        catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to serialize packet contents.", ex);
        }
        final byte[] bytes = getBytes(buffer);
        buffer.release();
        return new WirePacket(id, bytes);
    }
    
    public static void writeVarInt(final ByteBuf output, int i) {
        Preconditions.checkNotNull((Object)output, (Object)"output cannot be null!");
        while ((i & 0xFFFFFF80) != 0x0) {
            output.writeByte((i & 0x7F) | 0x80);
            i >>>= 7;
        }
        output.writeByte(i);
    }
    
    public static int readVarInt(final ByteBuf input) {
        Preconditions.checkNotNull((Object)input, (Object)"input cannot be null!");
        int i = 0;
        int j = 0;
        byte b0;
        do {
            b0 = input.readByte();
            i |= (b0 & 0x7F) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b0 & 0x80) == 0x80);
        return i;
    }
}
