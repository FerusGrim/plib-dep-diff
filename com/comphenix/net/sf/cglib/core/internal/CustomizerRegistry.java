package com.comphenix.net.sf.cglib.core.internal;

import com.comphenix.net.sf.cglib.core.Customizer;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import com.comphenix.net.sf.cglib.core.KeyFactoryCustomizer;
import java.util.List;
import java.util.Map;

public class CustomizerRegistry
{
    private final Class[] customizerTypes;
    private Map<Class, List<KeyFactoryCustomizer>> customizers;
    
    public CustomizerRegistry(final Class[] customizerTypes) {
        this.customizers = new HashMap<Class, List<KeyFactoryCustomizer>>();
        this.customizerTypes = customizerTypes;
    }
    
    public void add(final KeyFactoryCustomizer customizer) {
        final Class<? extends KeyFactoryCustomizer> klass = customizer.getClass();
        for (final Class type : this.customizerTypes) {
            if (type.isAssignableFrom(klass)) {
                List<KeyFactoryCustomizer> list = this.customizers.get(type);
                if (list == null) {
                    this.customizers.put(type, list = new ArrayList<KeyFactoryCustomizer>());
                }
                list.add(customizer);
            }
        }
    }
    
    public <T> List<T> get(final Class<T> klass) {
        final List<KeyFactoryCustomizer> list = this.customizers.get(klass);
        if (list == null) {
            return Collections.emptyList();
        }
        return (List<T>)list;
    }
    
    @Deprecated
    public static CustomizerRegistry singleton(final Customizer customizer) {
        final CustomizerRegistry registry = new CustomizerRegistry(new Class[] { Customizer.class });
        registry.add(customizer);
        return registry;
    }
}
