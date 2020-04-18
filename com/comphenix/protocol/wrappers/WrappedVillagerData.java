package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.EquivalentConverter;

public class WrappedVillagerData extends AbstractWrapper implements ClonableWrapper
{
    private static final Class<?> NMS_CLASS;
    private static final Class<?> TYPE_CLASS;
    private static final Class<?> PROF_CLASS;
    private static EquivalentConverter<Type> TYPE_CONVERTER;
    private static EquivalentConverter<Profession> PROF_CONVERTER;
    private StructureModifier<Object> modifier;
    private static ConstructorAccessor CONSTRUCTOR;
    
    private WrappedVillagerData(final Object handle) {
        super(WrappedVillagerData.NMS_CLASS);
        this.setHandle(handle);
        this.modifier = new StructureModifier<Object>(WrappedVillagerData.NMS_CLASS).withTarget(handle);
    }
    
    public static WrappedVillagerData fromHandle(final Object handle) {
        return new WrappedVillagerData(handle);
    }
    
    public static WrappedVillagerData fromValues(final Type type, final Profession profession, final int level) {
        final Object genericType = WrappedVillagerData.TYPE_CONVERTER.getGeneric(type);
        final Object genericProf = WrappedVillagerData.PROF_CONVERTER.getGeneric(profession);
        if (WrappedVillagerData.CONSTRUCTOR == null) {
            WrappedVillagerData.CONSTRUCTOR = Accessors.getConstructorAccessor(WrappedVillagerData.NMS_CLASS, WrappedVillagerData.TYPE_CLASS, WrappedVillagerData.PROF_CLASS, Integer.TYPE);
        }
        final Object handle = WrappedVillagerData.CONSTRUCTOR.invoke(genericType, genericProf, level);
        return fromHandle(handle);
    }
    
    public static Class<?> getNmsClass() {
        return WrappedVillagerData.NMS_CLASS;
    }
    
    public int getLevel() {
        return this.modifier.withType(Integer.TYPE).read(0);
    }
    
    public Type getType() {
        return this.modifier.withType(WrappedVillagerData.TYPE_CLASS, WrappedVillagerData.TYPE_CONVERTER).read(0);
    }
    
    public Profession getProfession() {
        return this.modifier.withType(WrappedVillagerData.PROF_CLASS, WrappedVillagerData.PROF_CONVERTER).read(0);
    }
    
    @Override
    public WrappedVillagerData deepClone() {
        return fromValues(this.getType(), this.getProfession(), this.getLevel());
    }
    
    static {
        NMS_CLASS = MinecraftReflection.getNullableNMS("VillagerData");
        TYPE_CLASS = MinecraftReflection.getNullableNMS("VillagerType");
        PROF_CLASS = MinecraftReflection.getNullableNMS("VillagerProfession");
        if (WrappedVillagerData.NMS_CLASS != null) {
            WrappedVillagerData.TYPE_CONVERTER = new EnumWrappers.FauxEnumConverter<Type>(Type.class, WrappedVillagerData.TYPE_CLASS);
            WrappedVillagerData.PROF_CONVERTER = new EnumWrappers.FauxEnumConverter<Profession>(Profession.class, WrappedVillagerData.PROF_CLASS);
        }
    }
    
    public enum Type
    {
        DESERT, 
        JUNGLE, 
        PLAINS, 
        SAVANNA, 
        SNOW, 
        SWAMP, 
        TAIGA;
    }
    
    public enum Profession
    {
        NONE, 
        ARMORER, 
        BUTCHER, 
        CARTOGRAPHER, 
        CLERIC, 
        FARMER, 
        FISHERMAN, 
        FLETCHER, 
        LEATHERWORKER, 
        LIBRARIAN, 
        MASON, 
        NITWIT, 
        SHEPHERD, 
        TOOLSMITH, 
        WEAPONSMITH;
    }
}
