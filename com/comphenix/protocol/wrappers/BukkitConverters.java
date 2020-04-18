package com.comphenix.protocol.wrappers;

import com.google.common.base.Objects;
import com.comphenix.protocol.ProtocolLogger;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableMap;
import org.bukkit.advancement.Advancement;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import java.util.Optional;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import org.bukkit.entity.EntityType;
import com.comphenix.protocol.ProtocolManager;
import java.lang.ref.WeakReference;
import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.entity.Entity;
import org.bukkit.World;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.FuzzyReflection;
import org.bukkit.WorldType;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.util.function.Function;
import java.lang.reflect.Array;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import org.bukkit.Sound;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import com.comphenix.protocol.injector.PacketConstructor;
import java.util.List;
import com.comphenix.protocol.reflect.EquivalentConverter;
import java.util.Map;

public class BukkitConverters
{
    private static boolean hasWorldType;
    private static boolean hasAttributeSnapshot;
    private static Map<Class<?>, EquivalentConverter<Object>> genericConverters;
    private static List<PacketConstructor.Unwrapper> unwrappers;
    private static Method worldTypeName;
    private static Method worldTypeGetType;
    private static volatile Constructor<?> mobEffectConstructor;
    private static volatile StructureModifier<Object> mobEffectModifier;
    private static FieldAccessor craftWorldField;
    private static MethodAccessor getEntityTypeName;
    private static MethodAccessor entityTypeFromName;
    private static MethodAccessor BLOCK_FROM_MATERIAL;
    private static MethodAccessor MATERIAL_FROM_BLOCK;
    private static Constructor<?> vec3dConstructor;
    private static StructureModifier<Object> vec3dModifier;
    private static MethodAccessor getSound;
    private static MethodAccessor getSoundEffect;
    private static FieldAccessor soundKey;
    private static Map<String, Sound> soundIndex;
    private static MethodAccessor getMobEffectId;
    private static MethodAccessor getMobEffect;
    private static MethodAccessor dimensionFromId;
    private static MethodAccessor idFromDimension;
    
    public static <K, V> EquivalentConverter<Map<K, V>> getMapConverter(final EquivalentConverter<K> keyConverter, final EquivalentConverter<V> valConverter) {
        return new EquivalentConverter<Map<K, V>>() {
            @Override
            public Map<K, V> getSpecific(final Object generic) {
                final Map<Object, Object> genericMap = (Map<Object, Object>)generic;
                Map<K, V> newMap;
                try {
                    newMap = (Map<K, V>)genericMap.getClass().newInstance();
                }
                catch (ReflectiveOperationException ex) {
                    newMap = new HashMap<K, V>();
                }
                for (final Map.Entry<Object, Object> entry : genericMap.entrySet()) {
                    newMap.put(keyConverter.getSpecific(entry.getKey()), valConverter.getSpecific(entry.getValue()));
                }
                return newMap;
            }
            
            @Override
            public Object getGeneric(final Map<K, V> specific) {
                Map<Object, Object> newMap;
                try {
                    newMap = (Map<Object, Object>)specific.getClass().newInstance();
                }
                catch (ReflectiveOperationException ex) {
                    newMap = new HashMap<Object, Object>();
                }
                for (final Map.Entry<K, V> entry : specific.entrySet()) {
                    newMap.put(keyConverter.getGeneric(entry.getKey()), valConverter.getGeneric(entry.getValue()));
                }
                return newMap;
            }
            
            @Override
            public Class<Map<K, V>> getSpecificType() {
                return null;
            }
        };
    }
    
    public static <T> EquivalentConverter<List<T>> getListConverter(final EquivalentConverter<T> itemConverter) {
        return Converters.ignoreNull((EquivalentConverter<List<T>>)new EquivalentConverter<List<T>>() {
            @Override
            public List<T> getSpecific(final Object generic) {
                if (generic instanceof Collection) {
                    final List<T> items = new ArrayList<T>();
                    for (final Object item : (Collection)generic) {
                        final T result = itemConverter.getSpecific(item);
                        if (item != null) {
                            items.add(result);
                        }
                    }
                    return items;
                }
                return null;
            }
            
            @Override
            public Object getGeneric(final List<T> specific) {
                List<Object> newList;
                try {
                    newList = (List<Object>)specific.getClass().newInstance();
                }
                catch (ReflectiveOperationException ex) {
                    newList = new ArrayList<Object>();
                }
                for (final T position : specific) {
                    if (position != null) {
                        final Object converted = itemConverter.getGeneric(position);
                        if (converted == null) {
                            continue;
                        }
                        newList.add(converted);
                    }
                    else {
                        newList.add(null);
                    }
                }
                return newList;
            }
            
            @Override
            public Class<List<T>> getSpecificType() {
                final Class<?> dummy = List.class;
                return (Class<List<T>>)dummy;
            }
        });
    }
    
    @Deprecated
    public static <T> EquivalentConverter<Set<T>> getSetConverter(final Class<?> genericType, final EquivalentConverter<T> itemConverter) {
        if (itemConverter instanceof EnumWrappers.EnumConverter) {
            ((EnumWrappers.EnumConverter)itemConverter).setGenericType(genericType);
        }
        return getSetConverter(itemConverter);
    }
    
    public static <T> EquivalentConverter<Set<T>> getSetConverter(final EquivalentConverter<T> itemConverter) {
        return Converters.ignoreNull((EquivalentConverter<Set<T>>)new EquivalentConverter<Set<T>>() {
            @Override
            public Set<T> getSpecific(final Object generic) {
                if (generic instanceof Collection) {
                    final Set<T> items = new HashSet<T>();
                    for (final Object item : (Collection)generic) {
                        final T result = itemConverter.getSpecific(item);
                        if (item != null) {
                            items.add(result);
                        }
                    }
                    return items;
                }
                return null;
            }
            
            @Override
            public Object getGeneric(final Set<T> specific) {
                Set<Object> newList;
                try {
                    newList = (Set<Object>)specific.getClass().newInstance();
                }
                catch (ReflectiveOperationException ex) {
                    newList = new HashSet<Object>();
                }
                for (final T position : specific) {
                    if (position != null) {
                        final Object converted = itemConverter.getGeneric(position);
                        if (converted == null) {
                            continue;
                        }
                        newList.add(converted);
                    }
                    else {
                        newList.add(null);
                    }
                }
                return newList;
            }
            
            @Override
            public Class<Set<T>> getSpecificType() {
                final Class<?> dummy = Set.class;
                return (Class<Set<T>>)dummy;
            }
        });
    }
    
    public static <T> EquivalentConverter<Iterable<? extends T>> getArrayConverter(final Class<?> genericItemType, final EquivalentConverter<T> itemConverter) {
        return Converters.ignoreNull((EquivalentConverter<Iterable<? extends T>>)new EquivalentConverter<Iterable<? extends T>>() {
            @Override
            public List<T> getSpecific(final Object generic) {
                if (generic instanceof Object[]) {
                    final ImmutableList.Builder<T> builder = (ImmutableList.Builder<T>)ImmutableList.builder();
                    for (final Object item : (Object[])generic) {
                        final T result = itemConverter.getSpecific(item);
                        builder.add((Object)result);
                    }
                    return (List<T>)builder.build();
                }
                return null;
            }
            
            @Override
            public Object getGeneric(final Iterable<? extends T> specific) {
                final List<T> list = (List<T>)Lists.newArrayList((Iterable)specific);
                final Object[] output = (Object[])Array.newInstance(genericItemType, list.size());
                for (int i = 0; i < output.length; ++i) {
                    final Object converted = itemConverter.getGeneric(list.get(i));
                    output[i] = converted;
                }
                return output;
            }
            
            @Override
            public Class<Iterable<? extends T>> getSpecificType() {
                final Class<?> dummy = Iterable.class;
                return (Class<Iterable<? extends T>>)dummy;
            }
        });
    }
    
    public static EquivalentConverter<WrappedGameProfile> getWrappedGameProfileConverter() {
        return Converters.ignoreNull((EquivalentConverter<WrappedGameProfile>)Converters.handle(AbstractWrapper::getHandle, (Function<Object, T>)WrappedGameProfile::fromHandle));
    }
    
    public static EquivalentConverter<WrappedChatComponent> getWrappedChatComponentConverter() {
        return Converters.ignoreNull((EquivalentConverter<WrappedChatComponent>)Converters.handle(AbstractWrapper::getHandle, (Function<Object, T>)WrappedChatComponent::fromHandle));
    }
    
    public static EquivalentConverter<WrappedBlockData> getWrappedBlockDataConverter() {
        return Converters.ignoreNull((EquivalentConverter<WrappedBlockData>)Converters.handle(AbstractWrapper::getHandle, (Function<Object, T>)WrappedBlockData::fromHandle));
    }
    
    public static EquivalentConverter<WrappedAttribute> getWrappedAttributeConverter() {
        return Converters.ignoreNull((EquivalentConverter<WrappedAttribute>)Converters.handle(AbstractWrapper::getHandle, (Function<Object, T>)WrappedAttribute::fromHandle));
    }
    
    public static EquivalentConverter<WrappedWatchableObject> getWatchableObjectConverter() {
        return Converters.ignoreNull((EquivalentConverter<WrappedWatchableObject>)new EquivalentConverter<WrappedWatchableObject>() {
            @Override
            public Object getGeneric(final WrappedWatchableObject specific) {
                return specific.getHandle();
            }
            
            @Override
            public WrappedWatchableObject getSpecific(final Object generic) {
                if (MinecraftReflection.isWatchableObject(generic)) {
                    return new WrappedWatchableObject(generic);
                }
                if (generic instanceof WrappedWatchableObject) {
                    return (WrappedWatchableObject)generic;
                }
                throw new IllegalArgumentException("Unrecognized type " + generic.getClass());
            }
            
            @Override
            public Class<WrappedWatchableObject> getSpecificType() {
                return WrappedWatchableObject.class;
            }
        });
    }
    
    public static EquivalentConverter<WrappedDataWatcher> getDataWatcherConverter() {
        return Converters.ignoreNull((EquivalentConverter<WrappedDataWatcher>)new EquivalentConverter<WrappedDataWatcher>() {
            @Override
            public Object getGeneric(final WrappedDataWatcher specific) {
                return specific.getHandle();
            }
            
            @Override
            public WrappedDataWatcher getSpecific(final Object generic) {
                if (MinecraftReflection.isDataWatcher(generic)) {
                    return new WrappedDataWatcher(generic);
                }
                if (generic instanceof WrappedDataWatcher) {
                    return (WrappedDataWatcher)generic;
                }
                throw new IllegalArgumentException("Unrecognized type " + generic.getClass());
            }
            
            @Override
            public Class<WrappedDataWatcher> getSpecificType() {
                return WrappedDataWatcher.class;
            }
        });
    }
    
    public static EquivalentConverter<WorldType> getWorldTypeConverter() {
        if (!BukkitConverters.hasWorldType) {
            return null;
        }
        final Class<?> worldType = MinecraftReflection.getWorldTypeClass();
        return Converters.ignoreNull((EquivalentConverter<WorldType>)new EquivalentConverter<WorldType>() {
            @Override
            public Object getGeneric(final WorldType specific) {
                try {
                    if (BukkitConverters.worldTypeGetType == null) {
                        BukkitConverters.worldTypeGetType = FuzzyReflection.fromClass(worldType).getMethodByParameters("getType", worldType, new Class[] { String.class });
                    }
                    return BukkitConverters.worldTypeGetType.invoke(this, specific.getName());
                }
                catch (Exception e) {
                    throw new FieldAccessException("Cannot find the WorldType.getType() method.", e);
                }
            }
            
            @Override
            public WorldType getSpecific(final Object generic) {
                try {
                    if (BukkitConverters.worldTypeName == null) {
                        try {
                            BukkitConverters.worldTypeName = worldType.getMethod("name", (Class[])new Class[0]);
                        }
                        catch (Exception e) {
                            BukkitConverters.worldTypeName = FuzzyReflection.fromClass(worldType).getMethodByParameters("name", String.class, new Class[0]);
                        }
                    }
                    final String name = (String)BukkitConverters.worldTypeName.invoke(generic, new Object[0]);
                    return WorldType.getByName(name);
                }
                catch (Exception e) {
                    throw new FieldAccessException("Cannot call the name method in WorldType.", e);
                }
            }
            
            @Override
            public Class<WorldType> getSpecificType() {
                return WorldType.class;
            }
        });
    }
    
    public static EquivalentConverter<NbtBase<?>> getNbtConverter() {
        return Converters.ignoreNull((EquivalentConverter<NbtBase<?>>)new EquivalentConverter<NbtBase<?>>() {
            @Override
            public Object getGeneric(final NbtBase<?> specific) {
                return NbtFactory.fromBase(specific).getHandle();
            }
            
            @Override
            public NbtBase<?> getSpecific(final Object generic) {
                return NbtFactory.fromNMS(generic, null);
            }
            
            @Override
            public Class<NbtBase<?>> getSpecificType() {
                final Class<?> dummy = NbtBase.class;
                return (Class<NbtBase<?>>)dummy;
            }
        });
    }
    
    public static EquivalentConverter<Entity> getEntityConverter(final World world) {
        final WeakReference<ProtocolManager> managerRef = new WeakReference<ProtocolManager>(ProtocolLibrary.getProtocolManager());
        return new WorldSpecificConverter<Entity>(world) {
            @Override
            public Object getGeneric(final Entity specific) {
                return specific.getEntityId();
            }
            
            @Override
            public Entity getSpecific(final Object generic) {
                try {
                    final Integer id = (Integer)generic;
                    final ProtocolManager manager = (ProtocolManager)managerRef.get();
                    if (id != null && manager != null) {
                        return manager.getEntityFromID(this.world, id);
                    }
                    return null;
                }
                catch (FieldAccessException e) {
                    throw new RuntimeException("Cannot retrieve entity from ID.", e);
                }
            }
            
            @Override
            public Class<Entity> getSpecificType() {
                return Entity.class;
            }
        };
    }
    
    public static EquivalentConverter<EntityType> getEntityTypeConverter() {
        return Converters.ignoreNull((EquivalentConverter<EntityType>)new EquivalentConverter<EntityType>() {
            @Override
            public Object getGeneric(final EntityType specific) {
                if (BukkitConverters.entityTypeFromName == null) {
                    final Class<?> entityTypesClass = MinecraftReflection.getMinecraftClass("EntityTypes");
                    BukkitConverters.entityTypeFromName = Accessors.getMethodAccessor(FuzzyReflection.fromClass(entityTypesClass, false).getMethod(FuzzyMethodContract.newBuilder().returnDerivedOf(Optional.class).parameterExactArray(String.class).build()));
                }
                final Optional<?> opt = (Optional<?>)BukkitConverters.entityTypeFromName.invoke(null, specific.getName());
                return opt.orElse(null);
            }
            
            @Override
            public EntityType getSpecific(final Object generic) {
                if (BukkitConverters.getEntityTypeName == null) {
                    final Class<?> entityTypesClass = MinecraftReflection.getMinecraftClass("EntityTypes");
                    BukkitConverters.getEntityTypeName = Accessors.getMethodAccessor(FuzzyReflection.fromClass(entityTypesClass, false).getMethod(FuzzyMethodContract.newBuilder().returnTypeExact(MinecraftReflection.getMinecraftKeyClass()).parameterExactArray(entityTypesClass).build()));
                }
                final MinecraftKey key = MinecraftKey.fromHandle(BukkitConverters.getEntityTypeName.invoke(null, generic));
                return EntityType.fromName(key.getKey());
            }
            
            @Override
            public Class<EntityType> getSpecificType() {
                return EntityType.class;
            }
        });
    }
    
    public static EquivalentConverter<ItemStack> getItemStackConverter() {
        return new EquivalentConverter<ItemStack>() {
            @Override
            public ItemStack getSpecific(final Object generic) {
                return MinecraftReflection.getBukkitItemStack(generic);
            }
            
            @Override
            public Object getGeneric(final ItemStack specific) {
                return MinecraftReflection.getMinecraftItemStack(specific);
            }
            
            @Override
            public Class<ItemStack> getSpecificType() {
                return ItemStack.class;
            }
        };
    }
    
    public static EquivalentConverter<WrappedServerPing> getWrappedServerPingConverter() {
        return Converters.ignoreNull((EquivalentConverter<WrappedServerPing>)Converters.handle(AbstractWrapper::getHandle, (Function<Object, T>)WrappedServerPing::fromHandle));
    }
    
    public static EquivalentConverter<WrappedStatistic> getWrappedStatisticConverter() {
        return Converters.ignoreNull((EquivalentConverter<WrappedStatistic>)Converters.handle(AbstractWrapper::getHandle, (Function<Object, T>)WrappedStatistic::fromHandle));
    }
    
    public static EquivalentConverter<Material> getBlockConverter() {
        if (BukkitConverters.BLOCK_FROM_MATERIAL == null || BukkitConverters.MATERIAL_FROM_BLOCK == null) {
            final Class<?> magicNumbers = MinecraftReflection.getCraftBukkitClass("util.CraftMagicNumbers");
            final Class<?> block = MinecraftReflection.getBlockClass();
            final FuzzyReflection fuzzy = FuzzyReflection.fromClass(magicNumbers);
            FuzzyMethodContract.Builder builder = FuzzyMethodContract.newBuilder().requireModifier(8).returnTypeExact(Material.class).parameterExactArray(block);
            BukkitConverters.MATERIAL_FROM_BLOCK = Accessors.getMethodAccessor(fuzzy.getMethod(builder.build()));
            builder = FuzzyMethodContract.newBuilder().requireModifier(8).returnTypeExact(block).parameterExactArray(Material.class);
            BukkitConverters.BLOCK_FROM_MATERIAL = Accessors.getMethodAccessor(fuzzy.getMethod(builder.build()));
        }
        return Converters.ignoreNull((EquivalentConverter<Material>)new EquivalentConverter<Material>() {
            @Override
            public Object getGeneric(final Material specific) {
                return BukkitConverters.BLOCK_FROM_MATERIAL.invoke(null, specific);
            }
            
            @Override
            public Material getSpecific(final Object generic) {
                return (Material)BukkitConverters.MATERIAL_FROM_BLOCK.invoke(null, generic);
            }
            
            @Override
            public Class<Material> getSpecificType() {
                return Material.class;
            }
        });
    }
    
    public static EquivalentConverter<World> getWorldConverter() {
        return Converters.ignoreNull((EquivalentConverter<World>)new EquivalentConverter<World>() {
            @Override
            public Object getGeneric(final World specific) {
                return BukkitUnwrapper.getInstance().unwrapItem(specific);
            }
            
            @Override
            public World getSpecific(final Object generic) {
                return (World)BukkitConverters.craftWorldField.get(generic);
            }
            
            @Override
            public Class<World> getSpecificType() {
                return World.class;
            }
        });
    }
    
    public static EquivalentConverter<PotionEffect> getPotionEffectConverter() {
        return Converters.ignoreNull((EquivalentConverter<PotionEffect>)new EquivalentConverter<PotionEffect>() {
            @Override
            public Object getGeneric(final PotionEffect specific) {
                if (BukkitConverters.mobEffectConstructor == null) {
                    try {
                        BukkitConverters.mobEffectConstructor = MinecraftReflection.getMobEffectClass().getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE);
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Cannot find mob effect constructor (int, int, int, boolean).", e);
                    }
                }
                try {
                    return BukkitConverters.mobEffectConstructor.newInstance(specific.getType().getId(), specific.getDuration(), specific.getAmplifier(), specific.isAmbient());
                }
                catch (Exception e) {
                    throw new RuntimeException("Cannot construct MobEffect.", e);
                }
            }
            
            @Override
            public PotionEffect getSpecific(final Object generic) {
                if (BukkitConverters.mobEffectModifier == null) {
                    BukkitConverters.mobEffectModifier = (StructureModifier<Object>)new StructureModifier(MinecraftReflection.getMobEffectClass(), false);
                }
                final StructureModifier<Integer> ints = (StructureModifier<Integer>)BukkitConverters.mobEffectModifier.withTarget(generic).withType(Integer.TYPE);
                final StructureModifier<Boolean> bools = (StructureModifier<Boolean>)BukkitConverters.mobEffectModifier.withTarget(generic).withType(Boolean.TYPE);
                return new PotionEffect(PotionEffectType.getById((int)ints.read(0)), (int)ints.read(1), (int)ints.read(2), (boolean)bools.read(1));
            }
            
            @Override
            public Class<PotionEffect> getSpecificType() {
                return PotionEffect.class;
            }
        });
    }
    
    public static EquivalentConverter<Vector> getVectorConverter() {
        return Converters.ignoreNull((EquivalentConverter<Vector>)new EquivalentConverter<Vector>() {
            @Override
            public Class<Vector> getSpecificType() {
                return Vector.class;
            }
            
            @Override
            public Object getGeneric(final Vector specific) {
                if (BukkitConverters.vec3dConstructor == null) {
                    try {
                        BukkitConverters.vec3dConstructor = MinecraftReflection.getVec3DClass().getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);
                    }
                    catch (Throwable ex) {
                        throw new RuntimeException("Could not find Vec3d constructor (double, double, double)");
                    }
                }
                try {
                    return BukkitConverters.vec3dConstructor.newInstance(specific.getX(), specific.getY(), specific.getZ());
                }
                catch (Throwable ex) {
                    throw new RuntimeException("Could not construct Vec3d.", ex);
                }
            }
            
            @Override
            public Vector getSpecific(final Object generic) {
                if (BukkitConverters.vec3dModifier == null) {
                    BukkitConverters.vec3dModifier = (StructureModifier<Object>)new StructureModifier(MinecraftReflection.getVec3DClass(), false);
                }
                final StructureModifier<Double> doubles = (StructureModifier<Double>)BukkitConverters.vec3dModifier.withTarget(generic).withType(Double.TYPE);
                return new Vector((double)doubles.read(0), (double)doubles.read(1), (double)doubles.read(2));
            }
        });
    }
    
    public static EquivalentConverter<Sound> getSoundConverter() {
        if (BukkitConverters.getSound == null || BukkitConverters.getSoundEffect == null) {
            final Class<?> craftSound = MinecraftReflection.getCraftSoundClass();
            final FuzzyReflection fuzzy = FuzzyReflection.fromClass(craftSound, true);
            BukkitConverters.getSound = Accessors.getMethodAccessor(fuzzy.getMethodByParameters("getSound", String.class, new Class[] { Sound.class }));
            BukkitConverters.getSoundEffect = Accessors.getMethodAccessor(fuzzy.getMethodByParameters("getSoundEffect", MinecraftReflection.getSoundEffectClass(), new Class[] { String.class }));
        }
        return Converters.ignoreNull((EquivalentConverter<Sound>)new EquivalentConverter<Sound>() {
            @Override
            public Class<Sound> getSpecificType() {
                return Sound.class;
            }
            
            @Override
            public Object getGeneric(final Sound specific) {
                final String key = (String)BukkitConverters.getSound.invoke(null, specific);
                return BukkitConverters.getSoundEffect.invoke(null, key);
            }
            
            @Override
            public Sound getSpecific(final Object generic) {
                if (BukkitConverters.soundKey == null) {
                    final Class<?> soundEffect = generic.getClass();
                    final FuzzyReflection fuzzy = FuzzyReflection.fromClass(soundEffect, true);
                    BukkitConverters.soundKey = Accessors.getFieldAccessor(fuzzy.getFieldByType("key", MinecraftReflection.getMinecraftKeyClass()));
                }
                final MinecraftKey minecraftKey = MinecraftKey.fromHandle(BukkitConverters.soundKey.get(generic));
                final String key = minecraftKey.getKey();
                if (BukkitConverters.soundIndex != null) {
                    return BukkitConverters.soundIndex.get(key);
                }
                try {
                    return Sound.valueOf(minecraftKey.getEnumFormat());
                }
                catch (IllegalArgumentException ex) {
                    BukkitConverters.soundIndex = (Map<String, Sound>)new ConcurrentHashMap();
                    for (final Sound sound : Sound.values()) {
                        final String index = (String)BukkitConverters.getSound.invoke(null, sound);
                        BukkitConverters.soundIndex.put(index, sound);
                    }
                    return BukkitConverters.soundIndex.get(key);
                }
            }
        });
    }
    
    public static EquivalentConverter<WrappedParticle> getParticleConverter() {
        return (EquivalentConverter<WrappedParticle>)Converters.ignoreNull((EquivalentConverter<WrappedParticle>)Converters.handle(WrappedParticle::getHandle, (Function<Object, T>)WrappedParticle::fromHandle));
    }
    
    public static EquivalentConverter<Advancement> getAdvancementConverter() {
        return Converters.ignoreNull((EquivalentConverter<Advancement>)new EquivalentConverter<Advancement>() {
            @Override
            public Advancement getSpecific(final Object generic) {
                try {
                    return (Advancement)MinecraftReflection.getCraftBukkitClass("advancement.CraftAdvancement").getConstructor(MinecraftReflection.getMinecraftClass("Advancement")).newInstance(generic);
                }
                catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            }
            
            @Override
            public Object getGeneric(final Advancement specific) {
                return BukkitUnwrapper.getInstance().unwrapItem(specific);
            }
            
            @Override
            public Class<Advancement> getSpecificType() {
                return Advancement.class;
            }
        });
    }
    
    public static PacketConstructor.Unwrapper asUnwrapper(final Class<?> nativeType, final EquivalentConverter<Object> converter) {
        final Class<?> type;
        return wrappedObject -> {
            type = PacketConstructor.getClass(wrappedObject);
            if (converter.getSpecificType().isAssignableFrom(type)) {
                if (wrappedObject instanceof Class) {
                    return nativeType;
                }
                else {
                    return converter.getGeneric(wrappedObject);
                }
            }
            else {
                return null;
            }
        };
    }
    
    public static Map<Class<?>, EquivalentConverter<Object>> getConvertersForGeneric() {
        if (BukkitConverters.genericConverters == null) {
            final ImmutableMap.Builder<Class<?>, EquivalentConverter<Object>> builder = (ImmutableMap.Builder<Class<?>, EquivalentConverter<Object>>)ImmutableMap.builder();
            addConverter(builder, MinecraftReflection::getDataWatcherClass, BukkitConverters::getDataWatcherConverter);
            addConverter(builder, MinecraftReflection::getItemStackClass, BukkitConverters::getItemStackConverter);
            addConverter(builder, MinecraftReflection::getNBTBaseClass, BukkitConverters::getNbtConverter);
            addConverter(builder, MinecraftReflection::getNBTCompoundClass, BukkitConverters::getNbtConverter);
            addConverter(builder, MinecraftReflection::getDataWatcherItemClass, BukkitConverters::getWatchableObjectConverter);
            addConverter(builder, MinecraftReflection::getMobEffectClass, BukkitConverters::getPotionEffectConverter);
            addConverter(builder, MinecraftReflection::getNmsWorldClass, BukkitConverters::getWorldConverter);
            addConverter(builder, MinecraftReflection::getWorldTypeClass, BukkitConverters::getWorldTypeConverter);
            addConverter(builder, MinecraftReflection::getAttributeSnapshotClass, BukkitConverters::getWrappedAttributeConverter);
            addConverter(builder, MinecraftReflection::getBlockClass, BukkitConverters::getBlockConverter);
            addConverter(builder, MinecraftReflection::getGameProfileClass, BukkitConverters::getWrappedGameProfileConverter);
            addConverter(builder, MinecraftReflection::getServerPingClass, BukkitConverters::getWrappedServerPingConverter);
            addConverter(builder, MinecraftReflection::getStatisticClass, BukkitConverters::getWrappedStatisticConverter);
            addConverter(builder, MinecraftReflection::getIBlockDataClass, BukkitConverters::getWrappedBlockDataConverter);
            for (final Map.Entry<Class<?>, EquivalentConverter<?>> entry : EnumWrappers.getFromNativeMap().entrySet()) {
                addConverter(builder, entry::getKey, entry::getValue);
            }
            BukkitConverters.genericConverters = (Map<Class<?>, EquivalentConverter<Object>>)builder.build();
        }
        return BukkitConverters.genericConverters;
    }
    
    private static void addConverter(final ImmutableMap.Builder<Class<?>, EquivalentConverter<Object>> builder, final Supplier<Class<?>> getClass, final Supplier<EquivalentConverter> getConverter) {
        try {
            final Class<?> clazz = getClass.get();
            if (clazz != null) {
                final EquivalentConverter converter = getConverter.get();
                if (converter != null) {
                    builder.put((Object)clazz, (Object)converter);
                }
            }
        }
        catch (Exception ex) {
            ProtocolLogger.debug("Exception registering converter", ex);
        }
    }
    
    public static List<PacketConstructor.Unwrapper> getUnwrappers() {
        if (BukkitConverters.unwrappers == null) {
            final ImmutableList.Builder<PacketConstructor.Unwrapper> builder = (ImmutableList.Builder<PacketConstructor.Unwrapper>)ImmutableList.builder();
            for (final Map.Entry<Class<?>, EquivalentConverter<Object>> entry : getConvertersForGeneric().entrySet()) {
                builder.add((Object)asUnwrapper(entry.getKey(), entry.getValue()));
            }
            BukkitConverters.unwrappers = (List<PacketConstructor.Unwrapper>)builder.build();
        }
        return BukkitConverters.unwrappers;
    }
    
    public static EquivalentConverter<PotionEffectType> getEffectTypeConverter() {
        return Converters.ignoreNull((EquivalentConverter<PotionEffectType>)new EquivalentConverter<PotionEffectType>() {
            @Override
            public Class<PotionEffectType> getSpecificType() {
                return PotionEffectType.class;
            }
            
            @Override
            public Object getGeneric(final PotionEffectType specific) {
                final Class<?> clazz = MinecraftReflection.getMobEffectListClass();
                if (BukkitConverters.getMobEffect == null) {
                    BukkitConverters.getMobEffect = Accessors.getMethodAccessor(clazz, "fromId", Integer.TYPE);
                }
                final int id = specific.getId();
                return BukkitConverters.getMobEffect.invoke(null, id);
            }
            
            @Override
            public PotionEffectType getSpecific(final Object generic) {
                final Class<?> clazz = MinecraftReflection.getMobEffectListClass();
                if (BukkitConverters.getMobEffectId == null) {
                    BukkitConverters.getMobEffectId = Accessors.getMethodAccessor(clazz, "getId", clazz);
                }
                final int id = (int)BukkitConverters.getMobEffectId.invoke(null, generic);
                return PotionEffectType.getById(id);
            }
        });
    }
    
    public static EquivalentConverter<Integer> getDimensionIDConverter() {
        return new EquivalentConverter<Integer>() {
            @Override
            public Object getGeneric(final Integer specific) {
                if (BukkitConverters.dimensionFromId == null) {
                    final Class<?> clazz = MinecraftReflection.getMinecraftClass("DimensionManager");
                    final FuzzyReflection reflection = FuzzyReflection.fromClass(clazz, false);
                    final FuzzyMethodContract contract = FuzzyMethodContract.newBuilder().requireModifier(8).parameterExactType(Integer.TYPE).returnTypeExact(clazz).build();
                    BukkitConverters.dimensionFromId = Accessors.getMethodAccessor(reflection.getMethod(contract));
                }
                return BukkitConverters.dimensionFromId.invoke(null, specific);
            }
            
            @Override
            public Integer getSpecific(final Object generic) {
                if (BukkitConverters.idFromDimension == null) {
                    final Class<?> clazz = MinecraftReflection.getMinecraftClass("DimensionManager");
                    final FuzzyReflection reflection = FuzzyReflection.fromClass(clazz, false);
                    final FuzzyMethodContract contract = FuzzyMethodContract.newBuilder().banModifier(8).returnTypeExact(Integer.TYPE).parameterCount(0).build();
                    BukkitConverters.idFromDimension = Accessors.getMethodAccessor(reflection.getMethod(contract));
                }
                return (Integer)BukkitConverters.idFromDimension.invoke(generic, new Object[0]);
            }
            
            @Override
            public Class<Integer> getSpecificType() {
                return Integer.class;
            }
        };
    }
    
    static {
        BukkitConverters.hasWorldType = false;
        BukkitConverters.hasAttributeSnapshot = false;
        try {
            MinecraftReflection.getWorldTypeClass();
            BukkitConverters.hasWorldType = true;
        }
        catch (Exception ex) {}
        try {
            MinecraftReflection.getAttributeSnapshotClass();
            BukkitConverters.hasAttributeSnapshot = true;
        }
        catch (Exception ex2) {}
        try {
            BukkitConverters.craftWorldField = Accessors.getFieldAccessor(MinecraftReflection.getNmsWorldClass(), MinecraftReflection.getCraftWorldClass(), true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        BukkitConverters.getSound = null;
        BukkitConverters.getSoundEffect = null;
        BukkitConverters.soundKey = null;
        BukkitConverters.soundIndex = null;
        BukkitConverters.getMobEffectId = null;
        BukkitConverters.getMobEffect = null;
        BukkitConverters.dimensionFromId = null;
        BukkitConverters.idFromDimension = null;
    }
    
    @Deprecated
    public abstract static class IgnoreNullConverter<TType> implements EquivalentConverter<TType>
    {
        @Override
        public final Object getGeneric(final TType specific) {
            if (specific != null) {
                return this.getGenericValue(specific);
            }
            return null;
        }
        
        public abstract Object getGenericValue(final TType p0);
        
        @Override
        public final TType getSpecific(final Object generic) {
            if (generic != null) {
                return this.getSpecificValue(generic);
            }
            return null;
        }
        
        public abstract TType getSpecificValue(final Object p0);
        
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof EquivalentConverter) {
                final EquivalentConverter<?> that = (EquivalentConverter<?>)obj;
                return Objects.equal((Object)this.getSpecificType(), (Object)that.getSpecificType());
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(new Object[] { this.getSpecificType() });
        }
    }
    
    private abstract static class WorldSpecificConverter<TType> implements EquivalentConverter<TType>
    {
        protected World world;
        
        WorldSpecificConverter(final World world) {
            this.world = world;
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof WorldSpecificConverter && super.equals(obj)) {
                final WorldSpecificConverter<?> that = (WorldSpecificConverter<?>)obj;
                return Objects.equal((Object)this.world, (Object)that.world);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(new Object[] { this.getSpecificType(), this.world });
        }
    }
}
