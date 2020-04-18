package com.comphenix.protocol.wrappers;

import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import com.google.common.base.Charsets;
import io.netty.handler.codec.base64.Base64;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.awt.image.RenderedImage;
import java.io.IOException;
import com.google.common.io.ByteStreams;
import java.io.InputStream;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.accessors.Accessors;
import java.util.Iterator;
import java.util.List;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.google.common.collect.Lists;
import org.bukkit.entity.Player;
import com.google.common.collect.ImmutableList;
import org.bukkit.Server;
import com.comphenix.protocol.utility.Util;
import org.bukkit.Bukkit;
import com.comphenix.protocol.utility.MinecraftProtocolVersion;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;

public class WrappedServerPing extends AbstractWrapper
{
    private static Class<?> GAME_PROFILE;
    private static Class<?> GAME_PROFILE_ARRAY;
    private static Class<?> SERVER_PING;
    private static ConstructorAccessor SERVER_PING_CONSTRUCTOR;
    private static FieldAccessor DESCRIPTION;
    private static FieldAccessor PLAYERS;
    private static FieldAccessor VERSION;
    private static FieldAccessor FAVICON;
    private static EquivalentConverter<Iterable<? extends WrappedGameProfile>> PROFILE_CONVERT;
    private static Class<?> PLAYERS_CLASS;
    private static ConstructorAccessor PLAYERS_CONSTRUCTOR;
    private static FieldAccessor[] PLAYERS_INTS;
    private static FieldAccessor PLAYERS_PROFILES;
    private static FieldAccessor PLAYERS_MAXIMUM;
    private static FieldAccessor PLAYERS_ONLINE;
    private static Class<?> GSON_CLASS;
    private static MethodAccessor GSON_TO_JSON;
    private static MethodAccessor GSON_FROM_JSON;
    private static FieldAccessor PING_GSON;
    private static Class<?> VERSION_CLASS;
    private static ConstructorAccessor VERSION_CONSTRUCTOR;
    private static FieldAccessor VERSION_NAME;
    private static FieldAccessor VERSION_PROTOCOL;
    private static FieldAccessor ENTITY_HUMAN_PROFILE;
    private Object players;
    private Object version;
    
    public WrappedServerPing() {
        super(MinecraftReflection.getServerPingClass());
        this.setHandle(WrappedServerPing.SERVER_PING_CONSTRUCTOR.invoke(new Object[0]));
        this.resetPlayers();
        this.resetVersion();
    }
    
    private WrappedServerPing(final Object handle) {
        super(MinecraftReflection.getServerPingClass());
        this.setHandle(handle);
        this.players = WrappedServerPing.PLAYERS.get(handle);
        this.version = WrappedServerPing.VERSION.get(handle);
    }
    
    protected void resetPlayers() {
        this.players = WrappedServerPing.PLAYERS_CONSTRUCTOR.invoke(0, 0);
        WrappedServerPing.PLAYERS.set(this.handle, this.players);
    }
    
    protected void resetVersion() {
        final MinecraftVersion minecraftVersion = MinecraftVersion.getCurrentVersion();
        this.version = WrappedServerPing.VERSION_CONSTRUCTOR.invoke(minecraftVersion.toString(), MinecraftProtocolVersion.getCurrentVersion());
        WrappedServerPing.VERSION.set(this.handle, this.version);
    }
    
    public static WrappedServerPing fromHandle(final Object handle) {
        return new WrappedServerPing(handle);
    }
    
    public static WrappedServerPing fromJson(final String json) {
        return fromHandle(WrappedServerPing.GSON_FROM_JSON.invoke(WrappedServerPing.PING_GSON.get(null), json, WrappedServerPing.SERVER_PING));
    }
    
    public WrappedChatComponent getMotD() {
        return WrappedChatComponent.fromHandle(WrappedServerPing.DESCRIPTION.get(this.handle));
    }
    
    public void setMotD(final WrappedChatComponent description) {
        WrappedServerPing.DESCRIPTION.set(this.handle, description.getHandle());
    }
    
    public void setMotD(final String message) {
        this.setMotD(WrappedChatComponent.fromText(message));
    }
    
    public CompressedImage getFavicon() {
        final String favicon = (String)WrappedServerPing.FAVICON.get(this.handle);
        return (favicon != null) ? CompressedImage.fromEncodedText(favicon) : null;
    }
    
    public void setFavicon(final CompressedImage image) {
        WrappedServerPing.FAVICON.set(this.handle, (image != null) ? image.toEncodedText() : null);
    }
    
    public int getPlayersOnline() {
        if (this.players == null) {
            throw new IllegalStateException("The player count has been hidden.");
        }
        return (int)WrappedServerPing.PLAYERS_ONLINE.get(this.players);
    }
    
    public void setPlayersOnline(final int online) {
        if (this.players == null) {
            this.resetPlayers();
        }
        WrappedServerPing.PLAYERS_ONLINE.set(this.players, online);
    }
    
    public int getPlayersMaximum() {
        if (this.players == null) {
            throw new IllegalStateException("The player maximum has been hidden.");
        }
        return (int)WrappedServerPing.PLAYERS_MAXIMUM.get(this.players);
    }
    
    public void setPlayersMaximum(final int maximum) {
        if (this.players == null) {
            this.resetPlayers();
        }
        WrappedServerPing.PLAYERS_MAXIMUM.set(this.players, maximum);
    }
    
    public void setPlayersVisible(final boolean visible) {
        if (this.isPlayersVisible() != visible) {
            if (visible) {
                final Server server = Bukkit.getServer();
                this.setPlayersMaximum(server.getMaxPlayers());
                this.setPlayersOnline(Util.getOnlinePlayers().size());
            }
            else {
                WrappedServerPing.PLAYERS.set(this.handle, this.players = null);
            }
        }
    }
    
    public boolean isPlayersVisible() {
        return this.players != null;
    }
    
    public ImmutableList<WrappedGameProfile> getPlayers() {
        if (this.players == null) {
            return (ImmutableList<WrappedGameProfile>)ImmutableList.of();
        }
        final Object playerProfiles = WrappedServerPing.PLAYERS_PROFILES.get(this.players);
        if (playerProfiles == null) {
            return (ImmutableList<WrappedGameProfile>)ImmutableList.of();
        }
        return (ImmutableList<WrappedGameProfile>)ImmutableList.copyOf((Iterable)WrappedServerPing.PROFILE_CONVERT.getSpecific(playerProfiles));
    }
    
    public void setPlayers(final Iterable<? extends WrappedGameProfile> profile) {
        if (this.players == null) {
            this.resetPlayers();
        }
        WrappedServerPing.PLAYERS_PROFILES.set(this.players, (profile != null) ? WrappedServerPing.PROFILE_CONVERT.getGeneric(profile) : null);
    }
    
    public void setBukkitPlayers(final Iterable<? extends Player> players) {
        final List<WrappedGameProfile> profiles = (List<WrappedGameProfile>)Lists.newArrayList();
        for (final Player player : players) {
            final Object profile = WrappedServerPing.ENTITY_HUMAN_PROFILE.get(BukkitUnwrapper.getInstance().unwrapItem(player));
            profiles.add(WrappedGameProfile.fromHandle(profile));
        }
        this.setPlayers(profiles);
    }
    
    public String getVersionName() {
        return (String)WrappedServerPing.VERSION_NAME.get(this.version);
    }
    
    public void setVersionName(final String name) {
        WrappedServerPing.VERSION_NAME.set(this.version, name);
    }
    
    public int getVersionProtocol() {
        return (int)WrappedServerPing.VERSION_PROTOCOL.get(this.version);
    }
    
    public void setVersionProtocol(final int protocol) {
        WrappedServerPing.VERSION_PROTOCOL.set(this.version, protocol);
    }
    
    public WrappedServerPing deepClone() {
        final WrappedServerPing copy = new WrappedServerPing();
        final WrappedChatComponent motd = this.getMotD();
        copy.setPlayers((Iterable<? extends WrappedGameProfile>)this.getPlayers());
        copy.setFavicon(this.getFavicon());
        copy.setMotD((motd != null) ? motd.deepClone() : null);
        copy.setVersionName(this.getVersionName());
        copy.setVersionProtocol(this.getVersionProtocol());
        if (this.isPlayersVisible()) {
            copy.setPlayersMaximum(this.getPlayersMaximum());
            copy.setPlayersOnline(this.getPlayersOnline());
        }
        else {
            copy.setPlayersVisible(false);
        }
        return copy;
    }
    
    public String toJson() {
        return (String)WrappedServerPing.GSON_TO_JSON.invoke(WrappedServerPing.PING_GSON.get(null), this.handle);
    }
    
    @Override
    public String toString() {
        return "WrappedServerPing< " + this.toJson() + ">";
    }
    
    static {
        WrappedServerPing.GAME_PROFILE = MinecraftReflection.getGameProfileClass();
        WrappedServerPing.GAME_PROFILE_ARRAY = MinecraftReflection.getArrayClass(WrappedServerPing.GAME_PROFILE);
        WrappedServerPing.SERVER_PING = MinecraftReflection.getServerPingClass();
        WrappedServerPing.SERVER_PING_CONSTRUCTOR = Accessors.getConstructorAccessor(WrappedServerPing.SERVER_PING, (Class<?>[])new Class[0]);
        WrappedServerPing.DESCRIPTION = Accessors.getFieldAccessor(WrappedServerPing.SERVER_PING, MinecraftReflection.getIChatBaseComponentClass(), true);
        WrappedServerPing.PLAYERS = Accessors.getFieldAccessor(WrappedServerPing.SERVER_PING, MinecraftReflection.getServerPingPlayerSampleClass(), true);
        WrappedServerPing.VERSION = Accessors.getFieldAccessor(WrappedServerPing.SERVER_PING, MinecraftReflection.getServerPingServerDataClass(), true);
        WrappedServerPing.FAVICON = Accessors.getFieldAccessor(WrappedServerPing.SERVER_PING, String.class, true);
        WrappedServerPing.PROFILE_CONVERT = BukkitConverters.getArrayConverter(WrappedServerPing.GAME_PROFILE, BukkitConverters.getWrappedGameProfileConverter());
        WrappedServerPing.PLAYERS_CLASS = MinecraftReflection.getServerPingPlayerSampleClass();
        WrappedServerPing.PLAYERS_CONSTRUCTOR = Accessors.getConstructorAccessor(WrappedServerPing.PLAYERS_CLASS, Integer.TYPE, Integer.TYPE);
        WrappedServerPing.PLAYERS_INTS = Accessors.getFieldAccessorArray(WrappedServerPing.PLAYERS_CLASS, Integer.TYPE, true);
        WrappedServerPing.PLAYERS_PROFILES = Accessors.getFieldAccessor(WrappedServerPing.PLAYERS_CLASS, WrappedServerPing.GAME_PROFILE_ARRAY, true);
        WrappedServerPing.PLAYERS_MAXIMUM = WrappedServerPing.PLAYERS_INTS[0];
        WrappedServerPing.PLAYERS_ONLINE = WrappedServerPing.PLAYERS_INTS[1];
        WrappedServerPing.GSON_CLASS = MinecraftReflection.getMinecraftGsonClass();
        WrappedServerPing.GSON_TO_JSON = Accessors.getMethodAccessor(WrappedServerPing.GSON_CLASS, "toJson", Object.class);
        WrappedServerPing.GSON_FROM_JSON = Accessors.getMethodAccessor(WrappedServerPing.GSON_CLASS, "fromJson", String.class, Class.class);
        WrappedServerPing.PING_GSON = Accessors.getCached(Accessors.getFieldAccessor(PacketType.Status.Server.SERVER_INFO.getPacketClass(), WrappedServerPing.GSON_CLASS, true));
        WrappedServerPing.VERSION_CLASS = MinecraftReflection.getServerPingServerDataClass();
        WrappedServerPing.VERSION_CONSTRUCTOR = Accessors.getConstructorAccessor(WrappedServerPing.VERSION_CLASS, String.class, Integer.TYPE);
        WrappedServerPing.VERSION_NAME = Accessors.getFieldAccessor(WrappedServerPing.VERSION_CLASS, String.class, true);
        WrappedServerPing.VERSION_PROTOCOL = Accessors.getFieldAccessor(WrappedServerPing.VERSION_CLASS, Integer.TYPE, true);
        WrappedServerPing.ENTITY_HUMAN_PROFILE = Accessors.getFieldAccessor(MinecraftReflection.getEntityPlayerClass().getSuperclass(), WrappedServerPing.GAME_PROFILE, true);
    }
    
    public static class CompressedImage
    {
        protected volatile String mime;
        protected volatile byte[] data;
        protected volatile String encoded;
        
        protected CompressedImage() {
        }
        
        public CompressedImage(final String mime, final byte[] data) {
            this.mime = (String)Preconditions.checkNotNull((Object)mime, (Object)"mime cannot be NULL");
            this.data = (byte[])Preconditions.checkNotNull((Object)data, (Object)"data cannot be NULL");
        }
        
        public static CompressedImage fromPng(final InputStream input) throws IOException {
            return new CompressedImage("image/png", ByteStreams.toByteArray(input));
        }
        
        public static CompressedImage fromPng(final byte[] data) {
            return new CompressedImage("image/png", data);
        }
        
        public static CompressedImage fromBase64Png(final String base64) {
            try {
                return new EncodedCompressedImage("data:image/png;base64," + base64);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Must be a pure base64 encoded string. Cannot be an encoded text.", e);
            }
        }
        
        public static CompressedImage fromPng(final RenderedImage image) throws IOException {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return new CompressedImage("image/png", output.toByteArray());
        }
        
        public static CompressedImage fromEncodedText(final String text) {
            return new EncodedCompressedImage(text);
        }
        
        public String getMime() {
            return this.mime;
        }
        
        public byte[] getDataCopy() {
            return this.getData().clone();
        }
        
        protected byte[] getData() {
            return this.data;
        }
        
        public BufferedImage getImage() throws IOException {
            return ImageIO.read(new ByteArrayInputStream(this.getData()));
        }
        
        public String toEncodedText() {
            if (this.encoded == null) {
                final ByteBuf buffer = Unpooled.wrappedBuffer(this.getDataCopy());
                this.encoded = "data:" + this.getMime() + ";base64," + Base64.encode(buffer).toString(Charsets.UTF_8);
            }
            return this.encoded;
        }
    }
    
    private static class EncodedCompressedImage extends CompressedImage
    {
        public EncodedCompressedImage(final String encoded) {
            this.encoded = (String)Preconditions.checkNotNull((Object)encoded, (Object)"encoded favicon cannot be NULL");
        }
        
        protected void initialize() {
            if (this.mime == null || this.data == null) {
                this.decode();
            }
        }
        
        protected void decode() {
            for (final String segment : Splitter.on(";").split((CharSequence)this.encoded)) {
                if (segment.startsWith("data:")) {
                    this.mime = segment.substring(5);
                }
                else {
                    if (!segment.startsWith("base64,")) {
                        continue;
                    }
                    final byte[] encoded = segment.substring(7).getBytes(Charsets.UTF_8);
                    final ByteBuf decoded = Base64.decode(Unpooled.wrappedBuffer(encoded));
                    final byte[] data = new byte[decoded.readableBytes()];
                    decoded.readBytes(data);
                    this.data = data;
                }
            }
        }
        
        @Override
        protected byte[] getData() {
            this.initialize();
            return super.getData();
        }
        
        @Override
        public String getMime() {
            this.initialize();
            return super.getMime();
        }
        
        @Override
        public String toEncodedText() {
            return this.encoded;
        }
    }
}
