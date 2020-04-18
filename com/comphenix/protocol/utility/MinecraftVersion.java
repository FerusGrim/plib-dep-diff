package com.comphenix.protocol.utility;

import org.bukkit.Bukkit;
import java.util.regex.Matcher;
import com.google.common.base.Objects;
import java.util.Comparator;
import com.google.common.collect.Ordering;
import com.google.common.collect.ComparisonChain;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.bukkit.Server;
import java.util.regex.Pattern;
import java.io.Serializable;

public class MinecraftVersion implements Comparable<MinecraftVersion>, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final Pattern VERSION_PATTERN;
    public static final MinecraftVersion BEE_UPDATE;
    public static final MinecraftVersion VILLAGE_UPDATE;
    public static final MinecraftVersion AQUATIC_UPDATE;
    public static final MinecraftVersion COLOR_UPDATE;
    public static final MinecraftVersion EXPLORATION_UPDATE;
    public static final MinecraftVersion FROSTBURN_UPDATE;
    public static final MinecraftVersion COMBAT_UPDATE;
    public static final MinecraftVersion BOUNTIFUL_UPDATE;
    public static final MinecraftVersion SKIN_UPDATE;
    public static final MinecraftVersion WORLD_UPDATE;
    public static final MinecraftVersion HORSE_UPDATE;
    public static final MinecraftVersion REDSTONE_UPDATE;
    public static final MinecraftVersion SCARY_UPDATE;
    private final int major;
    private final int minor;
    private final int build;
    private final String development;
    private final SnapshotVersion snapshot;
    private static MinecraftVersion currentVersion;
    
    public MinecraftVersion(final Server server) {
        this(extractVersion(server.getVersion()));
    }
    
    public MinecraftVersion(final String versionOnly) {
        this(versionOnly, true);
    }
    
    private MinecraftVersion(final String versionOnly, final boolean parseSnapshot) {
        final String[] section = versionOnly.split("-");
        SnapshotVersion snapshot = null;
        int[] numbers = new int[3];
        try {
            numbers = this.parseVersion(section[0]);
        }
        catch (NumberFormatException cause) {
            if (!parseSnapshot) {
                throw cause;
            }
            try {
                snapshot = new SnapshotVersion(section[0]);
                final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                final MinecraftVersion latest = new MinecraftVersion("1.15.1", false);
                final boolean newer = snapshot.getSnapshotDate().compareTo(format.parse("2019-12-17")) > 0;
                numbers[0] = latest.getMajor();
                numbers[1] = latest.getMinor() + (newer ? 1 : -1);
                numbers[2] = 0;
            }
            catch (Exception e) {
                throw new IllegalStateException("Cannot parse " + section[0], e);
            }
        }
        this.major = numbers[0];
        this.minor = numbers[1];
        this.build = numbers[2];
        this.development = ((section.length > 1) ? section[1] : ((snapshot != null) ? "snapshot" : null));
        this.snapshot = snapshot;
    }
    
    public MinecraftVersion(final int major, final int minor, final int build) {
        this(major, minor, build, null);
    }
    
    public MinecraftVersion(final int major, final int minor, final int build, final String development) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.development = development;
        this.snapshot = null;
    }
    
    private int[] parseVersion(final String version) {
        final String[] elements = version.split("\\.");
        final int[] numbers = new int[3];
        if (elements.length < 1) {
            throw new IllegalStateException("Corrupt MC version: " + version);
        }
        for (int i = 0; i < Math.min(numbers.length, elements.length); ++i) {
            numbers[i] = Integer.parseInt(elements[i].trim());
        }
        return numbers;
    }
    
    public int getMajor() {
        return this.major;
    }
    
    public int getMinor() {
        return this.minor;
    }
    
    public int getBuild() {
        return this.build;
    }
    
    public String getDevelopmentStage() {
        return this.development;
    }
    
    public SnapshotVersion getSnapshot() {
        return this.snapshot;
    }
    
    public boolean isSnapshot() {
        return this.snapshot != null;
    }
    
    public boolean atOrAbove() {
        return getCurrentVersion().isAtLeast(this);
    }
    
    public String getVersion() {
        if (this.getDevelopmentStage() == null) {
            return String.format("%s.%s.%s", this.getMajor(), this.getMinor(), this.getBuild());
        }
        return String.format("%s.%s.%s-%s%s", this.getMajor(), this.getMinor(), this.getBuild(), this.getDevelopmentStage(), this.isSnapshot() ? this.snapshot : "");
    }
    
    @Override
    public int compareTo(final MinecraftVersion o) {
        if (o == null) {
            return 1;
        }
        return ComparisonChain.start().compare(this.getMajor(), o.getMajor()).compare(this.getMinor(), o.getMinor()).compare(this.getBuild(), o.getBuild()).compare((Object)this.getDevelopmentStage(), (Object)o.getDevelopmentStage(), (Comparator)Ordering.natural().nullsLast()).compare((Object)this.getSnapshot(), (Object)o.getSnapshot(), (Comparator)Ordering.natural().nullsFirst()).result();
    }
    
    public boolean isAtLeast(final MinecraftVersion other) {
        return other != null && this.compareTo(other) >= 0;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof MinecraftVersion) {
            final MinecraftVersion other = (MinecraftVersion)obj;
            return this.getMajor() == other.getMajor() && this.getMinor() == other.getMinor() && this.getBuild() == other.getBuild() && Objects.equal((Object)this.getDevelopmentStage(), (Object)other.getDevelopmentStage());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.getMajor(), this.getMinor(), this.getBuild() });
    }
    
    @Override
    public String toString() {
        return String.format("(MC: %s)", this.getVersion());
    }
    
    public static String extractVersion(final String text) {
        final Matcher version = MinecraftVersion.VERSION_PATTERN.matcher(text);
        if (version.matches() && version.group(1) != null) {
            return version.group(1);
        }
        throw new IllegalStateException("Cannot parse version String '" + text + "'");
    }
    
    public static MinecraftVersion fromServerVersion(final String serverVersion) {
        return new MinecraftVersion(extractVersion(serverVersion));
    }
    
    public static void setCurrentVersion(final MinecraftVersion version) {
        MinecraftVersion.currentVersion = version;
    }
    
    public static MinecraftVersion getCurrentVersion() {
        if (MinecraftVersion.currentVersion == null) {
            MinecraftVersion.currentVersion = fromServerVersion(Bukkit.getVersion());
        }
        return MinecraftVersion.currentVersion;
    }
    
    public static boolean atOrAbove(final MinecraftVersion version) {
        return getCurrentVersion().isAtLeast(version);
    }
    
    static {
        VERSION_PATTERN = Pattern.compile(".*\\(.*MC.\\s*([a-zA-z0-9\\-.]+).*");
        BEE_UPDATE = new MinecraftVersion("1.15");
        VILLAGE_UPDATE = new MinecraftVersion("1.14");
        AQUATIC_UPDATE = new MinecraftVersion("1.13");
        COLOR_UPDATE = new MinecraftVersion("1.12");
        EXPLORATION_UPDATE = new MinecraftVersion("1.11");
        FROSTBURN_UPDATE = new MinecraftVersion("1.10");
        COMBAT_UPDATE = new MinecraftVersion("1.9");
        BOUNTIFUL_UPDATE = new MinecraftVersion("1.8");
        SKIN_UPDATE = new MinecraftVersion("1.7.8");
        WORLD_UPDATE = new MinecraftVersion("1.7.2");
        HORSE_UPDATE = new MinecraftVersion("1.6.1");
        REDSTONE_UPDATE = new MinecraftVersion("1.5.0");
        SCARY_UPDATE = new MinecraftVersion("1.4.2");
    }
}
