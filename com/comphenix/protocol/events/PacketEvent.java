package com.comphenix.protocol.events;

import com.google.common.collect.Multimaps;
import com.google.common.collect.HashMultimap;
import java.io.ObjectInputStream;
import java.io.IOException;
import org.bukkit.OfflinePlayer;
import java.io.ObjectOutputStream;
import com.comphenix.protocol.injector.server.TemporaryPlayer;
import com.google.common.base.Preconditions;
import com.comphenix.protocol.error.PluginContext;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Objects;
import org.bukkit.Bukkit;
import com.comphenix.protocol.async.AsyncMarker;
import org.bukkit.entity.Player;
import java.lang.ref.WeakReference;
import com.comphenix.protocol.PacketType;
import com.google.common.collect.SetMultimap;
import com.comphenix.protocol.error.ReportType;
import org.bukkit.event.Cancellable;
import java.util.EventObject;

public class PacketEvent extends EventObject implements Cancellable
{
    public static final ReportType REPORT_CHANGING_PACKET_TYPE_IS_CONFUSING;
    private static final SetMultimap<PacketType, PacketType> CHANGE_WARNINGS;
    private static final long serialVersionUID = -5360289379097430620L;
    private transient WeakReference<Player> playerReference;
    private transient Player offlinePlayer;
    private PacketContainer packet;
    private boolean serverPacket;
    private boolean cancel;
    private AsyncMarker asyncMarker;
    private boolean asynchronous;
    NetworkMarker networkMarker;
    private boolean readOnly;
    private boolean filtered;
    
    public PacketEvent(final Object source) {
        super(source);
        this.filtered = true;
    }
    
    private PacketEvent(final Object source, final PacketContainer packet, final Player player, final boolean serverPacket) {
        this(source, packet, null, player, serverPacket, true);
    }
    
    private PacketEvent(final Object source, final PacketContainer packet, final NetworkMarker marker, final Player player, final boolean serverPacket, final boolean filtered) {
        super(source);
        this.packet = packet;
        this.playerReference = new WeakReference<Player>(player);
        this.networkMarker = marker;
        this.serverPacket = serverPacket;
        this.filtered = filtered;
    }
    
    private PacketEvent(final PacketEvent origial, final AsyncMarker asyncMarker) {
        super(origial.source);
        this.packet = origial.packet;
        this.playerReference = origial.playerReference;
        this.cancel = origial.cancel;
        this.serverPacket = origial.serverPacket;
        this.filtered = origial.filtered;
        this.networkMarker = origial.networkMarker;
        this.asyncMarker = asyncMarker;
        this.asynchronous = true;
    }
    
    public static PacketEvent fromClient(final Object source, final PacketContainer packet, final Player client) {
        return new PacketEvent(source, packet, client, false);
    }
    
    public static PacketEvent fromClient(final Object source, final PacketContainer packet, final NetworkMarker marker, final Player client) {
        return new PacketEvent(source, packet, marker, client, false, true);
    }
    
    public static PacketEvent fromClient(final Object source, final PacketContainer packet, final NetworkMarker marker, final Player client, final boolean filtered) {
        return new PacketEvent(source, packet, marker, client, false, filtered);
    }
    
    public static PacketEvent fromServer(final Object source, final PacketContainer packet, final Player recipient) {
        return new PacketEvent(source, packet, recipient, true);
    }
    
    public static PacketEvent fromServer(final Object source, final PacketContainer packet, final NetworkMarker marker, final Player recipient) {
        return new PacketEvent(source, packet, marker, recipient, true, true);
    }
    
    public static PacketEvent fromServer(final Object source, final PacketContainer packet, final NetworkMarker marker, final Player recipient, final boolean filtered) {
        return new PacketEvent(source, packet, marker, recipient, true, filtered);
    }
    
    public static PacketEvent fromSynchronous(final PacketEvent event, final AsyncMarker marker) {
        return new PacketEvent(event, marker);
    }
    
    public boolean isAsync() {
        return !Bukkit.isPrimaryThread();
    }
    
    public PacketContainer getPacket() {
        return this.packet;
    }
    
    public void setPacket(final PacketContainer packet) {
        if (this.readOnly) {
            throw new IllegalStateException("The packet event is read-only.");
        }
        if (packet == null) {
            throw new IllegalArgumentException("Cannot set packet to NULL. Use setCancelled() instead.");
        }
        final PacketType oldType = this.packet.getType();
        final PacketType newType = packet.getType();
        if (this.packet != null && !Objects.equal((Object)oldType, (Object)newType) && PacketEvent.CHANGE_WARNINGS.put((Object)oldType, (Object)newType)) {
            ProtocolLibrary.getErrorReporter().reportWarning(this, Report.newBuilder(PacketEvent.REPORT_CHANGING_PACKET_TYPE_IS_CONFUSING).messageParam(PluginContext.getPluginCaller(new Exception()), oldType, newType).build());
        }
        this.packet = packet;
    }
    
    @Deprecated
    public int getPacketID() {
        return this.packet.getID();
    }
    
    public PacketType getPacketType() {
        return this.packet.getType();
    }
    
    public boolean isCancelled() {
        return this.cancel;
    }
    
    public NetworkMarker getNetworkMarker() {
        if (this.networkMarker == null) {
            if (!this.isServerPacket()) {
                throw new IllegalStateException("Add the option ListenerOptions.INTERCEPT_INPUT_BUFFER to your listener.");
            }
            this.networkMarker = new NetworkMarker.EmptyBufferMarker(this.serverPacket ? ConnectionSide.SERVER_SIDE : ConnectionSide.CLIENT_SIDE);
        }
        return this.networkMarker;
    }
    
    public void setNetworkMarker(final NetworkMarker networkMarker) {
        this.networkMarker = (NetworkMarker)Preconditions.checkNotNull((Object)networkMarker, (Object)"marker cannot be NULL");
    }
    
    public void setCancelled(final boolean cancel) {
        if (this.readOnly) {
            throw new IllegalStateException("The packet event is read-only.");
        }
        this.cancel = cancel;
    }
    
    public Player getPlayer() {
        final Player player = this.playerReference.get();
        if (player instanceof TemporaryPlayer) {
            final Player updated = player.getPlayer();
            if (updated != null && !(updated instanceof TemporaryPlayer)) {
                this.playerReference.clear();
                this.playerReference = new WeakReference<Player>(updated);
                return updated;
            }
        }
        return player;
    }
    
    public boolean isPlayerTemporary() {
        return this.getPlayer() instanceof TemporaryPlayer;
    }
    
    public boolean isFiltered() {
        return this.filtered;
    }
    
    public boolean isServerPacket() {
        return this.serverPacket;
    }
    
    public AsyncMarker getAsyncMarker() {
        return this.asyncMarker;
    }
    
    public void setAsyncMarker(final AsyncMarker asyncMarker) {
        if (this.isAsynchronous()) {
            throw new IllegalStateException("The marker is immutable for asynchronous events");
        }
        if (this.readOnly) {
            throw new IllegalStateException("The packet event is read-only.");
        }
        this.asyncMarker = asyncMarker;
    }
    
    public boolean isReadOnly() {
        return this.readOnly;
    }
    
    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }
    
    public boolean isAsynchronous() {
        return this.asynchronous;
    }
    
    public void schedule(final ScheduledPacket scheduled) {
        this.getNetworkMarker().getScheduledPackets().add(scheduled);
    }
    
    public boolean unschedule(final ScheduledPacket scheduled) {
        return this.networkMarker != null && this.networkMarker.getScheduledPackets().remove(scheduled);
    }
    
    private void writeObject(final ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        output.writeObject((this.playerReference.get() != null) ? new SerializedOfflinePlayer(this.playerReference.get()) : null);
    }
    
    private void readObject(final ObjectInputStream input) throws ClassNotFoundException, IOException {
        input.defaultReadObject();
        final SerializedOfflinePlayer serialized = (SerializedOfflinePlayer)input.readObject();
        if (serialized != null) {
            this.offlinePlayer = serialized.getPlayer();
            this.playerReference = new WeakReference<Player>(this.offlinePlayer);
        }
    }
    
    @Override
    public String toString() {
        return "PacketEvent[player=" + this.getPlayer() + ", packet=" + this.packet + "]";
    }
    
    static {
        REPORT_CHANGING_PACKET_TYPE_IS_CONFUSING = new ReportType("Plugin %s changed packet type from %s to %s in packet listener. This is confusing for other plugins! (Not an error, though!)");
        CHANGE_WARNINGS = Multimaps.synchronizedSetMultimap((SetMultimap)HashMultimap.create());
    }
}
