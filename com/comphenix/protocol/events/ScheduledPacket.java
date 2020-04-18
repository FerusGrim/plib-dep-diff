package com.comphenix.protocol.events;

import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.PacketStream;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType;
import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;

public class ScheduledPacket
{
    protected PacketContainer packet;
    protected Player target;
    protected boolean filtered;
    
    public ScheduledPacket(final PacketContainer packet, final Player target, final boolean filtered) {
        this.setPacket(packet);
        this.setTarget(target);
        this.setFiltered(filtered);
    }
    
    public static ScheduledPacket fromSilent(final PacketContainer packet, final Player target) {
        return new ScheduledPacket(packet, target, false);
    }
    
    public static ScheduledPacket fromFiltered(final PacketContainer packet, final Player target) {
        return new ScheduledPacket(packet, target, true);
    }
    
    public PacketContainer getPacket() {
        return this.packet;
    }
    
    public void setPacket(final PacketContainer packet) {
        this.packet = (PacketContainer)Preconditions.checkNotNull((Object)packet, (Object)"packet cannot be NULL");
    }
    
    public Player getTarget() {
        return this.target;
    }
    
    public void setTarget(final Player target) {
        this.target = (Player)Preconditions.checkNotNull((Object)target, (Object)"target cannot be NULL");
    }
    
    public boolean isFiltered() {
        return this.filtered;
    }
    
    public void setFiltered(final boolean filtered) {
        this.filtered = filtered;
    }
    
    public PacketType.Sender getSender() {
        return this.packet.getType().getSender();
    }
    
    public void schedule() {
        this.schedule(ProtocolLibrary.getProtocolManager());
    }
    
    public void schedule(final PacketStream stream) {
        Preconditions.checkNotNull((Object)stream, (Object)"stream cannot be NULL");
        try {
            if (this.getSender() == PacketType.Sender.CLIENT) {
                stream.recieveClientPacket(this.getTarget(), this.getPacket(), this.isFiltered());
            }
            else {
                stream.sendServerPacket(this.getTarget(), this.getPacket(), this.isFiltered());
            }
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot send packet " + this + " to " + stream);
        }
        catch (IllegalAccessException e2) {
            throw new RuntimeException("Cannot send packet " + this + " to " + stream);
        }
    }
    
    @Override
    public String toString() {
        return "ScheduledPacket[packet=" + this.packet + ", target=" + this.target + ", filtered=" + this.filtered + "]";
    }
}
