package com.comphenix.net.sf.cglib.core;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.BitSet;
import java.util.HashMap;
import com.comphenix.net.sf.cglib.core.internal.CustomizerRegistry;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Arrays;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.asm.$Type;

public class EmitUtils
{
    private static final Signature CSTRUCT_NULL;
    private static final Signature CSTRUCT_THROWABLE;
    private static final Signature GET_NAME;
    private static final Signature HASH_CODE;
    private static final Signature EQUALS;
    private static final Signature STRING_LENGTH;
    private static final Signature STRING_CHAR_AT;
    private static final Signature FOR_NAME;
    private static final Signature DOUBLE_TO_LONG_BITS;
    private static final Signature FLOAT_TO_INT_BITS;
    private static final Signature TO_STRING;
    private static final Signature APPEND_STRING;
    private static final Signature APPEND_INT;
    private static final Signature APPEND_DOUBLE;
    private static final Signature APPEND_FLOAT;
    private static final Signature APPEND_CHAR;
    private static final Signature APPEND_LONG;
    private static final Signature APPEND_BOOLEAN;
    private static final Signature LENGTH;
    private static final Signature SET_LENGTH;
    private static final Signature GET_DECLARED_METHOD;
    public static final ArrayDelimiters DEFAULT_DELIMITERS;
    
    private EmitUtils() {
    }
    
    public static void factory_method(final ClassEmitter ce, final Signature sig) {
        final CodeEmitter e = ce.begin_method(1, sig, null);
        e.new_instance_this();
        e.dup();
        e.load_args();
        e.invoke_constructor_this(TypeUtils.parseConstructor(sig.getArgumentTypes()));
        e.return_value();
        e.end_method();
    }
    
    public static void null_constructor(final ClassEmitter ce) {
        final CodeEmitter e = ce.begin_method(1, EmitUtils.CSTRUCT_NULL, null);
        e.load_this();
        e.super_invoke_constructor();
        e.return_value();
        e.end_method();
    }
    
    public static void process_array(final CodeEmitter e, final $Type type, final ProcessArrayCallback callback) {
        final $Type componentType = TypeUtils.getComponentType(type);
        final Local array = e.make_local();
        final Local loopvar = e.make_local($Type.INT_TYPE);
        final $Label loopbody = e.make_label();
        final $Label checkloop = e.make_label();
        e.store_local(array);
        e.push(0);
        e.store_local(loopvar);
        e.goTo(checkloop);
        e.mark(loopbody);
        e.load_local(array);
        e.load_local(loopvar);
        e.array_load(componentType);
        callback.processElement(componentType);
        e.iinc(loopvar, 1);
        e.mark(checkloop);
        e.load_local(loopvar);
        e.load_local(array);
        e.arraylength();
        e.if_icmp(155, loopbody);
    }
    
    public static void process_arrays(final CodeEmitter e, final $Type type, final ProcessArrayCallback callback) {
        final $Type componentType = TypeUtils.getComponentType(type);
        final Local array1 = e.make_local();
        final Local array2 = e.make_local();
        final Local loopvar = e.make_local($Type.INT_TYPE);
        final $Label loopbody = e.make_label();
        final $Label checkloop = e.make_label();
        e.store_local(array1);
        e.store_local(array2);
        e.push(0);
        e.store_local(loopvar);
        e.goTo(checkloop);
        e.mark(loopbody);
        e.load_local(array1);
        e.load_local(loopvar);
        e.array_load(componentType);
        e.load_local(array2);
        e.load_local(loopvar);
        e.array_load(componentType);
        callback.processElement(componentType);
        e.iinc(loopvar, 1);
        e.mark(checkloop);
        e.load_local(loopvar);
        e.load_local(array1);
        e.arraylength();
        e.if_icmp(155, loopbody);
    }
    
    public static void string_switch(final CodeEmitter e, final String[] strings, final int switchStyle, final ObjectSwitchCallback callback) {
        try {
            switch (switchStyle) {
                case 0: {
                    string_switch_trie(e, strings, callback);
                    break;
                }
                case 1: {
                    string_switch_hash(e, strings, callback, false);
                    break;
                }
                case 2: {
                    string_switch_hash(e, strings, callback, true);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("unknown switch style " + switchStyle);
                }
            }
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Error ex2) {
            throw ex2;
        }
        catch (Exception ex3) {
            throw new CodeGenerationException(ex3);
        }
    }
    
    private static void string_switch_trie(final CodeEmitter e, final String[] strings, final ObjectSwitchCallback callback) throws Exception {
        final $Label def = e.make_label();
        final $Label end = e.make_label();
        final Map buckets = CollectionUtils.bucket(Arrays.asList(strings), new Transformer() {
            public Object transform(final Object value) {
                return new Integer(((String)value).length());
            }
        });
        e.dup();
        e.invoke_virtual(Constants.TYPE_STRING, EmitUtils.STRING_LENGTH);
        e.process_switch(getSwitchKeys(buckets), new ProcessSwitchCallback() {
            public void processCase(final int key, final $Label ignore_end) throws Exception {
                final List bucket = buckets.get(new Integer(key));
                stringSwitchHelper(e, bucket, callback, def, end, 0);
            }
            
            public void processDefault() {
                e.goTo(def);
            }
        });
        e.mark(def);
        e.pop();
        callback.processDefault();
        e.mark(end);
    }
    
    private static void stringSwitchHelper(final CodeEmitter e, final List strings, final ObjectSwitchCallback callback, final $Label def, final $Label end, final int index) throws Exception {
        final int len = strings.get(0).length();
        final Map buckets = CollectionUtils.bucket(strings, new Transformer() {
            public Object transform(final Object value) {
                return new Integer(((String)value).charAt(index));
            }
        });
        e.dup();
        e.push(index);
        e.invoke_virtual(Constants.TYPE_STRING, EmitUtils.STRING_CHAR_AT);
        e.process_switch(getSwitchKeys(buckets), new ProcessSwitchCallback() {
            public void processCase(final int key, final $Label ignore_end) throws Exception {
                final List bucket = buckets.get(new Integer(key));
                if (index + 1 == len) {
                    e.pop();
                    callback.processCase(bucket.get(0), end);
                }
                else {
                    stringSwitchHelper(e, bucket, callback, def, end, index + 1);
                }
            }
            
            public void processDefault() {
                e.goTo(def);
            }
        });
    }
    
    static int[] getSwitchKeys(final Map buckets) {
        final int[] keys = new int[buckets.size()];
        int index = 0;
        final Iterator it = buckets.keySet().iterator();
        while (it.hasNext()) {
            keys[index++] = it.next();
        }
        Arrays.sort(keys);
        return keys;
    }
    
    private static void string_switch_hash(final CodeEmitter e, final String[] strings, final ObjectSwitchCallback callback, final boolean skipEquals) throws Exception {
        final Map buckets = CollectionUtils.bucket(Arrays.asList(strings), new Transformer() {
            public Object transform(final Object value) {
                return new Integer(value.hashCode());
            }
        });
        final $Label def = e.make_label();
        final $Label end = e.make_label();
        e.dup();
        e.invoke_virtual(Constants.TYPE_OBJECT, EmitUtils.HASH_CODE);
        e.process_switch(getSwitchKeys(buckets), new ProcessSwitchCallback() {
            public void processCase(final int key, final $Label ignore_end) throws Exception {
                final List bucket = buckets.get(new Integer(key));
                $Label next = null;
                if (skipEquals && bucket.size() == 1) {
                    if (skipEquals) {
                        e.pop();
                    }
                    callback.processCase(bucket.get(0), end);
                }
                else {
                    final Iterator it = bucket.iterator();
                    while (it.hasNext()) {
                        final String string = it.next();
                        if (next != null) {
                            e.mark(next);
                        }
                        if (it.hasNext()) {
                            e.dup();
                        }
                        e.push(string);
                        e.invoke_virtual(Constants.TYPE_OBJECT, EmitUtils.EQUALS);
                        if (it.hasNext()) {
                            final CodeEmitter val$e = e;
                            final CodeEmitter val$e2 = e;
                            val$e.if_jump(153, next = e.make_label());
                            e.pop();
                        }
                        else {
                            final CodeEmitter val$e3 = e;
                            final CodeEmitter val$e4 = e;
                            val$e3.if_jump(153, def);
                        }
                        callback.processCase(string, end);
                    }
                }
            }
            
            public void processDefault() {
                e.pop();
            }
        });
        e.mark(def);
        callback.processDefault();
        e.mark(end);
    }
    
    public static void load_class_this(final CodeEmitter e) {
        load_class_helper(e, e.getClassEmitter().getClassType());
    }
    
    public static void load_class(final CodeEmitter e, final $Type type) {
        if (TypeUtils.isPrimitive(type)) {
            if (type == $Type.VOID_TYPE) {
                throw new IllegalArgumentException("cannot load void type");
            }
            e.getstatic(TypeUtils.getBoxedType(type), "TYPE", Constants.TYPE_CLASS);
        }
        else {
            load_class_helper(e, type);
        }
    }
    
    private static void load_class_helper(final CodeEmitter e, final $Type type) {
        if (e.isStaticHook()) {
            e.push(TypeUtils.emulateClassGetName(type));
            e.invoke_static(Constants.TYPE_CLASS, EmitUtils.FOR_NAME);
        }
        else {
            final ClassEmitter ce = e.getClassEmitter();
            final String typeName = TypeUtils.emulateClassGetName(type);
            final String fieldName = "CGLIB$load_class$" + TypeUtils.escapeType(typeName);
            if (!ce.isFieldDeclared(fieldName)) {
                ce.declare_field(26, fieldName, Constants.TYPE_CLASS, null);
                final CodeEmitter hook = ce.getStaticHook();
                hook.push(typeName);
                hook.invoke_static(Constants.TYPE_CLASS, EmitUtils.FOR_NAME);
                hook.putstatic(ce.getClassType(), fieldName, Constants.TYPE_CLASS);
            }
            e.getfield(fieldName);
        }
    }
    
    public static void push_array(final CodeEmitter e, final Object[] array) {
        e.push(array.length);
        e.newarray($Type.getType(remapComponentType(array.getClass().getComponentType())));
        for (int i = 0; i < array.length; ++i) {
            e.dup();
            e.push(i);
            push_object(e, array[i]);
            e.aastore();
        }
    }
    
    private static Class remapComponentType(final Class componentType) {
        if (componentType.equals($Type.class)) {
            return Class.class;
        }
        return componentType;
    }
    
    public static void push_object(final CodeEmitter e, final Object obj) {
        if (obj == null) {
            e.aconst_null();
        }
        else {
            final Class type = obj.getClass();
            if (type.isArray()) {
                push_array(e, (Object[])obj);
            }
            else if (obj instanceof String) {
                e.push((String)obj);
            }
            else if (obj instanceof $Type) {
                load_class(e, ($Type)obj);
            }
            else if (obj instanceof Class) {
                load_class(e, $Type.getType((Class)obj));
            }
            else if (obj instanceof BigInteger) {
                e.new_instance(Constants.TYPE_BIG_INTEGER);
                e.dup();
                e.push(obj.toString());
                e.invoke_constructor(Constants.TYPE_BIG_INTEGER);
            }
            else {
                if (!(obj instanceof BigDecimal)) {
                    throw new IllegalArgumentException("unknown type: " + obj.getClass());
                }
                e.new_instance(Constants.TYPE_BIG_DECIMAL);
                e.dup();
                e.push(obj.toString());
                e.invoke_constructor(Constants.TYPE_BIG_DECIMAL);
            }
        }
    }
    
    @Deprecated
    public static void hash_code(final CodeEmitter e, final $Type type, final int multiplier, final Customizer customizer) {
        hash_code(e, type, multiplier, CustomizerRegistry.singleton(customizer));
    }
    
    public static void hash_code(final CodeEmitter e, final $Type type, final int multiplier, final CustomizerRegistry registry) {
        if (TypeUtils.isArray(type)) {
            hash_array(e, type, multiplier, registry);
        }
        else {
            e.swap($Type.INT_TYPE, type);
            e.push(multiplier);
            e.math(104, $Type.INT_TYPE);
            e.swap(type, $Type.INT_TYPE);
            if (TypeUtils.isPrimitive(type)) {
                hash_primitive(e, type);
            }
            else {
                hash_object(e, type, registry);
            }
            e.math(96, $Type.INT_TYPE);
        }
    }
    
    private static void hash_array(final CodeEmitter e, final $Type type, final int multiplier, final CustomizerRegistry registry) {
        final $Label skip = e.make_label();
        final $Label end = e.make_label();
        e.dup();
        e.ifnull(skip);
        process_array(e, type, new ProcessArrayCallback() {
            public void processElement(final $Type type) {
                EmitUtils.hash_code(e, type, multiplier, registry);
            }
        });
        e.goTo(end);
        e.mark(skip);
        e.pop();
        e.mark(end);
    }
    
    private static void hash_object(final CodeEmitter e, final $Type type, final CustomizerRegistry registry) {
        final $Label skip = e.make_label();
        final $Label end = e.make_label();
        e.dup();
        e.ifnull(skip);
        boolean customHashCode = false;
        for (final HashCodeCustomizer customizer : registry.get(HashCodeCustomizer.class)) {
            if (customizer.customize(e, type)) {
                customHashCode = true;
                break;
            }
        }
        if (!customHashCode) {
            for (final Customizer customizer2 : registry.get(Customizer.class)) {
                customizer2.customize(e, type);
            }
            e.invoke_virtual(Constants.TYPE_OBJECT, EmitUtils.HASH_CODE);
        }
        e.goTo(end);
        e.mark(skip);
        e.pop();
        e.push(0);
        e.mark(end);
    }
    
    private static void hash_primitive(final CodeEmitter e, final $Type type) {
        switch (type.getSort()) {
            case 1: {
                e.push(1);
                e.math(130, $Type.INT_TYPE);
                break;
            }
            case 6: {
                e.invoke_static(Constants.TYPE_FLOAT, EmitUtils.FLOAT_TO_INT_BITS);
                break;
            }
            case 8: {
                e.invoke_static(Constants.TYPE_DOUBLE, EmitUtils.DOUBLE_TO_LONG_BITS);
            }
            case 7: {
                hash_long(e);
                break;
            }
        }
    }
    
    private static void hash_long(final CodeEmitter e) {
        e.dup2();
        e.push(32);
        e.math(124, $Type.LONG_TYPE);
        e.math(130, $Type.LONG_TYPE);
        e.cast_numeric($Type.LONG_TYPE, $Type.INT_TYPE);
    }
    
    @Deprecated
    public static void not_equals(final CodeEmitter e, final $Type type, final $Label notEquals, final Customizer customizer) {
        not_equals(e, type, notEquals, CustomizerRegistry.singleton(customizer));
    }
    
    public static void not_equals(final CodeEmitter e, final $Type type, final $Label notEquals, final CustomizerRegistry registry) {
        new ProcessArrayCallback() {
            public void processElement(final $Type type) {
                not_equals_helper(e, type, notEquals, registry, this);
            }
        }.processElement(type);
    }
    
    private static void not_equals_helper(final CodeEmitter e, final $Type type, final $Label notEquals, final CustomizerRegistry registry, final ProcessArrayCallback callback) {
        if (TypeUtils.isPrimitive(type)) {
            e.if_cmp(type, 154, notEquals);
        }
        else {
            final $Label end = e.make_label();
            nullcmp(e, notEquals, end);
            if (TypeUtils.isArray(type)) {
                final $Label checkContents = e.make_label();
                e.dup2();
                e.arraylength();
                e.swap();
                e.arraylength();
                e.if_icmp(153, checkContents);
                e.pop2();
                e.goTo(notEquals);
                e.mark(checkContents);
                process_arrays(e, type, callback);
            }
            else {
                final List<Customizer> customizers = registry.get(Customizer.class);
                if (!customizers.isEmpty()) {
                    for (final Customizer customizer : customizers) {
                        customizer.customize(e, type);
                    }
                    e.swap();
                    for (final Customizer customizer : customizers) {
                        customizer.customize(e, type);
                    }
                }
                e.invoke_virtual(Constants.TYPE_OBJECT, EmitUtils.EQUALS);
                e.if_jump(153, notEquals);
            }
            e.mark(end);
        }
    }
    
    private static void nullcmp(final CodeEmitter e, final $Label oneNull, final $Label bothNull) {
        e.dup2();
        final $Label nonNull = e.make_label();
        final $Label oneNullHelper = e.make_label();
        final $Label end = e.make_label();
        e.ifnonnull(nonNull);
        e.ifnonnull(oneNullHelper);
        e.pop2();
        e.goTo(bothNull);
        e.mark(nonNull);
        e.ifnull(oneNullHelper);
        e.goTo(end);
        e.mark(oneNullHelper);
        e.pop2();
        e.goTo(oneNull);
        e.mark(end);
    }
    
    @Deprecated
    public static void append_string(final CodeEmitter e, final $Type type, final ArrayDelimiters delims, final Customizer customizer) {
        append_string(e, type, delims, CustomizerRegistry.singleton(customizer));
    }
    
    public static void append_string(final CodeEmitter e, final $Type type, final ArrayDelimiters delims, final CustomizerRegistry registry) {
        final ArrayDelimiters d = (delims != null) ? delims : EmitUtils.DEFAULT_DELIMITERS;
        final ProcessArrayCallback callback = new ProcessArrayCallback() {
            public void processElement(final $Type type) {
                append_string_helper(e, type, d, registry, this);
                e.push(d.inside);
                e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_STRING);
            }
        };
        append_string_helper(e, type, d, registry, callback);
    }
    
    private static void append_string_helper(final CodeEmitter e, final $Type type, final ArrayDelimiters delims, final CustomizerRegistry registry, final ProcessArrayCallback callback) {
        final $Label skip = e.make_label();
        final $Label end = e.make_label();
        if (TypeUtils.isPrimitive(type)) {
            switch (type.getSort()) {
                case 3:
                case 4:
                case 5: {
                    e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_INT);
                    break;
                }
                case 8: {
                    e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_DOUBLE);
                    break;
                }
                case 6: {
                    e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_FLOAT);
                    break;
                }
                case 7: {
                    e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_LONG);
                    break;
                }
                case 1: {
                    e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_BOOLEAN);
                    break;
                }
                case 2: {
                    e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_CHAR);
                    break;
                }
            }
        }
        else if (TypeUtils.isArray(type)) {
            e.dup();
            e.ifnull(skip);
            e.swap();
            if (delims != null && delims.before != null && !"".equals(delims.before)) {
                e.push(delims.before);
                e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_STRING);
                e.swap();
            }
            process_array(e, type, callback);
            shrinkStringBuffer(e, 2);
            if (delims != null && delims.after != null && !"".equals(delims.after)) {
                e.push(delims.after);
                e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_STRING);
            }
        }
        else {
            e.dup();
            e.ifnull(skip);
            for (final Customizer customizer : registry.get(Customizer.class)) {
                customizer.customize(e, type);
            }
            e.invoke_virtual(Constants.TYPE_OBJECT, EmitUtils.TO_STRING);
            e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_STRING);
        }
        e.goTo(end);
        e.mark(skip);
        e.pop();
        e.push("null");
        e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.APPEND_STRING);
        e.mark(end);
    }
    
    private static void shrinkStringBuffer(final CodeEmitter e, final int amt) {
        e.dup();
        e.dup();
        e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.LENGTH);
        e.push(amt);
        e.math(100, $Type.INT_TYPE);
        e.invoke_virtual(Constants.TYPE_STRING_BUFFER, EmitUtils.SET_LENGTH);
    }
    
    public static void load_method(final CodeEmitter e, final MethodInfo method) {
        load_class(e, method.getClassInfo().getType());
        e.push(method.getSignature().getName());
        push_object(e, method.getSignature().getArgumentTypes());
        e.invoke_virtual(Constants.TYPE_CLASS, EmitUtils.GET_DECLARED_METHOD);
    }
    
    public static void method_switch(final CodeEmitter e, final List methods, final ObjectSwitchCallback callback) {
        member_switch_helper(e, methods, callback, true);
    }
    
    public static void constructor_switch(final CodeEmitter e, final List constructors, final ObjectSwitchCallback callback) {
        member_switch_helper(e, constructors, callback, false);
    }
    
    private static void member_switch_helper(final CodeEmitter e, final List members, final ObjectSwitchCallback callback, final boolean useName) {
        try {
            final Map cache = new HashMap();
            final ParameterTyper cached = new ParameterTyper() {
                public $Type[] getParameterTypes(final MethodInfo member) {
                    $Type[] types = cache.get(member);
                    if (types == null) {
                        cache.put(member, types = member.getSignature().getArgumentTypes());
                    }
                    return types;
                }
            };
            final $Label def = e.make_label();
            final $Label end = e.make_label();
            if (useName) {
                e.swap();
                final Map buckets = CollectionUtils.bucket(members, new Transformer() {
                    public Object transform(final Object value) {
                        return ((MethodInfo)value).getSignature().getName();
                    }
                });
                final String[] names = (String[])buckets.keySet().toArray(new String[buckets.size()]);
                string_switch(e, names, 1, new ObjectSwitchCallback() {
                    public void processCase(final Object key, final $Label dontUseEnd) throws Exception {
                        member_helper_size(e, buckets.get(key), callback, cached, def, end);
                    }
                    
                    public void processDefault() throws Exception {
                        e.goTo(def);
                    }
                });
            }
            else {
                member_helper_size(e, members, callback, cached, def, end);
            }
            e.mark(def);
            e.pop();
            callback.processDefault();
            e.mark(end);
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Error ex2) {
            throw ex2;
        }
        catch (Exception ex3) {
            throw new CodeGenerationException(ex3);
        }
    }
    
    private static void member_helper_size(final CodeEmitter e, final List members, final ObjectSwitchCallback callback, final ParameterTyper typer, final $Label def, final $Label end) throws Exception {
        final Map buckets = CollectionUtils.bucket(members, new Transformer() {
            public Object transform(final Object value) {
                return new Integer(typer.getParameterTypes((MethodInfo)value).length);
            }
        });
        e.dup();
        e.arraylength();
        e.process_switch(getSwitchKeys(buckets), new ProcessSwitchCallback() {
            public void processCase(final int key, final $Label dontUseEnd) throws Exception {
                final List bucket = buckets.get(new Integer(key));
                member_helper_type(e, bucket, callback, typer, def, end, new BitSet());
            }
            
            public void processDefault() throws Exception {
                e.goTo(def);
            }
        });
    }
    
    private static void member_helper_type(final CodeEmitter e, final List members, final ObjectSwitchCallback callback, final ParameterTyper typer, final $Label def, final $Label end, final BitSet checked) throws Exception {
        if (members.size() == 1) {
            final MethodInfo member = members.get(0);
            final $Type[] types = typer.getParameterTypes(member);
            for (int i = 0; i < types.length; ++i) {
                if (checked == null || !checked.get(i)) {
                    e.dup();
                    e.aaload(i);
                    e.invoke_virtual(Constants.TYPE_CLASS, EmitUtils.GET_NAME);
                    e.push(TypeUtils.emulateClassGetName(types[i]));
                    e.invoke_virtual(Constants.TYPE_OBJECT, EmitUtils.EQUALS);
                    e.if_jump(153, def);
                }
            }
            e.pop();
            callback.processCase(member, end);
        }
        else {
            final $Type[] example = typer.getParameterTypes(members.get(0));
            Map buckets = null;
            int index = -1;
            for (int j = 0; j < example.length; ++j) {
                final int k = j;
                final Map test = CollectionUtils.bucket(members, new Transformer() {
                    public Object transform(final Object value) {
                        return TypeUtils.emulateClassGetName(typer.getParameterTypes((MethodInfo)value)[k]);
                    }
                });
                if (buckets == null || test.size() > buckets.size()) {
                    buckets = test;
                    index = j;
                }
            }
            if (buckets == null || buckets.size() == 1) {
                e.goTo(def);
            }
            else {
                checked.set(index);
                e.dup();
                e.aaload(index);
                e.invoke_virtual(Constants.TYPE_CLASS, EmitUtils.GET_NAME);
                final Map fbuckets = buckets;
                final String[] names = (String[])buckets.keySet().toArray(new String[buckets.size()]);
                string_switch(e, names, 1, new ObjectSwitchCallback() {
                    public void processCase(final Object key, final $Label dontUseEnd) throws Exception {
                        member_helper_type(e, fbuckets.get(key), callback, typer, def, end, checked);
                    }
                    
                    public void processDefault() throws Exception {
                        e.goTo(def);
                    }
                });
            }
        }
    }
    
    public static void wrap_throwable(final Block block, final $Type wrapper) {
        final CodeEmitter e = block.getCodeEmitter();
        e.catch_exception(block, Constants.TYPE_THROWABLE);
        e.new_instance(wrapper);
        e.dup_x1();
        e.swap();
        e.invoke_constructor(wrapper, EmitUtils.CSTRUCT_THROWABLE);
        e.athrow();
    }
    
    public static void add_properties(final ClassEmitter ce, final String[] names, final $Type[] types) {
        for (int i = 0; i < names.length; ++i) {
            final String fieldName = "$cglib_prop_" + names[i];
            ce.declare_field(2, fieldName, types[i], null);
            add_property(ce, names[i], types[i], fieldName);
        }
    }
    
    public static void add_property(final ClassEmitter ce, final String name, final $Type type, final String fieldName) {
        final String property = TypeUtils.upperFirst(name);
        CodeEmitter e = ce.begin_method(1, new Signature("get" + property, type, Constants.TYPES_EMPTY), null);
        e.load_this();
        e.getfield(fieldName);
        e.return_value();
        e.end_method();
        e = ce.begin_method(1, new Signature("set" + property, $Type.VOID_TYPE, new $Type[] { type }), null);
        e.load_this();
        e.load_arg(0);
        e.putfield(fieldName);
        e.return_value();
        e.end_method();
    }
    
    public static void wrap_undeclared_throwable(final CodeEmitter e, final Block handler, final $Type[] exceptions, final $Type wrapper) {
        final Set set = (exceptions == null) ? Collections.EMPTY_SET : new HashSet(Arrays.asList(exceptions));
        if (set.contains(Constants.TYPE_THROWABLE)) {
            return;
        }
        boolean needThrow = exceptions != null;
        if (!set.contains(Constants.TYPE_RUNTIME_EXCEPTION)) {
            e.catch_exception(handler, Constants.TYPE_RUNTIME_EXCEPTION);
            needThrow = true;
        }
        if (!set.contains(Constants.TYPE_ERROR)) {
            e.catch_exception(handler, Constants.TYPE_ERROR);
            needThrow = true;
        }
        if (exceptions != null) {
            for (int i = 0; i < exceptions.length; ++i) {
                e.catch_exception(handler, exceptions[i]);
            }
        }
        if (needThrow) {
            e.athrow();
        }
        e.catch_exception(handler, Constants.TYPE_THROWABLE);
        e.new_instance(wrapper);
        e.dup_x1();
        e.swap();
        e.invoke_constructor(wrapper, EmitUtils.CSTRUCT_THROWABLE);
        e.athrow();
    }
    
    public static CodeEmitter begin_method(final ClassEmitter e, final MethodInfo method) {
        return begin_method(e, method, method.getModifiers());
    }
    
    public static CodeEmitter begin_method(final ClassEmitter e, final MethodInfo method, final int access) {
        return e.begin_method(access, method.getSignature(), method.getExceptionTypes());
    }
    
    static {
        CSTRUCT_NULL = TypeUtils.parseConstructor("");
        CSTRUCT_THROWABLE = TypeUtils.parseConstructor("Throwable");
        GET_NAME = TypeUtils.parseSignature("String getName()");
        HASH_CODE = TypeUtils.parseSignature("int hashCode()");
        EQUALS = TypeUtils.parseSignature("boolean equals(Object)");
        STRING_LENGTH = TypeUtils.parseSignature("int length()");
        STRING_CHAR_AT = TypeUtils.parseSignature("char charAt(int)");
        FOR_NAME = TypeUtils.parseSignature("Class forName(String)");
        DOUBLE_TO_LONG_BITS = TypeUtils.parseSignature("long doubleToLongBits(double)");
        FLOAT_TO_INT_BITS = TypeUtils.parseSignature("int floatToIntBits(float)");
        TO_STRING = TypeUtils.parseSignature("String toString()");
        APPEND_STRING = TypeUtils.parseSignature("StringBuffer append(String)");
        APPEND_INT = TypeUtils.parseSignature("StringBuffer append(int)");
        APPEND_DOUBLE = TypeUtils.parseSignature("StringBuffer append(double)");
        APPEND_FLOAT = TypeUtils.parseSignature("StringBuffer append(float)");
        APPEND_CHAR = TypeUtils.parseSignature("StringBuffer append(char)");
        APPEND_LONG = TypeUtils.parseSignature("StringBuffer append(long)");
        APPEND_BOOLEAN = TypeUtils.parseSignature("StringBuffer append(boolean)");
        LENGTH = TypeUtils.parseSignature("int length()");
        SET_LENGTH = TypeUtils.parseSignature("void setLength(int)");
        GET_DECLARED_METHOD = TypeUtils.parseSignature("java.lang.reflect.Method getDeclaredMethod(String, Class[])");
        DEFAULT_DELIMITERS = new ArrayDelimiters("{", ", ", "}");
    }
    
    public static class ArrayDelimiters
    {
        private String before;
        private String inside;
        private String after;
        
        public ArrayDelimiters(final String before, final String inside, final String after) {
            this.before = before;
            this.inside = inside;
            this.after = after;
        }
    }
    
    private interface ParameterTyper
    {
        $Type[] getParameterTypes(final MethodInfo p0);
    }
}
