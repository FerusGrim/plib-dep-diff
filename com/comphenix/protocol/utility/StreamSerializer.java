package com.comphenix.protocol.utility;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import org.apache.commons.lang.Validate;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import io.netty.buffer.Unpooled;
import org.bukkit.inventory.ItemStack;
import java.io.DataInput;
import io.netty.buffer.ByteBuf;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import java.io.DataOutput;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.wrappers.nbt.NbtType;
import com.comphenix.protocol.injector.netty.NettyByteBufAdapter;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import java.io.DataInputStream;
import java.io.IOException;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import java.io.DataOutputStream;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

public class StreamSerializer
{
    private static final StreamSerializer DEFAULT;
    private static MethodAccessor READ_ITEM_METHOD;
    private static MethodAccessor WRITE_ITEM_METHOD;
    private static MethodAccessor READ_NBT_METHOD;
    private static MethodAccessor WRITE_NBT_METHOD;
    private static MethodAccessor READ_STRING_METHOD;
    private static MethodAccessor WRITE_STRING_METHOD;
    
    public static StreamSerializer getDefault() {
        return StreamSerializer.DEFAULT;
    }
    
    public void serializeVarInt(@Nonnull final DataOutputStream destination, int value) throws IOException {
        Preconditions.checkNotNull((Object)destination, (Object)"source cannot be NULL");
        while ((value & 0xFFFFFF80) != 0x0) {
            destination.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        destination.writeByte(value);
    }
    
    public int deserializeVarInt(@Nonnull final DataInputStream source) throws IOException {
        Preconditions.checkNotNull((Object)source, (Object)"source cannot be NULL");
        int result = 0;
        int length = 0;
        byte currentByte;
        do {
            currentByte = source.readByte();
            result |= (currentByte & 0x7F) << length++ * 7;
            if (length > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((currentByte & 0x80) == 0x80);
        return result;
    }
    
    public void serializeCompound(@Nonnull final DataOutputStream output, final NbtCompound compound) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Output stream cannot be NULL.");
        }
        final Object handle = (compound != null) ? NbtFactory.fromBase((NbtBase<Object>)compound).getHandle() : null;
        if (MinecraftReflection.isUsingNetty()) {
            if (StreamSerializer.WRITE_NBT_METHOD == null) {
                StreamSerializer.WRITE_NBT_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketDataSerializerClass(), true).getMethodByParameters("writeNbtCompound", MinecraftReflection.getNBTCompoundClass()));
            }
            final ByteBuf buf = NettyByteBufAdapter.packetWriter(output);
            buf.writeByte(NbtType.TAG_COMPOUND.getRawID());
            StreamSerializer.WRITE_NBT_METHOD.invoke(buf, handle);
        }
        else {
            if (StreamSerializer.WRITE_NBT_METHOD == null) {
                StreamSerializer.WRITE_NBT_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketClass(), true).getMethod(FuzzyMethodContract.newBuilder().parameterCount(2).parameterDerivedOf(MinecraftReflection.getNBTBaseClass(), 0).parameterDerivedOf(DataOutput.class, 1).returnTypeVoid().build()));
            }
            StreamSerializer.WRITE_NBT_METHOD.invoke(null, handle, output);
        }
    }
    
    public NbtCompound deserializeCompound(@Nonnull final DataInputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Input stream cannot be NULL.");
        }
        Object nmsCompound = null;
        if (MinecraftReflection.isUsingNetty()) {
            if (StreamSerializer.READ_NBT_METHOD == null) {
                StreamSerializer.READ_NBT_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketDataSerializerClass(), true).getMethodByParameters("readNbtCompound", MinecraftReflection.getNBTCompoundClass(), new Class[0]));
            }
            final ByteBuf buf = NettyByteBufAdapter.packetReader(input);
            nmsCompound = StreamSerializer.READ_NBT_METHOD.invoke(buf, new Object[0]);
        }
        else {
            if (StreamSerializer.READ_NBT_METHOD == null) {
                StreamSerializer.READ_NBT_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethod(FuzzyMethodContract.newBuilder().parameterCount(1).parameterDerivedOf(DataInput.class).returnDerivedOf(MinecraftReflection.getNBTBaseClass()).build()));
            }
            try {
                nmsCompound = StreamSerializer.READ_NBT_METHOD.invoke(null, input);
            }
            catch (Exception e) {
                throw new IOException("Cannot read item stack.", e);
            }
        }
        if (nmsCompound != null) {
            return NbtFactory.fromNMSCompound(nmsCompound);
        }
        return null;
    }
    
    public void serializeString(@Nonnull final DataOutputStream output, final String text) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output stream cannot be NULL.");
        }
        if (text == null) {
            throw new IllegalArgumentException("text cannot be NULL.");
        }
        if (MinecraftReflection.isUsingNetty()) {
            if (StreamSerializer.WRITE_STRING_METHOD == null) {
                StreamSerializer.WRITE_STRING_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketDataSerializerClass(), true).getMethodByParameters("writeString", String.class));
            }
            final ByteBuf buf = NettyByteBufAdapter.packetWriter(output);
            StreamSerializer.WRITE_STRING_METHOD.invoke(buf, text);
        }
        else {
            if (StreamSerializer.WRITE_STRING_METHOD == null) {
                StreamSerializer.WRITE_STRING_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethod(FuzzyMethodContract.newBuilder().parameterCount(2).parameterExactType(String.class, 0).parameterDerivedOf(DataOutput.class, 1).returnTypeVoid().build()));
            }
            StreamSerializer.WRITE_STRING_METHOD.invoke(null, text, output);
        }
    }
    
    public String deserializeString(@Nonnull final DataInputStream input, final int maximumLength) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Input stream cannot be NULL.");
        }
        if (maximumLength > 32767) {
            throw new IllegalArgumentException("Maximum length cannot exceed 32767 characters.");
        }
        if (maximumLength < 0) {
            throw new IllegalArgumentException("Maximum length cannot be negative.");
        }
        if (MinecraftReflection.isUsingNetty()) {
            if (StreamSerializer.READ_STRING_METHOD == null) {
                StreamSerializer.READ_STRING_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketDataSerializerClass(), true).getMethodByParameters("readString", String.class, new Class[] { Integer.TYPE }));
            }
            final ByteBuf buf = NettyByteBufAdapter.packetReader(input);
            return (String)StreamSerializer.READ_STRING_METHOD.invoke(buf, maximumLength);
        }
        if (StreamSerializer.READ_STRING_METHOD == null) {
            StreamSerializer.READ_STRING_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethod(FuzzyMethodContract.newBuilder().parameterCount(2).parameterDerivedOf(DataInput.class, 0).parameterExactType(Integer.TYPE, 1).returnTypeExact(String.class).build()));
        }
        return (String)StreamSerializer.READ_STRING_METHOD.invoke(null, input, maximumLength);
    }
    
    public String serializeItemStack(final ItemStack stack) throws IOException {
        final Object nmsItem = MinecraftReflection.getMinecraftItemStack(stack);
        byte[] bytes = null;
        if (MinecraftReflection.isUsingNetty()) {
            final ByteBuf buf = Unpooled.buffer();
            final Object serializer = MinecraftReflection.getPacketDataSerializer(buf);
            if (StreamSerializer.WRITE_ITEM_METHOD == null) {
                StreamSerializer.WRITE_ITEM_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketDataSerializerClass(), true).getMethodByParameters("writeStack", MinecraftReflection.getItemStackClass()));
            }
            StreamSerializer.WRITE_ITEM_METHOD.invoke(serializer, nmsItem);
            bytes = buf.array();
        }
        else {
            if (StreamSerializer.WRITE_ITEM_METHOD == null) {
                StreamSerializer.WRITE_ITEM_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethod(FuzzyMethodContract.newBuilder().parameterCount(2).parameterDerivedOf(MinecraftReflection.getItemStackClass(), 0).parameterDerivedOf(DataOutput.class, 1).build()));
            }
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final DataOutputStream dataOutput = new DataOutputStream(outputStream);
            StreamSerializer.WRITE_ITEM_METHOD.invoke(null, nmsItem, dataOutput);
            bytes = outputStream.toByteArray();
        }
        return Base64Coder.encodeLines(bytes);
    }
    
    public ItemStack deserializeItemStack(final String input) throws IOException {
        Validate.notNull((Object)input, "input cannot be null!");
        Object nmsItem = null;
        final byte[] bytes = Base64Coder.decodeLines(input);
        if (MinecraftReflection.isUsingNetty()) {
            final ByteBuf buf = Unpooled.copiedBuffer(bytes);
            final Object serializer = MinecraftReflection.getPacketDataSerializer(buf);
            if (StreamSerializer.READ_ITEM_METHOD == null) {
                StreamSerializer.READ_ITEM_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketDataSerializerClass(), true).getMethodByParameters("readItemStack", MinecraftReflection.getItemStackClass(), new Class[0]));
            }
            nmsItem = StreamSerializer.READ_ITEM_METHOD.invoke(serializer, new Object[0]);
        }
        else {
            if (StreamSerializer.READ_ITEM_METHOD == null) {
                StreamSerializer.READ_ITEM_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethod(FuzzyMethodContract.newBuilder().parameterCount(1).parameterDerivedOf(DataInput.class).returnDerivedOf(MinecraftReflection.getItemStackClass()).build()));
            }
            final ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
            final DataInputStream inputStream = new DataInputStream(byteStream);
            nmsItem = StreamSerializer.READ_ITEM_METHOD.invoke(null, inputStream);
        }
        return (nmsItem != null) ? MinecraftReflection.getBukkitItemStack(nmsItem) : null;
    }
    
    public void serializeItemStack(final DataOutputStream output, final ItemStack stack) throws IOException {
        Validate.notNull((Object)"output cannot be null!");
        final Object nmsItem = MinecraftReflection.getMinecraftItemStack(stack);
        if (MinecraftReflection.isUsingNetty()) {
            if (StreamSerializer.WRITE_ITEM_METHOD == null) {
                StreamSerializer.WRITE_ITEM_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketDataSerializerClass(), true).getMethodByParameters("writeStack", MinecraftReflection.getItemStackClass()));
            }
            final ByteBuf buf = Unpooled.buffer();
            final Object serializer = MinecraftReflection.getPacketDataSerializer(buf);
            StreamSerializer.WRITE_ITEM_METHOD.invoke(serializer, nmsItem);
            output.write(buf.array());
        }
        else {
            if (StreamSerializer.WRITE_ITEM_METHOD == null) {
                StreamSerializer.WRITE_ITEM_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethod(FuzzyMethodContract.newBuilder().parameterCount(2).parameterDerivedOf(MinecraftReflection.getItemStackClass(), 0).parameterDerivedOf(DataOutput.class, 1).build()));
            }
            StreamSerializer.WRITE_ITEM_METHOD.invoke(null, nmsItem, output);
        }
    }
    
    @Deprecated
    public ItemStack deserializeItemStack(final DataInputStream input) throws IOException {
        Validate.notNull((Object)input, "input cannot be null!");
        Object nmsItem = null;
        if (MinecraftReflection.isUsingNetty()) {
            if (StreamSerializer.READ_ITEM_METHOD == null) {
                StreamSerializer.READ_ITEM_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketDataSerializerClass(), true).getMethodByParameters("readItemStack", MinecraftReflection.getItemStackClass(), new Class[0]));
            }
            final byte[] bytes = new byte[8192];
            input.read(bytes);
            final ByteBuf buf = Unpooled.copiedBuffer(bytes);
            final Object serializer = MinecraftReflection.getPacketDataSerializer(buf);
            nmsItem = StreamSerializer.READ_ITEM_METHOD.invoke(serializer, new Object[0]);
        }
        else {
            if (StreamSerializer.READ_ITEM_METHOD == null) {
                StreamSerializer.READ_ITEM_METHOD = Accessors.getMethodAccessor(FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).getMethod(FuzzyMethodContract.newBuilder().parameterCount(1).parameterDerivedOf(DataInput.class).returnDerivedOf(MinecraftReflection.getItemStackClass()).build()));
            }
            nmsItem = StreamSerializer.READ_ITEM_METHOD.invoke(null, input);
        }
        if (nmsItem != null) {
            return MinecraftReflection.getBukkitItemStack(nmsItem);
        }
        return null;
    }
    
    static {
        DEFAULT = new StreamSerializer();
    }
}
