package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.function.Supplier;
import java.util.UUID;
import java.lang.reflect.Constructor;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.EquivalentConverter;

public class WrappedAttributeModifier extends AbstractWrapper
{
    private static final Class<?> OPERATION_CLASS;
    private static final boolean OPERATION_ENUM;
    private static final EquivalentConverter<Operation> OPERATION_CONVERTER;
    private static StructureModifier<Object> BASE_MODIFIER;
    private static Constructor<?> ATTRIBUTE_MODIFIER_CONSTRUCTOR;
    protected StructureModifier<Object> modifier;
    private final UUID uuid;
    private final Supplier<String> name;
    private final Operation operation;
    private final double amount;
    
    protected WrappedAttributeModifier(final UUID uuid, final String name, final double amount, final Operation operation) {
        super(MinecraftReflection.getAttributeModifierClass());
        this.uuid = uuid;
        this.name = (() -> name);
        this.amount = amount;
        this.operation = operation;
    }
    
    protected WrappedAttributeModifier(@Nonnull final Object handle) {
        super(MinecraftReflection.getAttributeModifierClass());
        this.setHandle(handle);
        this.initializeModifier(handle);
        this.uuid = this.modifier.withType(UUID.class).read(0);
        final StructureModifier<String> stringMod = this.modifier.withType(String.class);
        if (stringMod.size() == 0) {
            final Supplier<String> supplier = this.modifier.withType(Supplier.class).read(0);
            this.name = supplier;
        }
        else {
            this.name = (() -> stringMod.read(0));
        }
        this.amount = this.modifier.withType(Double.TYPE).read(0);
        if (WrappedAttributeModifier.OPERATION_ENUM) {
            this.operation = this.modifier.withType(WrappedAttributeModifier.OPERATION_CLASS, WrappedAttributeModifier.OPERATION_CONVERTER).readSafely(0);
        }
        else {
            this.operation = Operation.fromId(this.modifier.withType(Integer.TYPE).readSafely(0));
        }
    }
    
    protected WrappedAttributeModifier(@Nonnull final Object handle, final UUID uuid, final String name, final double amount, final Operation operation) {
        this(uuid, name, amount, operation);
        this.setHandle(handle);
        this.initializeModifier(handle);
    }
    
    public static Builder newBuilder() {
        return new Builder((WrappedAttributeModifier)null).uuid(UUID.randomUUID());
    }
    
    public static Builder newBuilder(final UUID id) {
        return new Builder((WrappedAttributeModifier)null).uuid(id);
    }
    
    public static Builder newBuilder(@Nonnull final WrappedAttributeModifier template) {
        return new Builder((WrappedAttributeModifier)Preconditions.checkNotNull((Object)template, (Object)"template cannot be NULL."));
    }
    
    public static WrappedAttributeModifier fromHandle(@Nonnull final Object handle) {
        return new WrappedAttributeModifier(handle);
    }
    
    private void initializeModifier(@Nonnull final Object handle) {
        if (WrappedAttributeModifier.BASE_MODIFIER == null) {
            WrappedAttributeModifier.BASE_MODIFIER = new StructureModifier<Object>(MinecraftReflection.getAttributeModifierClass());
        }
        this.modifier = WrappedAttributeModifier.BASE_MODIFIER.withTarget(handle);
    }
    
    public UUID getUUID() {
        return this.uuid;
    }
    
    public String getName() {
        return this.name.get();
    }
    
    public Operation getOperation() {
        return this.operation;
    }
    
    public double getAmount() {
        return this.amount;
    }
    
    @Override
    public Object getHandle() {
        return this.handle;
    }
    
    public void setPendingSynchronization(final boolean pending) {
        this.modifier.withType(Boolean.TYPE).write(0, pending);
    }
    
    public boolean isPendingSynchronization() {
        return this.modifier.withType(Boolean.TYPE).read(0);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof WrappedAttributeModifier) {
            final WrappedAttributeModifier other = (WrappedAttributeModifier)obj;
            return Objects.equal((Object)this.uuid, (Object)other.getUUID());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return (this.uuid != null) ? this.uuid.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "[amount=" + this.amount + ", operation=" + this.operation + ", name='" + this.name + "', id=" + this.uuid + ", serialize=" + this.isPendingSynchronization() + "]";
    }
    
    private static Object getOperationParam(final Operation operation) {
        return WrappedAttributeModifier.OPERATION_ENUM ? WrappedAttributeModifier.OPERATION_CONVERTER.getGeneric(operation) : Integer.valueOf(operation.getId());
    }
    
    private static Constructor<?> getConstructor() {
        FuzzyMethodContract.Builder builder = FuzzyMethodContract.newBuilder().parameterCount(4).parameterDerivedOf(UUID.class, 0).parameterExactType(String.class, 1).parameterExactType(Double.TYPE, 2);
        if (WrappedAttributeModifier.OPERATION_ENUM) {
            builder = builder.parameterExactType(WrappedAttributeModifier.OPERATION_CLASS, 3);
        }
        else {
            builder = builder.parameterExactType(Integer.TYPE, 3);
        }
        final Constructor<?> ret = FuzzyReflection.fromClass(MinecraftReflection.getAttributeModifierClass(), true).getConstructor(builder.build());
        ret.setAccessible(true);
        return ret;
    }
    
    static {
        OPERATION_CLASS = MinecraftReflection.getMinecraftClass("AttributeModifier$Operation");
        OPERATION_ENUM = MinecraftVersion.atOrAbove(MinecraftVersion.VILLAGE_UPDATE);
        OPERATION_CONVERTER = new IndexedEnumConverter<Operation>((Class)Operation.class, (Class)WrappedAttributeModifier.OPERATION_CLASS);
    }
    
    private static class IndexedEnumConverter<T extends Enum<T>> implements EquivalentConverter<T>
    {
        private Class<T> specificClass;
        private Class<?> genericClass;
        
        private IndexedEnumConverter(final Class<T> specificClass, final Class<?> genericClass) {
            this.specificClass = specificClass;
            this.genericClass = genericClass;
        }
        
        @Override
        public Object getGeneric(final T specific) {
            final int ordinal = specific.ordinal();
            for (final Object elem : this.genericClass.getEnumConstants()) {
                if (((Enum)elem).ordinal() == ordinal) {
                    return elem;
                }
            }
            return null;
        }
        
        @Override
        public T getSpecific(final Object generic) {
            final int ordinal = ((Enum)generic).ordinal();
            for (final T elem : this.specificClass.getEnumConstants()) {
                if (elem.ordinal() == ordinal) {
                    return elem;
                }
            }
            return null;
        }
        
        @Override
        public Class<T> getSpecificType() {
            return this.specificClass;
        }
    }
    
    public enum Operation
    {
        ADD_NUMBER(0), 
        MULTIPLY_PERCENTAGE(1), 
        ADD_PERCENTAGE(2);
        
        private int id;
        
        private Operation(final int id) {
            this.id = id;
        }
        
        public int getId() {
            return this.id;
        }
        
        public static Operation fromId(final int id) {
            for (final Operation op : values()) {
                if (op.getId() == id) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Corrupt operation ID " + id + " detected.");
        }
    }
    
    public static class Builder
    {
        private Operation operation;
        private String name;
        private double amount;
        private UUID uuid;
        
        private Builder(final WrappedAttributeModifier template) {
            this.operation = Operation.ADD_NUMBER;
            this.name = "Unknown";
            if (template != null) {
                this.operation = template.getOperation();
                this.name = template.getName();
                this.amount = template.getAmount();
                this.uuid = template.getUUID();
            }
        }
        
        public Builder uuid(@Nonnull final UUID uuid) {
            this.uuid = (UUID)Preconditions.checkNotNull((Object)uuid, (Object)"uuid cannot be NULL.");
            return this;
        }
        
        public Builder operation(@Nonnull final Operation operation) {
            this.operation = (Operation)Preconditions.checkNotNull((Object)operation, (Object)"operation cannot be NULL.");
            return this;
        }
        
        public Builder name(@Nonnull final String name) {
            this.name = (String)Preconditions.checkNotNull((Object)name, (Object)"name cannot be NULL.");
            return this;
        }
        
        public Builder amount(final double amount) {
            this.amount = WrappedAttribute.checkDouble(amount);
            return this;
        }
        
        public WrappedAttributeModifier build() {
            Preconditions.checkNotNull((Object)this.uuid, (Object)"uuid cannot be NULL.");
            if (WrappedAttributeModifier.ATTRIBUTE_MODIFIER_CONSTRUCTOR == null) {
                WrappedAttributeModifier.ATTRIBUTE_MODIFIER_CONSTRUCTOR = getConstructor();
            }
            try {
                return new WrappedAttributeModifier(WrappedAttributeModifier.ATTRIBUTE_MODIFIER_CONSTRUCTOR.newInstance(this.uuid, this.name, this.amount, getOperationParam(this.operation)), this.uuid, this.name, this.amount, this.operation);
            }
            catch (Exception e) {
                throw new RuntimeException("Cannot construct AttributeModifier.", e);
            }
        }
    }
}