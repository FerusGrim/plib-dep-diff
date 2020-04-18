package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.Material;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

public class WrappedBlockData extends AbstractWrapper
{
    private static final Class<?> MAGIC_NUMBERS;
    private static final Class<?> IBLOCK_DATA;
    private static final Class<?> BLOCK;
    private static MethodAccessor FROM_LEGACY_DATA;
    private static MethodAccessor TO_LEGACY_DATA;
    private static MethodAccessor GET_NMS_BLOCK;
    private static MethodAccessor GET_BLOCK;
    
    public WrappedBlockData(final Object handle) {
        super(WrappedBlockData.IBLOCK_DATA);
        this.setHandle(handle);
    }
    
    public Material getType() {
        final Object block = WrappedBlockData.GET_BLOCK.invoke(this.handle, new Object[0]);
        return BukkitConverters.getBlockConverter().getSpecific(block);
    }
    
    @Deprecated
    public int getTypeId() {
        final Object block = WrappedBlockData.GET_BLOCK.invoke(this.handle, new Object[0]);
        return BukkitConverters.getBlockIDConverter().getSpecific(block);
    }
    
    public int getData() {
        final Object block = WrappedBlockData.GET_BLOCK.invoke(this.handle, new Object[0]);
        return (int)WrappedBlockData.TO_LEGACY_DATA.invoke(block, this.handle);
    }
    
    public void setType(final Material type) {
        this.setTypeAndData(type, 0);
    }
    
    public void setData(final int data) {
        this.setTypeAndData(this.getType(), data);
    }
    
    public void setTypeAndData(final Material type, final int data) {
        final Object nmsBlock = WrappedBlockData.GET_NMS_BLOCK.invoke(null, type);
        final Object blockData = WrappedBlockData.FROM_LEGACY_DATA.invoke(nmsBlock, data);
        this.setHandle(blockData);
    }
    
    public static WrappedBlockData createData(final Material type) {
        return createData(type, 0);
    }
    
    public static WrappedBlockData createData(final Material type, final int data) {
        final Object nmsBlock = WrappedBlockData.GET_NMS_BLOCK.invoke(null, type);
        final Object blockData = WrappedBlockData.FROM_LEGACY_DATA.invoke(nmsBlock, data);
        return new WrappedBlockData(blockData);
    }
    
    public WrappedBlockData deepClone() {
        return createData(this.getType(), this.getData());
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
        if (o instanceof WrappedBlockData) {
            final WrappedBlockData that = (WrappedBlockData)o;
            return this.getType() == that.getType() && this.getData() == that.getData();
        }
        return false;
    }
    
    static {
        MAGIC_NUMBERS = MinecraftReflection.getCraftBukkitClass("util.CraftMagicNumbers");
        IBLOCK_DATA = MinecraftReflection.getIBlockDataClass();
        BLOCK = MinecraftReflection.getBlockClass();
        WrappedBlockData.FROM_LEGACY_DATA = null;
        WrappedBlockData.TO_LEGACY_DATA = null;
        WrappedBlockData.GET_NMS_BLOCK = null;
        WrappedBlockData.GET_BLOCK = null;
        FuzzyReflection fuzzy = FuzzyReflection.fromClass(WrappedBlockData.BLOCK);
        FuzzyMethodContract contract = FuzzyMethodContract.newBuilder().banModifier(8).parameterExactArray(Integer.TYPE).returnTypeExact(WrappedBlockData.IBLOCK_DATA).build();
        WrappedBlockData.FROM_LEGACY_DATA = Accessors.getMethodAccessor(fuzzy.getMethod(contract));
        contract = FuzzyMethodContract.newBuilder().banModifier(8).parameterExactArray(WrappedBlockData.IBLOCK_DATA).returnTypeExact(Integer.TYPE).build();
        WrappedBlockData.TO_LEGACY_DATA = Accessors.getMethodAccessor(fuzzy.getMethod(contract, "toLegacyData"));
        fuzzy = FuzzyReflection.fromClass(WrappedBlockData.MAGIC_NUMBERS);
        WrappedBlockData.GET_NMS_BLOCK = Accessors.getMethodAccessor(fuzzy.getMethodByParameters("getBlock", WrappedBlockData.BLOCK, new Class[] { Material.class }));
        fuzzy = FuzzyReflection.fromClass(WrappedBlockData.IBLOCK_DATA);
        WrappedBlockData.GET_BLOCK = Accessors.getMethodAccessor(fuzzy.getMethodByParameters("getBlock", WrappedBlockData.BLOCK, new Class[0]));
    }
}
