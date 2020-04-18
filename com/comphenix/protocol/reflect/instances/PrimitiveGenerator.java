package com.comphenix.protocol.reflect.instances;

import java.lang.reflect.Array;
import com.google.common.primitives.Primitives;
import com.google.common.base.Defaults;
import javax.annotation.Nullable;

public class PrimitiveGenerator implements InstanceProvider
{
    public static final String STRING_DEFAULT = "";
    public static PrimitiveGenerator INSTANCE;
    private String stringDefault;
    
    public PrimitiveGenerator(final String stringDefault) {
        this.stringDefault = stringDefault;
    }
    
    public String getStringDefault() {
        return this.stringDefault;
    }
    
    @Override
    public Object create(@Nullable final Class<?> type) {
        if (type == null) {
            return null;
        }
        if (type.isPrimitive()) {
            return Defaults.defaultValue((Class)type);
        }
        if (Primitives.isWrapperType((Class)type)) {
            return Defaults.defaultValue(Primitives.unwrap((Class)type));
        }
        if (type.isArray()) {
            final Class<?> arrayType = type.getComponentType();
            return Array.newInstance(arrayType, 0);
        }
        if (type.isEnum()) {
            final Object[] values = (Object[])type.getEnumConstants();
            if (values != null && values.length > 0) {
                return values[0];
            }
        }
        else if (type.equals(String.class)) {
            return this.stringDefault;
        }
        return null;
    }
    
    static {
        PrimitiveGenerator.INSTANCE = new PrimitiveGenerator("");
    }
}
