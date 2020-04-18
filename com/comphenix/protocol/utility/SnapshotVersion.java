package com.comphenix.protocol.utility;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Pattern;
import java.io.Serializable;

public class SnapshotVersion implements Comparable<SnapshotVersion>, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final Pattern SNAPSHOT_PATTERN;
    private final Date snapshotDate;
    private final int snapshotWeekVersion;
    private transient String rawString;
    
    public SnapshotVersion(final String version) {
        final Matcher matcher = SnapshotVersion.SNAPSHOT_PATTERN.matcher(version.trim());
        if (matcher.matches()) {
            try {
                this.snapshotDate = getDateFormat().parse(matcher.group(1));
                this.snapshotWeekVersion = matcher.group(2).charAt(0) - 'a';
                this.rawString = version;
                return;
            }
            catch (ParseException e) {
                throw new IllegalArgumentException("Date implied by snapshot version is invalid.", e);
            }
            throw new IllegalArgumentException("Cannot parse " + version + " as a snapshot version.");
        }
        throw new IllegalArgumentException("Cannot parse " + version + " as a snapshot version.");
    }
    
    private static SimpleDateFormat getDateFormat() {
        final SimpleDateFormat format = new SimpleDateFormat("yy'w'ww", Locale.US);
        format.setLenient(false);
        return format;
    }
    
    public int getSnapshotWeekVersion() {
        return this.snapshotWeekVersion;
    }
    
    public Date getSnapshotDate() {
        return this.snapshotDate;
    }
    
    public String getSnapshotString() {
        if (this.rawString == null) {
            final Calendar current = Calendar.getInstance(Locale.US);
            current.setTime(this.snapshotDate);
            this.rawString = String.format("%02dw%02d%s", current.get(1) % 100, current.get(3), (char)(97 + this.snapshotWeekVersion));
        }
        return this.rawString;
    }
    
    @Override
    public int compareTo(final SnapshotVersion o) {
        if (o == null) {
            return 1;
        }
        return ComparisonChain.start().compare((Comparable)this.snapshotDate, (Comparable)o.getSnapshotDate()).compare(this.snapshotWeekVersion, o.getSnapshotWeekVersion()).result();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SnapshotVersion) {
            final SnapshotVersion other = (SnapshotVersion)obj;
            return Objects.equal((Object)this.snapshotDate, (Object)other.getSnapshotDate()) && this.snapshotWeekVersion == other.getSnapshotWeekVersion();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.snapshotDate, this.snapshotWeekVersion });
    }
    
    @Override
    public String toString() {
        return this.getSnapshotString();
    }
    
    static {
        SNAPSHOT_PATTERN = Pattern.compile("(\\d{2}w\\d{2})([a-z])");
    }
}
