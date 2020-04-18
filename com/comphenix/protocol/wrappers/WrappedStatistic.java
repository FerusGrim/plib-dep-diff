package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Map;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

public class WrappedStatistic extends AbstractWrapper
{
    private static final Class<?> STATISTIC;
    private static final Class<?> STATISTIC_LIST;
    private static MethodAccessor FIND_STATISTICS;
    private static FieldAccessor MAP_ACCESSOR;
    private static FieldAccessor GET_NAME;
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
        return map.values().stream().map((Function<? super Object, ?>)WrappedStatistic::fromHandle).collect((Collector<? super Object, ?, Iterable<WrappedStatistic>>)Collectors.toList());
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
        try {
            WrappedStatistic.FIND_STATISTICS = Accessors.getMethodAccessor(FuzzyReflection.fromClass(WrappedStatistic.STATISTIC_LIST).getMethodByParameters("findStatistic", WrappedStatistic.STATISTIC, new Class[] { String.class }));
            WrappedStatistic.MAP_ACCESSOR = Accessors.getFieldAccessor(WrappedStatistic.STATISTIC_LIST, Map.class, true);
            WrappedStatistic.GET_NAME = Accessors.getFieldAccessor(WrappedStatistic.STATISTIC, String.class, true);
        }
        catch (Exception ex) {}
    }
}
