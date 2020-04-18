package com.comphenix.protocol.wrappers.nbt;

import com.google.common.primitives.Primitives;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum NbtType
{
    TAG_END(0, (Class<?>)Void.class), 
    TAG_BYTE(1, (Class<?>)Byte.TYPE), 
    TAG_SHORT(2, (Class<?>)Short.TYPE), 
    TAG_INT(3, (Class<?>)Integer.TYPE), 
    TAG_LONG(4, (Class<?>)Long.TYPE), 
    TAG_FLOAT(5, (Class<?>)Float.TYPE), 
    TAG_DOUBLE(6, (Class<?>)Double.TYPE), 
    TAG_BYTE_ARRAY(7, (Class<?>)byte[].class), 
    TAG_INT_ARRAY(11, (Class<?>)int[].class), 
    TAG_STRING(8, (Class<?>)String.class), 
    TAG_LIST(9, (Class<?>)List.class), 
    TAG_COMPOUND(10, (Class<?>)Map.class);
    
    private int rawID;
    private Class<?> valueType;
    private static NbtType[] lookup;
    private static Map<Class<?>, NbtType> classLookup;
    
    private NbtType(final int rawID, final Class<?> valueType) {
        this.rawID = rawID;
        this.valueType = valueType;
    }
    
    public boolean isComposite() {
        return this == NbtType.TAG_COMPOUND || this == NbtType.TAG_LIST;
    }
    
    public int getRawID() {
        return this.rawID;
    }
    
    public Class<?> getValueType() {
        return this.valueType;
    }
    
    public static NbtType getTypeFromID(final int rawID) {
        if (rawID < 0 || rawID >= NbtType.lookup.length) {
            throw new IllegalArgumentException("Unrecognized raw ID " + rawID);
        }
        return NbtType.lookup[rawID];
    }
    
    public static NbtType getTypeFromClass(final Class<?> clazz) {
        final NbtType result = NbtType.classLookup.get(clazz);
        if (result != null) {
            return result;
        }
        for (final Class<?> implemented : clazz.getInterfaces()) {
            if (NbtType.classLookup.containsKey(implemented)) {
                return NbtType.classLookup.get(implemented);
            }
        }
        throw new IllegalArgumentException("No NBT tag can represent a " + clazz);
    }
    
    static {
        final NbtType[] values = values();
        NbtType.lookup = new NbtType[values.length];
        NbtType.classLookup = new HashMap<Class<?>, NbtType>();
        for (final NbtType type : values) {
            NbtType.lookup[type.getRawID()] = type;
            NbtType.classLookup.put(type.getValueType(), type);
            if (type.getValueType().isPrimitive()) {
                NbtType.classLookup.put(Primitives.wrap((Class)type.getValueType()), type);
            }
        }
        NbtType.classLookup.put(NbtList.class, NbtType.TAG_LIST);
        NbtType.classLookup.put(NbtCompound.class, NbtType.TAG_COMPOUND);
    }
}
