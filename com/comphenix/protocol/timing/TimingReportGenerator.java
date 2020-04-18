package com.comphenix.protocol.timing;

import com.google.common.base.Strings;
import java.util.Map;
import com.comphenix.protocol.PacketType;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Iterator;
import java.util.Date;
import java.io.BufferedWriter;
import java.io.Writer;
import com.google.common.io.Files;
import com.google.common.base.Charsets;
import java.io.File;

public class TimingReportGenerator
{
    private static final String NEWLINE;
    private static final String META_STARTED;
    private static final String META_STOPPED;
    private static final String PLUGIN_HEADER;
    private static final String LISTENER_HEADER;
    private static final String SEPERATION_LINE;
    private static final String STATISTICS_HEADER;
    private static final String STATISTICS_ROW;
    private static final String SUM_MAIN_THREAD;
    
    public void saveTo(final File destination, final TimedListenerManager manager) throws IOException {
        BufferedWriter writer = null;
        final Date started = manager.getStarted();
        final Date stopped = manager.getStopped();
        final long seconds = Math.abs((stopped.getTime() - started.getTime()) / 1000L);
        try {
            writer = Files.newWriter(destination, Charsets.UTF_8);
            writer.write(String.format(TimingReportGenerator.META_STARTED, started));
            writer.write(String.format(TimingReportGenerator.META_STOPPED, stopped, seconds));
            writer.write(TimingReportGenerator.NEWLINE);
            for (final String plugin : manager.getTrackedPlugins()) {
                writer.write(String.format(TimingReportGenerator.PLUGIN_HEADER, plugin));
                for (final TimedListenerManager.ListenerType type : TimedListenerManager.ListenerType.values()) {
                    final TimedTracker tracker = manager.getTracker(plugin, type);
                    if (tracker.getObservations() > 0) {
                        writer.write(String.format(TimingReportGenerator.LISTENER_HEADER, type));
                        writer.write(TimingReportGenerator.SEPERATION_LINE);
                        this.saveStatistics(writer, tracker, type);
                        writer.write(TimingReportGenerator.SEPERATION_LINE);
                    }
                }
                writer.write(TimingReportGenerator.NEWLINE);
            }
        }
        finally {
            if (writer != null) {
                writer.flush();
            }
        }
    }
    
    private void saveStatistics(final Writer destination, final TimedTracker tracker, final TimedListenerManager.ListenerType type) throws IOException {
        final Map<PacketType, StatisticsStream> streams = tracker.getStatistics();
        StatisticsStream sum = new StatisticsStream();
        int count = 0;
        destination.write(TimingReportGenerator.STATISTICS_HEADER);
        destination.write(TimingReportGenerator.SEPERATION_LINE);
        for (final PacketType key : Sets.newTreeSet((Iterable)streams.keySet())) {
            final StatisticsStream stream = streams.get(key);
            if (stream != null && stream.getCount() > 0) {
                this.printStatistic(destination, key, stream);
                ++count;
                sum = sum.add(stream);
            }
        }
        if (count > 1) {
            this.printStatistic(destination, null, sum);
        }
        if (type == TimedListenerManager.ListenerType.SYNC_SERVER_SIDE) {
            destination.write(String.format(TimingReportGenerator.SUM_MAIN_THREAD, this.toMilli(sum.getCount() * sum.getMean())));
        }
    }
    
    private void printStatistic(final Writer destination, final PacketType key, final StatisticsStream stream) throws IOException {
        destination.write(String.format(TimingReportGenerator.STATISTICS_ROW, (key != null) ? key.getProtocol() : "SUM", (key != null) ? key.name() : "-", (key != null) ? this.getPacketId(key) : "-", stream.getCount(), this.toMilli(stream.getMinimum()), this.toMilli(stream.getMaximum()), this.toMilli(stream.getMean()), this.toMilli(stream.getStandardDeviation())));
    }
    
    private String getPacketId(final PacketType type) {
        return Strings.padStart(Integer.toString(type.getCurrentId()), 2, '0') + " (Legacy: " + type.getLegacyId() + ")";
    }
    
    private double toMilli(final double value) {
        return value / 1000000.0;
    }
    
    static {
        NEWLINE = System.getProperty("line.separator");
        META_STARTED = "Started: %s" + TimingReportGenerator.NEWLINE;
        META_STOPPED = "Stopped: %s (after %s seconds)" + TimingReportGenerator.NEWLINE;
        PLUGIN_HEADER = "=== PLUGIN %s ===" + TimingReportGenerator.NEWLINE;
        LISTENER_HEADER = " TYPE: %s " + TimingReportGenerator.NEWLINE;
        SEPERATION_LINE = " " + Strings.repeat("-", 139) + TimingReportGenerator.NEWLINE;
        STATISTICS_HEADER = " Protocol:      Name:                         ID:                 Count:       Min (ms):       Max (ms):       Mean (ms):      Std (ms): " + TimingReportGenerator.NEWLINE;
        STATISTICS_ROW = " %-15s %-29s %-19s %-12d %-15.6f %-15.6f %-15.6f %.6f " + TimingReportGenerator.NEWLINE;
        SUM_MAIN_THREAD = " => Time on main thread: %.6f ms" + TimingReportGenerator.NEWLINE;
    }
}
