package com.comphenix.protocol.utility;

public final class Constants
{
    public static final String PACKAGE_VERSION = "v1_15_R1";
    public static final String NMS = "net.minecraft.server.v1_15_R1";
    public static final String OBC = "org.bukkit.craftbukkit.v1_15_R1";
    public static final MinecraftVersion CURRENT_VERSION;
    
    public static void init() {
        MinecraftReflection.setMinecraftPackage("net.minecraft.server.v1_15_R1", "org.bukkit.craftbukkit.v1_15_R1");
        MinecraftVersion.setCurrentVersion(Constants.CURRENT_VERSION);
    }
    
    static {
        CURRENT_VERSION = MinecraftVersion.BEE_UPDATE;
    }
}
