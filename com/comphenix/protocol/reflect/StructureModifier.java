package com.comphenix.protocol.reflect;

import com.google.common.collect.ImmutableList;
import com.google.common.base.Objects;
import com.comphenix.protocol.reflect.compiler.BackgroundCompiler;
import java.util.Iterator;
import com.google.common.base.Function;
import java.lang.reflect.Modifier;
import java.util.Optional;
import com.comphenix.protocol.ProtocolLogger;
import java.util.logging.Level;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.error.PluginContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.ArrayList;
import com.comphenix.protocol.reflect.instances.InstanceProvider;
import java.util.Collection;
import com.comphenix.protocol.reflect.instances.BannedGenerator;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.collect.Lists;
import com.comphenix.protocol.reflect.instances.DefaultInstances;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.List;

public class StructureModifier<TField>
{
    protected Class targetType;
    protected Object target;
    protected EquivalentConverter<TField> converter;
    protected Class fieldType;
    protected List<Field> data;
    protected Map<Field, Integer> defaultFields;
    protected Map<Class, StructureModifier> subtypeCache;
    protected boolean customConvertHandling;
    protected boolean useStructureCompiler;
    private static DefaultInstances DEFAULT_GENERATOR;
    
    private static DefaultInstances getDefaultGenerator() {
        final List<InstanceProvider> providers = (List<InstanceProvider>)Lists.newArrayList();
        providers.add(new BannedGenerator((Class<?>[])new Class[] { MinecraftReflection.getItemStackClass(), MinecraftReflection.getBlockClass() }));
        providers.addAll((Collection<? extends InstanceProvider>)DefaultInstances.DEFAULT.getRegistered());
        return DefaultInstances.fromCollection(providers);
    }
    
    public StructureModifier(final Class targetType) {
        this(targetType, null, true);
    }
    
    public StructureModifier(final Class targetType, final boolean useStructureCompiler) {
        this(targetType, null, true, useStructureCompiler);
    }
    
    public StructureModifier(final Class targetType, final Class superclassExclude, final boolean requireDefault) {
        this(targetType, superclassExclude, requireDefault, true);
    }
    
    public StructureModifier(final Class targetType, final Class superclassExclude, final boolean requireDefault, final boolean useStructureCompiler) {
        this.data = new ArrayList<Field>();
        final List<Field> fields = getFields(targetType, superclassExclude);
        final Map<Field, Integer> defaults = requireDefault ? generateDefaultFields(fields) : new HashMap<Field, Integer>();
        this.initialize(targetType, Object.class, fields, defaults, null, new ConcurrentHashMap<Class, StructureModifier>(), useStructureCompiler);
    }
    
    protected StructureModifier() {
        this.data = new ArrayList<Field>();
    }
    
    protected void initialize(final StructureModifier<TField> other) {
        this.initialize(other.targetType, other.fieldType, other.data, other.defaultFields, other.converter, other.subtypeCache, other.useStructureCompiler);
    }
    
    protected void initialize(final Class targetType, final Class fieldType, final List<Field> data, final Map<Field, Integer> defaultFields, final EquivalentConverter<TField> converter, final Map<Class, StructureModifier> subTypeCache) {
        this.initialize(targetType, fieldType, data, defaultFields, converter, subTypeCache, true);
    }
    
    protected void initialize(final Class targetType, final Class fieldType, final List<Field> data, final Map<Field, Integer> defaultFields, final EquivalentConverter<TField> converter, final Map<Class, StructureModifier> subTypeCache, final boolean useStructureCompiler) {
        this.targetType = targetType;
        this.fieldType = fieldType;
        this.data = data;
        this.defaultFields = defaultFields;
        this.converter = converter;
        this.subtypeCache = subTypeCache;
        this.useStructureCompiler = useStructureCompiler;
    }
    
    public TField read(final int fieldIndex) throws FieldAccessException {
        try {
            return this.readInternal(fieldIndex);
        }
        catch (FieldAccessException ex) {
            final String plugin = PluginContext.getPluginCaller(ex);
            if (ProtocolLibrary.INCOMPATIBLE.contains(plugin)) {
                ProtocolLogger.log(Level.WARNING, "Encountered an exception caused by incompatible plugin {0}.", plugin);
                ProtocolLogger.log(Level.WARNING, "It is advised that you remove it.", new Object[0]);
            }
            throw ex;
        }
    }
    
    private TField readInternal(final int fieldIndex) throws FieldAccessException {
        if (this.target == null) {
            throw new IllegalStateException("Cannot read from a null target!");
        }
        if (fieldIndex < 0) {
            throw new FieldAccessException(String.format("Field index (%s) cannot be negative.", fieldIndex));
        }
        if (this.data.size() == 0) {
            throw new FieldAccessException(String.format("No field with type %s exists in class %s.", this.fieldType.getName(), this.target.getClass().getSimpleName()));
        }
        if (fieldIndex >= this.data.size()) {
            throw new FieldAccessException(String.format("Field index out of bounds. (Index: %s, Size: %s)", fieldIndex, this.data.size()));
        }
        try {
            final Object result = FieldUtils.readField(this.data.get(fieldIndex), this.target, true);
            if (this.needConversion()) {
                return this.converter.getSpecific(result);
            }
            return (TField)result;
        }
        catch (IllegalAccessException e) {
            throw new FieldAccessException("Cannot read field due to a security limitation.", e);
        }
    }
    
    public TField readSafely(final int fieldIndex) throws FieldAccessException {
        if (fieldIndex >= 0 && fieldIndex < this.data.size()) {
            return this.read(fieldIndex);
        }
        return null;
    }
    
    public Optional<TField> optionRead(final int fieldIndex) {
        try {
            return Optional.ofNullable(this.read(fieldIndex));
        }
        catch (FieldAccessException ex) {
            return Optional.empty();
        }
    }
    
    public boolean isReadOnly(final int fieldIndex) {
        return Modifier.isFinal(this.getField(fieldIndex).getModifiers());
    }
    
    public boolean isPublic(final int fieldIndex) {
        return Modifier.isPublic(this.getField(fieldIndex).getModifiers());
    }
    
    public void setReadOnly(final int fieldIndex, final boolean value) throws FieldAccessException {
        if (fieldIndex < 0 || fieldIndex >= this.data.size()) {
            throw new IllegalArgumentException("Index parameter is not within [0 - " + this.data.size() + ")");
        }
        try {
            setFinalState(this.data.get(fieldIndex), value);
        }
        catch (IllegalAccessException e) {
            throw new FieldAccessException("Cannot write read only status due to a security limitation.", e);
        }
    }
    
    protected static void setFinalState(final Field field, final boolean isReadOnly) throws IllegalAccessException {
        if (isReadOnly) {
            FieldUtils.writeField((Object)field, "modifiers", field.getModifiers() | 0x10, true);
        }
        else {
            FieldUtils.writeField((Object)field, "modifiers", field.getModifiers() & 0xFFFFFFEF, true);
        }
    }
    
    public StructureModifier<TField> write(final int fieldIndex, final TField value) throws FieldAccessException {
        try {
            return this.writeInternal(fieldIndex, value);
        }
        catch (FieldAccessException ex) {
            final String plugin = PluginContext.getPluginCaller(ex);
            if (ProtocolLibrary.INCOMPATIBLE.contains(plugin)) {
                ProtocolLogger.log(Level.WARNING, "Encountered an exception caused by incompatible plugin {0}.", plugin);
                ProtocolLogger.log(Level.WARNING, "It is advised that you remove it.", new Object[0]);
            }
            throw ex;
        }
    }
    
    private StructureModifier<TField> writeInternal(final int fieldIndex, final TField value) throws FieldAccessException {
        if (this.target == null) {
            throw new IllegalStateException("Cannot read from a null target!");
        }
        if (fieldIndex < 0) {
            throw new FieldAccessException(String.format("Field index (%s) cannot be negative.", fieldIndex));
        }
        if (this.data.size() == 0) {
            throw new FieldAccessException(String.format("No field with type %s exists in class %s.", this.fieldType.getName(), this.target.getClass().getSimpleName()));
        }
        if (fieldIndex >= this.data.size()) {
            throw new FieldAccessException(String.format("Field index out of bounds. (Index: %s, Size: %s)", fieldIndex, this.data.size()));
        }
        final Object obj = this.needConversion() ? this.converter.getGeneric(value) : value;
        try {
            FieldUtils.writeField(this.data.get(fieldIndex), this.target, obj, true);
        }
        catch (IllegalAccessException e) {
            throw new FieldAccessException("Cannot read field due to a security limitation.", e);
        }
        return this;
    }
    
    protected Class<?> getFieldType(final int index) {
        return this.data.get(index).getType();
    }
    
    private final boolean needConversion() {
        return this.converter != null && !this.customConvertHandling;
    }
    
    public StructureModifier<TField> writeSafely(final int fieldIndex, final TField value) throws FieldAccessException {
        if (fieldIndex >= 0 && fieldIndex < this.data.size()) {
            this.write(fieldIndex, value);
        }
        return this;
    }
    
    public StructureModifier<TField> modify(final int fieldIndex, final Function<TField, TField> select) throws FieldAccessException {
        final TField value = this.read(fieldIndex);
        return (StructureModifier<TField>)this.write(fieldIndex, select.apply((Object)value));
    }
    
    public <T> StructureModifier<T> withType(final Class fieldType) {
        return this.withType(fieldType, (EquivalentConverter<T>)null);
    }
    
    public StructureModifier<TField> writeDefaults() throws FieldAccessException {
        final DefaultInstances generator = DefaultInstances.DEFAULT;
        for (final Field field : this.defaultFields.keySet()) {
            try {
                if (field.getType().getCanonicalName().equals("net.md_5.bungee.api.chat.BaseComponent[]")) {
                    FieldUtils.writeField(field, this.target, null, true);
                }
                else {
                    FieldUtils.writeField(field, this.target, generator.getDefault(field.getType()), true);
                }
            }
            catch (IllegalAccessException e) {
                throw new FieldAccessException("Cannot write to field due to a security limitation.", e);
            }
        }
        return this;
    }
    
    public <T> StructureModifier<T> withType(final Class fieldType, final EquivalentConverter<T> converter) {
        if (fieldType == null) {
            return new StructureModifier<T>() {
                @Override
                public T read(final int index) {
                    return null;
                }
                
                @Override
                public StructureModifier<T> write(final int index, final T value) {
                    return this;
                }
            };
        }
        StructureModifier<T> result = this.subtypeCache.get(fieldType);
        if (result == null) {
            final List<Field> filtered = new ArrayList<Field>();
            final Map<Field, Integer> defaults = new HashMap<Field, Integer>();
            int index = 0;
            for (final Field field : this.data) {
                if (fieldType.isAssignableFrom(field.getType())) {
                    filtered.add(field);
                    if (this.defaultFields.containsKey(field)) {
                        defaults.put(field, index);
                    }
                }
                ++index;
            }
            result = this.withFieldType(fieldType, filtered, defaults);
            this.subtypeCache.put(fieldType, result);
            if (this.useStructureCompiler && BackgroundCompiler.getInstance() != null) {
                BackgroundCompiler.getInstance().scheduleCompilation(this.subtypeCache, fieldType);
            }
        }
        result = result.withTarget(this.target);
        if (!Objects.equal((Object)result.converter, (Object)converter)) {
            result = result.withConverter(converter);
        }
        return result;
    }
    
    public Class getFieldType() {
        return this.fieldType;
    }
    
    public Class getTargetType() {
        return this.targetType;
    }
    
    public Object getTarget() {
        return this.target;
    }
    
    public int size() {
        return this.data.size();
    }
    
    protected <T> StructureModifier<T> withFieldType(final Class fieldType, final List<Field> filtered, final Map<Field, Integer> defaults) {
        return this.withFieldType(fieldType, filtered, defaults, (EquivalentConverter<T>)null);
    }
    
    protected <T> StructureModifier<T> withFieldType(final Class fieldType, final List<Field> filtered, final Map<Field, Integer> defaults, final EquivalentConverter<T> converter) {
        final StructureModifier<T> result = new StructureModifier<T>();
        result.initialize(this.targetType, fieldType, filtered, defaults, converter, new ConcurrentHashMap<Class, StructureModifier<T>>(), this.useStructureCompiler);
        return result;
    }
    
    public StructureModifier<TField> withTarget(final Object target) {
        final StructureModifier<TField> copy = new StructureModifier<TField>();
        copy.initialize(this);
        copy.target = target;
        return copy;
    }
    
    private <T> StructureModifier<T> withConverter(final EquivalentConverter<T> converter) {
        final StructureModifier copy = this.withTarget(this.target);
        copy.setConverter(converter);
        return (StructureModifier<T>)copy;
    }
    
    protected void setConverter(final EquivalentConverter<TField> converter) {
        this.converter = converter;
    }
    
    public List<Field> getFields() {
        return (List<Field>)ImmutableList.copyOf((Collection)this.data);
    }
    
    public Field getField(final int fieldIndex) {
        if (fieldIndex < 0 || fieldIndex >= this.data.size()) {
            throw new IllegalArgumentException("Index parameter is not within [0 - " + this.data.size() + ")");
        }
        return this.data.get(fieldIndex);
    }
    
    public List<TField> getValues() throws FieldAccessException {
        final List<TField> values = new ArrayList<TField>();
        for (int i = 0; i < this.size(); ++i) {
            values.add(this.read(i));
        }
        return values;
    }
    
    private static Map<Field, Integer> generateDefaultFields(final List<Field> fields) {
        final Map<Field, Integer> requireDefaults = new HashMap<Field, Integer>();
        final DefaultInstances generator = StructureModifier.DEFAULT_GENERATOR;
        int index = 0;
        for (final Field field : fields) {
            final Class<?> type = field.getType();
            final int modifier = field.getModifiers();
            if (!type.isPrimitive() && !Modifier.isFinal(modifier) && generator.getDefault(type) != null) {
                requireDefaults.put(field, index);
            }
            ++index;
        }
        return requireDefaults;
    }
    
    private static List<Field> getFields(final Class type, final Class superclassExclude) {
        final List<Field> result = new ArrayList<Field>();
        for (final Field field : FuzzyReflection.fromClass(type, true).getDeclaredFields(superclassExclude)) {
            final int mod = field.getModifiers();
            if (!Modifier.isStatic(mod) && (superclassExclude == null || !field.getDeclaringClass().equals(superclassExclude))) {
                result.add(field);
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "StructureModifier[fieldType=" + this.fieldType + ", data=" + this.data + "]";
    }
    
    static {
        StructureModifier.DEFAULT_GENERATOR = getDefaultGenerator();
    }
}
