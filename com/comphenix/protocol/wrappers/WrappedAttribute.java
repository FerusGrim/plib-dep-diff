package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.google.common.collect.Sets;
import java.util.Collections;
import com.comphenix.protocol.wrappers.collection.CachedSet;
import com.comphenix.protocol.wrappers.collection.ConvertedSet;
import java.util.Collection;
import java.util.Iterator;
import com.google.common.base.Objects;
import java.util.UUID;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.utility.MinecraftReflection;
import javax.annotation.Nonnull;
import java.util.Set;
import java.lang.reflect.Constructor;
import com.comphenix.protocol.reflect.StructureModifier;

public class WrappedAttribute extends AbstractWrapper
{
    private static StructureModifier<Object> ATTRIBUTE_MODIFIER;
    private static Constructor<?> ATTRIBUTE_CONSTRUCTOR;
    protected Object handle;
    protected StructureModifier<Object> modifier;
    private double computedValue;
    private Set<WrappedAttributeModifier> attributeModifiers;
    
    private WrappedAttribute(@Nonnull final Object handle) {
        super(MinecraftReflection.getAttributeSnapshotClass());
        this.computedValue = Double.NaN;
        this.setHandle(handle);
        if (WrappedAttribute.ATTRIBUTE_MODIFIER == null) {
            WrappedAttribute.ATTRIBUTE_MODIFIER = new StructureModifier<Object>(MinecraftReflection.getAttributeSnapshotClass());
        }
        this.modifier = WrappedAttribute.ATTRIBUTE_MODIFIER.withTarget(handle);
    }
    
    public static WrappedAttribute fromHandle(@Nonnull final Object handle) {
        return new WrappedAttribute(handle);
    }
    
    public static Builder newBuilder() {
        return new Builder((WrappedAttribute)null);
    }
    
    public static Builder newBuilder(@Nonnull final WrappedAttribute template) {
        return new Builder((WrappedAttribute)Preconditions.checkNotNull((Object)template, (Object)"template cannot be NULL."));
    }
    
    public String getAttributeKey() {
        return this.modifier.withType(String.class).read(0);
    }
    
    public double getBaseValue() {
        return this.modifier.withType(Double.TYPE).read(0);
    }
    
    public double getFinalValue() {
        if (Double.isNaN(this.computedValue)) {
            this.computedValue = this.computeValue();
        }
        return this.computedValue;
    }
    
    public PacketContainer getParentPacket() {
        return new PacketContainer(PacketType.Play.Server.UPDATE_ATTRIBUTES, this.modifier.withType(MinecraftReflection.getPacketClass()).read(0));
    }
    
    public boolean hasModifier(final UUID id) {
        return this.getModifiers().contains(WrappedAttributeModifier.newBuilder(id).build());
    }
    
    public WrappedAttributeModifier getModifierByUUID(final UUID id) {
        if (this.hasModifier(id)) {
            for (final WrappedAttributeModifier modifier : this.getModifiers()) {
                if (Objects.equal((Object)modifier.getUUID(), (Object)id)) {
                    return modifier;
                }
            }
        }
        return null;
    }
    
    public Set<WrappedAttributeModifier> getModifiers() {
        if (this.attributeModifiers == null) {
            final Collection<Object> collection = this.modifier.withType(Collection.class).read(0);
            final ConvertedSet<Object, WrappedAttributeModifier> converted = new ConvertedSet<Object, WrappedAttributeModifier>(getSetSafely(collection)) {
                @Override
                protected Object toInner(final WrappedAttributeModifier outer) {
                    return outer.getHandle();
                }
                
                @Override
                protected WrappedAttributeModifier toOuter(final Object inner) {
                    return WrappedAttributeModifier.fromHandle(inner);
                }
            };
            this.attributeModifiers = new CachedSet<WrappedAttributeModifier>(converted);
        }
        return Collections.unmodifiableSet((Set<? extends WrappedAttributeModifier>)this.attributeModifiers);
    }
    
    public WrappedAttribute withModifiers(final Collection<WrappedAttributeModifier> modifiers) {
        return newBuilder(this).modifiers(modifiers).build();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof WrappedAttribute) {
            final WrappedAttribute other = (WrappedAttribute)obj;
            if (this.getBaseValue() == other.getBaseValue() && Objects.equal((Object)this.getAttributeKey(), (Object)other.getAttributeKey())) {
                return this.getModifiers().stream().filter(elem -> !other.getModifiers().contains(elem)).count() == 0L;
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        if (this.attributeModifiers == null) {
            this.getModifiers();
        }
        return Objects.hashCode(new Object[] { this.getAttributeKey(), this.getBaseValue(), this.attributeModifiers });
    }
    
    private double computeValue() {
        final Collection<WrappedAttributeModifier> modifiers = this.getModifiers();
        double x = this.getBaseValue();
        double y = 0.0;
        for (int phase = 0; phase < 3; ++phase) {
            for (final WrappedAttributeModifier modifier : modifiers) {
                if (modifier.getOperation().getId() == phase) {
                    switch (phase) {
                        case 0: {
                            x += modifier.getAmount();
                            continue;
                        }
                        case 1: {
                            y += x * modifier.getAmount();
                            continue;
                        }
                        case 2: {
                            y *= 1.0 + modifier.getAmount();
                            continue;
                        }
                        default: {
                            throw new IllegalStateException("Unknown phase: " + phase);
                        }
                    }
                }
            }
            if (phase == 0) {
                y = x;
            }
        }
        return y;
    }
    
    @Override
    public String toString() {
        return "WrappedAttribute[key=" + this.getAttributeKey() + ", base=" + this.getBaseValue() + ", final=" + this.getFinalValue() + ", modifiers=" + this.getModifiers() + "]";
    }
    
    private static <U> Set<U> getSetSafely(final Collection<U> collection) {
        return (Set<U>)((collection instanceof Set) ? ((Set)collection) : Sets.newHashSet((Iterable)collection));
    }
    
    static double checkDouble(final double value) {
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException("value cannot be infinite.");
        }
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("value cannot be NaN.");
        }
        return value;
    }
    
    public static class Builder
    {
        private double baseValue;
        private String attributeKey;
        private PacketContainer packet;
        private Collection<WrappedAttributeModifier> modifiers;
        
        private Builder(final WrappedAttribute template) {
            this.baseValue = Double.NaN;
            this.modifiers = (Collection<WrappedAttributeModifier>)Collections.emptyList();
            if (template != null) {
                this.baseValue = template.getBaseValue();
                this.attributeKey = template.getAttributeKey();
                this.packet = template.getParentPacket();
                this.modifiers = template.getModifiers();
            }
        }
        
        public Builder baseValue(final double baseValue) {
            this.baseValue = WrappedAttribute.checkDouble(baseValue);
            return this;
        }
        
        public Builder attributeKey(final String attributeKey) {
            this.attributeKey = (String)Preconditions.checkNotNull((Object)attributeKey, (Object)"attributeKey cannot be NULL.");
            return this;
        }
        
        public Builder modifiers(final Collection<WrappedAttributeModifier> modifiers) {
            this.modifiers = (Collection<WrappedAttributeModifier>)Preconditions.checkNotNull((Object)modifiers, (Object)"modifiers cannot be NULL - use an empty list instead.");
            return this;
        }
        
        public Builder packet(final PacketContainer packet) {
            if (((PacketContainer)Preconditions.checkNotNull((Object)packet, (Object)"packet cannot be NULL")).getType() != PacketType.Play.Server.UPDATE_ATTRIBUTES) {
                throw new IllegalArgumentException("Packet must be UPDATE_ATTRIBUTES (44)");
            }
            this.packet = packet;
            return this;
        }
        
        private Set<Object> getUnwrappedModifiers() {
            final Set<Object> output = (Set<Object>)Sets.newHashSet();
            for (final WrappedAttributeModifier modifier : this.modifiers) {
                output.add(modifier.getHandle());
            }
            return output;
        }
        
        public WrappedAttribute build() {
            Preconditions.checkNotNull((Object)this.packet, (Object)"packet cannot be NULL.");
            Preconditions.checkNotNull((Object)this.attributeKey, (Object)"attributeKey cannot be NULL.");
            if (Double.isNaN(this.baseValue)) {
                throw new IllegalStateException("Base value has not been set.");
            }
            if (WrappedAttribute.ATTRIBUTE_CONSTRUCTOR == null) {
                WrappedAttribute.ATTRIBUTE_CONSTRUCTOR = FuzzyReflection.fromClass(MinecraftReflection.getAttributeSnapshotClass(), true).getConstructor(FuzzyMethodContract.newBuilder().parameterCount(4).parameterDerivedOf(MinecraftReflection.getPacketClass(), 0).parameterExactType(String.class, 1).parameterExactType(Double.TYPE, 2).parameterDerivedOf(Collection.class, 3).build());
                WrappedAttribute.ATTRIBUTE_CONSTRUCTOR.setAccessible(true);
            }
            try {
                final Object handle = WrappedAttribute.ATTRIBUTE_CONSTRUCTOR.newInstance(this.packet.getHandle(), this.attributeKey, this.baseValue, this.getUnwrappedModifiers());
                return new WrappedAttribute(handle, null);
            }
            catch (Exception e) {
                throw new RuntimeException("Cannot construct AttributeSnapshot.", e);
            }
        }
    }
}
