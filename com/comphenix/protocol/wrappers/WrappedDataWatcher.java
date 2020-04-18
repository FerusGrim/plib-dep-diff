package com.comphenix.protocol.wrappers;

import java.util.UUID;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Method;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Set;
import com.comphenix.protocol.wrappers.collection.ConvertedMap;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.util.Map;
import com.comphenix.protocol.reflect.accessors.Accessors;
import java.util.Iterator;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.List;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import org.bukkit.entity.Entity;
import com.google.common.collect.ImmutableBiMap;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

public class WrappedDataWatcher extends AbstractWrapper implements Iterable<WrappedWatchableObject>
{
    private static final Class<?> HANDLE_TYPE;
    private static MethodAccessor GETTER;
    private static MethodAccessor SETTER;
    private static MethodAccessor REGISTER;
    private static FieldAccessor ENTITY_DATA_FIELD;
    private static FieldAccessor ENTITY_FIELD;
    private static FieldAccessor MAP_FIELD;
    private static ConstructorAccessor constructor;
    private static ConstructorAccessor lightningConstructor;
    private static Object fakeEntity;
    private static final ImmutableBiMap<Class<?>, Integer> CLASS_TO_ID;
    
    public WrappedDataWatcher(final Object handle) {
        super(WrappedDataWatcher.HANDLE_TYPE);
        this.setHandle(handle);
    }
    
    public WrappedDataWatcher() {
        this(newHandle(fakeEntity()));
    }
    
    public WrappedDataWatcher(final Entity entity) {
        this(newHandle(BukkitUnwrapper.getInstance().unwrapItem(entity)));
    }
    
    public WrappedDataWatcher(final List<WrappedWatchableObject> objects) {
        this();
        if (MinecraftReflection.watcherObjectExists()) {
            for (final WrappedWatchableObject object : objects) {
                this.setObject(object.getWatcherObject(), object);
            }
        }
        else {
            for (final WrappedWatchableObject object : objects) {
                this.setObject(object.getIndex(), object);
            }
        }
    }
    
    private static Object newHandle(final Object entity) {
        if (WrappedDataWatcher.constructor == null) {
            WrappedDataWatcher.constructor = Accessors.getConstructorAccessor(WrappedDataWatcher.HANDLE_TYPE, MinecraftReflection.getEntityClass());
        }
        return WrappedDataWatcher.constructor.invoke(entity);
    }
    
    private static Object fakeEntity() {
        if (WrappedDataWatcher.fakeEntity != null) {
            return WrappedDataWatcher.fakeEntity;
        }
        if (WrappedDataWatcher.lightningConstructor == null) {
            WrappedDataWatcher.lightningConstructor = Accessors.getConstructorAccessor(MinecraftReflection.getMinecraftClass("EntityLightning"), MinecraftReflection.getNmsWorldClass(), Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE);
        }
        return WrappedDataWatcher.fakeEntity = WrappedDataWatcher.lightningConstructor.invoke(null, 0, 0, 0, true);
    }
    
    private Map<Integer, Object> getMap() {
        if (WrappedDataWatcher.MAP_FIELD == null) {
            final FuzzyReflection fuzzy = FuzzyReflection.fromClass(this.handleType, true);
            WrappedDataWatcher.MAP_FIELD = Accessors.getFieldAccessor(fuzzy.getField(FuzzyFieldContract.newBuilder().banModifier(8).typeDerivedOf(Map.class).build()));
        }
        if (WrappedDataWatcher.MAP_FIELD == null) {
            throw new FieldAccessException("Could not find index <-> Item map.");
        }
        return (Map<Integer, Object>)WrappedDataWatcher.MAP_FIELD.get(this.handle);
    }
    
    public Map<Integer, WrappedWatchableObject> asMap() {
        return new ConvertedMap<Integer, Object, WrappedWatchableObject>(this.getMap()) {
            @Override
            protected WrappedWatchableObject toOuter(final Object inner) {
                return (inner != null) ? new WrappedWatchableObject(inner) : null;
            }
            
            protected Object toInner(final WrappedWatchableObject outer) {
                return (outer != null) ? outer.getHandle() : null;
            }
        };
    }
    
    public Set<Integer> getIndexes() {
        return this.getMap().keySet();
    }
    
    public List<WrappedWatchableObject> getWatchableObjects() {
        return new ArrayList<WrappedWatchableObject>(this.asMap().values());
    }
    
    @Override
    public Iterator<WrappedWatchableObject> iterator() {
        return this.getWatchableObjects().iterator();
    }
    
    public int size() {
        return this.getMap().size();
    }
    
    public WrappedWatchableObject getWatchableObject(final int index) {
        final Object handle = this.getMap().get(index);
        if (handle != null) {
            return new WrappedWatchableObject(handle);
        }
        return null;
    }
    
    @Deprecated
    public WrappedWatchableObject removeObject(final int index) {
        return this.remove(index);
    }
    
    public WrappedWatchableObject remove(final int index) {
        final Object removed = this.getMap().remove(index);
        return (removed != null) ? new WrappedWatchableObject(removed) : null;
    }
    
    public boolean hasIndex(final int index) {
        return this.getObject(index) != null;
    }
    
    public Set<Integer> indexSet() {
        return this.getMap().keySet();
    }
    
    public void clear() {
        this.getMap().clear();
    }
    
    public Byte getByte(final int index) {
        return (Byte)this.getObject(index);
    }
    
    public Short getShort(final int index) {
        return (Short)this.getObject(index);
    }
    
    public Integer getInteger(final int index) {
        return (Integer)this.getObject(index);
    }
    
    public Float getFloat(final int index) {
        return (Float)this.getObject(index);
    }
    
    public String getString(final int index) {
        return (String)this.getObject(index);
    }
    
    public ItemStack getItemStack(final int index) {
        return (ItemStack)this.getObject(index);
    }
    
    public WrappedChunkCoordinate getChunkCoordinate(final int index) {
        return (WrappedChunkCoordinate)this.getObject(index);
    }
    
    public Object getObject(final int index) {
        return this.getObject(WrappedDataWatcherObject.fromIndex(index));
    }
    
    public Object getObject(final WrappedDataWatcherObject object) {
        Validate.notNull((Object)object, "Watcher object cannot be null!");
        if (WrappedDataWatcher.GETTER == null) {
            final FuzzyReflection fuzzy = FuzzyReflection.fromClass(this.handleType, true);
            if (MinecraftReflection.watcherObjectExists()) {
                WrappedDataWatcher.GETTER = Accessors.getMethodAccessor(fuzzy.getMethod(FuzzyMethodContract.newBuilder().parameterExactType(object.getHandleType()).returnTypeExact(Object.class).build(), "get"));
            }
            else {
                WrappedDataWatcher.GETTER = Accessors.getMethodAccessor(fuzzy.getMethod(FuzzyMethodContract.newBuilder().parameterExactType(Integer.TYPE).returnTypeExact(MinecraftReflection.getDataWatcherItemClass()).build()));
            }
        }
        try {
            final Object value = WrappedDataWatcher.GETTER.invoke(this.handle, object.getHandle());
            return WrappedWatchableObject.getWrapped(value);
        }
        catch (RuntimeException ex) {
            return null;
        }
    }
    
    public void setObject(final int index, final Object value, final boolean update) {
        if (MinecraftReflection.watcherObjectExists() && !this.hasIndex(index)) {
            throw new IllegalArgumentException("You cannot register objects without a watcher object!");
        }
        this.setObject(WrappedDataWatcherObject.fromIndex(index), value, update);
    }
    
    public void setObject(final int index, final Object value) {
        this.setObject(index, value, false);
    }
    
    public void setObject(final int index, final Serializer serializer, final Object value, final boolean update) {
        this.setObject(new WrappedDataWatcherObject(index, serializer), value, update);
    }
    
    public void setObject(final int index, final Serializer serializer, final Object value) {
        this.setObject(new WrappedDataWatcherObject(index, serializer), value, false);
    }
    
    public void setObject(final int index, final WrappedWatchableObject value, final boolean update) {
        this.setObject(index, value.getRawValue(), update);
    }
    
    public void setObject(final int index, final WrappedWatchableObject value) {
        this.setObject(index, value.getRawValue(), false);
    }
    
    public void setObject(final WrappedDataWatcherObject object, final WrappedWatchableObject value, final boolean update) {
        this.setObject(object, value.getRawValue(), update);
    }
    
    public void setObject(final WrappedDataWatcherObject object, final WrappedWatchableObject value) {
        this.setObject(object, value.getRawValue(), false);
    }
    
    public void setObject(final WrappedDataWatcherObject object, Object value, final boolean update) {
        Validate.notNull((Object)object, "Watcher object cannot be null!");
        if (WrappedDataWatcher.SETTER == null && WrappedDataWatcher.REGISTER == null) {
            final FuzzyReflection fuzzy = FuzzyReflection.fromClass(this.handleType, true);
            final FuzzyMethodContract contract = FuzzyMethodContract.newBuilder().banModifier(8).requireModifier(1).parameterExactArray(object.getHandleType(), Object.class).build();
            final List<Method> methods = fuzzy.getMethodList(contract);
            for (final Method method : methods) {
                if (method.getName().equals("set") || method.getName().equals("watch")) {
                    WrappedDataWatcher.SETTER = Accessors.getMethodAccessor(method);
                }
                else {
                    WrappedDataWatcher.REGISTER = Accessors.getMethodAccessor(method);
                }
            }
        }
        value = WrappedWatchableObject.getUnwrapped(value);
        if (this.hasIndex(object.getIndex())) {
            WrappedDataWatcher.SETTER.invoke(this.handle, object.getHandle(), value);
        }
        else {
            object.checkSerializer();
            WrappedDataWatcher.REGISTER.invoke(this.handle, object.getHandle(), value);
        }
        if (update) {
            this.getWatchableObject(object.getIndex()).setDirtyState(update);
        }
    }
    
    public void setObject(final WrappedDataWatcherObject object, final Object value) {
        this.setObject(object, value, false);
    }
    
    public WrappedDataWatcher deepClone() {
        final WrappedDataWatcher clone = new WrappedDataWatcher(this.getEntity());
        if (MinecraftReflection.watcherObjectExists()) {
            for (final WrappedWatchableObject wrapper : this) {
                clone.setObject(wrapper.getWatcherObject(), wrapper);
            }
        }
        else {
            for (final WrappedWatchableObject wrapper : this) {
                clone.setObject(wrapper.getIndex(), wrapper);
            }
        }
        return clone;
    }
    
    public static WrappedDataWatcher getEntityWatcher(final Entity entity) {
        if (WrappedDataWatcher.ENTITY_DATA_FIELD == null) {
            WrappedDataWatcher.ENTITY_DATA_FIELD = Accessors.getFieldAccessor(MinecraftReflection.getEntityClass(), MinecraftReflection.getDataWatcherClass(), true);
        }
        final BukkitUnwrapper unwrapper = new BukkitUnwrapper();
        final Object handle = WrappedDataWatcher.ENTITY_DATA_FIELD.get(unwrapper.unwrapItem(entity));
        return (handle != null) ? new WrappedDataWatcher(handle) : null;
    }
    
    public Entity getEntity() {
        if (WrappedDataWatcher.ENTITY_FIELD == null) {
            WrappedDataWatcher.ENTITY_FIELD = Accessors.getFieldAccessor(WrappedDataWatcher.HANDLE_TYPE, MinecraftReflection.getEntityClass(), true);
        }
        return (Entity)MinecraftReflection.getBukkitEntity(WrappedDataWatcher.ENTITY_FIELD.get(this.handle));
    }
    
    public void setEntity(final Entity entity) {
        if (WrappedDataWatcher.ENTITY_FIELD == null) {
            WrappedDataWatcher.ENTITY_FIELD = Accessors.getFieldAccessor(WrappedDataWatcher.HANDLE_TYPE, MinecraftReflection.getEntityClass(), true);
        }
        WrappedDataWatcher.ENTITY_FIELD.set(this.handle, BukkitUnwrapper.getInstance().unwrapItem(entity));
    }
    
    public static Integer getTypeID(final Class<?> clazz) {
        return (Integer)WrappedDataWatcher.CLASS_TO_ID.get((Object)clazz);
    }
    
    public static Class<?> getTypeClass(final int typeID) {
        return (Class<?>)WrappedDataWatcher.CLASS_TO_ID.inverse().get((Object)typeID);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WrappedDataWatcher)) {
            return false;
        }
        final WrappedDataWatcher other = (WrappedDataWatcher)obj;
        final Iterator<WrappedWatchableObject> first = this.iterator();
        final Iterator<WrappedWatchableObject> second = other.iterator();
        if (this.size() != other.size()) {
            return false;
        }
        while (first.hasNext() && second.hasNext()) {
            if (!first.next().equals(second.next())) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        return this.getWatchableObjects().hashCode();
    }
    
    @Override
    public String toString() {
        return "WrappedDataWatcher[handle=" + this.handle + "]";
    }
    
    static {
        HANDLE_TYPE = MinecraftReflection.getDataWatcherClass();
        WrappedDataWatcher.GETTER = null;
        WrappedDataWatcher.SETTER = null;
        WrappedDataWatcher.REGISTER = null;
        WrappedDataWatcher.ENTITY_DATA_FIELD = null;
        WrappedDataWatcher.ENTITY_FIELD = null;
        WrappedDataWatcher.MAP_FIELD = null;
        WrappedDataWatcher.constructor = null;
        WrappedDataWatcher.lightningConstructor = null;
        WrappedDataWatcher.fakeEntity = null;
        CLASS_TO_ID = new ImmutableBiMap.Builder().put((Object)Byte.class, (Object)0).put((Object)Short.class, (Object)1).put((Object)Integer.class, (Object)2).put((Object)Float.class, (Object)3).put((Object)String.class, (Object)4).put((Object)MinecraftReflection.getItemStackClass(), (Object)5).put((Object)MinecraftReflection.getBlockPositionClass(), (Object)6).put((Object)Vector3F.getMinecraftClass(), (Object)7).build();
    }
    
    public static class WrappedDataWatcherObject
    {
        private static final Class<?> HANDLE_TYPE;
        private static ConstructorAccessor constructor;
        private static MethodAccessor getSerializer;
        private StructureModifier<Object> modifier;
        private Object handle;
        
        protected WrappedDataWatcherObject() {
        }
        
        public WrappedDataWatcherObject(final Object handle) {
            this.handle = handle;
            this.modifier = new StructureModifier<Object>(WrappedDataWatcherObject.HANDLE_TYPE).withTarget(handle);
        }
        
        public WrappedDataWatcherObject(final int index, final Serializer serializer) {
            this(newHandle(index, serializer));
        }
        
        static WrappedDataWatcherObject fromIndex(final int index) {
            if (MinecraftReflection.watcherObjectExists()) {
                return new WrappedDataWatcherObject(index, null);
            }
            return new DummyWatcherObject(index);
        }
        
        private static Object newHandle(final int index, final Serializer serializer) {
            if (WrappedDataWatcherObject.constructor == null) {
                WrappedDataWatcherObject.constructor = Accessors.getConstructorAccessor(WrappedDataWatcherObject.HANDLE_TYPE.getConstructors()[0]);
            }
            final Object handle = (serializer != null) ? serializer.getHandle() : null;
            return WrappedDataWatcherObject.constructor.invoke(index, handle);
        }
        
        public int getIndex() {
            return this.modifier.read(0);
        }
        
        public Serializer getSerializer() {
            if (WrappedDataWatcherObject.getSerializer == null) {
                WrappedDataWatcherObject.getSerializer = Accessors.getMethodAccessor(FuzzyReflection.fromClass(WrappedDataWatcherObject.HANDLE_TYPE, true).getMethodByParameters("getSerializer", MinecraftReflection.getDataWatcherSerializerClass(), new Class[0]));
            }
            final Object serializer = WrappedDataWatcherObject.getSerializer.invoke(this.handle, new Object[0]);
            if (serializer == null) {
                return null;
            }
            final Serializer wrapper = Registry.fromHandle(serializer);
            if (wrapper != null) {
                return wrapper;
            }
            return new Serializer(null, serializer, false);
        }
        
        public void checkSerializer() {
            Validate.notNull((Object)this.getSerializer(), "You must specify a serializer to register an object!");
        }
        
        public Object getHandle() {
            return this.handle;
        }
        
        public Class<?> getHandleType() {
            return WrappedDataWatcherObject.HANDLE_TYPE;
        }
        
        @Override
        public String toString() {
            return "DataWatcherObject[index=" + this.getIndex() + ", serializer=" + this.getSerializer() + "]";
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof WrappedDataWatcherObject) {
                final WrappedDataWatcherObject other = (WrappedDataWatcherObject)obj;
                return this.handle.equals(other.handle);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return this.handle.hashCode();
        }
        
        static {
            HANDLE_TYPE = MinecraftReflection.getDataWatcherObjectClass();
            WrappedDataWatcherObject.constructor = null;
            WrappedDataWatcherObject.getSerializer = null;
        }
    }
    
    private static class DummyWatcherObject extends WrappedDataWatcherObject
    {
        private final int index;
        
        public DummyWatcherObject(final int index) {
            this.index = index;
        }
        
        @Override
        public int getIndex() {
            return this.index;
        }
        
        @Override
        public Serializer getSerializer() {
            return null;
        }
        
        @Override
        public Object getHandle() {
            return this.getIndex();
        }
        
        @Override
        public Class<?> getHandleType() {
            return Integer.TYPE;
        }
        
        @Override
        public void checkSerializer() {
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (obj instanceof DummyWatcherObject) {
                final DummyWatcherObject that = (DummyWatcherObject)obj;
                return this.index == that.index;
            }
            return false;
        }
    }
    
    public static class Serializer extends AbstractWrapper
    {
        private static final Class<?> HANDLE_TYPE;
        private final Class<?> type;
        private final boolean optional;
        
        public Serializer(final Class<?> type, final Object handle, final boolean optional) {
            super(Serializer.HANDLE_TYPE);
            this.type = type;
            this.optional = optional;
            this.setHandle(handle);
        }
        
        public Class<?> getType() {
            return this.type;
        }
        
        public boolean isOptional() {
            return this.optional;
        }
        
        @Override
        public String toString() {
            return "Serializer[type=" + this.type + ", handle=" + this.handle + ", optional=" + this.optional + "]";
        }
        
        static {
            HANDLE_TYPE = MinecraftReflection.getDataWatcherSerializerClass();
        }
    }
    
    public static class Registry
    {
        private static boolean INITIALIZED;
        private static List<Serializer> REGISTRY;
        
        public static Serializer get(final Class<?> clazz) {
            Validate.notNull((Object)"Class cannot be null!");
            initialize();
            for (final Serializer serializer : Registry.REGISTRY) {
                if (serializer.getType().equals(clazz)) {
                    return serializer;
                }
            }
            return null;
        }
        
        public static Serializer get(final Class<?> clazz, final boolean optional) {
            Validate.notNull((Object)clazz, "Class cannot be null!");
            initialize();
            for (final Serializer serializer : Registry.REGISTRY) {
                if (serializer.getType().equals(clazz) && serializer.isOptional() == optional) {
                    return serializer;
                }
            }
            return null;
        }
        
        public static Serializer fromHandle(final Object handle) {
            Validate.notNull((Object)"handle cannot be null!");
            initialize();
            for (final Serializer serializer : Registry.REGISTRY) {
                if (serializer.getHandle().equals(handle)) {
                    return serializer;
                }
            }
            return null;
        }
        
        private static void initialize() {
            if (!Registry.INITIALIZED) {
                Registry.INITIALIZED = true;
                final List<Field> candidates = FuzzyReflection.fromClass(MinecraftReflection.getDataWatcherRegistryClass(), true).getFieldListByType(MinecraftReflection.getDataWatcherSerializerClass());
                for (final Field candidate : candidates) {
                    final Type generic = candidate.getGenericType();
                    if (generic instanceof ParameterizedType) {
                        final ParameterizedType type = (ParameterizedType)generic;
                        final Type[] args = type.getActualTypeArguments();
                        final Type arg = args[0];
                        Class<?> innerClass = null;
                        boolean optional = false;
                        if (arg instanceof Class) {
                            innerClass = (Class<?>)arg;
                        }
                        else {
                            if (!(arg instanceof ParameterizedType)) {
                                throw new IllegalStateException("Failed to find inner class of field " + candidate);
                            }
                            innerClass = (Class<?>)((ParameterizedType)arg).getActualTypeArguments()[0];
                            optional = true;
                        }
                        Object serializer;
                        try {
                            serializer = candidate.get(null);
                        }
                        catch (ReflectiveOperationException e) {
                            throw new IllegalStateException("Failed to read field " + candidate);
                        }
                        Registry.REGISTRY.add(new Serializer(innerClass, serializer, optional));
                    }
                }
            }
        }
        
        public static Serializer getChatComponentSerializer() {
            return get(MinecraftReflection.getIChatBaseComponentClass());
        }
        
        public static Serializer getItemStackSerializer(final boolean optional) {
            return get(MinecraftReflection.getItemStackClass(), optional);
        }
        
        public static Serializer getBlockDataSerializer(final boolean optional) {
            return get(MinecraftReflection.getIBlockDataClass(), optional);
        }
        
        public static Serializer getVectorSerializer() {
            return get(Vector3F.getMinecraftClass());
        }
        
        public static Serializer getBlockPositionSerializer(final boolean optional) {
            return get(MinecraftReflection.getBlockPositionClass(), optional);
        }
        
        public static Serializer getDirectionSerializer() {
            return get(EnumWrappers.getDirectionClass());
        }
        
        public static Serializer getUUIDSerializer(final boolean optional) {
            return get(UUID.class, optional);
        }
        
        public static Serializer getNBTCompoundSerializer() {
            return get(MinecraftReflection.getNBTCompoundClass(), false);
        }
        
        static {
            Registry.INITIALIZED = false;
            Registry.REGISTRY = new ArrayList<Serializer>();
        }
    }
}
