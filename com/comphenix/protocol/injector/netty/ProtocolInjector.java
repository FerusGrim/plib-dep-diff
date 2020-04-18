package com.comphenix.protocol.injector.netty;

import java.util.Collection;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.injector.spigot.AbstractPacketInjector;
import com.comphenix.protocol.injector.packet.PacketInjector;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.ListenerOptions;
import java.util.Set;
import com.comphenix.protocol.PacketType;
import java.net.InetSocketAddress;
import com.comphenix.protocol.injector.spigot.AbstractPlayerHandler;
import com.comphenix.protocol.injector.player.PlayerInjectionHandler;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.NetworkMarker;
import io.netty.channel.ChannelFuture;
import org.bukkit.entity.Player;
import java.lang.reflect.Field;
import java.util.Iterator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.utility.MinecraftVersion;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import com.comphenix.protocol.ProtocolLogger;
import java.lang.reflect.Method;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.collect.Lists;
import org.bukkit.plugin.Plugin;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.injector.ListenerInvoker;
import com.comphenix.protocol.concurrency.PacketTypeSet;
import com.comphenix.protocol.reflect.VolatileField;
import java.util.List;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import com.comphenix.protocol.error.ReportType;

public class ProtocolInjector implements ChannelListener
{
    public static final ReportType REPORT_CANNOT_INJECT_INCOMING_CHANNEL;
    private volatile boolean injected;
    private volatile boolean closed;
    private TemporaryPlayerFactory playerFactory;
    private List<VolatileField> bootstrapFields;
    private InjectionFactory injectionFactory;
    private volatile List<Object> networkManagers;
    private PacketTypeSet sendingFilters;
    private PacketTypeSet reveivedFilters;
    private PacketTypeSet mainThreadFilters;
    private PacketTypeSet bufferedPackets;
    private ListenerInvoker invoker;
    private ErrorReporter reporter;
    private boolean debug;
    
    public ProtocolInjector(final Plugin plugin, final ListenerInvoker invoker, final ErrorReporter reporter) {
        this.playerFactory = new TemporaryPlayerFactory();
        this.bootstrapFields = (List<VolatileField>)Lists.newArrayList();
        this.sendingFilters = new PacketTypeSet();
        this.reveivedFilters = new PacketTypeSet();
        this.mainThreadFilters = new PacketTypeSet();
        this.bufferedPackets = new PacketTypeSet();
        this.injectionFactory = new InjectionFactory(plugin);
        this.invoker = invoker;
        this.reporter = reporter;
    }
    
    @Override
    public boolean isDebug() {
        return this.debug;
    }
    
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }
    
    public synchronized void inject() {
        if (this.injected) {
            throw new IllegalStateException("Cannot inject twice.");
        }
        try {
            final FuzzyReflection fuzzyServer = FuzzyReflection.fromClass(MinecraftReflection.getMinecraftServerClass());
            final List<Method> serverConnectionMethods = fuzzyServer.getMethodListByParameters(MinecraftReflection.getServerConnectionClass(), new Class[0]);
            final Object server = fuzzyServer.getSingleton();
            Object serverConnection = null;
            for (final Method method : serverConnectionMethods) {
                try {
                    serverConnection = method.invoke(server, new Object[0]);
                    if (serverConnection != null) {
                        break;
                    }
                    continue;
                }
                catch (Exception ex) {
                    ProtocolLogger.debug("Encountered an exception invoking " + method, ex);
                }
            }
            if (serverConnection == null) {
                throw new ReflectiveOperationException("Failed to obtain server connection");
            }
            final ChannelInboundHandler endInitProtocol = (ChannelInboundHandler)new ChannelInitializer<Channel>() {
                protected void initChannel(final Channel channel) throws Exception {
                    try {
                        synchronized (ProtocolInjector.this.networkManagers) {
                            if (MinecraftVersion.getCurrentVersion().getMinor() >= 12) {
                                channel.eventLoop().submit(() -> ProtocolInjector.this.injectionFactory.fromChannel(channel, ProtocolInjector.this, ProtocolInjector.this.playerFactory).inject());
                            }
                            else {
                                ProtocolInjector.this.injectionFactory.fromChannel(channel, ProtocolInjector.this, ProtocolInjector.this.playerFactory).inject();
                            }
                        }
                    }
                    catch (Exception ex) {
                        ProtocolInjector.this.reporter.reportDetailed(ProtocolInjector.this, Report.newBuilder(ProtocolInjector.REPORT_CANNOT_INJECT_INCOMING_CHANNEL).messageParam(channel).error(ex));
                    }
                }
            };
            final ChannelInboundHandler beginInitProtocol = (ChannelInboundHandler)new ChannelInitializer<Channel>() {
                protected void initChannel(final Channel channel) throws Exception {
                    channel.pipeline().addLast(new ChannelHandler[] { (ChannelHandler)endInitProtocol });
                }
            };
            final ChannelHandler connectionHandler = (ChannelHandler)new ChannelInboundHandlerAdapter() {
                public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
                    final Channel channel = (Channel)msg;
                    channel.pipeline().addFirst(new ChannelHandler[] { (ChannelHandler)beginInitProtocol });
                    ctx.fireChannelRead(msg);
                }
            };
            final FuzzyReflection fuzzy = FuzzyReflection.fromObject(serverConnection, true);
            try {
                final Field field = fuzzy.getParameterizedField(List.class, MinecraftReflection.getNetworkManagerClass());
                field.setAccessible(true);
                this.networkManagers = (List<Object>)field.get(serverConnection);
            }
            catch (Exception ex2) {
                ProtocolLogger.debug("Encountered an exception checking list fields", ex2);
                final Method method2 = fuzzy.getMethodByParameters("getNetworkManagers", List.class, new Class[] { serverConnection.getClass() });
                method2.setAccessible(true);
                this.networkManagers = (List<Object>)method2.invoke(null, serverConnection);
            }
            if (this.networkManagers == null) {
                throw new ReflectiveOperationException("Failed to obtain list of network managers");
            }
            this.bootstrapFields = this.getBootstrapFields(serverConnection);
            for (final VolatileField field2 : this.bootstrapFields) {
                final List<Object> list = (List<Object>)field2.getValue();
                if (list == this.networkManagers) {
                    continue;
                }
                field2.setValue(new BootstrapList(list, connectionHandler));
            }
            this.injected = true;
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to inject channel futures.", e);
        }
    }
    
    @Override
    public boolean hasListener(final Class<?> packetClass) {
        return this.reveivedFilters.contains(packetClass) || this.sendingFilters.contains(packetClass);
    }
    
    @Override
    public boolean hasMainThreadListener(final Class<?> packetClass) {
        return this.mainThreadFilters.contains(packetClass);
    }
    
    @Override
    public ErrorReporter getReporter() {
        return this.reporter;
    }
    
    public void injectPlayer(final Player player) {
        this.injectionFactory.fromPlayer(player, this).inject();
    }
    
    private List<VolatileField> getBootstrapFields(final Object serverConnection) {
        final List<VolatileField> result = (List<VolatileField>)Lists.newArrayList();
        for (final Field field : FuzzyReflection.fromObject(serverConnection, true).getFieldListByType(List.class)) {
            final VolatileField volatileField = new VolatileField(field, serverConnection, true).toSynchronized();
            final List<Object> list = (List<Object>)volatileField.getValue();
            if (list.size() == 0 || list.get(0) instanceof ChannelFuture) {
                result.add(volatileField);
            }
        }
        return result;
    }
    
    public synchronized void close() {
        if (!this.closed) {
            this.closed = true;
            for (final VolatileField field : this.bootstrapFields) {
                final Object value = field.getValue();
                if (value instanceof BootstrapList) {
                    ((BootstrapList)value).close();
                }
                field.revertValue();
            }
            this.injectionFactory.close();
        }
    }
    
    @Override
    public PacketEvent onPacketSending(final Injector injector, final Object packet, final NetworkMarker marker) {
        final Class<?> clazz = packet.getClass();
        if (!this.sendingFilters.contains(clazz)) {
            if (marker == null) {
                return null;
            }
        }
        try {
            final PacketContainer container = new PacketContainer(PacketRegistry.getPacketType(clazz), packet);
            return this.packetQueued(container, injector.getPlayer(), marker);
        }
        catch (LinkageError e) {
            System.err.println("[ProtocolLib] Encountered a LinkageError. Make sure you're not using this jar for multiple server instances!");
            System.err.println("[ProtocolLib] If you're getting this error for other reasons, please report it!");
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public PacketEvent onPacketReceiving(final Injector injector, final Object packet, final NetworkMarker marker) {
        final Class<?> clazz = packet.getClass();
        if (this.reveivedFilters.contains(clazz) || marker != null) {
            final PacketContainer container = new PacketContainer(PacketRegistry.getPacketType(clazz), packet);
            return this.packetReceived(container, injector.getPlayer(), marker);
        }
        return null;
    }
    
    @Override
    public boolean includeBuffer(final Class<?> packetClass) {
        return this.bufferedPackets.contains(packetClass);
    }
    
    private PacketEvent packetQueued(final PacketContainer packet, final Player receiver, final NetworkMarker marker) {
        final PacketEvent event = PacketEvent.fromServer(this, packet, marker, receiver);
        this.invoker.invokePacketSending(event);
        return event;
    }
    
    private PacketEvent packetReceived(final PacketContainer packet, final Player sender, final NetworkMarker marker) {
        final PacketEvent event = PacketEvent.fromClient(this, packet, marker, sender);
        this.invoker.invokePacketRecieving(event);
        return event;
    }
    
    public PlayerInjectionHandler getPlayerInjector() {
        return new AbstractPlayerHandler(this.sendingFilters) {
            private ChannelListener listener = ProtocolInjector.this;
            
            @Override
            public int getProtocolVersion(final Player player) {
                return ProtocolInjector.this.injectionFactory.fromPlayer(player, this.listener).getProtocolVersion();
            }
            
            @Override
            public void updatePlayer(final Player player) {
                ProtocolInjector.this.injectionFactory.fromPlayer(player, this.listener).inject();
            }
            
            @Override
            public void injectPlayer(final Player player, final PlayerInjectionHandler.ConflictStrategy strategy) {
                ProtocolInjector.this.injectionFactory.fromPlayer(player, this.listener).inject();
            }
            
            @Override
            public boolean uninjectPlayer(final InetSocketAddress address) {
                return true;
            }
            
            @Override
            public void addPacketHandler(final PacketType type, final Set<ListenerOptions> options) {
                if (!type.isAsyncForced() && (options == null || !options.contains(ListenerOptions.ASYNC))) {
                    ProtocolInjector.this.mainThreadFilters.addType(type);
                }
                super.addPacketHandler(type, options);
            }
            
            @Override
            public void removePacketHandler(final PacketType type) {
                ProtocolInjector.this.mainThreadFilters.removeType(type);
                super.removePacketHandler(type);
            }
            
            @Override
            public boolean uninjectPlayer(final Player player) {
                return true;
            }
            
            @Override
            public void sendServerPacket(final Player receiver, final PacketContainer packet, final NetworkMarker marker, final boolean filters) throws InvocationTargetException {
                ProtocolInjector.this.injectionFactory.fromPlayer(receiver, this.listener).sendServerPacket(packet.getHandle(), marker, filters);
            }
            
            @Override
            public boolean hasMainThreadListener(final PacketType type) {
                return ProtocolInjector.this.mainThreadFilters.contains(type);
            }
            
            @Override
            public void recieveClientPacket(final Player player, final Object mcPacket) throws IllegalAccessException, InvocationTargetException {
                ProtocolInjector.this.injectionFactory.fromPlayer(player, this.listener).recieveClientPacket(mcPacket);
            }
            
            @Override
            public PacketEvent handlePacketRecieved(final PacketContainer packet, final InputStream input, final byte[] buffered) {
                return null;
            }
            
            @Override
            public void handleDisconnect(final Player player) {
                ProtocolInjector.this.injectionFactory.fromPlayer(player, this.listener).close();
            }
            
            @Override
            public Channel getChannel(final Player player) {
                final Injector injector = ProtocolInjector.this.injectionFactory.fromPlayer(player, this.listener);
                if (injector instanceof ChannelInjector) {
                    return ((ChannelInjector)injector).getChannel();
                }
                return null;
            }
        };
    }
    
    public PacketInjector getPacketInjector() {
        return new AbstractPacketInjector(this.reveivedFilters) {
            @Override
            public PacketEvent packetRecieved(final PacketContainer packet, final Player client, final byte[] buffered) {
                final NetworkMarker marker = (buffered != null) ? new NettyNetworkMarker(ConnectionSide.CLIENT_SIDE, buffered) : null;
                ProtocolInjector.this.injectionFactory.fromPlayer(client, ProtocolInjector.this).saveMarker(packet.getHandle(), marker);
                return ProtocolInjector.this.packetReceived(packet, client, marker);
            }
            
            @Override
            public void inputBuffersChanged(final Set<PacketType> set) {
                ProtocolInjector.this.bufferedPackets = new PacketTypeSet(set);
            }
        };
    }
    
    static {
        REPORT_CANNOT_INJECT_INCOMING_CHANNEL = new ReportType("Unable to inject incoming channel %s.");
    }
}
