package com.comphenix.protocol.wrappers;

import java.util.function.Function;
import com.comphenix.protocol.reflect.EquivalentConverter;

public class Converters
{
    public static <T> EquivalentConverter<T> ignoreNull(final EquivalentConverter<T> converter) {
        return new EquivalentConverter<T>() {
            @Override
            public T getSpecific(final Object generic) {
                return (generic != null) ? converter.getSpecific(generic) : null;
            }
            
            @Override
            public Object getGeneric(final T specific) {
                return (specific != null) ? converter.getGeneric(specific) : null;
            }
            
            @Override
            public Class<T> getSpecificType() {
                return converter.getSpecificType();
            }
        };
    }
    
    public static <T> EquivalentConverter<T> passthrough(final Class<T> clazz) {
        return ignoreNull((EquivalentConverter<T>)new EquivalentConverter<T>() {
            @Override
            public T getSpecific(final Object generic) {
                return (T)generic;
            }
            
            @Override
            public Object getGeneric(final T specific) {
                return specific;
            }
            
            @Override
            public Class<T> getSpecificType() {
                return clazz;
            }
        });
    }
    
    public static <T> EquivalentConverter<T> handle(final Function<T, Object> toHandle, final Function<Object, T> fromHandle) {
        return new EquivalentConverter<T>() {
            @Override
            public T getSpecific(final Object generic) {
                return fromHandle.apply(generic);
            }
            
            @Override
            public Object getGeneric(final T specific) {
                return toHandle.apply(specific);
            }
            
            @Override
            public Class<T> getSpecificType() {
                return null;
            }
        };
    }
}
