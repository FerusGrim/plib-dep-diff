package com.comphenix.protocol.injector.player;

import com.comphenix.protocol.injector.PlayerInjectHooks;
import com.comphenix.protocol.injector.GamePhase;
import java.util.Iterator;
import com.comphenix.protocol.reflect.StructureModifier;
import java.util.Collections;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketListener;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.util.ArrayList;
import java.util.Map;
import com.google.common.collect.Sets;
import java.util.concurrent.ConcurrentHashMap;
import com.comphenix.protocol.injector.ListenerInvoker;
import org.bukkit.entity.Player;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.concurrency.IntegerSet;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.VolatileField;
import java.util.List;
import java.util.Set;
import com.comphenix.protocol.utility.MinecraftVersion;

class NetworkFieldInjector extends PlayerInjector
{
    private MinecraftVersion safeVersion;
    private Set<Object> ignoredPackets;
    private List<VolatileField> overridenLists;
    private static Field syncField;
    private Object syncObject;
    private IntegerSet sendingFilters;
    
    public NetworkFieldInjector(final ErrorReporter reporter, final Player player, final ListenerInvoker manager, final IntegerSet sendingFilters) {
        super(reporter, player, manager);
        this.safeVersion = new MinecraftVersion("1.4.4");
        this.ignoredPackets = (Set<Object>)Sets.newSetFromMap((Map)new ConcurrentHashMap());
        this.overridenLists = new ArrayList<VolatileField>();
        this.sendingFilters = sendingFilters;
    }
    
    @Override
    protected boolean hasListener(final int packetID) {
        return this.sendingFilters.contains(packetID);
    }
    
    @Override
    public synchronized void initialize(final Object injectionSource) throws IllegalAccessException {
        super.initialize(injectionSource);
        if (this.hasInitialized) {
            if (NetworkFieldInjector.syncField == null) {
                NetworkFieldInjector.syncField = FuzzyReflection.fromObject(this.networkManager, true).getFieldByType("java\\.lang\\.Object");
            }
            this.syncObject = FieldUtils.readField(NetworkFieldInjector.syncField, this.networkManager, true);
        }
    }
    
    @Override
    public void sendServerPacket(final Object packet, final NetworkMarker marker, final boolean filtered) throws InvocationTargetException {
        if (this.networkManager != null) {
            try {
                if (!filtered) {
                    this.ignoredPackets.add(packet);
                }
                if (marker != null) {
                    this.queuedMarkers.put(packet, marker);
                }
                NetworkFieldInjector.queueMethod.invoke(this.networkManager, packet);
                return;
            }
            catch (IllegalArgumentException e) {
                throw e;
            }
            catch (InvocationTargetException e2) {
                throw e2;
            }
            catch (IllegalAccessException e3) {
                throw new IllegalStateException("Unable to access queue method.", e3);
            }
            throw new IllegalStateException("Unable to load network mananager. Cannot send packet.");
        }
        throw new IllegalStateException("Unable to load network mananager. Cannot send packet.");
    }
    
    @Override
    public UnsupportedListener checkListener(final MinecraftVersion version, final PacketListener listener) {
        if (version != null && version.compareTo(this.safeVersion) > 0) {
            return null;
        }
        final int[] unsupported = { 51, 56 };
        if (ListeningWhitelist.containsAny(listener.getSendingWhitelist(), unsupported)) {
            return new UnsupportedListener("The NETWORK_FIELD_INJECTOR hook doesn't support map chunk listeners.", unsupported);
        }
        return null;
    }
    
    @Override
    public void injectManager() {
        if (this.networkManager != null) {
            final StructureModifier<List> list = (StructureModifier<List>)NetworkFieldInjector.networkModifier.withType(List.class);
            for (final Field field : list.getFields()) {
                final VolatileField overwriter = new VolatileField(field, this.networkManager, true);
                final List<Object> minecraftList = (List<Object>)overwriter.getOldValue();
                synchronized (this.syncObject) {
                    final List<Object> hackedList = new InjectedArrayList(this, this.ignoredPackets);
                    for (final Object packet : minecraftList) {
                        hackedList.add(packet);
                    }
                    minecraftList.clear();
                    overwriter.setValue(Collections.synchronizedList(hackedList));
                }
                this.overridenLists.add(overwriter);
            }
        }
    }
    
    @Override
    protected void cleanHook() {
        for (final VolatileField overriden : this.overridenLists) {
            final List<Object> minecraftList = (List<Object>)overriden.getOldValue();
            final List<Object> hacketList = (List<Object>)overriden.getValue();
            if (minecraftList == hacketList) {
                return;
            }
            synchronized (this.syncObject) {
                try {
                    for (final Object packet : (List)overriden.getValue()) {
                        minecraftList.add(packet);
                    }
                }
                finally {
                    overriden.revertValue();
                }
            }
        }
        this.overridenLists.clear();
    }
    
    @Override
    public void handleDisconnect() {
    }
    
    @Override
    public boolean canInject(final GamePhase phase) {
        return true;
    }
    
    @Override
    public PlayerInjectHooks getHookType() {
        return PlayerInjectHooks.NETWORK_HANDLER_FIELDS;
    }
    
    public interface FakePacket
    {
    }
}
