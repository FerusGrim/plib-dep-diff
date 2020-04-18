package com.comphenix.protocol.wrappers;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.google.common.base.Objects;
import java.security.PublicKey;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;

public class WrappedSignedProperty extends AbstractWrapper
{
    private static final String CLASS_NAME = "com.mojang.authlib.properties.Property";
    private static final String UTIL_PACKAGE = "net.minecraft.util.";
    private static Class<?> PROPERTY;
    private static ConstructorAccessor CONSTRUCTOR;
    private static MethodAccessor GET_NAME;
    private static MethodAccessor GET_SIGNATURE;
    private static MethodAccessor GET_VALUE;
    private static MethodAccessor HAS_SIGNATURE;
    private static MethodAccessor IS_SIGNATURE_VALID;
    
    public WrappedSignedProperty(final String name, final String value, final String signature) {
        this(WrappedSignedProperty.CONSTRUCTOR.invoke(name, value, signature));
    }
    
    private WrappedSignedProperty(final Object handle) {
        super(WrappedSignedProperty.PROPERTY);
        this.setHandle(handle);
    }
    
    public static WrappedSignedProperty fromHandle(final Object handle) {
        return new WrappedSignedProperty(handle);
    }
    
    public static WrappedSignedProperty fromValues(final String name, final String value, final String signature) {
        return new WrappedSignedProperty(name, value, signature);
    }
    
    public String getName() {
        return (String)WrappedSignedProperty.GET_NAME.invoke(this.handle, new Object[0]);
    }
    
    public String getSignature() {
        return (String)WrappedSignedProperty.GET_SIGNATURE.invoke(this.handle, new Object[0]);
    }
    
    public String getValue() {
        return (String)WrappedSignedProperty.GET_VALUE.invoke(this.handle, new Object[0]);
    }
    
    public boolean hasSignature() {
        return (boolean)WrappedSignedProperty.HAS_SIGNATURE.invoke(this.handle, new Object[0]);
    }
    
    public boolean isSignatureValid(final PublicKey key) {
        return (boolean)WrappedSignedProperty.IS_SIGNATURE_VALID.invoke(this.handle, key);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.getName(), this.getValue(), this.getSignature() });
    }
    
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof WrappedSignedProperty) {
            final WrappedSignedProperty that = (WrappedSignedProperty)object;
            return Objects.equal((Object)this.getName(), (Object)that.getName()) && Objects.equal((Object)this.getValue(), (Object)that.getValue()) && Objects.equal((Object)this.getSignature(), (Object)that.getSignature());
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "WrappedSignedProperty[name=" + this.getName() + ", value=" + this.getValue() + ", signature=" + this.getSignature() + "]";
    }
    
    static {
        try {
            WrappedSignedProperty.PROPERTY = Class.forName("com.mojang.authlib.properties.Property");
        }
        catch (ClassNotFoundException ex3) {
            try {
                WrappedSignedProperty.PROPERTY = Class.forName("net.minecraft.util.com.mojang.authlib.properties.Property");
            }
            catch (ClassNotFoundException ex1) {
                throw new RuntimeException("Failed to obtain Property class.", ex1);
            }
        }
        try {
            WrappedSignedProperty.CONSTRUCTOR = Accessors.getConstructorAccessor(WrappedSignedProperty.PROPERTY, String.class, String.class, String.class);
            WrappedSignedProperty.GET_NAME = Accessors.getMethodAccessor(WrappedSignedProperty.PROPERTY, "getName", (Class<?>[])new Class[0]);
            WrappedSignedProperty.GET_SIGNATURE = Accessors.getMethodAccessor(WrappedSignedProperty.PROPERTY, "getSignature", (Class<?>[])new Class[0]);
            WrappedSignedProperty.GET_VALUE = Accessors.getMethodAccessor(WrappedSignedProperty.PROPERTY, "getValue", (Class<?>[])new Class[0]);
            WrappedSignedProperty.HAS_SIGNATURE = Accessors.getMethodAccessor(WrappedSignedProperty.PROPERTY, "hasSignature", (Class<?>[])new Class[0]);
            WrappedSignedProperty.IS_SIGNATURE_VALID = Accessors.getMethodAccessor(WrappedSignedProperty.PROPERTY, "isSignatureValid", PublicKey.class);
        }
        catch (Throwable ex2) {
            throw new RuntimeException("Failed to obtain methods for Property.", ex2);
        }
    }
}
