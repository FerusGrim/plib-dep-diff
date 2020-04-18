package com.comphenix.protocol.wrappers.nbt;

import com.google.common.base.Objects;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import java.io.DataOutput;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Method;

class WrappedElement<TType> implements NbtWrapper<TType>
{
    private static volatile Method methodGetTypeID;
    private static volatile Method methodClone;
    private static volatile Boolean hasNbtName;
    private static StructureModifier<?>[] modifiers;
    private Object handle;
    private NbtType type;
    private NameProperty nameProperty;
    
    public WrappedElement(final Object handle) {
        this.handle = handle;
        this.initializeProperty();
    }
    
    public WrappedElement(final Object handle, final String name) {
        this.handle = handle;
        this.initializeProperty();
        this.setName(name);
    }
    
    private void initializeProperty() {
        if (this.nameProperty == null) {
            final Class<?> base = MinecraftReflection.getNBTBaseClass();
            if (WrappedElement.hasNbtName == null) {
                WrappedElement.hasNbtName = NameProperty.hasStringIndex(base, 0);
            }
            if (WrappedElement.hasNbtName) {
                this.nameProperty = NameProperty.fromStringIndex(base, this.handle, 0);
            }
            else {
                this.nameProperty = NameProperty.fromBean();
            }
        }
    }
    
    protected StructureModifier<TType> getCurrentModifier() {
        final NbtType type = this.getType();
        return this.getCurrentBaseModifier().withType(type.getValueType());
    }
    
    protected StructureModifier<Object> getCurrentBaseModifier() {
        final int index = this.getType().ordinal();
        StructureModifier<Object> modifier = (StructureModifier<Object>)WrappedElement.modifiers[index];
        if (modifier == null) {
            synchronized (this) {
                if (WrappedElement.modifiers[index] == null) {
                    WrappedElement.modifiers[index] = new StructureModifier<Object>(this.handle.getClass(), MinecraftReflection.getNBTBaseClass(), false);
                }
                modifier = (StructureModifier<Object>)WrappedElement.modifiers[index];
            }
        }
        return modifier;
    }
    
    @Override
    public boolean accept(final NbtVisitor visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public Object getHandle() {
        return this.handle;
    }
    
    @Override
    public NbtType getType() {
        if (WrappedElement.methodGetTypeID == null) {
            WrappedElement.methodGetTypeID = FuzzyReflection.fromClass(MinecraftReflection.getNBTBaseClass()).getMethodByParameters("getTypeID", Byte.TYPE, new Class[0]);
        }
        if (this.type == null) {
            try {
                this.type = NbtType.getTypeFromID((byte)WrappedElement.methodGetTypeID.invoke(this.handle, new Object[0]));
            }
            catch (Exception e) {
                throw new FieldAccessException("Cannot get NBT type of " + this.handle, e);
            }
        }
        return this.type;
    }
    
    public NbtType getSubType() {
        final int subID = this.getCurrentBaseModifier().withType(Byte.TYPE).withTarget(this.handle).read(0);
        return NbtType.getTypeFromID(subID);
    }
    
    public void setSubType(final NbtType type) {
        final byte subID = (byte)type.getRawID();
        this.getCurrentBaseModifier().withType(Byte.TYPE).withTarget(this.handle).write(0, subID);
    }
    
    @Override
    public String getName() {
        return this.nameProperty.getName();
    }
    
    @Override
    public void setName(final String name) {
        this.nameProperty.setName(name);
    }
    
    @Override
    public TType getValue() {
        return this.getCurrentModifier().withTarget(this.handle).read(0);
    }
    
    @Override
    public void setValue(final TType newValue) {
        this.getCurrentModifier().withTarget(this.handle).write(0, newValue);
    }
    
    @Override
    public void write(final DataOutput destination) {
        NbtBinarySerializer.DEFAULT.serialize((NbtBase<Object>)this, destination);
    }
    
    @Override
    public NbtBase<TType> deepClone() {
        if (WrappedElement.methodClone == null) {
            final Class<?> base = MinecraftReflection.getNBTBaseClass();
            WrappedElement.methodClone = FuzzyReflection.fromClass(base).getMethodByParameters("clone", base, new Class[0]);
        }
        try {
            return (NbtBase<TType>)NbtFactory.fromNMS(WrappedElement.methodClone.invoke(this.handle, new Object[0]), this.getName());
        }
        catch (Exception e) {
            throw new FieldAccessException("Unable to clone " + this.handle, e);
        }
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.getName(), this.getType(), this.getValue() });
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof NbtBase) {
            final NbtBase<?> other = (NbtBase<?>)obj;
            if (other.getType().equals(this.getType())) {
                return Objects.equal(this.getValue(), (Object)other.getValue());
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        final String name = this.getName();
        result.append("{");
        if (name != null && name.length() > 0) {
            result.append("name: '" + name + "', ");
        }
        result.append("value: ");
        if (this.getType() == NbtType.TAG_STRING) {
            result.append("'" + this.getValue() + "'");
        }
        else {
            result.append(this.getValue());
        }
        result.append("}");
        return result.toString();
    }
    
    static {
        WrappedElement.modifiers = (StructureModifier<?>[])new StructureModifier[NbtType.values().length];
    }
}
