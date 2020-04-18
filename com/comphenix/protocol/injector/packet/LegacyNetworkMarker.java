package com.comphenix.protocol.injector.packet;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import com.google.common.io.ByteSource;
import com.google.common.primitives.Bytes;
import java.io.IOException;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import com.comphenix.protocol.PacketType;
import javax.annotation.Nonnull;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.NetworkMarker;

public class LegacyNetworkMarker extends NetworkMarker
{
    public LegacyNetworkMarker(@Nonnull final ConnectionSide side, final byte[] inputBuffer, final PacketType type) {
        super(side, inputBuffer, type);
    }
    
    public LegacyNetworkMarker(@Nonnull final ConnectionSide side, final ByteBuffer inputBuffer, final PacketType type) {
        super(side, inputBuffer, type);
    }
    
    @Override
    protected DataInputStream skipHeader(final DataInputStream input) throws IOException {
        return input;
    }
    
    @Override
    protected ByteBuffer addHeader(final ByteBuffer buffer, final PacketType type) {
        return ByteBuffer.wrap(Bytes.concat(new byte[][] { { (byte)type.getLegacyId() }, buffer.array() }));
    }
    
    @Override
    protected DataInputStream addHeader(final DataInputStream input, final PacketType type) {
        final ByteSource header = new ByteSource() {
            public InputStream openStream() throws IOException {
                final byte[] data = { (byte)type.getLegacyId() };
                return new ByteArrayInputStream(data);
            }
        };
        final ByteSource data = new ByteSource() {
            public InputStream openStream() throws IOException {
                return input;
            }
        };
        try {
            return new DataInputStream(ByteSource.concat(new ByteSource[] { header, data }).openStream());
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot add header.", e);
        }
    }
}
