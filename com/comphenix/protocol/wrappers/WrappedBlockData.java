package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import org.bukkit.Material;

public abstract class WrappedBlockData extends AbstractWrapper implements ClonableWrapper
{
    private static final Class<?> MAGIC_NUMBERS;
    private static final Class<?> IBLOCK_DATA;
    private static final Class<?> BLOCK;
    
    public WrappedBlockData(final Object handle) {
        super(WrappedBlockData.IBLOCK_DATA);
        this.setHandle(handle);
    }
    
    public abstract Material getType();
    
    @Deprecated
    public abstract int getData();
    
    public abstract void setType(final Material p0);
    
    @Deprecated
    public abstract void setData(final int p0);
    
    @Deprecated
    public abstract void setTypeAndData(final Material p0, final int p1);
    
    @Override
    public abstract WrappedBlockData deepClone();
    
    public static WrappedBlockData createData(final Material type) {
        return MinecraftVersion.atOrAbove(MinecraftVersion.AQUATIC_UPDATE) ? createNewData(type) : createOldData(type);
    }
    
    @Deprecated
    public static WrappedBlockData createData(final Material type, final int data) {
        return MinecraftVersion.atOrAbove(MinecraftVersion.AQUATIC_UPDATE) ? createNewData(type, data) : createOldData(type, data);
    }
    
    public static WrappedBlockData fromHandle(final Object handle) {
        return MinecraftVersion.atOrAbove(MinecraftVersion.AQUATIC_UPDATE) ? new NewBlockData(handle) : new OldBlockData(handle);
    }
    
    public static WrappedBlockData createData(final Object data) {
        return createNewData(data);
    }
    
    @Override
    public String toString() {
        return "WrappedBlockData[handle=" + this.handle + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = 31 * result + this.getType().hashCode();
        result = 31 * result + this.getData();
        return result;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof WrappedBlockData) {
            final WrappedBlockData that = (WrappedBlockData)o;
            return this.handle.equals(that.handle) || (this.getType() == that.getType() && this.getData() == that.getData());
        }
        return false;
    }
    
    static {
        MAGIC_NUMBERS = MinecraftReflection.getCraftBukkitClass("util.CraftMagicNumbers");
        IBLOCK_DATA = MinecraftReflection.getIBlockDataClass();
        BLOCK = MinecraftReflection.getBlockClass();
    }
    
    private static class NewBlockData extends WrappedBlockData
    {
        private static MethodAccessor MATERIAL_FROM_BLOCK;
        private static MethodAccessor TO_LEGACY_DATA;
        private static MethodAccessor GET_BLOCK;
        private static MethodAccessor BLOCK_FROM_MATERIAL;
        private static MethodAccessor GET_BLOCK_DATA;
        private static MethodAccessor FROM_LEGACY_DATA;
        private static MethodAccessor GET_HANDLE;
        
        private NewBlockData(final Object handle) {
            super(handle);
        }
        
        @Override
        public Material getType() {
            final Object block = NewBlockData.GET_BLOCK.invoke(this.handle, new Object[0]);
            return (Material)NewBlockData.MATERIAL_FROM_BLOCK.invoke(null, block);
        }
        
        @Override
        public int getData() {
            return ((Number)NewBlockData.TO_LEGACY_DATA.invoke(null, this.handle)).intValue();
        }
        
        @Override
        public void setType(final Material material) {
            final Object block = NewBlockData.BLOCK_FROM_MATERIAL.invoke(null, material);
            this.setHandle(NewBlockData.GET_BLOCK_DATA.invoke(block, new Object[0]));
        }
        
        @Override
        public void setData(final int data) {
            this.setTypeAndData(this.getType(), data);
        }
        
        @Override
        public void setTypeAndData(final Material material, final int data) {
            this.setHandle(NewBlockData.TO_LEGACY_DATA.invoke(null, material, (byte)data));
        }
        
        @Override
        public WrappedBlockData deepClone() {
            return new NewBlockData(this.handle);
        }
        
        private static WrappedBlockData createNewData(final Material material) {
            final Object block = NewBlockData.BLOCK_FROM_MATERIAL.invoke(null, material);
            return new NewBlockData(NewBlockData.GET_BLOCK_DATA.invoke(block, new Object[0]));
        }
        
        private static WrappedBlockData createNewData(final Material material, final int data) {
            return new NewBlockData(NewBlockData.FROM_LEGACY_DATA.invoke(null, material, (byte)data));
        }
        
        private static WrappedBlockData createNewData(final Object data) {
            return new NewBlockData(NewBlockData.GET_HANDLE.invoke(data, new Object[0]));
        }
        
        static {
            if (MinecraftVersion.atOrAbove(MinecraftVersion.AQUATIC_UPDATE)) {
                FuzzyReflection fuzzy = FuzzyReflection.fromClass(WrappedBlockData.MAGIC_NUMBERS);
                FuzzyMethodContract contract = FuzzyMethodContract.newBuilder().requireModifier(8).returnTypeExact(Material.class).parameterExactArray(WrappedBlockData.BLOCK).build();
                NewBlockData.MATERIAL_FROM_BLOCK = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
                contract = FuzzyMethodContract.newBuilder().requireModifier(8).parameterExactArray(Material.class).returnTypeExact(WrappedBlockData.BLOCK).build();
                NewBlockData.BLOCK_FROM_MATERIAL = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
                contract = FuzzyMethodContract.newBuilder().requireModifier(8).parameterExactArray(WrappedBlockData.IBLOCK_DATA).returnTypeExact(Byte.TYPE).build();
                NewBlockData.TO_LEGACY_DATA = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
                contract = FuzzyMethodContract.newBuilder().requireModifier(8).parameterExactArray(Material.class, Byte.TYPE).returnTypeExact(WrappedBlockData.IBLOCK_DATA).build();
                NewBlockData.FROM_LEGACY_DATA = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
                fuzzy = FuzzyReflection.fromClass(WrappedBlockData.IBLOCK_DATA);
                contract = FuzzyMethodContract.newBuilder().banModifier(8).returnTypeExact(WrappedBlockData.BLOCK).parameterCount(0).build();
                NewBlockData.GET_BLOCK = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
                fuzzy = FuzzyReflection.fromClass(WrappedBlockData.BLOCK);
                contract = FuzzyMethodContract.newBuilder().banModifier(8).parameterCount(0).returnTypeExact(WrappedBlockData.IBLOCK_DATA).build();
                NewBlockData.GET_BLOCK_DATA = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
                fuzzy = FuzzyReflection.fromClass(MinecraftReflection.getCraftBukkitClass("block.data.CraftBlockData"));
                contract = FuzzyMethodContract.newBuilder().banModifier(8).parameterCount(0).returnTypeExact(WrappedBlockData.IBLOCK_DATA).build();
                NewBlockData.GET_HANDLE = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
            }
        }
    }
    
    private static class OldBlockData extends WrappedBlockData
    {
        private static MethodAccessor FROM_LEGACY_DATA;
        private static MethodAccessor TO_LEGACY_DATA;
        private static MethodAccessor GET_NMS_BLOCK;
        private static MethodAccessor GET_BLOCK;
        
        private OldBlockData(final Object handle) {
            super(handle);
        }
        
        @Override
        public Material getType() {
            final Object block = OldBlockData.GET_BLOCK.invoke(this.handle, new Object[0]);
            return BukkitConverters.getBlockConverter().getSpecific(block);
        }
        
        @Override
        public int getData() {
            final Object block = OldBlockData.GET_BLOCK.invoke(this.handle, new Object[0]);
            return (int)OldBlockData.TO_LEGACY_DATA.invoke(block, this.handle);
        }
        
        @Override
        public void setType(final Material type) {
            this.setTypeAndData(type, 0);
        }
        
        @Override
        public void setData(final int data) {
            this.setTypeAndData(this.getType(), data);
        }
        
        @Override
        public void setTypeAndData(final Material type, final int data) {
            final Object nmsBlock = OldBlockData.GET_NMS_BLOCK.invoke(null, type);
            final Object blockData = OldBlockData.FROM_LEGACY_DATA.invoke(nmsBlock, data);
            this.setHandle(blockData);
        }
        
        @Override
        public WrappedBlockData deepClone() {
            return WrappedBlockData.createData(this.getType(), this.getData());
        }
        
        private static WrappedBlockData createOldData(final Material type) {
            final Object blockData = OldBlockData.GET_BLOCK.invoke(null, type);
            return new OldBlockData(blockData);
        }
        
        private static WrappedBlockData createOldData(final Material type, final int data) {
            final Object nmsBlock = OldBlockData.GET_NMS_BLOCK.invoke(null, type);
            final Object blockData = OldBlockData.FROM_LEGACY_DATA.invoke(nmsBlock, data);
            return new OldBlockData(blockData);
        }
        
        static {
            if (!MinecraftVersion.atOrAbove(MinecraftVersion.AQUATIC_UPDATE)) {
                FuzzyReflection fuzzy = FuzzyReflection.fromClass(WrappedBlockData.BLOCK);
                FuzzyMethodContract contract = FuzzyMethodContract.newBuilder().banModifier(8).parameterExactArray(Integer.TYPE).returnTypeExact(WrappedBlockData.IBLOCK_DATA).build();
                OldBlockData.FROM_LEGACY_DATA = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
                contract = FuzzyMethodContract.newBuilder().banModifier(8).parameterExactArray(WrappedBlockData.IBLOCK_DATA).returnTypeExact(Integer.TYPE).build();
                OldBlockData.TO_LEGACY_DATA = Accessors.getMethodAccessor(fuzzy.getMethod(contract, "toLegacyData"));
                fuzzy = FuzzyReflection.fromClass(WrappedBlockData.MAGIC_NUMBERS);
                OldBlockData.GET_NMS_BLOCK = Accessors.getMethodAccessor(fuzzy.getMethodByParameters("getBlock", WrappedBlockData.BLOCK, new Class[] { Material.class }));
                fuzzy = FuzzyReflection.fromClass(WrappedBlockData.IBLOCK_DATA);
                OldBlockData.GET_BLOCK = Accessors.getMethodAccessor(fuzzy.getMethodByParameters("getBlock", WrappedBlockData.BLOCK, new Class[0]));
            }
        }
    }
}
