package com.comphenix.protocol.reflect.instances;

import javax.annotation.Nullable;
import com.google.common.base.Objects;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import java.util.Iterator;
import com.comphenix.protocol.ProtocolLogger;
import java.util.logging.Level;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Collection;
import com.google.common.collect.ImmutableList;

public class DefaultInstances implements InstanceProvider
{
    public static final DefaultInstances DEFAULT;
    private int maximumRecursion;
    private ImmutableList<InstanceProvider> registered;
    private boolean nonNull;
    
    public DefaultInstances(final ImmutableList<InstanceProvider> registered) {
        this.maximumRecursion = 20;
        this.registered = registered;
    }
    
    public DefaultInstances(final DefaultInstances other) {
        this.maximumRecursion = 20;
        this.nonNull = other.nonNull;
        this.maximumRecursion = other.maximumRecursion;
        this.registered = other.registered;
    }
    
    public DefaultInstances(final InstanceProvider... instaceProviders) {
        this((ImmutableList<InstanceProvider>)ImmutableList.copyOf((Object[])instaceProviders));
    }
    
    public static DefaultInstances fromArray(final InstanceProvider... instanceProviders) {
        return new DefaultInstances((ImmutableList<InstanceProvider>)ImmutableList.copyOf((Object[])instanceProviders));
    }
    
    public static DefaultInstances fromCollection(final Collection<InstanceProvider> instanceProviders) {
        return new DefaultInstances((ImmutableList<InstanceProvider>)ImmutableList.copyOf((Collection)instanceProviders));
    }
    
    public ImmutableList<InstanceProvider> getRegistered() {
        return this.registered;
    }
    
    public boolean isNonNull() {
        return this.nonNull;
    }
    
    public void setNonNull(final boolean nonNull) {
        this.nonNull = nonNull;
    }
    
    public int getMaximumRecursion() {
        return this.maximumRecursion;
    }
    
    public void setMaximumRecursion(final int maximumRecursion) {
        if (maximumRecursion < 1) {
            throw new IllegalArgumentException("Maxmimum recursion height must be one or higher.");
        }
        this.maximumRecursion = maximumRecursion;
    }
    
    public <T> T getDefault(final Class<T> type) {
        return this.getDefaultInternal(type, (List<InstanceProvider>)this.registered, 0);
    }
    
    public <T> Constructor<T> getMinimumConstructor(final Class<T> type) {
        return this.getMinimumConstructor(type, (List<InstanceProvider>)this.registered, 0);
    }
    
    private <T> Constructor<T> getMinimumConstructor(final Class<T> type, final List<InstanceProvider> providers, final int recursionLevel) {
        Constructor<T> minimum = null;
        int lastCount = Integer.MAX_VALUE;
        for (final Constructor<?> candidate : type.getConstructors()) {
            final Class<?>[] types = candidate.getParameterTypes();
            if (types.length < lastCount && !this.contains(types, type)) {
                if (!this.nonNull || !this.isAnyNull(types, providers, recursionLevel)) {
                    minimum = (Constructor<T>)candidate;
                    lastCount = types.length;
                    if (lastCount == 0) {
                        break;
                    }
                }
            }
        }
        return minimum;
    }
    
    private boolean isAnyNull(final Class<?>[] types, final List<InstanceProvider> providers, final int recursionLevel) {
        for (final Class<?> type : types) {
            if (this.getDefaultInternal(type, providers, recursionLevel) == null) {
                return true;
            }
        }
        return false;
    }
    
    public <T> T getDefault(final Class<T> type, final List<InstanceProvider> providers) {
        return this.getDefaultInternal(type, providers, 0);
    }
    
    private <T> T getDefaultInternal(final Class<T> type, final List<InstanceProvider> providers, final int recursionLevel) {
        try {
            for (final InstanceProvider generator : providers) {
                final Object value = generator.create(type);
                if (value != null) {
                    return (T)value;
                }
            }
        }
        catch (NotConstructableException e) {
            return null;
        }
        if (recursionLevel >= this.maximumRecursion) {
            return null;
        }
        final Constructor<T> minimum = this.getMinimumConstructor(type, providers, recursionLevel + 1);
        try {
            if (minimum != null) {
                final int parameterCount = minimum.getParameterTypes().length;
                final Object[] params = new Object[parameterCount];
                final Class<?>[] types = minimum.getParameterTypes();
                for (int i = 0; i < parameterCount; ++i) {
                    params[i] = this.getDefaultInternal(types[i], providers, recursionLevel + 1);
                    if (params[i] == null && this.nonNull) {
                        ProtocolLogger.log(Level.WARNING, "Nonnull contract broken.", new Object[0]);
                        return null;
                    }
                }
                return this.createInstance(type, minimum, types, params);
            }
        }
        catch (Exception ex) {}
        return null;
    }
    
    public DefaultInstances forEnhancer(final Enhancer enhancer) {
        final Enhancer ex = enhancer;
        return new DefaultInstances(this) {
            @Override
            protected <T> T createInstance(final Class<T> type, final Constructor<T> constructor, final Class<?>[] types, final Object[] params) {
                return (T)ex.create(types, params);
            }
        };
    }
    
    protected <T> T createInstance(final Class<T> type, final Constructor<T> constructor, final Class<?>[] types, final Object[] params) {
        try {
            return constructor.newInstance(params);
        }
        catch (Exception e) {
            return null;
        }
    }
    
    protected <T> boolean contains(final T[] elements, final T elementToFind) {
        for (final T element : elements) {
            if (Objects.equal((Object)elementToFind, (Object)element)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Object create(@Nullable final Class<?> type) {
        return this.getDefault(type);
    }
    
    static {
        DEFAULT = fromArray(PrimitiveGenerator.INSTANCE, CollectionGenerator.INSTANCE);
    }
}
