package com.comphenix.net.sf.cglib.transform.impl;

import com.comphenix.net.sf.cglib.core.ObjectSwitchCallback;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.core.ProcessSwitchCallback;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.CodeGenerationException;
import java.util.HashMap;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import java.util.Map;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.transform.ClassEmitterTransformer;

public class FieldProviderTransformer extends ClassEmitterTransformer
{
    private static final String FIELD_NAMES = "CGLIB$FIELD_NAMES";
    private static final String FIELD_TYPES = "CGLIB$FIELD_TYPES";
    private static final $Type FIELD_PROVIDER;
    private static final $Type ILLEGAL_ARGUMENT_EXCEPTION;
    private static final Signature PROVIDER_GET;
    private static final Signature PROVIDER_SET;
    private static final Signature PROVIDER_SET_BY_INDEX;
    private static final Signature PROVIDER_GET_BY_INDEX;
    private static final Signature PROVIDER_GET_TYPES;
    private static final Signature PROVIDER_GET_NAMES;
    private int access;
    private Map fields;
    
    @Override
    public void begin_class(final int version, final int access, final String className, final $Type superType, $Type[] interfaces, final String sourceFile) {
        if (!TypeUtils.isAbstract(access)) {
            interfaces = TypeUtils.add(interfaces, FieldProviderTransformer.FIELD_PROVIDER);
        }
        this.access = access;
        this.fields = new HashMap();
        super.begin_class(version, access, className, superType, interfaces, sourceFile);
    }
    
    @Override
    public void declare_field(final int access, final String name, final $Type type, final Object value) {
        super.declare_field(access, name, type, value);
        if (!TypeUtils.isStatic(access)) {
            this.fields.put(name, type);
        }
    }
    
    @Override
    public void end_class() {
        if (!TypeUtils.isInterface(this.access)) {
            try {
                this.generate();
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e2) {
                throw new CodeGenerationException(e2);
            }
        }
        super.end_class();
    }
    
    private void generate() throws Exception {
        final String[] names = (String[])this.fields.keySet().toArray(new String[this.fields.size()]);
        final int[] indexes = new int[names.length];
        for (int i = 0; i < indexes.length; ++i) {
            indexes[i] = i;
        }
        super.declare_field(26, "CGLIB$FIELD_NAMES", Constants.TYPE_STRING_ARRAY, null);
        super.declare_field(26, "CGLIB$FIELD_TYPES", Constants.TYPE_CLASS_ARRAY, null);
        this.initFieldProvider(names);
        this.getNames();
        this.getTypes();
        this.getField(names);
        this.setField(names);
        this.setByIndex(names, indexes);
        this.getByIndex(names, indexes);
    }
    
    private void initFieldProvider(final String[] names) {
        final CodeEmitter e = this.getStaticHook();
        EmitUtils.push_object(e, names);
        e.putstatic(this.getClassType(), "CGLIB$FIELD_NAMES", Constants.TYPE_STRING_ARRAY);
        e.push(names.length);
        e.newarray(Constants.TYPE_CLASS);
        e.dup();
        for (int i = 0; i < names.length; ++i) {
            e.dup();
            e.push(i);
            final $Type type = this.fields.get(names[i]);
            EmitUtils.load_class(e, type);
            e.aastore();
        }
        e.putstatic(this.getClassType(), "CGLIB$FIELD_TYPES", Constants.TYPE_CLASS_ARRAY);
    }
    
    private void getNames() {
        final CodeEmitter e = super.begin_method(1, FieldProviderTransformer.PROVIDER_GET_NAMES, null);
        e.getstatic(this.getClassType(), "CGLIB$FIELD_NAMES", Constants.TYPE_STRING_ARRAY);
        e.return_value();
        e.end_method();
    }
    
    private void getTypes() {
        final CodeEmitter e = super.begin_method(1, FieldProviderTransformer.PROVIDER_GET_TYPES, null);
        e.getstatic(this.getClassType(), "CGLIB$FIELD_TYPES", Constants.TYPE_CLASS_ARRAY);
        e.return_value();
        e.end_method();
    }
    
    private void setByIndex(final String[] names, final int[] indexes) throws Exception {
        final CodeEmitter e = super.begin_method(1, FieldProviderTransformer.PROVIDER_SET_BY_INDEX, null);
        e.load_this();
        e.load_arg(1);
        e.load_arg(0);
        e.process_switch(indexes, new ProcessSwitchCallback() {
            public void processCase(final int key, final $Label end) throws Exception {
                final $Type type = FieldProviderTransformer.this.fields.get(names[key]);
                e.unbox(type);
                e.putfield(names[key]);
                e.return_value();
            }
            
            public void processDefault() throws Exception {
                e.throw_exception(FieldProviderTransformer.ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field index");
            }
        });
        e.end_method();
    }
    
    private void getByIndex(final String[] names, final int[] indexes) throws Exception {
        final CodeEmitter e = super.begin_method(1, FieldProviderTransformer.PROVIDER_GET_BY_INDEX, null);
        e.load_this();
        e.load_arg(0);
        e.process_switch(indexes, new ProcessSwitchCallback() {
            public void processCase(final int key, final $Label end) throws Exception {
                final $Type type = FieldProviderTransformer.this.fields.get(names[key]);
                e.getfield(names[key]);
                e.box(type);
                e.return_value();
            }
            
            public void processDefault() throws Exception {
                e.throw_exception(FieldProviderTransformer.ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field index");
            }
        });
        e.end_method();
    }
    
    private void getField(final String[] names) throws Exception {
        final CodeEmitter e = this.begin_method(1, FieldProviderTransformer.PROVIDER_GET, null);
        e.load_this();
        e.load_arg(0);
        EmitUtils.string_switch(e, names, 1, new ObjectSwitchCallback() {
            public void processCase(final Object key, final $Label end) {
                final $Type type = FieldProviderTransformer.this.fields.get(key);
                e.getfield((String)key);
                e.box(type);
                e.return_value();
            }
            
            public void processDefault() {
                e.throw_exception(FieldProviderTransformer.ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field name");
            }
        });
        e.end_method();
    }
    
    private void setField(final String[] names) throws Exception {
        final CodeEmitter e = this.begin_method(1, FieldProviderTransformer.PROVIDER_SET, null);
        e.load_this();
        e.load_arg(1);
        e.load_arg(0);
        EmitUtils.string_switch(e, names, 1, new ObjectSwitchCallback() {
            public void processCase(final Object key, final $Label end) {
                final $Type type = FieldProviderTransformer.this.fields.get(key);
                e.unbox(type);
                e.putfield((String)key);
                e.return_value();
            }
            
            public void processDefault() {
                e.throw_exception(FieldProviderTransformer.ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field name");
            }
        });
        e.end_method();
    }
    
    static {
        FIELD_PROVIDER = TypeUtils.parseType("com.comphenix.net.sf.cglib.transform.impl.FieldProvider");
        ILLEGAL_ARGUMENT_EXCEPTION = TypeUtils.parseType("IllegalArgumentException");
        PROVIDER_GET = TypeUtils.parseSignature("Object getField(String)");
        PROVIDER_SET = TypeUtils.parseSignature("void setField(String, Object)");
        PROVIDER_SET_BY_INDEX = TypeUtils.parseSignature("void setField(int, Object)");
        PROVIDER_GET_BY_INDEX = TypeUtils.parseSignature("Object getField(int)");
        PROVIDER_GET_TYPES = TypeUtils.parseSignature("Class[] getFieldTypes()");
        PROVIDER_GET_NAMES = TypeUtils.parseSignature("String[] getFieldNames()");
    }
}
