package com.comphenix.protocol.injector.server;

import org.bukkit.entity.Player;
import java.net.SocketAddress;
import java.net.Socket;
import java.io.InputStream;
import org.bukkit.Server;
import com.comphenix.protocol.error.ErrorReporter;

public abstract class AbstractInputStreamLookup
{
    protected final ErrorReporter reporter;
    protected final Server server;
    
    protected AbstractInputStreamLookup(final ErrorReporter reporter, final Server server) {
        this.reporter = reporter;
        this.server = server;
    }
    
    public abstract void inject(final Object p0);
    
    public abstract SocketInjector waitSocketInjector(final InputStream p0);
    
    public abstract SocketInjector waitSocketInjector(final Socket p0);
    
    public abstract SocketInjector waitSocketInjector(final SocketAddress p0);
    
    public abstract SocketInjector peekSocketInjector(final SocketAddress p0);
    
    public abstract void setSocketInjector(final SocketAddress p0, final SocketInjector p1);
    
    protected void onPreviousSocketOverwritten(final SocketInjector previous, final SocketInjector current) {
        final Player player = previous.getPlayer();
        if (player instanceof TemporaryPlayer) {
            TemporaryPlayerFactory.setInjectorInPlayer(player, current);
        }
    }
    
    public abstract void cleanupAll();
}
