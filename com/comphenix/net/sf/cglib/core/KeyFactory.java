package com.comphenix.net.sf.cglib.core;

import com.comphenix.net.sf.cglib.asm.$Label;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import java.security.ProtectionDomain;
import com.comphenix.net.sf.cglib.core.internal.CustomizerRegistry;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import com.comphenix.net.sf.cglib.asm.$Type;

public abstract class KeyFactory
{
    private static final Signature GET_NAME;
    private static final Signature GET_CLASS;
    private static final Signature HASH_CODE;
    private static final Signature EQUALS;
    private static final Signature TO_STRING;
    private static final Signature APPEND_STRING;
    private static final $Type KEY_FACTORY;
    private static final Signature GET_SORT;
    private static final int[] PRIMES;
    public static final Customizer CLASS_BY_NAME;
    public static final FieldTypeCustomizer STORE_CLASS_AS_STRING;
    public static final HashCodeCustomizer HASH_ASM_TYPE;
    @Deprecated
    public static final Customizer OBJECT_BY_CLASS;
    
    protected KeyFactory() {
    }
    
    public static KeyFactory create(final Class keyInterface) {
        return create(keyInterface, null);
    }
    
    public static KeyFactory create(final Class keyInterface, final Customizer customizer) {
        return create(keyInterface.getClassLoader(), keyInterface, customizer);
    }
    
    public static KeyFactory create(final Class keyInterface, final KeyFactoryCustomizer first, final List<KeyFactoryCustomizer> next) {
        return create(keyInterface.getClassLoader(), keyInterface, first, next);
    }
    
    public static KeyFactory create(final ClassLoader loader, final Class keyInterface, final Customizer customizer) {
        return create(loader, keyInterface, customizer, Collections.emptyList());
    }
    
    public static KeyFactory create(final ClassLoader loader, final Class keyInterface, final KeyFactoryCustomizer customizer, final List<KeyFactoryCustomizer> next) {
        final Generator gen = new Generator();
        gen.setInterface(keyInterface);
        if (customizer != null) {
            gen.addCustomizer(customizer);
        }
        if (next != null && !next.isEmpty()) {
            for (final KeyFactoryCustomizer keyFactoryCustomizer : next) {
                gen.addCustomizer(keyFactoryCustomizer);
            }
        }
        gen.setClassLoader(loader);
        return gen.create();
    }
    
    static {
        GET_NAME = TypeUtils.parseSignature("String getName()");
        GET_CLASS = TypeUtils.parseSignature("Class getClass()");
        HASH_CODE = TypeUtils.parseSignature("int hashCode()");
        EQUALS = TypeUtils.parseSignature("boolean equals(Object)");
        TO_STRING = TypeUtils.parseSignature("String toString()");
        APPEND_STRING = TypeUtils.parseSignature("StringBuffer append(String)");
        KEY_FACTORY = TypeUtils.parseType("com.comphenix.net.sf.cglib.core.KeyFactory");
        GET_SORT = TypeUtils.parseSignature("int getSort()");
        PRIMES = new int[] { 11, 73, 179, 331, 521, 787, 1213, 1823, 2609, 3691, 5189, 7247, 10037, 13931, 19289, 26627, 36683, 50441, 69403, 95401, 131129, 180179, 247501, 340057, 467063, 641371, 880603, 1209107, 1660097, 2279161, 3129011, 4295723, 5897291, 8095873, 11114263, 15257791, 20946017, 28754629, 39474179, 54189869, 74391461, 102123817, 140194277, 192456917, 264202273, 362693231, 497900099, 683510293, 938313161, 1288102441, 1768288259 };
        CLASS_BY_NAME = new Customizer() {
            public void customize(final CodeEmitter e, final $Type type) {
                if (type.equals(Constants.TYPE_CLASS)) {
                    e.invoke_virtual(Constants.TYPE_CLASS, KeyFactory.GET_NAME);
                }
            }
        };
        STORE_CLASS_AS_STRING = new FieldTypeCustomizer() {
            public void customize(final CodeEmitter e, final int index, final $Type type) {
                if (type.equals(Constants.TYPE_CLASS)) {
                    e.invoke_virtual(Constants.TYPE_CLASS, KeyFactory.GET_NAME);
                }
            }
            
            public $Type getOutType(final int index, final $Type type) {
                if (type.equals(Constants.TYPE_CLASS)) {
                    return Constants.TYPE_STRING;
                }
                return type;
            }
        };
        HASH_ASM_TYPE = new HashCodeCustomizer() {
            public boolean customize(final CodeEmitter e, final $Type type) {
                if (Constants.TYPE_TYPE.equals(type)) {
                    e.invoke_virtual(type, KeyFactory.GET_SORT);
                    return true;
                }
                return false;
            }
        };
        OBJECT_BY_CLASS = new Customizer() {
            public void customize(final CodeEmitter e, final $Type type) {
                e.invoke_virtual(Constants.TYPE_OBJECT, KeyFactory.GET_CLASS);
            }
        };
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private static final Class[] KNOWN_CUSTOMIZER_TYPES;
        private Class keyInterface;
        private CustomizerRegistry customizers;
        private int constant;
        private int multiplier;
        
        public Generator() {
            super(Generator.SOURCE);
            this.customizers = new CustomizerRegistry(Generator.KNOWN_CUSTOMIZER_TYPES);
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return this.keyInterface.getClassLoader();
        }
        
        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(this.keyInterface);
        }
        
        @Deprecated
        public void setCustomizer(final Customizer customizer) {
            this.customizers = CustomizerRegistry.singleton(customizer);
        }
        
        public void addCustomizer(final KeyFactoryCustomizer customizer) {
            this.customizers.add(customizer);
        }
        
        public <T> List<T> getCustomizers(final Class<T> klass) {
            return this.customizers.get(klass);
        }
        
        public void setInterface(final Class keyInterface) {
            this.keyInterface = keyInterface;
        }
        
        public KeyFactory create() {
            this.setNamePrefix(this.keyInterface.getName());
            return (KeyFactory)super.create(this.keyInterface.getName());
        }
        
        public void setHashConstant(final int constant) {
            this.constant = constant;
        }
        
        public void setHashMultiplier(final int multiplier) {
            this.multiplier = multiplier;
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            return ReflectUtils.newInstance(type);
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return instance;
        }
        
        public void generateClass(final $ClassVisitor v) {
            final ClassEmitter ce = new ClassEmitter(v);
            final Method newInstance = ReflectUtils.findNewInstance(this.keyInterface);
            if (!newInstance.getReturnType().equals(Object.class)) {
                throw new IllegalArgumentException("newInstance method must return Object");
            }
            final $Type[] parameterTypes = TypeUtils.getTypes(newInstance.getParameterTypes());
            ce.begin_class(46, 1, this.getClassName(), KeyFactory.KEY_FACTORY, new $Type[] { $Type.getType(this.keyInterface) }, "<generated>");
            EmitUtils.null_constructor(ce);
            EmitUtils.factory_method(ce, ReflectUtils.getSignature(newInstance));
            int seed = 0;
            CodeEmitter e = ce.begin_method(1, TypeUtils.parseConstructor(parameterTypes), null);
            e.load_this();
            e.super_invoke_constructor();
            e.load_this();
            final List<FieldTypeCustomizer> fieldTypeCustomizers = this.getCustomizers(FieldTypeCustomizer.class);
            for (int i = 0; i < parameterTypes.length; ++i) {
                $Type fieldType;
                final $Type parameterType = fieldType = parameterTypes[i];
                for (final FieldTypeCustomizer customizer : fieldTypeCustomizers) {
                    fieldType = customizer.getOutType(i, fieldType);
                }
                seed += fieldType.hashCode();
                ce.declare_field(18, this.getFieldName(i), fieldType, null);
                e.dup();
                e.load_arg(i);
                for (final FieldTypeCustomizer customizer : fieldTypeCustomizers) {
                    customizer.customize(e, i, parameterType);
                }
                e.putfield(this.getFieldName(i));
            }
            e.return_value();
            e.end_method();
            e = ce.begin_method(1, KeyFactory.HASH_CODE, null);
            final int hc = (this.constant != 0) ? this.constant : KeyFactory.PRIMES[Math.abs(seed) % KeyFactory.PRIMES.length];
            final int hm = (this.multiplier != 0) ? this.multiplier : KeyFactory.PRIMES[Math.abs(seed * 13) % KeyFactory.PRIMES.length];
            e.push(hc);
            for (int j = 0; j < parameterTypes.length; ++j) {
                e.load_this();
                e.getfield(this.getFieldName(j));
                EmitUtils.hash_code(e, parameterTypes[j], hm, this.customizers);
            }
            e.return_value();
            e.end_method();
            e = ce.begin_method(1, KeyFactory.EQUALS, null);
            final $Label fail = e.make_label();
            e.load_arg(0);
            e.instance_of_this();
            e.if_jump(153, fail);
            for (int k = 0; k < parameterTypes.length; ++k) {
                e.load_this();
                e.getfield(this.getFieldName(k));
                e.load_arg(0);
                e.checkcast_this();
                e.getfield(this.getFieldName(k));
                EmitUtils.not_equals(e, parameterTypes[k], fail, this.customizers);
            }
            e.push(1);
            e.return_value();
            e.mark(fail);
            e.push(0);
            e.return_value();
            e.end_method();
            e = ce.begin_method(1, KeyFactory.TO_STRING, null);
            e.new_instance(Constants.TYPE_STRING_BUFFER);
            e.dup();
            e.invoke_constructor(Constants.TYPE_STRING_BUFFER);
            for (int k = 0; k < parameterTypes.length; ++k) {
                if (k > 0) {
                    e.push(", ");
                    e.invoke_virtual(Constants.TYPE_STRING_BUFFER, KeyFactory.APPEND_STRING);
                }
                e.load_this();
                e.getfield(this.getFieldName(k));
                EmitUtils.append_string(e, parameterTypes[k], EmitUtils.DEFAULT_DELIMITERS, this.customizers);
            }
            e.invoke_virtual(Constants.TYPE_STRING_BUFFER, KeyFactory.TO_STRING);
            e.return_value();
            e.end_method();
            ce.end_class();
        }
        
        private String getFieldName(final int arg) {
            return "FIELD_" + arg;
        }
        
        static {
            SOURCE = new Source(KeyFactory.class.getName());
            KNOWN_CUSTOMIZER_TYPES = new Class[] { Customizer.class, FieldTypeCustomizer.class };
        }
    }
}
