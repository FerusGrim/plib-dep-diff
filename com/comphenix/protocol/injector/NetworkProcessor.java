package com.comphenix.protocol.injector;

import com.comphenix.protocol.ProtocolManager;
import java.util.List;
import com.comphenix.protocol.PacketStream;
import com.comphenix.protocol.events.ScheduledPacket;
import com.comphenix.protocol.ProtocolLibrary;
import java.util.Iterator;
import com.comphenix.protocol.events.PacketPostListener;
import com.comphenix.protocol.events.PacketOutputHandler;
import java.util.PriorityQueue;
import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.error.ErrorReporter;

public class NetworkProcessor
{
    private ErrorReporter reporter;
    
    public NetworkProcessor(final ErrorReporter reporter) {
        this.reporter = reporter;
    }
    
    public byte[] processOutput(final PacketEvent event, final NetworkMarker marker, final byte[] input) {
        final PriorityQueue<PacketOutputHandler> handlers = (PriorityQueue<PacketOutputHandler>)(PriorityQueue)marker.getOutputHandlers();
        byte[] output = input;
        while (!handlers.isEmpty()) {
            final PacketOutputHandler handler = handlers.poll();
            try {
                final byte[] changed = handler.handle(event, output);
                if (changed == null) {
                    throw new IllegalStateException("Handler cannot return a NULL array.");
                }
                output = changed;
            }
            catch (OutOfMemoryError e) {
                throw e;
            }
            catch (ThreadDeath e2) {
                throw e2;
            }
            catch (Throwable e3) {
                this.reporter.reportMinimal(handler.getPlugin(), "PacketOutputHandler.handle()", e3);
            }
        }
        return output;
    }
    
    public void invokePostEvent(final PacketEvent event, final NetworkMarker marker) {
        if (marker == null) {
            return;
        }
        if (NetworkMarker.hasPostListeners(marker)) {
            for (final PacketPostListener listener : marker.getPostListeners()) {
                try {
                    listener.onPostEvent(event);
                }
                catch (OutOfMemoryError e) {
                    throw e;
                }
                catch (ThreadDeath e2) {
                    throw e2;
                }
                catch (Throwable e3) {
                    this.reporter.reportMinimal(listener.getPlugin(), "SentListener.run()", e3);
                }
            }
        }
        this.sendScheduledPackets(marker);
    }
    
    private void sendScheduledPackets(final NetworkMarker marker) {
        final List<ScheduledPacket> scheduled = NetworkMarker.readScheduledPackets(marker);
        final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        if (scheduled != null) {
            for (final ScheduledPacket packet : scheduled) {
                packet.schedule(manager);
            }
        }
    }
}
