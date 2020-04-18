package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.collect.Iterables;
import com.google.common.base.Function;
import java.util.Map;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

public class WrappedStatistic extends AbstractWrapper
{
    private static final Class<?> STATISTIC;
    private static final Class<?> STATISTIC_LIST;
    private static final MethodAccessor FIND_STATISTICS;
    private static final FieldAccessor MAP_ACCESSOR;
    private static final FieldAccessor GET_NAME;
    private final String name;
    
    private WrappedStatistic(final Object handle) {
        super(WrappedStatistic.STATISTIC);
        this.setHandle(handle);
        this.name = (String)WrappedStatistic.GET_NAME.get(handle);
    }
    
    public static WrappedStatistic fromHandle(final Object handle) {
        return new WrappedStatistic(handle);
    }
    
    public static WrappedStatistic fromName(final String name) {
        final Object handle = WrappedStatistic.FIND_STATISTICS.invoke(null, name);
        return (handle != null) ? fromHandle(handle) : null;
    }
    
    public static Iterable<WrappedStatistic> values() {
        final Map<Object, Object> map = (Map<Object, Object>)WrappedStatistic.MAP_ACCESSOR.get(null);
        return (Iterable<WrappedStatistic>)Iterables.transform((Iterable)map.values(), (Function)new Function<Object, WrappedStatistic>() {
            public WrappedStatistic apply(final Object handle) {
                return WrappedStatistic.fromHandle(handle);
            }
        });
    }
    
    public String getName() {
        return this.name;
    }
    
    @Override
    public String toString() {
        return String.valueOf(this.handle);
    }
    
    static {
        STATISTIC = MinecraftReflection.getStatisticClass();
        STATISTIC_LIST = MinecraftReflection.getStatisticListClass();
        FIND_STATISTICS = Accessors.getMethodAccessor(FuzzyReflection.fromClass(WrappedStatistic.STATISTIC_LIST).getMethodByParameters("findStatistic", WrappedStatistic.STATISTIC, new Class[] { String.class }));
        MAP_ACCESSOR = Accessors.getFieldAccessor(WrappedStatistic.STATISTIC_LIST, Map.class, true);
        GET_NAME = Accessors.getFieldAccessor(WrappedStatistic.STATISTIC, String.class, true);
    }
}
