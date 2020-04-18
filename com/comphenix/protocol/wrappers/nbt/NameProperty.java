package com.comphenix.protocol.wrappers.nbt;

import com.google.common.collect.Maps;
import com.comphenix.protocol.reflect.StructureModifier;
import java.util.Map;

public abstract class NameProperty
{
    private static final Map<Class<?>, StructureModifier<String>> MODIFIERS;
    
    public abstract String getName();
    
    public abstract void setName(final String p0);
    
    private static StructureModifier<String> getModifier(final Class<?> baseClass) {
        StructureModifier<String> modifier = NameProperty.MODIFIERS.get(baseClass);
        if (modifier == null) {
            modifier = (StructureModifier<String>)new StructureModifier(baseClass, Object.class, false).withType(String.class);
            NameProperty.MODIFIERS.put(baseClass, modifier);
        }
        return modifier;
    }
    
    public static boolean hasStringIndex(final Class<?> baseClass, final int index) {
        return index >= 0 && index < getModifier(baseClass).size();
    }
    
    public static NameProperty fromStringIndex(final Class<?> baseClass, final Object target, final int index) {
        final StructureModifier<String> modifier = getModifier(baseClass).withTarget(target);
        return new NameProperty() {
            @Override
            public String getName() {
                return modifier.read(index);
            }
            
            @Override
            public void setName(final String name) {
                modifier.write(index, name);
            }
        };
    }
    
    public static NameProperty fromBean() {
        return new NameProperty() {
            private String name;
            
            @Override
            public void setName(final String name) {
                this.name = name;
            }
            
            @Override
            public String getName() {
                return this.name;
            }
        };
    }
    
    static {
        MODIFIERS = Maps.newConcurrentMap();
    }
}
