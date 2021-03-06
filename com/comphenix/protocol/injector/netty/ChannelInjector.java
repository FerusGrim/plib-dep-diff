package com.comphenix.protocol.injector.netty;

import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import io.netty.channel.socket.SocketChannel;
import java.net.Socket;
import com.comphenix.protocol.injector.server.SocketInjector;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import com.comphenix.protocol.utility.MinecraftFields;
import io.netty.util.concurrent.GenericFutureListener;
import com.comphenix.protocol.utility.MinecraftMethods;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.PacketType;
import java.util.ListIterator;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.error.Report;
import io.netty.util.internal.TypeParameterMatcher;
import java.util.concurrent.Callable;
import io.netty.channel.ChannelPipeline;
import com.comphenix.protocol.utility.MinecraftReflection;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import com.comphenix.protocol.reflect.accessors.Accessors;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.bukkit.Bukkit;
import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.utility.MinecraftProtocolVersion;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import com.google.common.collect.MapMaker;
import com.comphenix.protocol.injector.NetworkProcessor;
import java.util.Deque;
import io.netty.handler.codec.MessageToByteEncoder;
import com.comphenix.protocol.events.NetworkMarker;
import java.util.concurrent.ConcurrentMap;
import com.comphenix.protocol.reflect.VolatileField;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import io.netty.util.AttributeKey;
import java.util.concurrent.atomic.AtomicInteger;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.error.ReportType;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ChannelInjector extends ByteToMessageDecoder implements Injector
{
    public static final ReportType REPORT_CANNOT_INTERCEPT_SERVER_PACKET;
    public static final ReportType REPORT_CANNOT_INTERCEPT_CLIENT_PACKET;
    public static final ReportType REPORT_CANNOT_EXECUTE_IN_CHANNEL_THREAD;
    public static final ReportType REPORT_CANNOT_FIND_GET_VERSION;
    public static final ReportType REPORT_CANNOT_SEND_PACKET;
    private static final PacketEvent BYPASSED_PACKET;
    private static Class<?> PACKET_LOGIN_CLIENT;
    private static FieldAccessor LOGIN_GAME_PROFILE;
    private static Class<?> PACKET_SET_PROTOCOL;
    private static AtomicInteger keyId;
    private static AttributeKey<Integer> PROTOCOL_KEY;
    private static MethodAccessor DECODE_BUFFER;
    private static MethodAccessor ENCODE_BUFFER;
    private static FieldAccessor ENCODER_TYPE_MATCHER;
    private static FieldAccessor PROTOCOL_ACCESSOR;
    private InjectionFactory factory;
    private Player player;
    private Player updated;
    private String playerName;
    private Object playerConnection;
    private final Object networkManager;
    private final Channel originalChannel;
    private VolatileField channelField;
    private ConcurrentMap<Object, NetworkMarker> packetMarker;
    private PacketEvent currentEvent;
    private PacketEvent finalEvent;
    private final ThreadLocal<Boolean> scheduleProcessPackets;
    private ByteToMessageDecoder vanillaDecoder;
    private MessageToByteEncoder<Object> vanillaEncoder;
    private Deque<PacketEvent> finishQueue;
    private ChannelListener channelListener;
    private NetworkProcessor processor;
    private boolean injected;
    private boolean closed;
    
    public ChannelInjector(final Player player, final Object networkManager, final Channel channel, final ChannelListener channelListener, final InjectionFactory factory) {
        this.packetMarker = (ConcurrentMap<Object, NetworkMarker>)new MapMaker().weakKeys().makeMap();
        this.scheduleProcessPackets = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return true;
            }
        };
        this.finishQueue = new ArrayDeque<PacketEvent>();
        this.player = (Player)Preconditions.checkNotNull((Object)player, (Object)"player cannot be NULL");
        this.networkManager = Preconditions.checkNotNull(networkManager, (Object)"networkMananger cannot be NULL");
        this.originalChannel = (Channel)Preconditions.checkNotNull((Object)channel, (Object)"channel cannot be NULL");
        this.channelListener = (ChannelListener)Preconditions.checkNotNull((Object)channelListener, (Object)"channelListener cannot be NULL");
        this.factory = (InjectionFactory)Preconditions.checkNotNull((Object)factory, (Object)"factory cannot be NULL");
        this.processor = new NetworkProcessor(ProtocolLibrary.getErrorReporter());
        this.channelField = new VolatileField(FuzzyReflection.fromObject(networkManager, true).getFieldByType("channel", Channel.class), networkManager, true);
    }
    
    public int getProtocolVersion() {
        final Integer value = (Integer)this.originalChannel.attr((AttributeKey)ChannelInjector.PROTOCOL_KEY).get();
        return (value != null) ? value : MinecraftProtocolVersion.getCurrentVersion();
    }
    
    public boolean inject() {
        synchronized (this.networkManager) {
            if (this.closed) {
                return false;
            }
            if (this.originalChannel instanceof Factory) {
                return false;
            }
            if (!this.originalChannel.isActive()) {
                return false;
            }
            if (Bukkit.isPrimaryThread()) {
                this.executeInChannelThread(new Runnable() {
                    @Override
                    public void run() {
                        ChannelInjector.this.inject();
                    }
                });
                return false;
            }
            if (findChannelHandler(this.originalChannel, ChannelInjector.class) != null) {
                return false;
            }
            this.vanillaDecoder = (ByteToMessageDecoder)this.originalChannel.pipeline().get("decoder");
            this.vanillaEncoder = (MessageToByteEncoder<Object>)this.originalChannel.pipeline().get("encoder");
            if (this.vanillaDecoder == null) {
                throw new IllegalArgumentException("Unable to find vanilla decoder in " + this.originalChannel.pipeline());
            }
            if (this.vanillaEncoder == null) {
                throw new IllegalArgumentException("Unable to find vanilla encoder in " + this.originalChannel.pipeline());
            }
            this.patchEncoder(this.vanillaEncoder);
            if (ChannelInjector.DECODE_BUFFER == null) {
                ChannelInjector.DECODE_BUFFER = Accessors.getMethodAccessor(this.vanillaDecoder.getClass(), "decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
            }
            if (ChannelInjector.ENCODE_BUFFER == null) {
                ChannelInjector.ENCODE_BUFFER = Accessors.getMethodAccessor(this.vanillaEncoder.getClass(), "encode", ChannelHandlerContext.class, Object.class, ByteBuf.class);
            }
            final MessageToByteEncoder<Object> protocolEncoder = new MessageToByteEncoder<Object>() {
                protected void encode(final ChannelHandlerContext ctx, final Object packet, final ByteBuf output) throws Exception {
                    if (packet instanceof WirePacket) {
                        ChannelInjector.this.encodeWirePacket((WirePacket)packet, output);
                    }
                    else {
                        ChannelInjector.this.encode(ctx, packet, output);
                    }
                }
                
                public void write(final ChannelHandlerContext ctx, final Object packet, final ChannelPromise promise) throws Exception {
                    super.write(ctx, packet, promise);
                    ChannelInjector.this.finalWrite(ctx, packet, promise);
                }
            };
            final ChannelInboundHandlerAdapter finishHandler = new ChannelInboundHandlerAdapter() {
                public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
                    ctx.fireChannelRead(msg);
                    ChannelInjector.this.finishRead(ctx, msg);
                }
            };
            this.originalChannel.pipeline().addBefore("decoder", "protocol_lib_decoder", (ChannelHandler)this);
            this.originalChannel.pipeline().addBefore("protocol_lib_decoder", "protocol_lib_finish", (ChannelHandler)finishHandler);
            this.originalChannel.pipeline().addAfter("encoder", "protocol_lib_encoder", (ChannelHandler)protocolEncoder);
            this.channelField.setValue(new ChannelProxy(MinecraftReflection.getPacketClass()) {
                private final PipelineProxy pipelineProxy = new PipelineProxy(ChannelInjector.this.originalChannel.pipeline(), (Channel)this) {
                    @Override
                    public ChannelPipeline addBefore(final String baseName, final String name, final ChannelHandler handler) {
                        if ("decoder".equals(baseName) && super.get("protocol_lib_decoder") != null && ChannelInjector.this.guessCompression(handler)) {
                            super.addBefore("protocol_lib_decoder", name, handler);
                            return (ChannelPipeline)this;
                        }
                        return super.addBefore(baseName, name, handler);
                    }
                };
                
                @Override
                public ChannelPipeline pipeline() {
                    return (ChannelPipeline)this.pipelineProxy;
                }
                
                @Override
                protected <T> Callable<T> onMessageScheduled(final Callable<T> callable, final FieldAccessor packetAccessor) {
                    final PacketEvent event = this.handleScheduled(callable, packetAccessor);
                    if (event != null && event.isCancelled()) {
                        return null;
                    }
                    return new Callable<T>() {
                        @Override
                        public T call() throws Exception {
                            T result = null;
                            ChannelInjector.this.currentEvent = event;
                            result = callable.call();
                            ChannelInjector.this.currentEvent = null;
                            return result;
                        }
                    };
                }
                
                @Override
                protected Runnable onMessageScheduled(final Runnable runnable, final FieldAccessor packetAccessor) {
                    final PacketEvent event = this.handleScheduled(runnable, packetAccessor);
                    if (event != null && event.isCancelled()) {
                        return null;
                    }
                    return new Runnable() {
                        @Override
                        public void run() {
                            ChannelInjector.this.currentEvent = event;
                            runnable.run();
                            ChannelInjector.this.currentEvent = null;
                        }
                    };
                }
                
                protected PacketEvent handleScheduled(final Object instance, final FieldAccessor accessor) {
                    final Object original = accessor.get(instance);
                    if (ChannelInjector.this.scheduleProcessPackets.get()) {
                        final PacketEvent event = ChannelInjector.this.processSending(original);
                        if (event != null && !event.isCancelled()) {
                            final Object changed = event.getPacket().getHandle();
                            if (original != changed) {
                                accessor.set(instance, changed);
                            }
                        }
                        return (event != null) ? event : ChannelInjector.BYPASSED_PACKET;
                    }
                    final NetworkMarker marker = ChannelInjector.this.getMarker(original);
                    if (marker != null) {
                        final PacketEvent result = new PacketEvent(ChannelInjector.class);
                        result.setNetworkMarker(marker);
                        return result;
                    }
                    return ChannelInjector.BYPASSED_PACKET;
                }
            });
            return this.injected = true;
        }
    }
    
    private boolean guessCompression(final ChannelHandler handler) {
        final String className = (handler != null) ? handler.getClass().getCanonicalName() : "";
        return className.contains("Compressor") || className.contains("Decompressor");
    }
    
    private PacketEvent processSending(final Object message) {
        return this.channelListener.onPacketSending(this, message, this.getMarker(message));
    }
    
    private void patchEncoder(final MessageToByteEncoder<Object> encoder) {
        if (ChannelInjector.ENCODER_TYPE_MATCHER == null) {
            ChannelInjector.ENCODER_TYPE_MATCHER = Accessors.getFieldAccessor(encoder.getClass(), "matcher", true);
        }
        ChannelInjector.ENCODER_TYPE_MATCHER.set(encoder, TypeParameterMatcher.get((Class)MinecraftReflection.getPacketClass()));
    }
    
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (this.channelListener.isDebug()) {
            cause.printStackTrace();
        }
        super.exceptionCaught(ctx, cause);
    }
    
    protected void encodeWirePacket(final WirePacket packet, final ByteBuf output) throws Exception {
        packet.writeId(output);
        packet.writeBytes(output);
    }
    
    protected void encode(final ChannelHandlerContext ctx, Object packet, final ByteBuf output) throws Exception {
        NetworkMarker marker = null;
        PacketEvent event = this.currentEvent;
        try {
            if (!this.scheduleProcessPackets.get()) {
                return;
            }
            if (event == null) {
                final Class<?> clazz = packet.getClass();
                if (this.channelListener.hasMainThreadListener(clazz)) {
                    this.scheduleMainThread(packet);
                    packet = null;
                }
                else {
                    event = this.processSending(packet);
                    if (event != null) {
                        packet = (event.isCancelled() ? null : event.getPacket().getHandle());
                    }
                }
            }
            if (event != null) {
                marker = NetworkMarker.getNetworkMarker(event);
            }
            if (packet != null && event != null && NetworkMarker.hasOutputHandlers(marker)) {
                final ByteBuf packetBuffer = ctx.alloc().buffer();
                ChannelInjector.ENCODE_BUFFER.invoke(this.vanillaEncoder, ctx, packet, packetBuffer);
                final byte[] data = this.processor.processOutput(event, marker, this.getBytes(packetBuffer));
                output.writeBytes(data);
                packet = null;
                this.finalEvent = event;
            }
        }
        catch (Exception e) {
            this.channelListener.getReporter().reportDetailed(this, Report.newBuilder(ChannelInjector.REPORT_CANNOT_INTERCEPT_SERVER_PACKET).callerParam(packet).error(e).build());
        }
        finally {
            if (packet != null) {
                ChannelInjector.ENCODE_BUFFER.invoke(this.vanillaEncoder, ctx, packet, output);
                this.finalEvent = event;
            }
        }
    }
    
    protected void finalWrite(final ChannelHandlerContext ctx, final Object packet, final ChannelPromise promise) {
        final PacketEvent event = this.finalEvent;
        if (event != null) {
            this.finalEvent = null;
            this.currentEvent = null;
            this.processor.invokePostEvent(event, NetworkMarker.getNetworkMarker(event));
        }
    }
    
    private void scheduleMainThread(final Object packetCopy) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.factory.getPlugin(), (Runnable)new Runnable() {
            @Override
            public void run() {
                ChannelInjector.this.invokeSendPacket(packetCopy);
            }
        });
    }
    
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf byteBuffer, final List<Object> packets) throws Exception {
        byteBuffer.markReaderIndex();
        ChannelInjector.DECODE_BUFFER.invoke(this.vanillaDecoder, ctx, byteBuffer, packets);
        try {
            this.finishQueue.clear();
            final ListIterator<Object> it = packets.listIterator();
            while (it.hasNext()) {
                final Object input = it.next();
                final Class<?> packetClass = input.getClass();
                NetworkMarker marker = null;
                this.handleLogin(packetClass, input);
                if (this.channelListener.includeBuffer(packetClass)) {
                    byteBuffer.resetReaderIndex();
                    marker = new NettyNetworkMarker(ConnectionSide.CLIENT_SIDE, this.getBytes(byteBuffer));
                }
                final PacketEvent output = this.channelListener.onPacketReceiving(this, input, marker);
                if (output != null) {
                    if (output.isCancelled()) {
                        it.remove();
                    }
                    else {
                        if (output.getPacket().getHandle() != input) {
                            it.set(output.getPacket().getHandle());
                        }
                        this.finishQueue.addLast(output);
                    }
                }
            }
        }
        catch (Exception e) {
            this.channelListener.getReporter().reportDetailed(this, Report.newBuilder(ChannelInjector.REPORT_CANNOT_INTERCEPT_CLIENT_PACKET).callerParam(byteBuffer).error(e).build());
        }
    }
    
    protected void finishRead(final ChannelHandlerContext ctx, final Object msg) {
        final PacketEvent event = this.finishQueue.pollFirst();
        if (event != null) {
            final NetworkMarker marker = NetworkMarker.getNetworkMarker(event);
            if (marker != null) {
                this.processor.invokePostEvent(event, marker);
            }
        }
    }
    
    protected void handleLogin(final Class<?> packetClass, final Object packet) {
        if (ChannelInjector.PACKET_LOGIN_CLIENT == null) {
            ChannelInjector.PACKET_LOGIN_CLIENT = PacketType.Login.Client.START.getPacketClass();
        }
        if (ChannelInjector.PACKET_LOGIN_CLIENT == null) {
            throw new IllegalStateException("Failed to obtain login start packet. Did you build Spigot with BuildTools?");
        }
        if (ChannelInjector.LOGIN_GAME_PROFILE == null) {
            ChannelInjector.LOGIN_GAME_PROFILE = Accessors.getFieldAccessor(ChannelInjector.PACKET_LOGIN_CLIENT, MinecraftReflection.getGameProfileClass(), true);
        }
        if (ChannelInjector.PACKET_LOGIN_CLIENT.equals(packetClass)) {
            final WrappedGameProfile profile = WrappedGameProfile.fromHandle(ChannelInjector.LOGIN_GAME_PROFILE.get(packet));
            this.factory.cacheInjector(profile.getName(), this);
        }
        if (ChannelInjector.PACKET_SET_PROTOCOL == null) {
            try {
                ChannelInjector.PACKET_SET_PROTOCOL = PacketType.Handshake.Client.SET_PROTOCOL.getPacketClass();
            }
            catch (Throwable ex) {
                ChannelInjector.PACKET_SET_PROTOCOL = this.getClass();
            }
        }
        if (ChannelInjector.PACKET_SET_PROTOCOL.equals(packetClass)) {
            final FuzzyReflection fuzzy = FuzzyReflection.fromObject(packet);
            try {
                final int protocol = (int)fuzzy.invokeMethod(packet, "getProtocol", Integer.TYPE, new Object[0]);
                this.originalChannel.attr((AttributeKey)ChannelInjector.PROTOCOL_KEY).set((Object)protocol);
            }
            catch (Throwable t) {}
        }
    }
    
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if (this.channelField != null) {
            this.channelField.refreshValue();
        }
    }
    
    private byte[] getBytes(final ByteBuf buffer) {
        final byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        return data;
    }
    
    private void disconnect(final String message) {
        if (this.playerConnection == null || this.player instanceof Factory) {
            this.originalChannel.disconnect();
        }
        else {
            try {
                MinecraftMethods.getDisconnectMethod(this.playerConnection.getClass()).invoke(this.playerConnection, message);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Unable to invoke disconnect method.", e);
            }
        }
    }
    
    public void sendServerPacket(final Object packet, final NetworkMarker marker, final boolean filtered) {
        this.saveMarker(packet, marker);
        try {
            this.scheduleProcessPackets.set(filtered);
            this.invokeSendPacket(packet);
        }
        finally {
            this.scheduleProcessPackets.set(true);
        }
    }
    
    private void invokeSendPacket(final Object packet) {
        try {
            if (this.player instanceof Factory) {
                MinecraftMethods.getNetworkManagerHandleMethod().invoke(this.networkManager, packet, new GenericFutureListener[0]);
            }
            else {
                MinecraftMethods.getSendPacketMethod().invoke(this.getPlayerConnection(), packet);
            }
        }
        catch (Throwable ex) {
            ProtocolLibrary.getErrorReporter().reportWarning(this, Report.newBuilder(ChannelInjector.REPORT_CANNOT_SEND_PACKET).messageParam(packet, this.playerName).error(ex).build());
        }
    }
    
    public void recieveClientPacket(final Object packet) {
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                try {
                    MinecraftMethods.getNetworkManagerReadPacketMethod().invoke(ChannelInjector.this.networkManager, null, packet);
                }
                catch (Exception e) {
                    ProtocolLibrary.getErrorReporter().reportMinimal(ChannelInjector.this.factory.getPlugin(), "recieveClientPacket", e);
                }
            }
        };
        if (this.originalChannel.eventLoop().inEventLoop()) {
            action.run();
        }
        else {
            this.originalChannel.eventLoop().execute(action);
        }
    }
    
    public PacketType.Protocol getCurrentProtocol() {
        if (ChannelInjector.PROTOCOL_ACCESSOR == null) {
            ChannelInjector.PROTOCOL_ACCESSOR = Accessors.getFieldAccessor(this.networkManager.getClass(), MinecraftReflection.getEnumProtocolClass(), true);
        }
        return PacketType.Protocol.fromVanilla((Enum<?>)ChannelInjector.PROTOCOL_ACCESSOR.get(this.networkManager));
    }
    
    private Object getPlayerConnection() {
        if (this.playerConnection == null) {
            this.playerConnection = MinecraftFields.getPlayerConnection(this.getPlayer());
        }
        return this.playerConnection;
    }
    
    public NetworkMarker getMarker(final Object packet) {
        return this.packetMarker.get(packet);
    }
    
    public void saveMarker(final Object packet, final NetworkMarker marker) {
        if (marker != null) {
            this.packetMarker.put(packet, marker);
        }
    }
    
    public Player getPlayer() {
        if (this.player == null && this.playerName != null) {
            return Bukkit.getPlayer(this.playerName);
        }
        return this.player;
    }
    
    public void setPlayer(final Player player) {
        this.player = player;
        this.playerName = player.getName();
    }
    
    public void setUpdatedPlayer(final Player updated) {
        this.updated = updated;
        this.playerName = updated.getName();
    }
    
    public boolean isInjected() {
        return this.injected;
    }
    
    public boolean isClosed() {
        return this.closed;
    }
    
    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.injected) {
                this.channelField.revertValue();
                this.executeInChannelThread(new Runnable() {
                    @Override
                    public void run() {
                        final String[] array;
                        final String[] handlers = array = new String[] { "protocol_lib_decoder", "protocol_lib_finish", "protocol_lib_encoder" };
                        for (final String handler : array) {
                            try {
                                ChannelInjector.this.originalChannel.pipeline().remove(handler);
                            }
                            catch (NoSuchElementException ex) {}
                        }
                    }
                });
                this.factory.invalidate(this.player);
                this.player = null;
                this.updated = null;
            }
        }
    }
    
    private void executeInChannelThread(final Runnable command) {
        this.originalChannel.eventLoop().execute((Runnable)new Runnable() {
            @Override
            public void run() {
                try {
                    command.run();
                }
                catch (Exception e) {
                    ProtocolLibrary.getErrorReporter().reportDetailed(ChannelInjector.this, Report.newBuilder(ChannelInjector.REPORT_CANNOT_EXECUTE_IN_CHANNEL_THREAD).error(e).build());
                }
            }
        });
    }
    
    public static ChannelHandler findChannelHandler(final Channel channel, final Class<?> clazz) {
        for (final Map.Entry<String, ChannelHandler> entry : channel.pipeline()) {
            if (clazz.isAssignableFrom(entry.getValue().getClass())) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    public Channel getChannel() {
        return this.originalChannel;
    }
    
    static {
        REPORT_CANNOT_INTERCEPT_SERVER_PACKET = new ReportType("Unable to intercept a written server packet.");
        REPORT_CANNOT_INTERCEPT_CLIENT_PACKET = new ReportType("Unable to intercept a read client packet.");
        REPORT_CANNOT_EXECUTE_IN_CHANNEL_THREAD = new ReportType("Cannot execute code in channel thread.");
        REPORT_CANNOT_FIND_GET_VERSION = new ReportType("Cannot find getVersion() in NetworkMananger");
        REPORT_CANNOT_SEND_PACKET = new ReportType("Unable to send packet %s to %s");
        BYPASSED_PACKET = new PacketEvent(ChannelInjector.class);
        ChannelInjector.PACKET_LOGIN_CLIENT = null;
        ChannelInjector.LOGIN_GAME_PROFILE = null;
        ChannelInjector.PACKET_SET_PROTOCOL = null;
        ChannelInjector.keyId = new AtomicInteger();
        try {
            ChannelInjector.PROTOCOL_KEY = (AttributeKey<Integer>)AttributeKey.valueOf("PROTOCOL");
        }
        catch (IllegalArgumentException ex) {
            ChannelInjector.PROTOCOL_KEY = (AttributeKey<Integer>)AttributeKey.valueOf("PROTOCOL-" + ChannelInjector.keyId.getAndIncrement());
        }
    }
    
    public static class ChannelSocketInjector implements SocketInjector
    {
        private final ChannelInjector injector;
        
        public ChannelSocketInjector(final ChannelInjector injector) {
            this.injector = (ChannelInjector)Preconditions.checkNotNull((Object)injector, (Object)"injector cannot be NULL");
        }
        
        @Override
        public Socket getSocket() throws IllegalAccessException {
            return SocketAdapter.adapt((SocketChannel)this.injector.originalChannel);
        }
        
        @Override
        public SocketAddress getAddress() throws IllegalAccessException {
            return this.injector.originalChannel.remoteAddress();
        }
        
        @Override
        public void disconnect(final String message) throws InvocationTargetException {
            this.injector.disconnect(message);
        }
        
        @Override
        public void sendServerPacket(final Object packet, final NetworkMarker marker, final boolean filtered) throws InvocationTargetException {
            this.injector.sendServerPacket(packet, marker, filtered);
        }
        
        @Override
        public Player getPlayer() {
            return this.injector.getPlayer();
        }
        
        @Override
        public Player getUpdatedPlayer() {
            return (this.injector.updated != null) ? this.injector.updated : this.getPlayer();
        }
        
        @Override
        public void transferState(final SocketInjector delegate) {
        }
        
        @Override
        public void setUpdatedPlayer(final Player updatedPlayer) {
            this.injector.setPlayer(updatedPlayer);
        }
        
        public ChannelInjector getChannelInjector() {
            return this.injector;
        }
    }
}
