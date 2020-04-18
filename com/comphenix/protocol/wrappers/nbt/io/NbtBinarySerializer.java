package com.comphenix.protocol.wrappers.nbt.io;

import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.wrappers.nbt.NbtWrapper;
import java.io.DataInput;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.io.DataOutput;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import java.lang.reflect.Method;

public class NbtBinarySerializer
{
    private static final Class<?> NBT_BASE_CLASS;
    private static Method methodWrite;
    private static LoadMethod loadMethod;
    public static final NbtBinarySerializer DEFAULT;
    
    public <TType> void serialize(final NbtBase<TType> value, final DataOutput destination) {
        if (NbtBinarySerializer.methodWrite == null) {
            final Class<?> base = MinecraftReflection.getNBTBaseClass();
            (NbtBinarySerializer.methodWrite = getUtilityClass().getMethodByParameters("writeNBT", base, DataOutput.class)).setAccessible(true);
        }
        try {
            NbtBinarySerializer.methodWrite.invoke(null, NbtFactory.fromBase(value).getHandle(), destination);
        }
        catch (Exception e) {
            throw new FieldAccessException("Unable to write NBT " + value, e);
        }
    }
    
    public <TType> NbtWrapper<TType> deserialize(final DataInput source) {
        LoadMethod method = NbtBinarySerializer.loadMethod;
        if (NbtBinarySerializer.loadMethod == null) {
            if (MinecraftReflection.isUsingNetty()) {
                try {
                    method = new LoadMethodWorldUpdate();
                }
                catch (IllegalArgumentException e2) {
                    method = new LoadMethodSkinUpdate();
                }
            }
            else {
                method = new LoadMethodNbtClass();
            }
            NbtBinarySerializer.loadMethod = method;
        }
        try {
            return NbtFactory.fromNMS(method.loadNbt(source), null);
        }
        catch (Exception e) {
            throw new FieldAccessException("Unable to read NBT from " + source, e);
        }
    }
    
    private static MethodAccessor getNbtLoadMethod(final Class<?>... parameters) {
        return Accessors.getMethodAccessor(getUtilityClass().getMethodByParameters("load", NbtBinarySerializer.NBT_BASE_CLASS, parameters), true);
    }
    
    private static FuzzyReflection getUtilityClass() {
        if (MinecraftReflection.isUsingNetty()) {
            return FuzzyReflection.fromClass(MinecraftReflection.getNbtCompressedStreamToolsClass(), true);
        }
        return FuzzyReflection.fromClass(MinecraftReflection.getNBTBaseClass(), true);
    }
    
    public NbtCompound deserializeCompound(final DataInput source) {
        return (NbtCompound)this.deserialize(source);
    }
    
    public <T> NbtList<T> deserializeList(final DataInput source) {
        return (NbtList<T>)(NbtList)this.deserialize(source);
    }
    
    static {
        NBT_BASE_CLASS = MinecraftReflection.getNBTBaseClass();
        DEFAULT = new NbtBinarySerializer();
    }
    
    private static class LoadMethodNbtClass implements LoadMethod
    {
        private MethodAccessor accessor;
        
        private LoadMethodNbtClass() {
            this.accessor = getNbtLoadMethod((Class<?>[])new Class[] { DataInput.class });
        }
        
        @Override
        public Object loadNbt(final DataInput input) {
            return this.accessor.invoke(null, input);
        }
    }
    
    private static class LoadMethodWorldUpdate implements LoadMethod
    {
        private MethodAccessor accessor;
        
        private LoadMethodWorldUpdate() {
            this.accessor = getNbtLoadMethod((Class<?>[])new Class[] { DataInput.class, Integer.TYPE });
        }
        
        @Override
        public Object loadNbt(final DataInput input) {
            return this.accessor.invoke(null, input, 0);
        }
    }
    
    private static class LoadMethodSkinUpdate implements LoadMethod
    {
        private Class<?> readLimitClass;
        private Object readLimiter;
        private MethodAccessor accessor;
        
        private LoadMethodSkinUpdate() {
            this.readLimitClass = MinecraftReflection.getNBTReadLimiterClass();
            this.readLimiter = FuzzyReflection.fromClass(this.readLimitClass).getSingleton();
            this.accessor = getNbtLoadMethod((Class<?>[])new Class[] { DataInput.class, Integer.TYPE, this.readLimitClass });
        }
        
        @Override
        public Object loadNbt(final DataInput input) {
            return this.accessor.invoke(null, input, 0, this.readLimiter);
        }
    }
    
    private interface LoadMethod
    {
        Object loadNbt(final DataInput p0);
    }
}
