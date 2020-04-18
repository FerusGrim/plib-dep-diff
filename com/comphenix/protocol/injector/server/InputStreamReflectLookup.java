package com.comphenix.protocol.injector.server;

import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.io.FilterInputStream;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import com.google.common.collect.MapMaker;
import org.bukkit.Server;
import com.comphenix.protocol.error.ErrorReporter;
import java.io.InputStream;
import java.util.concurrent.ConcurrentMap;
import java.net.SocketAddress;
import com.comphenix.protocol.concurrency.BlockingHashMap;
import java.lang.reflect.Field;

class InputStreamReflectLookup extends AbstractInputStreamLookup
{
    private static Field filteredInputField;
    private static final long DEFAULT_TIMEOUT = 2000L;
    protected BlockingHashMap<SocketAddress, SocketInjector> addressLookup;
    protected ConcurrentMap<InputStream, SocketAddress> inputLookup;
    private final long injectorTimeout;
    
    public InputStreamReflectLookup(final ErrorReporter reporter, final Server server) {
        this(reporter, server, 2000L);
    }
    
    public InputStreamReflectLookup(final ErrorReporter reporter, final Server server, final long injectorTimeout) {
        super(reporter, server);
        this.addressLookup = new BlockingHashMap<SocketAddress, SocketInjector>();
        this.inputLookup = (ConcurrentMap<InputStream, SocketAddress>)new MapMaker().weakValues().makeMap();
        this.injectorTimeout = injectorTimeout;
    }
    
    @Override
    public void inject(final Object container) {
    }
    
    @Override
    public SocketInjector peekSocketInjector(final SocketAddress address) {
        try {
            return this.addressLookup.get(address, 0L, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            return null;
        }
    }
    
    @Override
    public SocketInjector waitSocketInjector(final SocketAddress address) {
        try {
            return this.addressLookup.get(address, this.injectorTimeout, TimeUnit.MILLISECONDS, true);
        }
        catch (InterruptedException e) {
            throw new IllegalStateException("Impossible exception occured!", e);
        }
    }
    
    @Override
    public SocketInjector waitSocketInjector(final Socket socket) {
        return this.waitSocketInjector(socket.getRemoteSocketAddress());
    }
    
    @Override
    public SocketInjector waitSocketInjector(final InputStream input) {
        try {
            final SocketAddress address = this.waitSocketAddress(input);
            if (address != null) {
                return this.waitSocketInjector(address);
            }
            return null;
        }
        catch (IllegalAccessException e) {
            throw new FieldAccessException("Cannot find or access socket field for " + input, e);
        }
    }
    
    private SocketAddress waitSocketAddress(final InputStream stream) throws IllegalAccessException {
        if (stream instanceof FilterInputStream) {
            return this.waitSocketAddress(getInputStream((FilterInputStream)stream));
        }
        SocketAddress result = this.inputLookup.get(stream);
        if (result == null) {
            final Socket socket = lookupSocket(stream);
            result = socket.getRemoteSocketAddress();
            this.inputLookup.put(stream, result);
        }
        return result;
    }
    
    protected static InputStream getInputStream(final FilterInputStream filtered) {
        if (InputStreamReflectLookup.filteredInputField == null) {
            InputStreamReflectLookup.filteredInputField = FuzzyReflection.fromClass(FilterInputStream.class, true).getFieldByType("in", InputStream.class);
        }
        InputStream current = filtered;
        try {
            while (current instanceof FilterInputStream) {
                current = (InputStream)FieldUtils.readField(InputStreamReflectLookup.filteredInputField, current, true);
            }
            return current;
        }
        catch (IllegalAccessException e) {
            throw new FieldAccessException("Cannot access filtered input field.", e);
        }
    }
    
    @Override
    public void setSocketInjector(final SocketAddress address, final SocketInjector injector) {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be NULL");
        }
        if (injector == null) {
            throw new IllegalArgumentException("injector cannot be NULL.");
        }
        final SocketInjector previous = this.addressLookup.put(address, injector);
        if (previous != null) {
            this.onPreviousSocketOverwritten(previous, injector);
        }
    }
    
    @Override
    public void cleanupAll() {
    }
    
    private static Socket lookupSocket(final InputStream stream) throws IllegalAccessException {
        if (stream instanceof FilterInputStream) {
            return lookupSocket(getInputStream((FilterInputStream)stream));
        }
        final Field socketField = FuzzyReflection.fromObject(stream, true).getFieldByType("socket", Socket.class);
        return (Socket)FieldUtils.readField(socketField, stream, true);
    }
}
