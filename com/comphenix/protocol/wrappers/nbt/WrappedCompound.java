package com.comphenix.protocol.wrappers.nbt;

import com.comphenix.protocol.wrappers.ClonableWrapper;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import java.io.DataOutput;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import com.comphenix.protocol.wrappers.collection.ConvertedMap;
import java.util.Map;

class WrappedCompound implements NbtWrapper<Map<String, NbtBase<?>>>, NbtCompound
{
    private WrappedElement<Map<String, Object>> container;
    private ConvertedMap<String, Object, NbtBase<?>> savedMap;
    
    public static WrappedCompound fromName(final String name) {
        return (WrappedCompound)NbtFactory.ofWrapper(NbtType.TAG_COMPOUND, name);
    }
    
    public static NbtCompound fromList(final String name, final Collection<? extends NbtBase<?>> list) {
        final WrappedCompound copy = fromName(name);
        for (final NbtBase<?> base : list) {
            copy.getValue().put(base.getName(), base);
        }
        return copy;
    }
    
    public WrappedCompound(final Object handle) {
        this.container = new WrappedElement<Map<String, Object>>(handle);
    }
    
    public WrappedCompound(final Object handle, final String name) {
        this.container = new WrappedElement<Map<String, Object>>(handle, name);
    }
    
    @Override
    public boolean accept(final NbtVisitor visitor) {
        if (visitor.visitEnter(this)) {
            for (final NbtBase<?> node : this) {
                if (!node.accept(visitor)) {
                    break;
                }
            }
        }
        return visitor.visitLeave(this);
    }
    
    @Override
    public Object getHandle() {
        return this.container.getHandle();
    }
    
    @Override
    public NbtType getType() {
        return NbtType.TAG_COMPOUND;
    }
    
    @Override
    public String getName() {
        return this.container.getName();
    }
    
    @Override
    public void setName(final String name) {
        this.container.setName(name);
    }
    
    @Override
    public boolean containsKey(final String key) {
        return this.getValue().containsKey(key);
    }
    
    @Override
    public Set<String> getKeys() {
        return this.getValue().keySet();
    }
    
    @Override
    public Map<String, NbtBase<?>> getValue() {
        if (this.savedMap == null) {
            this.savedMap = new ConvertedMap<String, Object, NbtBase<?>>((Map<String, Object>)this.container.getValue()) {
                protected Object toInner(final NbtBase<?> outer) {
                    if (outer == null) {
                        return null;
                    }
                    return NbtFactory.fromBase(outer).getHandle();
                }
                
                @Override
                protected NbtBase<?> toOuter(final Object inner) {
                    if (inner == null) {
                        return null;
                    }
                    return NbtFactory.fromNMS(inner);
                }
                
                @Override
                protected NbtBase<?> toOuter(final String key, final Object inner) {
                    if (inner == null) {
                        return null;
                    }
                    return NbtFactory.fromNMS(inner, key);
                }
                
                @Override
                public String toString() {
                    return WrappedCompound.this.toString();
                }
            };
        }
        return this.savedMap;
    }
    
    @Override
    public void setValue(final Map<String, NbtBase<?>> newValue) {
        for (final Map.Entry<String, NbtBase<?>> entry : newValue.entrySet()) {
            final Object value = entry.getValue();
            if (value instanceof NbtBase) {
                this.put(entry.getValue());
            }
            else {
                this.putObject(entry.getKey(), entry.getValue());
            }
        }
    }
    
    @Override
    public <T> NbtBase<T> getValue(final String key) {
        return (NbtBase<T>)this.getValue().get(key);
    }
    
    @Override
    public NbtBase<?> getValueOrDefault(final String key, final NbtType type) {
        NbtBase<?> nbt = this.getValue(key);
        if (nbt == null) {
            nbt = NbtFactory.ofWrapper(type, key);
            this.put(nbt);
        }
        else if (nbt.getType() != type) {
            throw new IllegalArgumentException("Cannot get tag " + nbt + ": Not a " + type);
        }
        return nbt;
    }
    
    private <T> NbtBase<T> getValueExact(final String key) {
        final NbtBase<T> value = (NbtBase<T>)this.getValue(key);
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException("Cannot find key " + key);
    }
    
    @Override
    public NbtBase<Map<String, NbtBase<?>>> deepClone() {
        return (NbtBase<Map<String, NbtBase<?>>>)this.container.deepClone();
    }
    
    @Override
    public <T> NbtCompound put(final NbtBase<T> entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry cannot be NULL.");
        }
        this.getValue().put(entry.getName(), entry);
        return this;
    }
    
    @Override
    public String getString(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public String getStringOrDefault(final String key) {
        return (String)this.getValueOrDefault(key, NbtType.TAG_STRING).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final String value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public NbtCompound putObject(final String key, final Object value) {
        if (value == null) {
            this.remove(key);
        }
        else if (value instanceof NbtBase) {
            this.put(key, (NbtBase<?>)value);
        }
        else {
            final NbtBase<?> base = new MemoryElement<Object>(key, value);
            this.put(base);
        }
        return this;
    }
    
    @Override
    public Object getObject(final String key) {
        final NbtBase<?> base = this.getValue(key);
        if (base != null && base.getType() != NbtType.TAG_LIST && base.getType() != NbtType.TAG_COMPOUND) {
            return base.getValue();
        }
        return base;
    }
    
    @Override
    public byte getByte(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public byte getByteOrDefault(final String key) {
        return (byte)this.getValueOrDefault(key, NbtType.TAG_BYTE).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final byte value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public Short getShort(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public short getShortOrDefault(final String key) {
        return (short)this.getValueOrDefault(key, NbtType.TAG_SHORT).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final short value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public int getInteger(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public int getIntegerOrDefault(final String key) {
        return (int)this.getValueOrDefault(key, NbtType.TAG_INT).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final int value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public long getLong(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public long getLongOrDefault(final String key) {
        return (long)this.getValueOrDefault(key, NbtType.TAG_LONG).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final long value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public float getFloat(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public float getFloatOrDefault(final String key) {
        return (float)this.getValueOrDefault(key, NbtType.TAG_FLOAT).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final float value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public double getDouble(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public double getDoubleOrDefault(final String key) {
        return (double)this.getValueOrDefault(key, NbtType.TAG_DOUBLE).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final double value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public byte[] getByteArray(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final byte[] value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public int[] getIntegerArray(final String key) {
        return this.getValueExact(key).getValue();
    }
    
    @Override
    public NbtCompound put(final String key, final int[] value) {
        this.getValue().put(key, NbtFactory.of(key, value));
        return this;
    }
    
    @Override
    public NbtCompound getCompound(final String key) {
        return (NbtCompound)this.getValueExact(key);
    }
    
    @Override
    public NbtCompound getCompoundOrDefault(final String key) {
        return (NbtCompound)this.getValueOrDefault(key, NbtType.TAG_COMPOUND);
    }
    
    @Override
    public NbtCompound put(final NbtCompound compound) {
        this.getValue().put(compound.getName(), compound);
        return this;
    }
    
    @Override
    public <T> NbtList<T> getList(final String key) {
        return (NbtList<T>)(NbtList)this.getValueExact(key);
    }
    
    @Override
    public <T> NbtList<T> getListOrDefault(final String key) {
        return (NbtList<T>)(NbtList)this.getValueOrDefault(key, NbtType.TAG_LIST);
    }
    
    @Override
    public <T> NbtCompound put(final NbtList<T> list) {
        this.getValue().put(list.getName(), list);
        return this;
    }
    
    @Override
    public NbtCompound put(final String key, final NbtBase<?> entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry cannot be NULL.");
        }
        final NbtBase<?> clone = entry.deepClone();
        clone.setName(key);
        return this.put(clone);
    }
    
    @Override
    public <T> NbtCompound put(final String key, final Collection<? extends NbtBase<T>> list) {
        return this.put(WrappedList.fromList(key, (Collection<?>)list));
    }
    
    @Override
    public NbtBase<?> remove(final String key) {
        return this.getValue().remove(key);
    }
    
    @Override
    public void write(final DataOutput destination) {
        NbtBinarySerializer.DEFAULT.serialize(this.container, destination);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof WrappedCompound) {
            final WrappedCompound other = (WrappedCompound)obj;
            return this.container.equals(other.container);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return this.container.hashCode();
    }
    
    @Override
    public Iterator<NbtBase<?>> iterator() {
        return this.getValue().values().iterator();
    }
    
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"name\": \"" + this.getName() + "\"");
        for (final NbtBase<?> element : this) {
            builder.append(", ");
            if (element.getType() == NbtType.TAG_STRING) {
                builder.append("\"" + element.getName() + "\": \"" + element.getValue() + "\"");
            }
            else {
                builder.append("\"" + element.getName() + "\": " + element.getValue());
            }
        }
        builder.append("}");
        return builder.toString();
    }
}
