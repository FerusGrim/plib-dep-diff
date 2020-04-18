package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.EquivalentConverter;
import java.lang.reflect.Constructor;

public class Vector3F
{
    protected float x;
    protected float y;
    protected float z;
    private static Constructor<?> constructor;
    private static Class<?> clazz;
    
    public Vector3F() {
        this(0.0f, 0.0f, 0.0f);
    }
    
    public Vector3F(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public float getX() {
        return this.x;
    }
    
    public Vector3F setX(final float x) {
        this.x = x;
        return this;
    }
    
    public float getY() {
        return this.y;
    }
    
    public Vector3F setY(final float y) {
        this.y = y;
        return this;
    }
    
    public float getZ() {
        return this.z;
    }
    
    public Vector3F setZ(final float z) {
        this.z = z;
        return this;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = 31 * result + Float.floatToIntBits(this.x);
        result = 31 * result + Float.floatToIntBits(this.y);
        result = 31 * result + Float.floatToIntBits(this.z);
        return result;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Vector3F) {
            final Vector3F that = (Vector3F)obj;
            return Float.floatToIntBits(this.x) == Float.floatToIntBits(that.x) && Float.floatToIntBits(this.y) == Float.floatToIntBits(that.y) && Float.floatToIntBits(this.z) == Float.floatToIntBits(that.z);
        }
        return false;
    }
    
    public static Class<?> getMinecraftClass() {
        return Vector3F.clazz;
    }
    
    public static EquivalentConverter<Vector3F> getConverter() {
        return Converters.ignoreNull((EquivalentConverter<Vector3F>)new EquivalentConverter<Vector3F>() {
            @Override
            public Class<Vector3F> getSpecificType() {
                return Vector3F.class;
            }
            
            @Override
            public Object getGeneric(final Vector3F specific) {
                if (Vector3F.constructor == null) {
                    try {
                        Vector3F.constructor = (Constructor<?>)Vector3F.clazz.getConstructor(Float.TYPE, Float.TYPE, Float.TYPE);
                    }
                    catch (ReflectiveOperationException ex) {
                        throw new RuntimeException("Failed to find constructor for Vector3f", ex);
                    }
                }
                try {
                    return Vector3F.constructor.newInstance(specific.x, specific.y, specific.z);
                }
                catch (ReflectiveOperationException ex) {
                    throw new RuntimeException("Failed to create new instance of Vector3f", ex);
                }
            }
            
            @Override
            public Vector3F getSpecific(final Object generic) {
                final StructureModifier<Float> modifier = (StructureModifier<Float>)new StructureModifier(generic.getClass()).withTarget(generic).withType(Float.TYPE);
                final float x = modifier.read(0);
                final float y = modifier.read(1);
                final float z = modifier.read(2);
                return new Vector3F(x, y, z);
            }
        });
    }
    
    static {
        Vector3F.constructor = null;
        Vector3F.clazz = MinecraftReflection.getMinecraftClass("Vector3f");
    }
}
