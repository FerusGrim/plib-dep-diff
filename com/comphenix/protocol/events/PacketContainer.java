package com.comphenix.protocol.events;

import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import java.lang.reflect.Array;
import com.google.common.collect.Sets;
import com.comphenix.protocol.reflect.cloning.CollectionCloner;
import com.comphenix.protocol.reflect.cloning.GuavaOptionalCloner;
import com.comphenix.protocol.reflect.cloning.JavaOptionalCloner;
import com.comphenix.protocol.reflect.cloning.ImmutableDetector;
import com.comphenix.protocol.reflect.cloning.BukkitCloner;
import com.comphenix.protocol.reflect.instances.DefaultInstances;
import com.google.common.collect.Maps;
import com.comphenix.protocol.reflect.MethodInfo;
import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.util.function.Supplier;
import java.util.Optional;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.io.DataInputStream;
import java.io.DataInput;
import java.io.InputStream;
import java.io.ObjectInputStream;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataOutput;
import java.io.OutputStream;
import com.comphenix.protocol.utility.MinecraftMethods;
import java.io.ObjectOutputStream;
import com.comphenix.protocol.reflect.ObjectWriter;
import com.comphenix.protocol.reflect.instances.InstanceProvider;
import com.comphenix.protocol.reflect.cloning.FieldCloner;
import javax.annotation.Nullable;
import com.comphenix.protocol.reflect.cloning.Cloner;
import com.google.common.base.Function;
import com.comphenix.protocol.reflect.cloning.SerializableCloner;
import com.comphenix.protocol.wrappers.MinecraftKey;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;
import com.comphenix.protocol.wrappers.WrappedParticle;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Material;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.comphenix.protocol.wrappers.WrappedAttribute;
import org.bukkit.util.Vector;
import java.util.Collection;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkPosition;
import org.bukkit.entity.EntityType;
import com.google.common.base.Preconditions;
import org.bukkit.entity.Entity;
import javax.annotation.Nonnull;
import org.bukkit.World;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.WorldType;
import com.comphenix.protocol.wrappers.WrappedStatistic;
import java.util.Map;
import java.util.List;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.inventory.ItemStack;
import com.comphenix.protocol.utility.StreamSerializer;
import java.util.UUID;
import com.comphenix.protocol.injector.StructureCache;
import java.util.Set;
import com.comphenix.protocol.reflect.cloning.AggregateCloner;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentMap;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.PacketType;
import java.io.Serializable;

public class PacketContainer implements Serializable
{
    private static final long serialVersionUID = 3L;
    protected PacketType type;
    protected transient Object handle;
    protected transient StructureModifier<Object> structureModifier;
    private static ConcurrentMap<Class<?>, Method> writeMethods;
    private static ConcurrentMap<Class<?>, Method> readMethods;
    private static final AggregateCloner DEEP_CLONER;
    private static final AggregateCloner SHALLOW_CLONER;
    private static final Set<PacketType> CLONING_UNSUPPORTED;
    
    @Deprecated
    public PacketContainer(final int id) {
        this(PacketType.findLegacy(id), StructureCache.newPacket(PacketType.findLegacy(id)));
    }
    
    @Deprecated
    public PacketContainer(final int id, final Object handle) {
        this(PacketType.findLegacy(id), handle);
    }
    
    @Deprecated
    public PacketContainer(final int id, final Object handle, final StructureModifier<Object> structure) {
        this(PacketType.findLegacy(id), handle, structure);
    }
    
    public PacketContainer(final PacketType type) {
        this(type, StructureCache.newPacket(type));
    }
    
    public PacketContainer(final PacketType type, final Object handle) {
        this(type, handle, StructureCache.getStructure(type).withTarget(handle));
    }
    
    public PacketContainer(final PacketType type, final Object handle, final StructureModifier<Object> structure) {
        if (handle == null) {
            throw new IllegalArgumentException("handle cannot be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null.");
        }
        this.type = type;
        this.handle = handle;
        this.structureModifier = structure;
    }
    
    public static PacketContainer fromPacket(final Object packet) {
        final PacketType type = PacketType.fromClass(packet.getClass());
        return new PacketContainer(type, packet);
    }
    
    protected PacketContainer() {
    }
    
    public Object getHandle() {
        return this.handle;
    }
    
    public StructureModifier<Object> getModifier() {
        return this.structureModifier;
    }
    
    public <T> StructureModifier<T> getSpecificModifier(final Class<T> primitiveType) {
        return this.structureModifier.withType(primitiveType);
    }
    
    public StructureModifier<Byte> getBytes() {
        return this.structureModifier.withType(Byte.TYPE);
    }
    
    public StructureModifier<Boolean> getBooleans() {
        return this.structureModifier.withType(Boolean.TYPE);
    }
    
    public StructureModifier<Short> getShorts() {
        return this.structureModifier.withType(Short.TYPE);
    }
    
    public StructureModifier<Integer> getIntegers() {
        return this.structureModifier.withType(Integer.TYPE);
    }
    
    public StructureModifier<Long> getLongs() {
        return this.structureModifier.withType(Long.TYPE);
    }
    
    public StructureModifier<Float> getFloat() {
        return this.structureModifier.withType(Float.TYPE);
    }
    
    public StructureModifier<Double> getDoubles() {
        return this.structureModifier.withType(Double.TYPE);
    }
    
    public StructureModifier<String> getStrings() {
        return this.structureModifier.withType(String.class);
    }
    
    public StructureModifier<UUID> getUUIDs() {
        return this.structureModifier.withType(UUID.class);
    }
    
    public StructureModifier<String[]> getStringArrays() {
        return this.structureModifier.withType(String[].class);
    }
    
    public StructureModifier<byte[]> getByteArrays() {
        return this.structureModifier.withType(byte[].class);
    }
    
    public StreamSerializer getByteArraySerializer() {
        return new StreamSerializer();
    }
    
    public StructureModifier<int[]> getIntegerArrays() {
        return this.structureModifier.withType(int[].class);
    }
    
    public StructureModifier<ItemStack> getItemModifier() {
        return this.structureModifier.withType(MinecraftReflection.getItemStackClass(), BukkitConverters.getItemStackConverter());
    }
    
    public StructureModifier<ItemStack[]> getItemArrayModifier() {
        return this.structureModifier.withType(MinecraftReflection.getItemStackArrayClass(), Converters.ignoreNull((EquivalentConverter<ItemStack[]>)new ItemStackArrayConverter()));
    }
    
    public StructureModifier<List<ItemStack>> getItemListModifier() {
        return this.structureModifier.withType(List.class, BukkitConverters.getListConverter(BukkitConverters.getItemStackConverter()));
    }
    
    public StructureModifier<Map<WrappedStatistic, Integer>> getStatisticMaps() {
        return this.getMaps(BukkitConverters.getWrappedStatisticConverter(), (EquivalentConverter<Integer>)Converters.passthrough((Class<V>)Integer.class));
    }
    
    public StructureModifier<WorldType> getWorldTypeModifier() {
        return this.structureModifier.withType(MinecraftReflection.getWorldTypeClass(), BukkitConverters.getWorldTypeConverter());
    }
    
    public StructureModifier<WrappedDataWatcher> getDataWatcherModifier() {
        return this.structureModifier.withType(MinecraftReflection.getDataWatcherClass(), BukkitConverters.getDataWatcherConverter());
    }
    
    public StructureModifier<Entity> getEntityModifier(@Nonnull final World world) {
        Preconditions.checkNotNull((Object)world, (Object)"world cannot be NULL.");
        return this.structureModifier.withType(Integer.TYPE, BukkitConverters.getEntityConverter(world));
    }
    
    public StructureModifier<Entity> getEntityModifier(@Nonnull final PacketEvent event) {
        Preconditions.checkNotNull((Object)event, (Object)"event cannot be NULL.");
        return this.getEntityModifier(event.getPlayer().getWorld());
    }
    
    public StructureModifier<EntityType> getEntityTypeModifier() {
        return this.structureModifier.withType(MinecraftReflection.getMinecraftClass("EntityTypes"), BukkitConverters.getEntityTypeConverter());
    }
    
    public StructureModifier<ChunkPosition> getPositionModifier() {
        return this.structureModifier.withType(MinecraftReflection.getChunkPositionClass(), ChunkPosition.getConverter());
    }
    
    public StructureModifier<BlockPosition> getBlockPositionModifier() {
        return this.structureModifier.withType(MinecraftReflection.getBlockPositionClass(), BlockPosition.getConverter());
    }
    
    public StructureModifier<ChunkCoordIntPair> getChunkCoordIntPairs() {
        return this.structureModifier.withType(MinecraftReflection.getChunkCoordIntPair(), ChunkCoordIntPair.getConverter());
    }
    
    public StructureModifier<NbtBase<?>> getNbtModifier() {
        return this.structureModifier.withType(MinecraftReflection.getNBTBaseClass(), BukkitConverters.getNbtConverter());
    }
    
    public StructureModifier<List<NbtBase<?>>> getListNbtModifier() {
        return this.structureModifier.withType(Collection.class, BukkitConverters.getListConverter(BukkitConverters.getNbtConverter()));
    }
    
    public StructureModifier<Vector> getVectors() {
        return this.structureModifier.withType(MinecraftReflection.getVec3DClass(), BukkitConverters.getVectorConverter());
    }
    
    public StructureModifier<List<WrappedAttribute>> getAttributeCollectionModifier() {
        return this.structureModifier.withType(Collection.class, BukkitConverters.getListConverter(BukkitConverters.getWrappedAttributeConverter()));
    }
    
    public StructureModifier<List<ChunkPosition>> getPositionCollectionModifier() {
        return this.structureModifier.withType(Collection.class, BukkitConverters.getListConverter(ChunkPosition.getConverter()));
    }
    
    public StructureModifier<List<BlockPosition>> getBlockPositionCollectionModifier() {
        return this.structureModifier.withType(Collection.class, BukkitConverters.getListConverter(BlockPosition.getConverter()));
    }
    
    public StructureModifier<List<WrappedWatchableObject>> getWatchableCollectionModifier() {
        return this.structureModifier.withType(Collection.class, BukkitConverters.getListConverter(BukkitConverters.getWatchableObjectConverter()));
    }
    
    public StructureModifier<Material> getBlocks() {
        return this.structureModifier.withType(MinecraftReflection.getBlockClass(), BukkitConverters.getBlockConverter());
    }
    
    public StructureModifier<WrappedGameProfile> getGameProfiles() {
        return this.structureModifier.withType(MinecraftReflection.getGameProfileClass(), BukkitConverters.getWrappedGameProfileConverter());
    }
    
    public StructureModifier<WrappedBlockData> getBlockData() {
        return this.structureModifier.withType(MinecraftReflection.getIBlockDataClass(), BukkitConverters.getWrappedBlockDataConverter());
    }
    
    public StructureModifier<MultiBlockChangeInfo[]> getMultiBlockChangeInfoArrays() {
        final ChunkCoordIntPair chunk = this.getChunkCoordIntPairs().read(0);
        return this.structureModifier.withType(MinecraftReflection.getMultiBlockChangeInfoArrayClass(), MultiBlockChangeInfo.getArrayConverter(chunk));
    }
    
    public StructureModifier<WrappedChatComponent> getChatComponents() {
        return this.structureModifier.withType(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter());
    }
    
    public StructureModifier<WrappedChatComponent[]> getChatComponentArrays() {
        return this.structureModifier.withType(ComponentArrayConverter.getGenericType(), Converters.ignoreNull((EquivalentConverter<WrappedChatComponent[]>)new ComponentArrayConverter()));
    }
    
    public StructureModifier<WrappedServerPing> getServerPings() {
        return this.structureModifier.withType(MinecraftReflection.getServerPingClass(), BukkitConverters.getWrappedServerPingConverter());
    }
    
    public StructureModifier<List<PlayerInfoData>> getPlayerInfoDataLists() {
        return this.structureModifier.withType(Collection.class, BukkitConverters.getListConverter(PlayerInfoData.getConverter()));
    }
    
    public StructureModifier<PacketType.Protocol> getProtocols() {
        return this.structureModifier.withType(EnumWrappers.getProtocolClass(), EnumWrappers.getProtocolConverter());
    }
    
    public StructureModifier<EnumWrappers.ClientCommand> getClientCommands() {
        return this.structureModifier.withType(EnumWrappers.getClientCommandClass(), EnumWrappers.getClientCommandConverter());
    }
    
    public StructureModifier<EnumWrappers.ChatVisibility> getChatVisibilities() {
        return this.structureModifier.withType(EnumWrappers.getChatVisibilityClass(), EnumWrappers.getChatVisibilityConverter());
    }
    
    public StructureModifier<EnumWrappers.Difficulty> getDifficulties() {
        return this.structureModifier.withType(EnumWrappers.getDifficultyClass(), EnumWrappers.getDifficultyConverter());
    }
    
    public StructureModifier<EnumWrappers.EntityUseAction> getEntityUseActions() {
        return this.structureModifier.withType(EnumWrappers.getEntityUseActionClass(), EnumWrappers.getEntityUseActionConverter());
    }
    
    public StructureModifier<EnumWrappers.NativeGameMode> getGameModes() {
        return this.structureModifier.withType(EnumWrappers.getGameModeClass(), EnumWrappers.getGameModeConverter());
    }
    
    public StructureModifier<EnumWrappers.ResourcePackStatus> getResourcePackStatus() {
        return this.structureModifier.withType(EnumWrappers.getResourcePackStatusClass(), EnumWrappers.getResourcePackStatusConverter());
    }
    
    public StructureModifier<EnumWrappers.PlayerInfoAction> getPlayerInfoAction() {
        return this.structureModifier.withType(EnumWrappers.getPlayerInfoActionClass(), EnumWrappers.getPlayerInfoActionConverter());
    }
    
    public StructureModifier<EnumWrappers.TitleAction> getTitleActions() {
        return this.structureModifier.withType(EnumWrappers.getTitleActionClass(), EnumWrappers.getTitleActionConverter());
    }
    
    public StructureModifier<EnumWrappers.WorldBorderAction> getWorldBorderActions() {
        return this.structureModifier.withType(EnumWrappers.getWorldBorderActionClass(), EnumWrappers.getWorldBorderActionConverter());
    }
    
    public StructureModifier<EnumWrappers.CombatEventType> getCombatEvents() {
        return this.structureModifier.withType(EnumWrappers.getCombatEventTypeClass(), EnumWrappers.getCombatEventTypeConverter());
    }
    
    public StructureModifier<EnumWrappers.PlayerDigType> getPlayerDigTypes() {
        return this.structureModifier.withType(EnumWrappers.getPlayerDigTypeClass(), EnumWrappers.getPlayerDiggingActionConverter());
    }
    
    public StructureModifier<EnumWrappers.PlayerAction> getPlayerActions() {
        return this.structureModifier.withType(EnumWrappers.getPlayerActionClass(), EnumWrappers.getEntityActionConverter());
    }
    
    public StructureModifier<EnumWrappers.ScoreboardAction> getScoreboardActions() {
        return this.structureModifier.withType(EnumWrappers.getScoreboardActionClass(), EnumWrappers.getUpdateScoreActionConverter());
    }
    
    public StructureModifier<EnumWrappers.Particle> getParticles() {
        return this.structureModifier.withType(EnumWrappers.getParticleClass(), EnumWrappers.getParticleConverter());
    }
    
    public StructureModifier<WrappedParticle> getNewParticles() {
        return (StructureModifier<WrappedParticle>)this.structureModifier.withType(MinecraftReflection.getMinecraftClass("ParticleParam"), BukkitConverters.getParticleConverter());
    }
    
    public StructureModifier<PotionEffectType> getEffectTypes() {
        return this.structureModifier.withType(MinecraftReflection.getMobEffectListClass(), BukkitConverters.getEffectTypeConverter());
    }
    
    public StructureModifier<EnumWrappers.SoundCategory> getSoundCategories() {
        return this.structureModifier.withType(EnumWrappers.getSoundCategoryClass(), EnumWrappers.getSoundCategoryConverter());
    }
    
    public StructureModifier<Sound> getSoundEffects() {
        return this.structureModifier.withType(MinecraftReflection.getSoundEffectClass(), BukkitConverters.getSoundConverter());
    }
    
    public StructureModifier<EnumWrappers.ItemSlot> getItemSlots() {
        return this.structureModifier.withType(EnumWrappers.getItemSlotClass(), EnumWrappers.getItemSlotConverter());
    }
    
    public StructureModifier<EnumWrappers.Hand> getHands() {
        return this.structureModifier.withType(EnumWrappers.getHandClass(), EnumWrappers.getHandConverter());
    }
    
    public StructureModifier<EnumWrappers.Direction> getDirections() {
        return this.structureModifier.withType(EnumWrappers.getDirectionClass(), EnumWrappers.getDirectionConverter());
    }
    
    public StructureModifier<EnumWrappers.ChatType> getChatTypes() {
        return this.structureModifier.withType(EnumWrappers.getChatTypeClass(), EnumWrappers.getChatTypeConverter());
    }
    
    public StructureModifier<MinecraftKey> getMinecraftKeys() {
        return this.structureModifier.withType(MinecraftReflection.getMinecraftKeyClass(), MinecraftKey.getConverter());
    }
    
    public StructureModifier<Integer> getDimensions() {
        return this.structureModifier.withType(MinecraftReflection.getMinecraftClass("DimensionManager"), BukkitConverters.getDimensionIDConverter());
    }
    
    public <K, V> StructureModifier<Map<K, V>> getMaps(final EquivalentConverter<K> keyConverter, final EquivalentConverter<V> valConverter) {
        return this.structureModifier.withType(Map.class, BukkitConverters.getMapConverter(keyConverter, valConverter));
    }
    
    public <E> StructureModifier<Set<E>> getSets(final EquivalentConverter<E> converter) {
        return this.structureModifier.withType(Set.class, BukkitConverters.getSetConverter(converter));
    }
    
    public <E> StructureModifier<List<E>> getLists(final EquivalentConverter<E> converter) {
        return this.structureModifier.withType(List.class, BukkitConverters.getListConverter(converter));
    }
    
    public <T extends Enum<T>> StructureModifier<T> getEnumModifier(final Class<T> enumClass, final Class<?> nmsClass) {
        return this.structureModifier.withType(nmsClass, new EnumWrappers.EnumConverter<T>(nmsClass, enumClass));
    }
    
    public <T extends Enum<T>> StructureModifier<T> getEnumModifier(final Class<T> enumClass, final int index) {
        return this.getEnumModifier(enumClass, this.structureModifier.getField(index).getType());
    }
    
    @Deprecated
    public int getID() {
        return this.type.getLegacyId();
    }
    
    public PacketType getType() {
        return this.type;
    }
    
    public PacketContainer shallowClone() {
        final Object clonedPacket = PacketContainer.SHALLOW_CLONER.clone(this.getHandle());
        return new PacketContainer(this.getType(), clonedPacket);
    }
    
    public PacketContainer deepClone() {
        Object clonedPacket;
        if (PacketContainer.CLONING_UNSUPPORTED.contains(this.type)) {
            clonedPacket = SerializableCloner.clone(this).getHandle();
        }
        else {
            clonedPacket = PacketContainer.DEEP_CLONER.clone(this.getHandle());
        }
        return new PacketContainer(this.getType(), clonedPacket);
    }
    
    private static Function<AggregateCloner.BuilderParameters, Cloner> getSpecializedDeepClonerFactory() {
        return (Function<AggregateCloner.BuilderParameters, Cloner>)new Function<AggregateCloner.BuilderParameters, Cloner>() {
            public Cloner apply(@Nullable final AggregateCloner.BuilderParameters param) {
                return new FieldCloner(param.getInstanceProvider()) {
                    {
                        this.writer = new ObjectWriter() {
                            @Override
                            protected void transformField(final StructureModifier<Object> modifierSource, final StructureModifier<Object> modifierDest, final int fieldIndex) {
                                if (modifierSource.getField(fieldIndex).getName().startsWith("inflatedBuffer")) {
                                    modifierDest.write(fieldIndex, modifierSource.read(fieldIndex));
                                }
                                else {
                                    FieldCloner.this.defaultTransform(modifierSource, modifierDest, FieldCloner.this.getDefaultCloner(), fieldIndex);
                                }
                            }
                        };
                    }
                };
            }
        };
    }
    
    private void writeObject(final ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        output.writeBoolean(this.handle != null);
        try {
            if (MinecraftReflection.isUsingNetty()) {
                final ByteBuf buffer = createPacketBuffer();
                MinecraftMethods.getPacketWriteByteBufMethod().invoke(this.handle, buffer);
                output.writeInt(buffer.readableBytes());
                buffer.readBytes((OutputStream)output, buffer.readableBytes());
            }
            else {
                output.writeInt(-1);
                this.getMethodLazily(PacketContainer.writeMethods, this.handle.getClass(), "write", DataOutput.class).invoke(this.handle, new DataOutputStream(output));
            }
        }
        catch (IllegalArgumentException e) {
            throw new IOException("Minecraft packet doesn't support DataOutputStream", e);
        }
        catch (IllegalAccessException e2) {
            throw new RuntimeException("Insufficient security privileges.", e2);
        }
        catch (InvocationTargetException e3) {
            throw new IOException("Could not serialize Minecraft packet.", e3);
        }
    }
    
    private void readObject(final ObjectInputStream input) throws ClassNotFoundException, IOException {
        input.defaultReadObject();
        this.structureModifier = StructureCache.getStructure(this.type);
        if (input.readBoolean()) {
            this.handle = StructureCache.newPacket(this.type);
            try {
                if (MinecraftReflection.isUsingNetty()) {
                    final ByteBuf buffer = createPacketBuffer();
                    buffer.writeBytes((InputStream)input, input.readInt());
                    MinecraftMethods.getPacketReadByteBufMethod().invoke(this.handle, buffer);
                }
                else {
                    if (input.readInt() != -1) {
                        throw new IllegalArgumentException("Cannot load a packet from 1.7.2 in 1.6.4.");
                    }
                    this.getMethodLazily(PacketContainer.readMethods, this.handle.getClass(), "read", DataInput.class).invoke(this.handle, new DataInputStream(input));
                }
            }
            catch (IllegalArgumentException e) {
                throw new IOException("Minecraft packet doesn't support DataInputStream", e);
            }
            catch (IllegalAccessException e2) {
                throw new RuntimeException("Insufficient security privileges.", e2);
            }
            catch (InvocationTargetException e3) {
                throw new IOException("Could not deserialize Minecraft packet.", e3);
            }
            this.structureModifier = this.structureModifier.withTarget(this.handle);
        }
    }
    
    public static ByteBuf createPacketBuffer() {
        final ByteBuf buffer = UnpooledByteBufAllocator.DEFAULT.buffer();
        final Class<?> packetSerializer = MinecraftReflection.getPacketDataSerializerClass();
        try {
            return (ByteBuf)packetSerializer.getConstructor(ByteBuf.class).newInstance(buffer);
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot construct packet serializer.", e);
        }
    }
    
    public <T> Optional<T> getMeta(final String key) {
        return PacketMetadata.get(this.handle, key);
    }
    
    public <T> void setMeta(final String key, final T value) {
        PacketMetadata.set(this.handle, key, value);
    }
    
    public void removeMeta(final String key) {
        PacketMetadata.remove(this.handle, key);
    }
    
    @Deprecated
    public Object getMetadata(final String key) {
        return this.getMeta(key).orElse(null);
    }
    
    @Deprecated
    public void addMetadata(final String key, final Object value) {
        this.setMeta(key, value);
    }
    
    @Deprecated
    public Object removeMetadata(final String key) {
        return PacketMetadata.remove(this.handle, key).orElseGet(null);
    }
    
    @Deprecated
    public boolean hasMetadata(final String key) {
        return this.getMeta(key).isPresent();
    }
    
    private Method getMethodLazily(final ConcurrentMap<Class<?>, Method> lookup, final Class<?> handleClass, final String methodName, final Class<?> parameterClass) {
        Method method = lookup.get(handleClass);
        if (method == null) {
            final Method initialized = FuzzyReflection.fromClass(handleClass).getMethod(FuzzyMethodContract.newBuilder().parameterCount(1).parameterDerivedOf(parameterClass).returnTypeVoid().build());
            method = lookup.putIfAbsent(handleClass, initialized);
            if (method == null) {
                method = initialized;
            }
        }
        return method;
    }
    
    @Override
    public String toString() {
        return "PacketContainer[type=" + this.type + ", structureModifier=" + this.structureModifier + "]";
    }
    
    static {
        PacketContainer.writeMethods = (ConcurrentMap<Class<?>, Method>)Maps.newConcurrentMap();
        PacketContainer.readMethods = (ConcurrentMap<Class<?>, Method>)Maps.newConcurrentMap();
        DEEP_CLONER = AggregateCloner.newBuilder().instanceProvider(DefaultInstances.DEFAULT).andThen(BukkitCloner.class).andThen(ImmutableDetector.class).andThen(JavaOptionalCloner.class).andThen(GuavaOptionalCloner.class).andThen(CollectionCloner.class).andThen(getSpecializedDeepClonerFactory()).build();
        SHALLOW_CLONER = AggregateCloner.newBuilder().instanceProvider(DefaultInstances.DEFAULT).andThen((Function<AggregateCloner.BuilderParameters, Cloner>)(param -> {
            if (param == null) {
                throw new IllegalArgumentException("Cannot be NULL.");
            }
            return new FieldCloner(param.getAggregateCloner(), param.getInstanceProvider()) {
                {
                    this.writer = new ObjectWriter();
                }
            };
        })).build();
        CLONING_UNSUPPORTED = Sets.newHashSet((Object[])new PacketType[] { PacketType.Play.Server.UPDATE_ATTRIBUTES, PacketType.Status.Server.SERVER_INFO });
    }
    
    private static class ItemStackArrayConverter implements EquivalentConverter<ItemStack[]>
    {
        final EquivalentConverter<ItemStack> stackConverter;
        
        private ItemStackArrayConverter() {
            this.stackConverter = BukkitConverters.getItemStackConverter();
        }
        
        @Override
        public Object getGeneric(final ItemStack[] specific) {
            final Class<?> nmsStack = MinecraftReflection.getItemStackClass();
            final Object[] result = (Object[])Array.newInstance(nmsStack, specific.length);
            for (int i = 0; i < result.length; ++i) {
                result[i] = this.stackConverter.getGeneric(specific[i]);
            }
            return result;
        }
        
        @Override
        public ItemStack[] getSpecific(final Object generic) {
            final Object[] input = (Object[])generic;
            final ItemStack[] result = new ItemStack[input.length];
            for (int i = 0; i < result.length; ++i) {
                result[i] = this.stackConverter.getSpecific(input[i]);
            }
            return result;
        }
        
        @Override
        public Class<ItemStack[]> getSpecificType() {
            return ItemStack[].class;
        }
    }
    
    private static class LegacyComponentConverter implements EquivalentConverter<WrappedChatComponent[]>
    {
        final EquivalentConverter<WrappedChatComponent> componentConverter;
        
        private LegacyComponentConverter() {
            this.componentConverter = BukkitConverters.getWrappedChatComponentConverter();
        }
        
        @Override
        public Object getGeneric(final WrappedChatComponent[] specific) {
            final Class<?> nmsComponent = MinecraftReflection.getIChatBaseComponentClass();
            final Object[] result = (Object[])Array.newInstance(nmsComponent, specific.length);
            for (int i = 0; i < result.length; ++i) {
                result[i] = this.componentConverter.getGeneric(specific[i]);
            }
            return result;
        }
        
        @Override
        public WrappedChatComponent[] getSpecific(final Object generic) {
            final Object[] input = (Object[])generic;
            final WrappedChatComponent[] result = new WrappedChatComponent[input.length];
            for (int i = 0; i < result.length; ++i) {
                result[i] = this.componentConverter.getSpecific(input[i]);
            }
            return result;
        }
        
        @Override
        public Class<WrappedChatComponent[]> getSpecificType() {
            return WrappedChatComponent[].class;
        }
    }
    
    private static class NBTComponentConverter implements EquivalentConverter<WrappedChatComponent[]>
    {
        private EquivalentConverter<NbtBase<?>> nbtConverter;
        private final int lines = 4;
        
        private NBTComponentConverter() {
            this.nbtConverter = BukkitConverters.getNbtConverter();
        }
        
        @Override
        public WrappedChatComponent[] getSpecific(final Object generic) {
            final NbtBase<?> nbtBase = this.nbtConverter.getSpecific(generic);
            final NbtCompound compound = (NbtCompound)nbtBase;
            final WrappedChatComponent[] components = new WrappedChatComponent[4];
            for (int i = 0; i < 4; ++i) {
                if (compound.containsKey("Text" + (i + 1))) {
                    components[i] = WrappedChatComponent.fromJson(compound.getString("Text" + (i + 1)));
                }
                else {
                    components[i] = WrappedChatComponent.fromText("");
                }
            }
            return components;
        }
        
        @Override
        public Object getGeneric(final WrappedChatComponent[] specific) {
            final NbtCompound compound = NbtFactory.ofCompound("");
            for (int i = 0; i < 4; ++i) {
                WrappedChatComponent component;
                if (i < specific.length && specific[i] != null) {
                    component = specific[i];
                }
                else {
                    component = WrappedChatComponent.fromText("");
                }
                compound.put("Text" + (i + 1), component.getJson());
            }
            return this.nbtConverter.getGeneric(compound);
        }
        
        @Override
        public Class<WrappedChatComponent[]> getSpecificType() {
            return WrappedChatComponent[].class;
        }
    }
    
    private static class ComponentArrayConverter implements EquivalentConverter<WrappedChatComponent[]>
    {
        private static final EquivalentConverter<WrappedChatComponent[]> DELEGATE;
        
        @Override
        public WrappedChatComponent[] getSpecific(final Object generic) {
            return ComponentArrayConverter.DELEGATE.getSpecific(generic);
        }
        
        @Override
        public Object getGeneric(final WrappedChatComponent[] specific) {
            return ComponentArrayConverter.DELEGATE.getGeneric(specific);
        }
        
        @Override
        public Class<WrappedChatComponent[]> getSpecificType() {
            return ComponentArrayConverter.DELEGATE.getSpecificType();
        }
        
        public static Class<?> getGenericType() {
            if (ComponentArrayConverter.DELEGATE instanceof NBTComponentConverter) {
                return MinecraftReflection.getNBTCompoundClass();
            }
            return MinecraftReflection.getIChatBaseComponentArrayClass();
        }
        
        static {
            if (MinecraftReflection.signUpdateExists()) {
                DELEGATE = new LegacyComponentConverter();
            }
            else {
                DELEGATE = new NBTComponentConverter();
            }
        }
    }
}
