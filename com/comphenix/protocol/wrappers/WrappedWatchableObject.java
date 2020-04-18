package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import org.bukkit.inventory.ItemStack;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.google.common.base.Optional;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;

public class WrappedWatchableObject extends AbstractWrapper
{
    private static final Class<?> HANDLE_TYPE;
    private static Integer VALUE_INDEX;
    private static ConstructorAccessor constructor;
    private final StructureModifier<Object> modifier;
    
    public WrappedWatchableObject(final Object handle) {
        super(WrappedWatchableObject.HANDLE_TYPE);
        this.setHandle(handle);
        this.modifier = new StructureModifier<Object>(this.handleType).withTarget(handle);
    }
    
    public WrappedWatchableObject(final int index, final Object value) {
        this(newHandle(WrappedDataWatcher.WrappedDataWatcherObject.fromIndex(index), value));
    }
    
    public WrappedWatchableObject(final WrappedDataWatcher.WrappedDataWatcherObject watcherObject, final Object value) {
        this(newHandle(watcherObject, value));
    }
    
    private static Object newHandle(final WrappedDataWatcher.WrappedDataWatcherObject watcherObject, final Object value) {
        if (WrappedWatchableObject.constructor == null) {
            WrappedWatchableObject.constructor = Accessors.getConstructorAccessor(WrappedWatchableObject.HANDLE_TYPE.getConstructors()[0]);
        }
        if (MinecraftReflection.watcherObjectExists()) {
            return WrappedWatchableObject.constructor.invoke(watcherObject.getHandle(), value);
        }
        return WrappedWatchableObject.constructor.invoke(WrappedDataWatcher.getTypeID(value.getClass()), watcherObject.getIndex(), value);
    }
    
    public WrappedDataWatcher.WrappedDataWatcherObject getWatcherObject() {
        return new WrappedDataWatcher.WrappedDataWatcherObject(this.modifier.read(0));
    }
    
    public int getIndex() {
        if (MinecraftReflection.watcherObjectExists()) {
            return this.getWatcherObject().getIndex();
        }
        return this.modifier.withType(Integer.TYPE).read(1);
    }
    
    public Object getValue() {
        return getWrapped(this.getRawValue());
    }
    
    public Object getRawValue() {
        if (WrappedWatchableObject.VALUE_INDEX == null) {
            WrappedWatchableObject.VALUE_INDEX = (MinecraftReflection.watcherObjectExists() ? 1 : 2);
        }
        return this.modifier.readSafely(WrappedWatchableObject.VALUE_INDEX);
    }
    
    public void setValue(final Object value, final boolean updateClient) {
        if (WrappedWatchableObject.VALUE_INDEX == null) {
            WrappedWatchableObject.VALUE_INDEX = (MinecraftReflection.watcherObjectExists() ? 1 : 2);
        }
        this.modifier.write(WrappedWatchableObject.VALUE_INDEX, getUnwrapped(value));
        if (updateClient) {
            this.setDirtyState(true);
        }
    }
    
    public void setValue(final Object value) {
        this.setValue(value, false);
    }
    
    public boolean getDirtyState() {
        return this.modifier.withType(Boolean.TYPE).read(0);
    }
    
    public void setDirtyState(final boolean dirty) {
        this.modifier.withType(Boolean.TYPE).write(0, dirty);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof WrappedWatchableObject) {
            final WrappedWatchableObject that = (WrappedWatchableObject)obj;
            return this.getIndex() == that.getIndex() && this.getRawValue().equals(that.getRawValue()) && this.getDirtyState() == that.getDirtyState();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = 31 * result + this.getIndex();
        result = 31 * result + this.getRawValue().hashCode();
        result = 31 * result + (this.getDirtyState() ? 1231 : 1237);
        return result;
    }
    
    @Override
    public String toString() {
        return "DataWatcherItem[index=" + this.getIndex() + ", value=" + this.getValue() + ", dirty=" + this.getDirtyState() + "]";
    }
    
    static Object getWrapped(final Object value) {
        if (MinecraftReflection.is(MinecraftReflection.getDataWatcherItemClass(), value)) {
            return getWrapped(new WrappedWatchableObject(value).getRawValue());
        }
        if (value instanceof Optional) {
            final Optional<?> optional = (Optional<?>)value;
            if (optional.isPresent()) {
                return Optional.of(getWrapped(optional.get()));
            }
            return Optional.absent();
        }
        else {
            if (MinecraftReflection.is(MinecraftReflection.getIChatBaseComponentClass(), value)) {
                return WrappedChatComponent.fromHandle(value);
            }
            if (MinecraftReflection.is(MinecraftReflection.getItemStackClass(), value)) {
                return BukkitConverters.getItemStackConverter().getSpecific(value);
            }
            if (MinecraftReflection.is(MinecraftReflection.getIBlockDataClass(), value)) {
                return BukkitConverters.getWrappedBlockDataConverter().getSpecific(value);
            }
            if (MinecraftReflection.is(Vector3F.getMinecraftClass(), value)) {
                return Vector3F.getConverter().getSpecific(value);
            }
            if (MinecraftReflection.is(MinecraftReflection.getBlockPositionClass(), value)) {
                return BlockPosition.getConverter().getSpecific(value);
            }
            if (MinecraftReflection.is(EnumWrappers.getDirectionClass(), value)) {
                return EnumWrappers.getDirectionConverter().getSpecific(value);
            }
            if (MinecraftReflection.is(MinecraftReflection.getNBTCompoundClass(), value)) {
                return NbtFactory.fromNMSCompound(value);
            }
            if (MinecraftReflection.is(MinecraftReflection.getChunkCoordinatesClass(), value)) {
                return new WrappedChunkCoordinate((Comparable)value);
            }
            if (MinecraftReflection.is(MinecraftReflection.getChunkPositionClass(), value)) {
                return ChunkPosition.getConverter().getSpecific(value);
            }
            return value;
        }
    }
    
    static Object getUnwrapped(final Object wrapped) {
        if (wrapped instanceof Optional) {
            final Optional<?> optional = (Optional<?>)wrapped;
            if (optional.isPresent()) {
                return Optional.of(getUnwrapped(optional.get()));
            }
            return Optional.absent();
        }
        else {
            if (wrapped instanceof WrappedChatComponent) {
                return ((WrappedChatComponent)wrapped).getHandle();
            }
            if (wrapped instanceof ItemStack) {
                return BukkitConverters.getItemStackConverter().getGeneric((ItemStack)wrapped);
            }
            if (wrapped instanceof WrappedBlockData) {
                return BukkitConverters.getWrappedBlockDataConverter().getGeneric((WrappedBlockData)wrapped);
            }
            if (wrapped instanceof Vector3F) {
                return Vector3F.getConverter().getGeneric((Vector3F)wrapped);
            }
            if (wrapped instanceof BlockPosition) {
                return BlockPosition.getConverter().getGeneric((BlockPosition)wrapped);
            }
            if (wrapped instanceof EnumWrappers.Direction) {
                return EnumWrappers.getDirectionConverter().getGeneric((EnumWrappers.Direction)wrapped);
            }
            if (wrapped instanceof NbtCompound) {
                return NbtFactory.fromBase((NbtBase<Object>)wrapped).getHandle();
            }
            if (wrapped instanceof ChunkPosition) {
                return ChunkPosition.getConverter().getGeneric((ChunkPosition)wrapped);
            }
            if (wrapped instanceof WrappedChunkCoordinate) {
                return ((WrappedChunkCoordinate)wrapped).getHandle();
            }
            return wrapped;
        }
    }
    
    static {
        HANDLE_TYPE = MinecraftReflection.getDataWatcherItemClass();
        WrappedWatchableObject.VALUE_INDEX = null;
    }
}
