package com.comphenix.protocol.wrappers.nbt;

import com.comphenix.protocol.wrappers.ClonableWrapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import java.io.DataOutput;
import java.util.Iterator;
import java.util.Collection;
import com.comphenix.protocol.wrappers.collection.ConvertedList;
import java.util.List;

class WrappedList<TType> implements NbtWrapper<List<NbtBase<TType>>>, NbtList<TType>
{
    private WrappedElement<List<Object>> container;
    private ConvertedList<Object, NbtBase<TType>> savedList;
    private NbtType elementType;
    
    public static <T> NbtList<T> fromName(final String name) {
        return (NbtList<T>)(NbtList)NbtFactory.ofWrapper(NbtType.TAG_LIST, name);
    }
    
    public static <T> NbtList<T> fromArray(final String name, final T... elements) {
        final NbtList<T> result = fromName(name);
        for (final T element : elements) {
            if (element == null) {
                throw new IllegalArgumentException("An NBT list cannot contain a null element!");
            }
            if (element instanceof NbtBase) {
                result.add((NbtBase<T>)element);
            }
            else {
                result.add(NbtFactory.ofWrapper(element.getClass(), "", element));
            }
        }
        return result;
    }
    
    public static <T> NbtList<T> fromList(final String name, final Collection<? extends T> elements) {
        final NbtList<T> result = fromName(name);
        for (final T element : elements) {
            if (element == null) {
                throw new IllegalArgumentException("An NBT list cannot contain a null element!");
            }
            if (element instanceof NbtBase) {
                result.add((NbtBase<T>)element);
            }
            else {
                result.add(NbtFactory.ofWrapper(element.getClass(), "", element));
            }
        }
        return result;
    }
    
    public WrappedList(final Object handle) {
        this.elementType = NbtType.TAG_END;
        this.container = new WrappedElement<List<Object>>(handle);
        this.elementType = this.container.getSubType();
    }
    
    public WrappedList(final Object handle, final String name) {
        this.elementType = NbtType.TAG_END;
        this.container = new WrappedElement<List<Object>>(handle, name);
        this.elementType = this.container.getSubType();
    }
    
    @Override
    public boolean accept(final NbtVisitor visitor) {
        if (visitor.visitEnter(this)) {
            for (final NbtBase<TType> node : this.getValue()) {
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
        return NbtType.TAG_LIST;
    }
    
    @Override
    public NbtType getElementType() {
        return this.elementType;
    }
    
    @Override
    public void setElementType(final NbtType type) {
        this.elementType = type;
        this.container.setSubType(type);
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
    public List<NbtBase<TType>> getValue() {
        if (this.savedList == null) {
            this.savedList = new ConvertedList<Object, NbtBase<TType>>((List<Object>)this.container.getValue()) {
                private void verifyElement(final NbtBase<TType> element) {
                    if (element == null) {
                        throw new IllegalArgumentException("Cannot store NULL elements in list.");
                    }
                    if (!element.getName().equals("")) {
                        throw new IllegalArgumentException("Cannot add a the named NBT tag " + element + " to a list.");
                    }
                    if (WrappedList.this.getElementType() != NbtType.TAG_END) {
                        if (!element.getType().equals(WrappedList.this.getElementType())) {
                            throw new IllegalArgumentException("Cannot add " + element + " of " + element.getType() + " to a list of type " + WrappedList.this.getElementType());
                        }
                    }
                    else {
                        WrappedList.this.container.setSubType(element.getType());
                    }
                }
                
                @Override
                public boolean add(final NbtBase<TType> e) {
                    this.verifyElement(e);
                    return super.add(e);
                }
                
                @Override
                public void add(final int index, final NbtBase<TType> element) {
                    this.verifyElement(element);
                    super.add(index, element);
                }
                
                @Override
                public boolean addAll(final Collection<? extends NbtBase<TType>> c) {
                    boolean result = false;
                    for (final NbtBase<TType> element : c) {
                        this.add(element);
                        result = true;
                    }
                    return result;
                }
                
                @Override
                protected Object toInner(final NbtBase<TType> outer) {
                    if (outer == null) {
                        return null;
                    }
                    return NbtFactory.fromBase(outer).getHandle();
                }
                
                @Override
                protected NbtBase<TType> toOuter(final Object inner) {
                    if (inner == null) {
                        return null;
                    }
                    return (NbtBase<TType>)NbtFactory.fromNMS(inner, null);
                }
                
                @Override
                public String toString() {
                    return WrappedList.this.toString();
                }
            };
        }
        return this.savedList;
    }
    
    @Override
    public NbtBase<List<NbtBase<TType>>> deepClone() {
        return (NbtBase<List<NbtBase<TType>>>)this.container.deepClone();
    }
    
    @Override
    public void addClosest(final Object value) {
        if (this.getElementType() == NbtType.TAG_END) {
            throw new IllegalStateException("This list has not been typed yet.");
        }
        if (value instanceof Number) {
            final Number number = (Number)value;
            switch (this.getElementType()) {
                case TAG_BYTE: {
                    this.add(number.byteValue());
                    break;
                }
                case TAG_SHORT: {
                    this.add(number.shortValue());
                    break;
                }
                case TAG_INT: {
                    this.add(number.intValue());
                    break;
                }
                case TAG_LONG: {
                    this.add(number.longValue());
                    break;
                }
                case TAG_FLOAT: {
                    this.add(number.floatValue());
                    break;
                }
                case TAG_DOUBLE: {
                    this.add(number.doubleValue());
                    break;
                }
                case TAG_STRING: {
                    this.add(number.toString());
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Cannot convert " + value + " to " + this.getType());
                }
            }
        }
        else if (value instanceof NbtBase) {
            this.add((NbtBase<TType>)value);
        }
        else {
            this.add(NbtFactory.ofWrapper(this.getElementType(), "", value));
        }
    }
    
    @Override
    public void add(final NbtBase<TType> element) {
        this.getValue().add(element);
    }
    
    @Override
    public void add(final String value) {
        this.add(NbtFactory.of("", value));
    }
    
    @Override
    public void add(final byte value) {
        this.add(NbtFactory.of("", value));
    }
    
    @Override
    public void add(final short value) {
        this.add(NbtFactory.of("", value));
    }
    
    @Override
    public void add(final int value) {
        this.add(NbtFactory.of("", value));
    }
    
    @Override
    public void add(final long value) {
        this.add(NbtFactory.of("", value));
    }
    
    @Override
    public void add(final double value) {
        this.add(NbtFactory.of("", value));
    }
    
    @Override
    public void add(final byte[] value) {
        this.add(NbtFactory.of("", value));
    }
    
    @Override
    public void add(final int[] value) {
        this.add(NbtFactory.of("", value));
    }
    
    @Override
    public int size() {
        return this.getValue().size();
    }
    
    @Override
    public TType getValue(final int index) {
        return this.getValue().get(index).getValue();
    }
    
    @Override
    public Collection<NbtBase<TType>> asCollection() {
        return this.getValue();
    }
    
    @Override
    public void setValue(final List<NbtBase<TType>> newValue) {
        NbtBase<TType> lastElement = null;
        final List<Object> list = this.container.getValue();
        list.clear();
        for (final NbtBase<TType> type : newValue) {
            if (type != null) {
                lastElement = type;
                list.add(NbtFactory.fromBase(type).getHandle());
            }
            else {
                list.add(null);
            }
        }
        if (lastElement != null) {
            this.container.setSubType(lastElement.getType());
        }
    }
    
    @Override
    public void write(final DataOutput destination) {
        NbtBinarySerializer.DEFAULT.serialize(this.container, destination);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof WrappedList) {
            final WrappedList<TType> other = (WrappedList<TType>)obj;
            return this.container.equals(other.container);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return this.container.hashCode();
    }
    
    @Override
    public Iterator<TType> iterator() {
        return Iterables.transform((Iterable)this.getValue(), (Function)new Function<NbtBase<TType>, TType>() {
            public TType apply(@Nullable final NbtBase<TType> param) {
                return param.getValue();
            }
        }).iterator();
    }
    
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{\"name\": \"" + this.getName() + "\", \"value\": [");
        if (this.size() > 0) {
            if (this.getElementType() == NbtType.TAG_STRING) {
                builder.append("\"" + Joiner.on("\", \"").join((Iterable)this) + "\"");
            }
            else {
                builder.append(Joiner.on(", ").join((Iterable)this));
            }
        }
        builder.append("]}");
        return builder.toString();
    }
    
    @Override
    public void remove(final Object remove) {
        this.getValue().remove(remove);
    }
}
