package com.comphenix.protocol.injector.player;

import java.util.Iterator;
import org.bukkit.entity.Player;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.injector.server.SocketInjector;
import com.comphenix.protocol.injector.GamePhase;
import com.google.common.collect.Maps;
import org.bukkit.Server;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import java.util.concurrent.ConcurrentMap;
import com.comphenix.protocol.error.ReportType;

class NetLoginInjector
{
    public static final ReportType REPORT_CANNOT_HOOK_LOGIN_HANDLER;
    public static final ReportType REPORT_CANNOT_CLEANUP_LOGIN_HANDLER;
    private ConcurrentMap<Object, PlayerInjector> injectedLogins;
    private ProxyPlayerInjectionHandler injectionHandler;
    private TemporaryPlayerFactory playerFactory;
    private ErrorReporter reporter;
    private Server server;
    
    public NetLoginInjector(final ErrorReporter reporter, final Server server, final ProxyPlayerInjectionHandler injectionHandler) {
        this.injectedLogins = (ConcurrentMap<Object, PlayerInjector>)Maps.newConcurrentMap();
        this.playerFactory = new TemporaryPlayerFactory();
        this.reporter = reporter;
        this.server = server;
        this.injectionHandler = injectionHandler;
    }
    
    public Object onNetLoginCreated(final Object inserting) {
        try {
            if (!this.injectionHandler.isInjectionNecessary(GamePhase.LOGIN)) {
                return inserting;
            }
            final Player temporary = this.playerFactory.createTemporaryPlayer(this.server);
            final PlayerInjector injector = this.injectionHandler.injectPlayer(temporary, inserting, PlayerInjectionHandler.ConflictStrategy.BAIL_OUT, GamePhase.LOGIN);
            if (injector != null) {
                TemporaryPlayerFactory.setInjectorInPlayer(temporary, injector);
                injector.updateOnLogin = true;
                this.injectedLogins.putIfAbsent(inserting, injector);
            }
            return inserting;
        }
        catch (OutOfMemoryError e) {
            throw e;
        }
        catch (ThreadDeath e2) {
            throw e2;
        }
        catch (Throwable e3) {
            this.reporter.reportDetailed(this, Report.newBuilder(NetLoginInjector.REPORT_CANNOT_HOOK_LOGIN_HANDLER).messageParam(MinecraftReflection.getNetLoginHandlerName()).callerParam(inserting, this.injectionHandler).error(e3));
            return inserting;
        }
    }
    
    public synchronized void cleanup(final Object removing) {
        final PlayerInjector injected = this.injectedLogins.get(removing);
        if (injected != null) {
            try {
                PlayerInjector newInjector = null;
                final Player player = injected.getPlayer();
                this.injectedLogins.remove(removing);
                if (injected.isClean()) {
                    return;
                }
                newInjector = this.injectionHandler.getInjectorByNetworkHandler(injected.getNetworkManager());
                this.injectionHandler.uninjectPlayer(player);
                if (newInjector != null && injected instanceof NetworkObjectInjector) {
                    newInjector.setNetworkManager(injected.getNetworkManager(), true);
                }
            }
            catch (OutOfMemoryError e) {
                throw e;
            }
            catch (ThreadDeath e2) {
                throw e2;
            }
            catch (Throwable e3) {
                this.reporter.reportDetailed(this, Report.newBuilder(NetLoginInjector.REPORT_CANNOT_CLEANUP_LOGIN_HANDLER).messageParam(MinecraftReflection.getNetLoginHandlerName()).callerParam(removing).error(e3));
            }
        }
    }
    
    public void cleanupAll() {
        for (final PlayerInjector injector : this.injectedLogins.values()) {
            injector.cleanupAll();
        }
        this.injectedLogins.clear();
    }
    
    static {
        REPORT_CANNOT_HOOK_LOGIN_HANDLER = new ReportType("Unable to hook %s.");
        REPORT_CANNOT_CLEANUP_LOGIN_HANDLER = new ReportType("Cannot cleanup %s.");
    }
}
