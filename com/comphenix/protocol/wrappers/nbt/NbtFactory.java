package com.comphenix.protocol.wrappers.nbt;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.Collection;
import javax.annotation.Nonnull;
import com.comphenix.protocol.wrappers.BukkitConverters;
import org.bukkit.Material;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.block.BlockState;
import org.bukkit.block.Block;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Closeable;
import com.google.common.io.Closeables;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import java.io.FileInputStream;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Method;

public class NbtFactory
{
    private static Method methodCreateTag;
    private static boolean methodCreateWithName;
    private static StructureModifier<Object> itemStackModifier;
    
    public static NbtCompound asCompound(final NbtBase<?> tag) {
        if (tag instanceof NbtCompound) {
            return (NbtCompound)tag;
        }
        if (tag != null) {
            throw new UnsupportedOperationException("Cannot cast a " + tag.getClass() + "( " + tag.getType() + ") to TAG_COMPUND.");
        }
        throw new IllegalArgumentException("Tag cannot be NULL.");
    }
    
    public static NbtList<?> asList(final NbtBase<?> tag) {
        if (tag instanceof NbtList) {
            return (NbtList<?>)(NbtList)tag;
        }
        if (tag != null) {
            throw new UnsupportedOperationException("Cannot cast a " + tag.getClass() + "( " + tag.getType() + ") to TAG_LIST.");
        }
        throw new IllegalArgumentException("Tag cannot be NULL.");
    }
    
    public static <T> NbtWrapper<T> fromBase(final NbtBase<T> base) {
        if (base instanceof NbtWrapper) {
            return (NbtWrapper<T>)(NbtWrapper)base;
        }
        if (base.getType() == NbtType.TAG_COMPOUND) {
            final WrappedCompound copy = WrappedCompound.fromName(base.getName());
            final T value = base.getValue();
            copy.setValue((Map<String, NbtBase<?>>)value);
            return (NbtWrapper<T>)copy;
        }
        if (base.getType() == NbtType.TAG_LIST) {
            final NbtList<T> copy2 = WrappedList.fromName(base.getName());
            copy2.setValue((List<NbtBase<T>>)base.getValue());
            return (NbtWrapper<T>)(NbtWrapper)copy2;
        }
        final NbtWrapper<T> copy3 = ofWrapper(base.getType(), base.getName());
        copy3.setValue(base.getValue());
        return copy3;
    }
    
    public static void setItemTag(final ItemStack stack, final NbtCompound compound) {
        checkItemStack(stack);
        final StructureModifier<NbtBase<?>> modifier = getStackModifier(stack);
        modifier.write(0, compound);
    }
    
    public static NbtWrapper<?> fromItemTag(final ItemStack stack) {
        checkItemStack(stack);
        final StructureModifier<NbtBase<?>> modifier = getStackModifier(stack);
        NbtBase<?> result = modifier.read(0);
        if (result == null) {
            result = ofCompound("tag");
            modifier.write(0, result);
        }
        return fromBase(result);
    }
    
    public static Optional<NbtWrapper<?>> fromItemOptional(final ItemStack stack) {
        checkItemStack(stack);
        final StructureModifier<NbtBase<?>> modifier = getStackModifier(stack);
        final NbtBase<?> result = modifier.read(0);
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(fromBase(result));
    }
    
    public static NbtCompound fromFile(final String file) throws IOException {
        Preconditions.checkNotNull((Object)file, (Object)"file cannot be NULL");
        FileInputStream stream = null;
        DataInputStream input = null;
        boolean swallow = true;
        try {
            stream = new FileInputStream(file);
            final NbtCompound result = NbtBinarySerializer.DEFAULT.deserializeCompound(input = new DataInputStream(new GZIPInputStream(stream)));
            swallow = false;
            return result;
        }
        finally {
            if (input != null) {
                Closeables.close((Closeable)input, swallow);
            }
            else if (stream != null) {
                Closeables.close((Closeable)stream, swallow);
            }
        }
    }
    
    public static void toFile(final NbtCompound compound, final String file) throws IOException {
        Preconditions.checkNotNull((Object)compound, (Object)"compound cannot be NULL");
        Preconditions.checkNotNull((Object)file, (Object)"file cannot be NULL");
        FileOutputStream stream = null;
        DataOutputStream output = null;
        boolean swallow = true;
        try {
            stream = new FileOutputStream(file);
            NbtBinarySerializer.DEFAULT.serialize((NbtBase<Object>)compound, output = new DataOutputStream(new GZIPOutputStream(stream)));
            swallow = false;
        }
        finally {
            if (output != null) {
                Closeables.close((Closeable)output, swallow);
            }
            else if (stream != null) {
                Closeables.close((Closeable)stream, swallow);
            }
        }
    }
    
    public static NbtCompound readBlockState(final Block block) {
        final BlockState state = block.getState();
        final TileEntityAccessor<BlockState> accessor = TileEntityAccessor.getAccessor(state);
        return (accessor != null) ? accessor.readBlockState(state) : null;
    }
    
    public static void writeBlockState(final Block target, final NbtCompound blockState) {
        final BlockState state = target.getState();
        final TileEntityAccessor<BlockState> accessor = TileEntityAccessor.getAccessor(state);
        if (accessor != null) {
            accessor.writeBlockState(state, blockState);
            return;
        }
        throw new IllegalArgumentException("Unable to find tile entity in " + target);
    }
    
    private static void checkItemStack(final ItemStack stack) {
        if (stack == null) {
            throw new IllegalArgumentException("Stack cannot be NULL.");
        }
        if (!MinecraftReflection.isCraftItemStack(stack)) {
            throw new IllegalArgumentException("Stack must be a CraftItemStack.");
        }
        if (stack.getType() == Material.AIR) {
            throw new IllegalArgumentException("ItemStacks representing air cannot store NMS information.");
        }
    }
    
    private static StructureModifier<NbtBase<?>> getStackModifier(final ItemStack stack) {
        final Object nmsStack = MinecraftReflection.getMinecraftItemStack(stack);
        if (NbtFactory.itemStackModifier == null) {
            NbtFactory.itemStackModifier = new StructureModifier<Object>(nmsStack.getClass(), Object.class, false);
        }
        return NbtFactory.itemStackModifier.withTarget(nmsStack).withType(MinecraftReflection.getNBTBaseClass(), BukkitConverters.getNbtConverter());
    }
    
    @Deprecated
    public static <T> NbtWrapper<T> fromNMS(final Object handle) {
        final WrappedElement<T> partial = new WrappedElement<T>(handle);
        if (partial.getType() == NbtType.TAG_COMPOUND) {
            return (NbtWrapper<T>)new WrappedCompound(handle);
        }
        if (partial.getType() == NbtType.TAG_LIST) {
            return (NbtWrapper<T>)new WrappedList(handle);
        }
        return partial;
    }
    
    public static <T> NbtWrapper<T> fromNMS(final Object handle, final String name) {
        final WrappedElement<T> partial = new WrappedElement<T>(handle, name);
        if (partial.getType() == NbtType.TAG_COMPOUND) {
            return (NbtWrapper<T>)new WrappedCompound(handle, name);
        }
        if (partial.getType() == NbtType.TAG_LIST) {
            return (NbtWrapper<T>)new WrappedList(handle, name);
        }
        return partial;
    }
    
    public static NbtCompound fromNMSCompound(@Nonnull final Object handle) {
        if (handle == null) {
            throw new IllegalArgumentException("handle cannot be NULL.");
        }
        return (NbtCompound)fromNMS(handle);
    }
    
    public static NbtBase<String> of(final String name, final String value) {
        return ofWrapper(NbtType.TAG_STRING, name, value);
    }
    
    public static NbtBase<Byte> of(final String name, final byte value) {
        return ofWrapper(NbtType.TAG_BYTE, name, value);
    }
    
    public static NbtBase<Short> of(final String name, final short value) {
        return ofWrapper(NbtType.TAG_SHORT, name, value);
    }
    
    public static NbtBase<Integer> of(final String name, final int value) {
        return ofWrapper(NbtType.TAG_INT, name, value);
    }
    
    public static NbtBase<Long> of(final String name, final long value) {
        return ofWrapper(NbtType.TAG_LONG, name, value);
    }
    
    public static NbtBase<Float> of(final String name, final float value) {
        return ofWrapper(NbtType.TAG_FLOAT, name, value);
    }
    
    public static NbtBase<Double> of(final String name, final double value) {
        return ofWrapper(NbtType.TAG_DOUBLE, name, value);
    }
    
    public static NbtBase<byte[]> of(final String name, final byte[] value) {
        return ofWrapper(NbtType.TAG_BYTE_ARRAY, name, value);
    }
    
    public static NbtBase<int[]> of(final String name, final int[] value) {
        return ofWrapper(NbtType.TAG_INT_ARRAY, name, value);
    }
    
    public static NbtCompound ofCompound(final String name, final Collection<? extends NbtBase<?>> list) {
        return WrappedCompound.fromList(name, list);
    }
    
    public static NbtCompound ofCompound(final String name) {
        return WrappedCompound.fromName(name);
    }
    
    @SafeVarargs
    public static <T> NbtList<T> ofList(final String name, final T... elements) {
        return WrappedList.fromArray(name, elements);
    }
    
    public static <T> NbtList<T> ofList(final String name, final Collection<? extends T> elements) {
        return WrappedList.fromList(name, elements);
    }
    
    public static <T> NbtWrapper<T> ofWrapper(final NbtType type, final String name) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be NULL.");
        }
        if (type == NbtType.TAG_END) {
            throw new IllegalArgumentException("Cannot create a TAG_END.");
        }
        if (NbtFactory.methodCreateTag == null) {
            final Class<?> base = MinecraftReflection.getNBTBaseClass();
            try {
                NbtFactory.methodCreateTag = findCreateMethod(base, Byte.TYPE, String.class);
                NbtFactory.methodCreateWithName = true;
            }
            catch (Exception e2) {
                NbtFactory.methodCreateTag = findCreateMethod(base, Byte.TYPE);
                NbtFactory.methodCreateWithName = false;
            }
        }
        try {
            if (NbtFactory.methodCreateWithName) {
                return (NbtWrapper<T>)createTagWithName(type, name);
            }
            return (NbtWrapper<T>)createTagSetName(type, name);
        }
        catch (Exception e) {
            throw new FieldAccessException(String.format("Cannot create NBT element %s (type: %s)", name, type), e);
        }
    }
    
    private static Method findCreateMethod(final Class<?> base, final Class<?>... params) {
        final Method method = FuzzyReflection.fromClass(base, true).getMethodByParameters("createTag", base, params);
        method.setAccessible(true);
        return method;
    }
    
    private static <T> NbtWrapper<T> createTagWithName(final NbtType type, final String name) throws Exception {
        final Object handle = NbtFactory.methodCreateTag.invoke(null, (byte)type.getRawID(), name);
        if (type == NbtType.TAG_COMPOUND) {
            return (NbtWrapper<T>)new WrappedCompound(handle);
        }
        if (type == NbtType.TAG_LIST) {
            return (NbtWrapper<T>)new WrappedList(handle);
        }
        return new WrappedElement<T>(handle);
    }
    
    private static <T> NbtWrapper<T> createTagSetName(final NbtType type, final String name) throws Exception {
        final Object handle = NbtFactory.methodCreateTag.invoke(null, (byte)type.getRawID());
        if (type == NbtType.TAG_COMPOUND) {
            return (NbtWrapper<T>)new WrappedCompound(handle, name);
        }
        if (type == NbtType.TAG_LIST) {
            return (NbtWrapper<T>)new WrappedList(handle, name);
        }
        return new WrappedElement<T>(handle, name);
    }
    
    public static <T> NbtWrapper<T> ofWrapper(final NbtType type, final String name, final T value) {
        final NbtWrapper<T> created = ofWrapper(type, name);
        created.setValue(value);
        return created;
    }
    
    public static <T> NbtWrapper<T> ofWrapper(final Class<?> type, final String name, final T value) {
        return ofWrapper(NbtType.getTypeFromClass(type), name, value);
    }
}
