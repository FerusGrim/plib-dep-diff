package com.comphenix.protocol.events;

import com.google.common.base.Objects;
import java.util.Collections;
import java.util.Arrays;
import com.google.common.collect.Sets;
import java.util.EnumSet;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import java.util.Collection;
import com.comphenix.protocol.PacketType;
import java.util.Set;
import com.comphenix.protocol.injector.GamePhase;

public class ListeningWhitelist
{
    public static final ListeningWhitelist EMPTY_WHITELIST;
    private final ListenerPriority priority;
    private final GamePhase gamePhase;
    private final Set<ListenerOptions> options;
    private final Set<PacketType> types;
    private transient Set<Integer> intWhitelist;
    
    private ListeningWhitelist(final Builder builder) {
        this.priority = builder.priority;
        this.types = builder.types;
        this.gamePhase = builder.gamePhase;
        this.options = builder.options;
    }
    
    @Deprecated
    public ListeningWhitelist(final ListenerPriority priority, final Set<Integer> whitelist) {
        this(priority, whitelist, GamePhase.PLAYING);
    }
    
    @Deprecated
    public ListeningWhitelist(final ListenerPriority priority, final Set<Integer> whitelist, final GamePhase gamePhase) {
        this.priority = priority;
        this.types = PacketRegistry.toPacketTypes(safeSet(whitelist));
        this.gamePhase = gamePhase;
        this.options = EnumSet.noneOf(ListenerOptions.class);
    }
    
    @Deprecated
    public ListeningWhitelist(final ListenerPriority priority, final Integer... whitelist) {
        this.priority = priority;
        this.types = PacketRegistry.toPacketTypes(Sets.newHashSet((Object[])whitelist));
        this.gamePhase = GamePhase.PLAYING;
        this.options = EnumSet.noneOf(ListenerOptions.class);
    }
    
    @Deprecated
    public ListeningWhitelist(final ListenerPriority priority, final Integer[] whitelist, final GamePhase gamePhase) {
        this.priority = priority;
        this.types = PacketRegistry.toPacketTypes(Sets.newHashSet((Object[])whitelist));
        this.gamePhase = gamePhase;
        this.options = EnumSet.noneOf(ListenerOptions.class);
    }
    
    @Deprecated
    public ListeningWhitelist(final ListenerPriority priority, final Integer[] whitelist, final GamePhase gamePhase, final ListenerOptions... options) {
        this.priority = priority;
        this.types = PacketRegistry.toPacketTypes(Sets.newHashSet((Object[])whitelist));
        this.gamePhase = gamePhase;
        this.options = safeEnumSet(Arrays.asList(options), ListenerOptions.class);
    }
    
    public boolean isEnabled() {
        return this.types != null && this.types.size() > 0;
    }
    
    public ListenerPriority getPriority() {
        return this.priority;
    }
    
    @Deprecated
    public Set<Integer> getWhitelist() {
        if (this.intWhitelist == null) {
            this.intWhitelist = PacketRegistry.toLegacy(this.types);
        }
        return this.intWhitelist;
    }
    
    public Set<PacketType> getTypes() {
        return this.types;
    }
    
    public GamePhase getGamePhase() {
        return this.gamePhase;
    }
    
    public Set<ListenerOptions> getOptions() {
        return Collections.unmodifiableSet((Set<? extends ListenerOptions>)this.options);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.priority, this.types, this.gamePhase, this.options });
    }
    
    public static boolean containsAny(final ListeningWhitelist whitelist, final int... idList) {
        if (whitelist != null) {
            for (int i = 0; i < idList.length; ++i) {
                if (whitelist.getWhitelist().contains(idList[i])) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static boolean isEmpty(final ListeningWhitelist whitelist) {
        return whitelist == ListeningWhitelist.EMPTY_WHITELIST || whitelist == null || whitelist.getTypes().isEmpty();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ListeningWhitelist) {
            final ListeningWhitelist other = (ListeningWhitelist)obj;
            return Objects.equal((Object)this.priority, (Object)other.priority) && Objects.equal((Object)this.types, (Object)other.types) && Objects.equal((Object)this.gamePhase, (Object)other.gamePhase) && Objects.equal((Object)this.options, (Object)other.options);
        }
        return false;
    }
    
    @Override
    public String toString() {
        if (this == ListeningWhitelist.EMPTY_WHITELIST) {
            return "EMPTY_WHITELIST";
        }
        return "ListeningWhitelist[priority=" + this.priority + ", packets=" + this.types + ", gamephase=" + this.gamePhase + ", options=" + this.options + "]";
    }
    
    public static Builder newBuilder() {
        return new Builder((ListeningWhitelist)null);
    }
    
    public static Builder newBuilder(final ListeningWhitelist template) {
        return new Builder(template);
    }
    
    private static <T extends Enum<T>> EnumSet<T> safeEnumSet(final Collection<T> options, final Class<T> enumClass) {
        if (options != null && !options.isEmpty()) {
            return EnumSet.copyOf(options);
        }
        return EnumSet.noneOf(enumClass);
    }
    
    private static <T> Set<T> safeSet(final Collection<T> set) {
        if (set != null) {
            return (Set<T>)Sets.newHashSet((Iterable)set);
        }
        return Collections.emptySet();
    }
    
    static {
        EMPTY_WHITELIST = new ListeningWhitelist(ListenerPriority.LOW, new Integer[0]);
    }
    
    public static class Builder
    {
        private ListenerPriority priority;
        private Set<PacketType> types;
        private GamePhase gamePhase;
        private Set<ListenerOptions> options;
        
        private Builder(final ListeningWhitelist template) {
            this.priority = ListenerPriority.NORMAL;
            this.types = (Set<PacketType>)Sets.newHashSet();
            this.gamePhase = GamePhase.PLAYING;
            this.options = (Set<ListenerOptions>)Sets.newHashSet();
            if (template != null) {
                this.priority(template.getPriority());
                this.gamePhase(template.getGamePhase());
                this.types(template.getTypes());
                this.options(template.getOptions());
            }
        }
        
        public Builder priority(final ListenerPriority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder monitor() {
            return this.priority(ListenerPriority.MONITOR);
        }
        
        public Builder normal() {
            return this.priority(ListenerPriority.NORMAL);
        }
        
        public Builder lowest() {
            return this.priority(ListenerPriority.LOWEST);
        }
        
        public Builder low() {
            return this.priority(ListenerPriority.LOW);
        }
        
        public Builder highest() {
            return this.priority(ListenerPriority.HIGHEST);
        }
        
        public Builder high() {
            return this.priority(ListenerPriority.HIGH);
        }
        
        @Deprecated
        public Builder whitelist(final Collection<Integer> whitelist) {
            this.types = PacketRegistry.toPacketTypes(safeSet((Collection<Object>)whitelist));
            return this;
        }
        
        public Builder types(final PacketType... types) {
            this.types = (Set<PacketType>)safeSet((Collection<Object>)Sets.newHashSet((Object[])types));
            return this;
        }
        
        public Builder types(final Collection<PacketType> types) {
            this.types = (Set<PacketType>)safeSet((Collection<Object>)types);
            return this;
        }
        
        public Builder gamePhase(final GamePhase gamePhase) {
            this.gamePhase = gamePhase;
            return this;
        }
        
        public Builder gamePhaseBoth() {
            return this.gamePhase(GamePhase.BOTH);
        }
        
        public Builder options(final Set<ListenerOptions> options) {
            this.options = (Set<ListenerOptions>)safeSet((Collection<Object>)options);
            return this;
        }
        
        public Builder options(final Collection<ListenerOptions> options) {
            this.options = (Set<ListenerOptions>)safeSet((Collection<Object>)options);
            return this;
        }
        
        public Builder options(final ListenerOptions[] serverOptions) {
            this.options = (Set<ListenerOptions>)safeSet((Collection<Object>)Sets.newHashSet((Object[])serverOptions));
            return this;
        }
        
        public Builder mergeOptions(final ListenerOptions... serverOptions) {
            return this.mergeOptions(Arrays.asList(serverOptions));
        }
        
        public Builder mergeOptions(final Collection<ListenerOptions> serverOptions) {
            if (this.options == null) {
                return this.options(serverOptions);
            }
            this.options.addAll(serverOptions);
            return this;
        }
        
        public ListeningWhitelist build() {
            return new ListeningWhitelist(this, null);
        }
    }
}
