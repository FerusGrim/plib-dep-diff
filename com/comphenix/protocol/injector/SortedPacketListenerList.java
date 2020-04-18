package com.comphenix.protocol.injector;

import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.timing.TimedTracker;
import java.util.Iterator;
import java.util.Collection;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.timing.TimedListenerManager;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.concurrency.AbstractConcurrentListenerMultimap;

public final class SortedPacketListenerList extends AbstractConcurrentListenerMultimap<PacketListener>
{
    private TimedListenerManager timedManager;
    
    public SortedPacketListenerList() {
        this.timedManager = TimedListenerManager.getInstance();
    }
    
    public void invokePacketRecieving(final ErrorReporter reporter, final PacketEvent event) {
        final Collection<PrioritizedListener<PacketListener>> list = this.getListener(event.getPacketType());
        if (list == null) {
            return;
        }
        if (this.timedManager.isTiming()) {
            for (final PrioritizedListener<PacketListener> element : list) {
                final TimedTracker tracker = this.timedManager.getTracker(element.getListener(), TimedListenerManager.ListenerType.SYNC_CLIENT_SIDE);
                final long token = tracker.beginTracking();
                this.invokeReceivingListener(reporter, event, element);
                tracker.endTracking(token, event.getPacketType());
            }
        }
        else {
            for (final PrioritizedListener<PacketListener> element : list) {
                this.invokeReceivingListener(reporter, event, element);
            }
        }
    }
    
    public void invokePacketRecieving(final ErrorReporter reporter, final PacketEvent event, final ListenerPriority priorityFilter) {
        final Collection<PrioritizedListener<PacketListener>> list = this.getListener(event.getPacketType());
        if (list == null) {
            return;
        }
        if (this.timedManager.isTiming()) {
            for (final PrioritizedListener<PacketListener> element : list) {
                if (element.getPriority() == priorityFilter) {
                    final TimedTracker tracker = this.timedManager.getTracker(element.getListener(), TimedListenerManager.ListenerType.SYNC_CLIENT_SIDE);
                    final long token = tracker.beginTracking();
                    this.invokeReceivingListener(reporter, event, element);
                    tracker.endTracking(token, event.getPacketType());
                }
            }
        }
        else {
            for (final PrioritizedListener<PacketListener> element : list) {
                if (element.getPriority() == priorityFilter) {
                    this.invokeReceivingListener(reporter, event, element);
                }
            }
        }
    }
    
    private final void invokeReceivingListener(final ErrorReporter reporter, final PacketEvent event, final PrioritizedListener<PacketListener> element) {
        try {
            event.setReadOnly(element.getPriority() == ListenerPriority.MONITOR);
            element.getListener().onPacketReceiving(event);
        }
        catch (OutOfMemoryError e) {
            throw e;
        }
        catch (ThreadDeath e2) {
            throw e2;
        }
        catch (Throwable e3) {
            reporter.reportMinimal(element.getListener().getPlugin(), "onPacketReceiving(PacketEvent)", e3, event.getPacket().getHandle());
        }
    }
    
    public void invokePacketSending(final ErrorReporter reporter, final PacketEvent event) {
        final Collection<PrioritizedListener<PacketListener>> list = this.getListener(event.getPacketType());
        if (list == null) {
            return;
        }
        if (this.timedManager.isTiming()) {
            for (final PrioritizedListener<PacketListener> element : list) {
                final TimedTracker tracker = this.timedManager.getTracker(element.getListener(), TimedListenerManager.ListenerType.SYNC_SERVER_SIDE);
                final long token = tracker.beginTracking();
                this.invokeSendingListener(reporter, event, element);
                tracker.endTracking(token, event.getPacketType());
            }
        }
        else {
            for (final PrioritizedListener<PacketListener> element : list) {
                this.invokeSendingListener(reporter, event, element);
            }
        }
    }
    
    public void invokePacketSending(final ErrorReporter reporter, final PacketEvent event, final ListenerPriority priorityFilter) {
        final Collection<PrioritizedListener<PacketListener>> list = this.getListener(event.getPacketType());
        if (list == null) {
            return;
        }
        if (this.timedManager.isTiming()) {
            for (final PrioritizedListener<PacketListener> element : list) {
                if (element.getPriority() == priorityFilter) {
                    final TimedTracker tracker = this.timedManager.getTracker(element.getListener(), TimedListenerManager.ListenerType.SYNC_SERVER_SIDE);
                    final long token = tracker.beginTracking();
                    this.invokeSendingListener(reporter, event, element);
                    tracker.endTracking(token, event.getPacketType());
                }
            }
        }
        else {
            for (final PrioritizedListener<PacketListener> element : list) {
                if (element.getPriority() == priorityFilter) {
                    this.invokeSendingListener(reporter, event, element);
                }
            }
        }
    }
    
    private final void invokeSendingListener(final ErrorReporter reporter, final PacketEvent event, final PrioritizedListener<PacketListener> element) {
        try {
            event.setReadOnly(element.getPriority() == ListenerPriority.MONITOR);
            element.getListener().onPacketSending(event);
        }
        catch (OutOfMemoryError e) {
            throw e;
        }
        catch (ThreadDeath e2) {
            throw e2;
        }
        catch (Throwable e3) {
            reporter.reportMinimal(element.getListener().getPlugin(), "onPacketSending(PacketEvent)", e3, event.getPacket().getHandle());
        }
    }
}
