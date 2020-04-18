package com.comphenix.protocol.async;

import com.comphenix.protocol.timing.TimedTracker;
import com.comphenix.protocol.injector.PrioritizedListener;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Joiner;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketAdapter;
import com.google.common.base.Function;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.Plugin;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import java.util.HashSet;
import com.comphenix.protocol.timing.TimedListenerManager;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import com.comphenix.protocol.events.PacketListener;
import java.util.concurrent.atomic.AtomicInteger;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.error.ReportType;

public class AsyncListenerHandler
{
    public static final ReportType REPORT_HANDLER_NOT_STARTED;
    private static final PacketEvent INTERUPT_PACKET;
    private static final PacketEvent WAKEUP_PACKET;
    private static final int TICKS_PER_SECOND = 20;
    private static final AtomicInteger nextID;
    private static final int DEFAULT_CAPACITY = 1024;
    private volatile boolean cancelled;
    private final AtomicInteger started;
    private PacketListener listener;
    private AsyncFilterManager filterManager;
    private NullPacketListener nullPacketListener;
    private ArrayBlockingQueue<PacketEvent> queuedPackets;
    private final Set<Integer> stoppedTasks;
    private final Object stopLock;
    private int syncTask;
    private Thread mainThread;
    private int warningTask;
    private TimedListenerManager timedManager;
    
    AsyncListenerHandler(final Thread mainThread, final AsyncFilterManager filterManager, final PacketListener listener) {
        this.started = new AtomicInteger();
        this.queuedPackets = new ArrayBlockingQueue<PacketEvent>(1024);
        this.stoppedTasks = new HashSet<Integer>();
        this.stopLock = new Object();
        this.syncTask = -1;
        this.timedManager = TimedListenerManager.getInstance();
        if (filterManager == null) {
            throw new IllegalArgumentException("filterManager cannot be NULL");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be NULL");
        }
        this.mainThread = mainThread;
        this.filterManager = filterManager;
        this.listener = listener;
        this.startWarningTask();
    }
    
    private void startWarningTask() {
        this.warningTask = this.filterManager.getScheduler().scheduleSyncDelayedTask(this.getPlugin(), (Runnable)new Runnable() {
            @Override
            public void run() {
                ProtocolLibrary.getErrorReporter().reportWarning(AsyncListenerHandler.this, Report.newBuilder(AsyncListenerHandler.REPORT_HANDLER_NOT_STARTED).messageParam(AsyncListenerHandler.this.listener.getPlugin(), AsyncListenerHandler.this).build());
            }
        }, 40L);
    }
    
    private void stopWarningTask() {
        final int taskId = this.warningTask;
        if (this.warningTask >= 0) {
            this.filterManager.getScheduler().cancelTask(taskId);
            this.warningTask = -1;
        }
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public PacketListener getAsyncListener() {
        return this.listener;
    }
    
    void setNullPacketListener(final NullPacketListener nullPacketListener) {
        this.nullPacketListener = nullPacketListener;
    }
    
    PacketListener getNullPacketListener() {
        return this.nullPacketListener;
    }
    
    public Plugin getPlugin() {
        return (this.listener != null) ? this.listener.getPlugin() : null;
    }
    
    public void cancel() {
        this.close();
    }
    
    public void enqueuePacket(final PacketEvent packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet is NULL");
        }
        this.queuedPackets.add(packet);
    }
    
    public AsyncRunnable getListenerLoop() {
        return new AsyncRunnable() {
            private final AtomicBoolean firstRun = new AtomicBoolean();
            private final AtomicBoolean finished = new AtomicBoolean();
            private final int id = AsyncListenerHandler.nextID.incrementAndGet();
            
            @Override
            public int getID() {
                return this.id;
            }
            
            @Override
            public void run() {
                if (this.firstRun.compareAndSet(false, true)) {
                    AsyncListenerHandler.this.listenerLoop(this.id);
                    synchronized (AsyncListenerHandler.this.stopLock) {
                        AsyncListenerHandler.this.stoppedTasks.remove(this.id);
                        AsyncListenerHandler.this.stopLock.notifyAll();
                        this.finished.set(true);
                    }
                    return;
                }
                if (this.finished.get()) {
                    throw new IllegalStateException("This listener has already been run. Create a new instead.");
                }
                throw new IllegalStateException("This listener loop has already been started. Create a new instead.");
            }
            
            @Override
            public boolean stop() throws InterruptedException {
                synchronized (AsyncListenerHandler.this.stopLock) {
                    if (!this.isRunning()) {
                        return false;
                    }
                    AsyncListenerHandler.this.stoppedTasks.add(this.id);
                    for (int i = 0; i < AsyncListenerHandler.this.getWorkers(); ++i) {
                        AsyncListenerHandler.this.queuedPackets.offer(AsyncListenerHandler.WAKEUP_PACKET);
                    }
                    this.finished.set(true);
                    AsyncListenerHandler.this.waitForStops();
                    return true;
                }
            }
            
            @Override
            public boolean isRunning() {
                return this.firstRun.get() && !this.finished.get();
            }
            
            @Override
            public boolean isFinished() {
                return this.finished.get();
            }
        };
    }
    
    public synchronized void start() {
        if (this.listener.getPlugin() == null) {
            throw new IllegalArgumentException("Cannot start task without a valid plugin.");
        }
        if (this.cancelled) {
            throw new IllegalStateException("Cannot start a worker when the listener is closing.");
        }
        final AsyncRunnable listenerLoop = this.getListenerLoop();
        this.stopWarningTask();
        this.scheduleAsync(new Runnable() {
            @Override
            public void run() {
                final Thread thread = Thread.currentThread();
                final String previousName = thread.getName();
                final String workerName = AsyncListenerHandler.this.getFriendlyWorkerName(listenerLoop.getID());
                thread.setName(workerName);
                listenerLoop.run();
                thread.setName(previousName);
            }
        });
    }
    
    public synchronized void start(final Function<AsyncRunnable, Void> executor) {
        if (this.listener.getPlugin() == null) {
            throw new IllegalArgumentException("Cannot start task without a valid plugin.");
        }
        if (this.cancelled) {
            throw new IllegalStateException("Cannot start a worker when the listener is closing.");
        }
        final AsyncRunnable listenerLoop = this.getListenerLoop();
        final Function<AsyncRunnable, Void> delegateCopy = executor;
        this.scheduleAsync(new Runnable() {
            @Override
            public void run() {
                delegateCopy.apply((Object)listenerLoop);
            }
        });
    }
    
    private void scheduleAsync(final Runnable runnable) {
        this.listener.getPlugin().getServer().getScheduler().runTaskAsynchronously(this.listener.getPlugin(), runnable);
    }
    
    public String getFriendlyWorkerName(final int id) {
        return String.format("Protocol Worker #%s - %s - [recv: %s, send: %s]", id, PacketAdapter.getPluginName(this.listener), this.fromWhitelist(this.listener.getReceivingWhitelist()), this.fromWhitelist(this.listener.getSendingWhitelist()));
    }
    
    private String fromWhitelist(final ListeningWhitelist whitelist) {
        if (whitelist == null) {
            return "";
        }
        return Joiner.on(", ").join((Iterable)whitelist.getTypes());
    }
    
    public synchronized boolean syncStart() {
        return this.syncStart(500L, TimeUnit.MICROSECONDS);
    }
    
    public synchronized boolean syncStart(final long time, final TimeUnit unit) {
        if (time <= 0L) {
            throw new IllegalArgumentException("Time must be greater than zero.");
        }
        if (unit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be NULL.");
        }
        final long tickDelay = 1L;
        final int workerID = AsyncListenerHandler.nextID.incrementAndGet();
        if (this.syncTask >= 0) {
            return false;
        }
        this.stopWarningTask();
        this.syncTask = this.filterManager.getScheduler().scheduleSyncRepeatingTask(this.getPlugin(), (Runnable)new Runnable() {
            @Override
            public void run() {
                final long stopTime = System.nanoTime() + unit.convert(time, TimeUnit.NANOSECONDS);
                while (!AsyncListenerHandler.this.cancelled) {
                    final PacketEvent packet = AsyncListenerHandler.this.queuedPackets.poll();
                    if (packet == AsyncListenerHandler.INTERUPT_PACKET || packet == AsyncListenerHandler.WAKEUP_PACKET) {
                        AsyncListenerHandler.this.queuedPackets.add(packet);
                        break;
                    }
                    if (packet == null || packet.getAsyncMarker() == null) {
                        break;
                    }
                    AsyncListenerHandler.this.processPacket(workerID, packet, "onSyncPacket()");
                    if (System.nanoTime() < stopTime) {
                        break;
                    }
                }
            }
        }, 1L, 1L);
        if (this.syncTask < 0) {
            throw new IllegalStateException("Cannot start synchronous task.");
        }
        return true;
    }
    
    public synchronized boolean syncStop() {
        if (this.syncTask > 0) {
            this.filterManager.getScheduler().cancelTask(this.syncTask);
            this.syncTask = -1;
            return true;
        }
        return false;
    }
    
    public synchronized void start(final int count) {
        for (int i = 0; i < count; ++i) {
            this.start();
        }
    }
    
    public synchronized void stop() {
        this.queuedPackets.add(AsyncListenerHandler.INTERUPT_PACKET);
    }
    
    public synchronized void stop(final int count) {
        for (int i = 0; i < count; ++i) {
            this.stop();
        }
    }
    
    public synchronized void setWorkers(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Number of workers cannot be less than zero.");
        }
        if (count > 1024) {
            throw new IllegalArgumentException("Cannot initiate more than 1024 workers");
        }
        if (this.cancelled && count > 0) {
            throw new IllegalArgumentException("Cannot add workers when the listener is closing.");
        }
        final long time = System.currentTimeMillis();
        while (this.started.get() != count) {
            if (this.started.get() < count) {
                this.start();
            }
            else {
                this.stop();
            }
            if (System.currentTimeMillis() - time > 50L) {
                throw new RuntimeException("Failed to set worker count.");
            }
        }
    }
    
    public synchronized int getWorkers() {
        return this.started.get();
    }
    
    private boolean waitForStops() throws InterruptedException {
        synchronized (this.stopLock) {
            while (this.stoppedTasks.size() > 0 && !this.cancelled) {
                this.stopLock.wait();
            }
            return this.cancelled;
        }
    }
    
    private void listenerLoop(final int workerID) {
        if (Thread.currentThread().getId() == this.mainThread.getId()) {
            throw new IllegalStateException("Do not call this method from the main thread.");
        }
        if (this.cancelled) {
            throw new IllegalStateException("Listener has been cancelled. Create a new listener instead.");
        }
        try {
            if (this.waitForStops()) {
                return;
            }
            this.started.incrementAndGet();
            while (!this.cancelled) {
                final PacketEvent packet = this.queuedPackets.take();
                if (packet == AsyncListenerHandler.WAKEUP_PACKET) {
                    synchronized (this.stopLock) {
                        if (this.stoppedTasks.contains(workerID)) {
                            return;
                        }
                        if (this.waitForStops()) {
                            return;
                        }
                    }
                }
                else if (packet == AsyncListenerHandler.INTERUPT_PACKET) {
                    return;
                }
                if (packet != null && packet.getAsyncMarker() != null) {
                    this.processPacket(workerID, packet, "onAsyncPacket()");
                }
            }
        }
        catch (InterruptedException ex) {}
        finally {
            this.started.decrementAndGet();
        }
    }
    
    private void processPacket(final int workerID, final PacketEvent packet, final String methodName) {
        final AsyncMarker marker = packet.getAsyncMarker();
        try {
            synchronized (marker.getProcessingLock()) {
                marker.setListenerHandler(this);
                marker.setWorkerID(workerID);
                if (this.timedManager.isTiming()) {
                    final TimedTracker tracker = this.timedManager.getTracker(this.listener, packet.isServerPacket() ? TimedListenerManager.ListenerType.ASYNC_SERVER_SIDE : TimedListenerManager.ListenerType.ASYNC_CLIENT_SIDE);
                    final long token = tracker.beginTracking();
                    if (packet.isServerPacket()) {
                        this.listener.onPacketSending(packet);
                    }
                    else {
                        this.listener.onPacketReceiving(packet);
                    }
                    tracker.endTracking(token, packet.getPacketType());
                }
                else if (packet.isServerPacket()) {
                    this.listener.onPacketSending(packet);
                }
                else {
                    this.listener.onPacketReceiving(packet);
                }
            }
        }
        catch (OutOfMemoryError e) {
            throw e;
        }
        catch (ThreadDeath e2) {
            throw e2;
        }
        catch (Throwable e3) {
            this.filterManager.getErrorReporter().reportMinimal(this.listener.getPlugin(), methodName, e3);
        }
        if (!marker.hasExpired()) {
            while (marker.getListenerTraversal().hasNext()) {
                final AsyncListenerHandler handler = marker.getListenerTraversal().next().getListener();
                if (!handler.isCancelled()) {
                    handler.enqueuePacket(packet);
                    return;
                }
            }
        }
        this.filterManager.signalFreeProcessingSlot(packet);
        this.filterManager.signalPacketTransmission(packet);
    }
    
    private synchronized void close() {
        if (!this.cancelled) {
            this.filterManager.unregisterAsyncHandlerInternal(this);
            this.cancelled = true;
            this.syncStop();
            this.stopThreads();
        }
    }
    
    private void stopThreads() {
        this.queuedPackets.clear();
        this.stop(this.started.get());
        synchronized (this.stopLock) {
            this.stopLock.notifyAll();
        }
    }
    
    static {
        REPORT_HANDLER_NOT_STARTED = new ReportType("Plugin %s did not start the asynchronous handler %s by calling start() or syncStart().");
        INTERUPT_PACKET = new PacketEvent(new Object());
        WAKEUP_PACKET = new PacketEvent(new Object());
        nextID = new AtomicInteger();
    }
}
