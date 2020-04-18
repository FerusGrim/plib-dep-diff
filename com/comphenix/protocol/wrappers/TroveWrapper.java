package com.comphenix.protocol.wrappers;

import java.lang.reflect.Method;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMatchers;
import com.comphenix.protocol.reflect.FieldUtils;
import java.util.List;
import java.util.Set;
import java.util.Map;
import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import com.google.common.base.Function;
import com.comphenix.protocol.reflect.accessors.ReadOnlyFieldAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.ClassSource;

public class TroveWrapper
{
    private static final String[] TROVE_LOCATIONS;
    private static final ClassSource[] TROVE_SOURCES;
    
    public static ReadOnlyFieldAccessor wrapMapField(final FieldAccessor accessor) {
        return wrapMapField(accessor, null);
    }
    
    public static ReadOnlyFieldAccessor wrapMapField(final FieldAccessor accessor, final Function<Integer, Integer> noEntryTransform) {
        return new ReadOnlyFieldAccessor() {
            @Override
            public Object get(final Object instance) {
                final Object troveMap = accessor.get(instance);
                if (noEntryTransform != null) {
                    TroveWrapper.transformNoEntryValue(troveMap, noEntryTransform);
                }
                return TroveWrapper.getDecoratedMap(troveMap);
            }
            
            @Override
            public Field getField() {
                return accessor.getField();
            }
        };
    }
    
    public static ReadOnlyFieldAccessor wrapSetField(final FieldAccessor accessor) {
        return new ReadOnlyFieldAccessor() {
            @Override
            public Object get(final Object instance) {
                return TroveWrapper.getDecoratedSet(accessor.get(instance));
            }
            
            @Override
            public Field getField() {
                return accessor.getField();
            }
        };
    }
    
    public static ReadOnlyFieldAccessor wrapListField(final FieldAccessor accessor) {
        return new ReadOnlyFieldAccessor() {
            @Override
            public Object get(final Object instance) {
                return TroveWrapper.getDecoratedList(accessor.get(instance));
            }
            
            @Override
            public Field getField() {
                return accessor.getField();
            }
        };
    }
    
    public static <TKey, TValue> Map<TKey, TValue> getDecoratedMap(@Nonnull final Object troveMap) {
        final Map<TKey, TValue> result = (Map<TKey, TValue>)getDecorated(troveMap);
        return result;
    }
    
    public static <TValue> Set<TValue> getDecoratedSet(@Nonnull final Object troveSet) {
        final Set<TValue> result = (Set<TValue>)getDecorated(troveSet);
        return result;
    }
    
    public static <TValue> List<TValue> getDecoratedList(@Nonnull final Object troveList) {
        final List<TValue> result = (List<TValue>)getDecorated(troveList);
        return result;
    }
    
    public static boolean isTroveClass(final Class<?> clazz) {
        return getClassSource(clazz) != null;
    }
    
    public static void transformNoEntryValue(final Object troveMap, final Function<Integer, Integer> transform) {
        try {
            final Field field = FieldUtils.getField(troveMap.getClass(), "no_entry_value", true);
            final int current = (int)FieldUtils.readField(field, troveMap, true);
            final int transformed = (int)transform.apply((Object)current);
            if (current != transformed) {
                FieldUtils.writeField(field, troveMap, transformed);
            }
        }
        catch (IllegalArgumentException e) {
            throw new CannotFindTroveNoEntryValue((Throwable)e);
        }
        catch (IllegalAccessException e2) {
            throw new IllegalStateException("Cannot access reflection.", e2);
        }
    }
    
    private static ClassSource getClassSource(final Class<?> clazz) {
        for (int i = 0; i < TroveWrapper.TROVE_LOCATIONS.length; ++i) {
            if (clazz.getCanonicalName().startsWith(TroveWrapper.TROVE_LOCATIONS[i])) {
                return TroveWrapper.TROVE_SOURCES[i];
            }
        }
        return null;
    }
    
    private static Object getDecorated(@Nonnull final Object trove) {
        if (trove == null) {
            throw new IllegalArgumentException("trove instance cannot be non-null.");
        }
        final AbstractFuzzyMatcher<Class<?>> match = FuzzyMatchers.matchSuper(trove.getClass());
        Class<?> decorators = null;
        try {
            decorators = getClassSource(trove.getClass()).loadClass("TDecorators");
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        for (final Method method : decorators.getMethods()) {
            final Class<?>[] types = method.getParameterTypes();
            if (types.length == 1 && match.isMatch(types[0], null)) {
                try {
                    final Object result = method.invoke(null, trove);
                    if (result == null) {
                        throw new FieldAccessException("Wrapper returned NULL.");
                    }
                    return result;
                }
                catch (IllegalArgumentException e2) {
                    throw new FieldAccessException("Cannot invoke wrapper method.", e2);
                }
                catch (IllegalAccessException e3) {
                    throw new FieldAccessException("Illegal access.", e3);
                }
                catch (InvocationTargetException e4) {
                    throw new FieldAccessException("Error in invocation.", e4);
                }
            }
        }
        throw new IllegalArgumentException("Cannot find decorator for " + trove + " (" + trove.getClass() + ")");
    }
    
    static {
        TROVE_LOCATIONS = new String[] { "net.minecraft.util.gnu.trove", "gnu.trove" };
        TROVE_SOURCES = new ClassSource[] { ClassSource.fromPackage(TroveWrapper.TROVE_LOCATIONS[0]), ClassSource.fromPackage(TroveWrapper.TROVE_LOCATIONS[1]) };
    }
    
    public static class CannotFindTroveNoEntryValue extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        
        private CannotFindTroveNoEntryValue(final Throwable inner) {
            super("Cannot correct trove map.", inner);
        }
    }
}
