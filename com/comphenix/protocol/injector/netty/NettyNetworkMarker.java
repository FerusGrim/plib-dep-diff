package com.comphenix.protocol.injector.netty;

import java.io.IOException;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import com.comphenix.protocol.PacketType;
import javax.annotation.Nonnull;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.NetworkMarker;

public class NettyNetworkMarker extends NetworkMarker
{
    public NettyNetworkMarker(@Nonnull final ConnectionSide side, final byte[] inputBuffer) {
        super(side, inputBuffer, null);
    }
    
    public NettyNetworkMarker(@Nonnull final ConnectionSide side, final ByteBuffer inputBuffer) {
        super(side, inputBuffer, null);
    }
    
    @Override
    protected DataInputStream skipHeader(final DataInputStream input) throws IOException {
        this.getSerializer().deserializeVarInt(input);
        return input;
    }
    
    @Override
    protected ByteBuffer addHeader(final ByteBuffer buffer, final PacketType type) {
        return buffer;
    }
    
    @Override
    protected DataInputStream addHeader(final DataInputStream input, final PacketType type) {
        return input;
    }
}
