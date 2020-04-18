package com.comphenix.protocol.events;

import com.google.common.base.Preconditions;
import java.util.List;
import com.google.common.collect.Lists;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.google.common.collect.Sets;
import com.comphenix.protocol.injector.GamePhase;
import java.util.Set;
import com.google.common.collect.Iterables;
import com.comphenix.protocol.PacketType;
import javax.annotation.Nonnull;
import org.bukkit.plugin.Plugin;

public abstract class PacketAdapter implements PacketListener
{
    protected Plugin plugin;
    protected ConnectionSide connectionSide;
    protected ListeningWhitelist receivingWhitelist;
    protected ListeningWhitelist sendingWhitelist;
    
    public PacketAdapter(@Nonnull final AdapterParameteters params) {
        this(checkValidity(params).plugin, params.connectionSide, params.listenerPriority, params.gamePhase, params.options, params.packets);
    }
    
    public PacketAdapter(final Plugin plugin, final PacketType... types) {
        this(plugin, ListenerPriority.NORMAL, types);
    }
    
    public PacketAdapter(final Plugin plugin, final Iterable<? extends PacketType> types) {
        this(params(plugin, (PacketType[])Iterables.toArray((Iterable)types, (Class)PacketType.class)));
    }
    
    public PacketAdapter(final Plugin plugin, final ListenerPriority listenerPriority, final Iterable<? extends PacketType> types) {
        this(params(plugin, (PacketType[])Iterables.toArray((Iterable)types, (Class)PacketType.class)).listenerPriority(listenerPriority));
    }
    
    public PacketAdapter(final Plugin plugin, final ListenerPriority listenerPriority, final Iterable<? extends PacketType> types, final ListenerOptions... options) {
        this(params(plugin, (PacketType[])Iterables.toArray((Iterable)types, (Class)PacketType.class)).listenerPriority(listenerPriority).options(options));
    }
    
    public PacketAdapter(final Plugin plugin, final ListenerPriority listenerPriority, final PacketType... types) {
        this(params(plugin, types).listenerPriority(listenerPriority));
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final Integer... packets) {
        this(plugin, connectionSide, ListenerPriority.NORMAL, packets);
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final ListenerPriority listenerPriority, final Set<Integer> packets) {
        this(plugin, connectionSide, listenerPriority, GamePhase.PLAYING, (Integer[])packets.toArray(new Integer[0]));
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final GamePhase gamePhase, final Set<Integer> packets) {
        this(plugin, connectionSide, ListenerPriority.NORMAL, gamePhase, (Integer[])packets.toArray(new Integer[0]));
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final ListenerPriority listenerPriority, final GamePhase gamePhase, final Set<Integer> packets) {
        this(plugin, connectionSide, listenerPriority, gamePhase, (Integer[])packets.toArray(new Integer[0]));
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final ListenerPriority listenerPriority, final Integer... packets) {
        this(plugin, connectionSide, listenerPriority, GamePhase.PLAYING, packets);
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final ListenerOptions[] options, final Integer... packets) {
        this(plugin, connectionSide, ListenerPriority.NORMAL, GamePhase.PLAYING, options, packets);
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final GamePhase gamePhase, final Integer... packets) {
        this(plugin, connectionSide, ListenerPriority.NORMAL, gamePhase, packets);
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final ListenerPriority listenerPriority, final GamePhase gamePhase, final Integer... packets) {
        this(plugin, connectionSide, listenerPriority, gamePhase, new ListenerOptions[0], packets);
    }
    
    @Deprecated
    public PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final ListenerPriority listenerPriority, final GamePhase gamePhase, final ListenerOptions[] options, final Integer... packets) {
        this(plugin, connectionSide, listenerPriority, gamePhase, options, (PacketType[])PacketRegistry.toPacketTypes(Sets.newHashSet((Object[])packets), connectionSide.getSender()).toArray(new PacketType[0]));
    }
    
    private PacketAdapter(final Plugin plugin, final ConnectionSide connectionSide, final ListenerPriority listenerPriority, final GamePhase gamePhase, final ListenerOptions[] options, final PacketType... packets) {
        this.receivingWhitelist = ListeningWhitelist.EMPTY_WHITELIST;
        this.sendingWhitelist = ListeningWhitelist.EMPTY_WHITELIST;
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (connectionSide == null) {
            throw new IllegalArgumentException("connectionSide cannot be null");
        }
        if (listenerPriority == null) {
            throw new IllegalArgumentException("listenerPriority cannot be null");
        }
        if (gamePhase == null) {
            throw new IllegalArgumentException("gamePhase cannot be NULL");
        }
        if (packets == null) {
            throw new IllegalArgumentException("packets cannot be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null");
        }
        ListenerOptions[] serverOptions = options;
        final ListenerOptions[] clientOptions = options;
        if (connectionSide == ConnectionSide.BOTH) {
            serverOptions = except(serverOptions, new ListenerOptions[0], ListenerOptions.INTERCEPT_INPUT_BUFFER);
        }
        if (connectionSide.isForServer()) {
            this.sendingWhitelist = ListeningWhitelist.newBuilder().priority(listenerPriority).types(packets).gamePhase(gamePhase).options(serverOptions).build();
        }
        if (connectionSide.isForClient()) {
            this.receivingWhitelist = ListeningWhitelist.newBuilder().priority(listenerPriority).types(packets).gamePhase(gamePhase).options(clientOptions).build();
        }
        this.plugin = plugin;
        this.connectionSide = connectionSide;
    }
    
    private static <T> T[] except(final T[] values, final T[] buffer, final T except) {
        final List<T> result = (List<T>)Lists.newArrayList((Object[])values);
        result.remove(except);
        return result.toArray(buffer);
    }
    
    @Override
    public void onPacketReceiving(final PacketEvent event) {
        throw new IllegalStateException("Override onPacketReceiving to get notifcations of received packets!");
    }
    
    @Override
    public void onPacketSending(final PacketEvent event) {
        throw new IllegalStateException("Override onPacketSending to get notifcations of sent packets!");
    }
    
    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return this.receivingWhitelist;
    }
    
    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return this.sendingWhitelist;
    }
    
    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }
    
    public static String getPluginName(final PacketListener listener) {
        return getPluginName(listener.getPlugin());
    }
    
    public static String getPluginName(final Plugin plugin) {
        if (plugin == null) {
            return "UNKNOWN";
        }
        try {
            return plugin.getName();
        }
        catch (NoSuchMethodError e) {
            return plugin.toString();
        }
    }
    
    @Override
    public String toString() {
        return String.format("PacketAdapter[plugin=%s, sending=%s, receiving=%s]", getPluginName(this), this.sendingWhitelist, this.receivingWhitelist);
    }
    
    public static AdapterParameteters params() {
        return new AdapterParameteters();
    }
    
    @Deprecated
    public static AdapterParameteters params(final Plugin plugin, final Integer... packets) {
        return new AdapterParameteters().plugin(plugin).packets(packets);
    }
    
    public static AdapterParameteters params(final Plugin plugin, final PacketType... packets) {
        return new AdapterParameteters().plugin(plugin).types(packets);
    }
    
    private static AdapterParameteters checkValidity(final AdapterParameteters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be NULL.");
        }
        if (params.plugin == null) {
            throw new IllegalStateException("Plugin was never set in the parameters.");
        }
        if (params.connectionSide == null) {
            throw new IllegalStateException("Connection side was never set in the parameters.");
        }
        if (params.packets == null) {
            throw new IllegalStateException("Packet IDs was never set in the parameters.");
        }
        return params;
    }
    
    public static class AdapterParameteters
    {
        private Plugin plugin;
        private ConnectionSide connectionSide;
        private PacketType[] packets;
        private GamePhase gamePhase;
        private ListenerOptions[] options;
        private ListenerPriority listenerPriority;
        
        public AdapterParameteters() {
            this.gamePhase = GamePhase.PLAYING;
            this.options = new ListenerOptions[0];
            this.listenerPriority = ListenerPriority.NORMAL;
        }
        
        public AdapterParameteters plugin(@Nonnull final Plugin plugin) {
            this.plugin = (Plugin)Preconditions.checkNotNull((Object)plugin, (Object)"plugin cannot be NULL.");
            return this;
        }
        
        public AdapterParameteters connectionSide(@Nonnull final ConnectionSide connectionSide) {
            this.connectionSide = (ConnectionSide)Preconditions.checkNotNull((Object)connectionSide, (Object)"connectionside cannot be NULL.");
            return this;
        }
        
        public AdapterParameteters clientSide() {
            return this.connectionSide(ConnectionSide.add(this.connectionSide, ConnectionSide.CLIENT_SIDE));
        }
        
        public AdapterParameteters serverSide() {
            return this.connectionSide(ConnectionSide.add(this.connectionSide, ConnectionSide.SERVER_SIDE));
        }
        
        public AdapterParameteters listenerPriority(@Nonnull final ListenerPriority listenerPriority) {
            this.listenerPriority = (ListenerPriority)Preconditions.checkNotNull((Object)listenerPriority, (Object)"listener priority cannot be NULL.");
            return this;
        }
        
        public AdapterParameteters gamePhase(@Nonnull final GamePhase gamePhase) {
            this.gamePhase = (GamePhase)Preconditions.checkNotNull((Object)gamePhase, (Object)"gamePhase cannot be NULL.");
            return this;
        }
        
        public AdapterParameteters loginPhase() {
            return this.gamePhase(GamePhase.LOGIN);
        }
        
        public AdapterParameteters options(@Nonnull final ListenerOptions... options) {
            this.options = (ListenerOptions[])Preconditions.checkNotNull((Object)options, (Object)"options cannot be NULL.");
            return this;
        }
        
        public AdapterParameteters options(@Nonnull final Set<? extends ListenerOptions> options) {
            Preconditions.checkNotNull((Object)options, (Object)"options cannot be NULL.");
            this.options = options.toArray(new ListenerOptions[0]);
            return this;
        }
        
        private AdapterParameteters addOption(final ListenerOptions option) {
            if (this.options == null) {
                return this.options(option);
            }
            final Set<ListenerOptions> current = (Set<ListenerOptions>)Sets.newHashSet((Object[])this.options);
            current.add(option);
            return this.options(current);
        }
        
        public AdapterParameteters optionIntercept() {
            return this.addOption(ListenerOptions.INTERCEPT_INPUT_BUFFER);
        }
        
        public AdapterParameteters optionManualGamePhase() {
            return this.addOption(ListenerOptions.DISABLE_GAMEPHASE_DETECTION);
        }
        
        public AdapterParameteters optionAsync() {
            return this.addOption(ListenerOptions.ASYNC);
        }
        
        @Deprecated
        public AdapterParameteters packets(@Nonnull final Integer... packets) {
            Preconditions.checkNotNull((Object)packets, (Object)"packets cannot be NULL");
            final PacketType[] types = new PacketType[packets.length];
            for (int i = 0; i < types.length; ++i) {
                types[i] = PacketType.findLegacy(packets[i]);
            }
            this.packets = types;
            return this;
        }
        
        @Deprecated
        public AdapterParameteters packets(@Nonnull final Set<Integer> packets) {
            return this.packets((Integer[])packets.toArray(new Integer[0]));
        }
        
        public AdapterParameteters types(@Nonnull final PacketType... packets) {
            if (this.connectionSide == null) {
                for (final PacketType type : packets) {
                    this.connectionSide = ConnectionSide.add(this.connectionSide, type.getSender().toSide());
                }
            }
            this.packets = (PacketType[])Preconditions.checkNotNull((Object)packets, (Object)"packets cannot be NULL");
            if (packets.length == 0) {
                throw new IllegalArgumentException("Passed an empty packet type array.");
            }
            return this;
        }
        
        public AdapterParameteters types(@Nonnull final Set<PacketType> packets) {
            return this.types((PacketType[])packets.toArray(new PacketType[0]));
        }
    }
}
