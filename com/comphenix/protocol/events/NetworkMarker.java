package com.comphenix.protocol.events;

import com.comphenix.protocol.utility.ByteBufferInputStream;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Collection;
import com.google.common.primitives.Ints;
import java.util.Comparator;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import com.comphenix.protocol.utility.StreamSerializer;
import com.comphenix.protocol.PacketType;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.PriorityQueue;

public abstract class NetworkMarker
{
    private PriorityQueue<PacketOutputHandler> outputHandlers;
    private List<PacketPostListener> postListeners;
    private List<ScheduledPacket> scheduledPackets;
    private ByteBuffer inputBuffer;
    private final ConnectionSide side;
    private final PacketType type;
    private StreamSerializer serializer;
    
    public NetworkMarker(@Nonnull final ConnectionSide side, final ByteBuffer inputBuffer, final PacketType type) {
        this.side = (ConnectionSide)Preconditions.checkNotNull((Object)side, (Object)"side cannot be NULL.");
        this.inputBuffer = inputBuffer;
        this.type = type;
    }
    
    public NetworkMarker(@Nonnull final ConnectionSide side, final byte[] inputBuffer, final PacketType type) {
        this.side = (ConnectionSide)Preconditions.checkNotNull((Object)side, (Object)"side cannot be NULL.");
        this.type = type;
        if (inputBuffer != null) {
            this.inputBuffer = ByteBuffer.wrap(inputBuffer);
        }
    }
    
    public ConnectionSide getSide() {
        return this.side;
    }
    
    public StreamSerializer getSerializer() {
        if (this.serializer == null) {
            this.serializer = new StreamSerializer();
        }
        return this.serializer;
    }
    
    public ByteBuffer getInputBuffer() {
        return this.getInputBuffer(true);
    }
    
    public ByteBuffer getInputBuffer(final boolean excludeHeader) {
        if (this.side.isForServer()) {
            throw new IllegalStateException("Server-side packets have no input buffer.");
        }
        if (this.inputBuffer != null) {
            ByteBuffer result = this.inputBuffer.asReadOnlyBuffer();
            try {
                if (excludeHeader) {
                    result = this.skipHeader(result);
                }
                else {
                    result = this.addHeader(result, this.type);
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Cannot skip packet header.", e);
            }
            return result;
        }
        return null;
    }
    
    public DataInputStream getInputStream() {
        return this.getInputStream(true);
    }
    
    public DataInputStream getInputStream(final boolean excludeHeader) {
        if (this.side.isForServer()) {
            throw new IllegalStateException("Server-side packets have no input buffer.");
        }
        if (this.inputBuffer == null) {
            return null;
        }
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(this.inputBuffer.array()));
        try {
            if (excludeHeader) {
                input = this.skipHeader(input);
            }
            else {
                input = this.addHeader(input, this.type);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot skip packet header.", e);
        }
        return input;
    }
    
    public boolean requireOutputHeader() {
        return MinecraftReflection.isUsingNetty();
    }
    
    public boolean addOutputHandler(@Nonnull final PacketOutputHandler handler) {
        this.checkServerSide();
        Preconditions.checkNotNull((Object)handler, (Object)"handler cannot be NULL.");
        if (this.outputHandlers == null) {
            this.outputHandlers = new PriorityQueue<PacketOutputHandler>(10, new Comparator<PacketOutputHandler>() {
                @Override
                public int compare(final PacketOutputHandler o1, final PacketOutputHandler o2) {
                    return Ints.compare(o1.getPriority().getSlot(), o2.getPriority().getSlot());
                }
            });
        }
        return this.outputHandlers.add(handler);
    }
    
    public boolean removeOutputHandler(@Nonnull final PacketOutputHandler handler) {
        this.checkServerSide();
        Preconditions.checkNotNull((Object)handler, (Object)"handler cannot be NULL.");
        return this.outputHandlers != null && this.outputHandlers.remove(handler);
    }
    
    @Nonnull
    public Collection<PacketOutputHandler> getOutputHandlers() {
        if (this.outputHandlers != null) {
            return this.outputHandlers;
        }
        return (Collection<PacketOutputHandler>)Collections.emptyList();
    }
    
    public boolean addPostListener(final PacketPostListener listener) {
        if (this.postListeners == null) {
            this.postListeners = (List<PacketPostListener>)Lists.newArrayList();
        }
        return this.postListeners.add(listener);
    }
    
    public boolean removePostListener(final PacketPostListener listener) {
        return this.postListeners != null && this.postListeners.remove(listener);
    }
    
    public List<PacketPostListener> getPostListeners() {
        return (this.postListeners != null) ? Collections.unmodifiableList((List<? extends PacketPostListener>)this.postListeners) : Collections.emptyList();
    }
    
    public List<ScheduledPacket> getScheduledPackets() {
        if (this.scheduledPackets == null) {
            this.scheduledPackets = (List<ScheduledPacket>)Lists.newArrayList();
        }
        return this.scheduledPackets;
    }
    
    private void checkServerSide() {
        if (this.side.isForClient()) {
            throw new IllegalStateException("Must be a server side packet.");
        }
    }
    
    protected ByteBuffer skipHeader(final ByteBuffer buffer) throws IOException {
        this.skipHeader(new DataInputStream(new ByteBufferInputStream(buffer)));
        return buffer;
    }
    
    protected abstract DataInputStream skipHeader(final DataInputStream p0) throws IOException;
    
    protected abstract ByteBuffer addHeader(final ByteBuffer p0, final PacketType p1);
    
    protected abstract DataInputStream addHeader(final DataInputStream p0, final PacketType p1);
    
    public static boolean hasOutputHandlers(final NetworkMarker marker) {
        return marker != null && !marker.getOutputHandlers().isEmpty();
    }
    
    public static boolean hasPostListeners(final NetworkMarker marker) {
        return marker != null && !marker.getPostListeners().isEmpty();
    }
    
    public static byte[] getByteBuffer(final NetworkMarker marker) {
        if (marker != null) {
            final ByteBuffer buffer = marker.getInputBuffer();
            if (buffer != null) {
                final byte[] data = new byte[buffer.remaining()];
                buffer.get(data, 0, data.length);
                return data;
            }
        }
        return null;
    }
    
    public static NetworkMarker getNetworkMarker(final PacketEvent event) {
        return event.networkMarker;
    }
    
    public static List<ScheduledPacket> readScheduledPackets(final NetworkMarker marker) {
        return marker.scheduledPackets;
    }
    
    public static class EmptyBufferMarker extends NetworkMarker
    {
        public EmptyBufferMarker(@Nonnull final ConnectionSide side) {
            super(side, (byte[])null, null);
        }
        
        @Override
        protected DataInputStream skipHeader(final DataInputStream input) throws IOException {
            throw new IllegalStateException("Buffer is empty.");
        }
        
        @Override
        protected ByteBuffer addHeader(final ByteBuffer buffer, final PacketType type) {
            throw new IllegalStateException("Buffer is empty.");
        }
        
        @Override
        protected DataInputStream addHeader(final DataInputStream input, final PacketType type) {
            throw new IllegalStateException("Buffer is empty.");
        }
    }
}
