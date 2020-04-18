package com.comphenix.protocol.events;

import java.util.concurrent.ConcurrentHashMap;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;
import com.comphenix.protocol.utility.EnhancerFactory;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import java.io.Serializable;
import org.bukkit.OfflinePlayer;

class SerializedOfflinePlayer implements OfflinePlayer, Serializable
{
    private static final long serialVersionUID = -2728976288470282810L;
    private transient Location bedSpawnLocation;
    private String name;
    private UUID uuid;
    private long firstPlayed;
    private long lastPlayed;
    private boolean operator;
    private boolean banned;
    private boolean playedBefore;
    private boolean online;
    private boolean whitelisted;
    private static Map<String, Method> lookup;
    
    public SerializedOfflinePlayer() {
    }
    
    public SerializedOfflinePlayer(final OfflinePlayer offline) {
        this.name = offline.getName();
        this.uuid = offline.getUniqueId();
        this.firstPlayed = offline.getFirstPlayed();
        this.lastPlayed = offline.getLastPlayed();
        this.operator = offline.isOp();
        this.banned = offline.isBanned();
        this.playedBefore = offline.hasPlayedBefore();
        this.online = offline.isOnline();
        this.whitelisted = offline.isWhitelisted();
    }
    
    public boolean isOp() {
        return this.operator;
    }
    
    public void setOp(final boolean operator) {
        this.operator = operator;
    }
    
    public Map<String, Object> serialize() {
        throw new UnsupportedOperationException();
    }
    
    public Location getBedSpawnLocation() {
        return this.bedSpawnLocation;
    }
    
    public long getFirstPlayed() {
        return this.firstPlayed;
    }
    
    public long getLastPlayed() {
        return this.lastPlayed;
    }
    
    public UUID getUniqueId() {
        return this.uuid;
    }
    
    public String getName() {
        return this.name;
    }
    
    public boolean hasPlayedBefore() {
        return this.playedBefore;
    }
    
    public boolean isBanned() {
        return this.banned;
    }
    
    public void setBanned(final boolean banned) {
        this.banned = banned;
    }
    
    public boolean isOnline() {
        return this.online;
    }
    
    public boolean isWhitelisted() {
        return this.whitelisted;
    }
    
    public void setWhitelisted(final boolean whitelisted) {
        this.whitelisted = whitelisted;
    }
    
    private void writeObject(final ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        output.writeUTF(this.bedSpawnLocation.getWorld().getName());
        output.writeDouble(this.bedSpawnLocation.getX());
        output.writeDouble(this.bedSpawnLocation.getY());
        output.writeDouble(this.bedSpawnLocation.getZ());
    }
    
    private void readObject(final ObjectInputStream input) throws ClassNotFoundException, IOException {
        input.defaultReadObject();
        this.bedSpawnLocation = new Location(this.getWorld(input.readUTF()), input.readDouble(), input.readDouble(), input.readDouble());
    }
    
    private World getWorld(final String name) {
        try {
            return Bukkit.getServer().getWorld(name);
        }
        catch (Exception e) {
            return null;
        }
    }
    
    public Player getPlayer() {
        try {
            return Bukkit.getServer().getPlayerExact(this.name);
        }
        catch (Exception e) {
            return this.getProxyPlayer();
        }
    }
    
    public Player getProxyPlayer() {
        if (SerializedOfflinePlayer.lookup.size() == 0) {
            for (final Method method : OfflinePlayer.class.getMethods()) {
                SerializedOfflinePlayer.lookup.put(method.getName(), method);
            }
        }
        final Enhancer ex = EnhancerFactory.getInstance().createEnhancer();
        ex.setSuperclass(Player.class);
        ex.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
                final Method offlineMethod = SerializedOfflinePlayer.lookup.get(method.getName());
                if (offlineMethod == null) {
                    throw new UnsupportedOperationException("The method " + method.getName() + " is not supported for offline players.");
                }
                return offlineMethod.invoke(SerializedOfflinePlayer.this, args);
            }
        });
        return (Player)ex.create();
    }
    
    static {
        SerializedOfflinePlayer.lookup = new ConcurrentHashMap<String, Method>();
    }
}
