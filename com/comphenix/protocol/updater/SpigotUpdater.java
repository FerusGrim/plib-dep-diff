package com.comphenix.protocol.updater;

import java.util.Iterator;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import com.comphenix.protocol.utility.Closer;
import org.bukkit.plugin.Plugin;

public final class SpigotUpdater extends Updater
{
    private String remoteVersion;
    private static final String PROTOCOL = "https://";
    private static final String RESOURCE_URL = "https://www.spigotmc.org/resources/protocollib.1997/";
    private static final String API_URL = "https://www.spigotmc.org/api/general.php";
    private static final String ACTION = "POST";
    private static final int ID = 1997;
    private static final byte[] API_KEY;
    
    public SpigotUpdater(final Plugin plugin, final UpdateType type, final boolean announce) {
        super(plugin, type, announce);
    }
    
    @Override
    public void start(final UpdateType type) {
        this.waitForThread();
        this.type = type;
        (this.thread = new Thread(new SpigotUpdateRunnable())).start();
    }
    
    @Override
    public String getResult() {
        this.waitForThread();
        return String.format(this.result.toString(), this.remoteVersion, this.plugin.getDescription().getVersion(), "https://www.spigotmc.org/resources/protocollib.1997/");
    }
    
    public String getSpigotVersion() throws IOException {
        try (final Closer closer = Closer.create()) {
            final HttpURLConnection con = (HttpURLConnection)new URL("https://www.spigotmc.org/api/general.php").openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.getOutputStream().write(SpigotUpdater.API_KEY);
            final InputStreamReader isr = closer.register(new InputStreamReader(con.getInputStream()));
            final BufferedReader br = closer.register(new BufferedReader(isr));
            return br.readLine();
        }
    }
    
    @Override
    public String getRemoteVersion() {
        return this.remoteVersion;
    }
    
    static {
        API_KEY = "key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=1997".getBytes(Charsets.UTF_8);
    }
    
    private class SpigotUpdateRunnable implements Runnable
    {
        @Override
        public void run() {
            try {
                final String version = SpigotUpdater.this.getSpigotVersion();
                SpigotUpdater.this.remoteVersion = version;
                if (SpigotUpdater.this.versionCheck(version)) {
                    SpigotUpdater.this.result = UpdateResult.SPIGOT_UPDATE_AVAILABLE;
                }
                else {
                    SpigotUpdater.this.result = UpdateResult.NO_UPDATE;
                }
            }
            catch (Throwable ex) {
                if (ProtocolLibrary.getConfig().isDebug()) {
                    ProtocolLibrary.getErrorReporter().reportDetailed(SpigotUpdater.this, Report.newBuilder(Updater.REPORT_CANNOT_UPDATE_PLUGIN).error(ex).callerParam(this));
                }
                ProtocolLibrary.disableUpdates();
            }
            finally {
                for (final Runnable listener : SpigotUpdater.this.listeners) {
                    SpigotUpdater.this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(SpigotUpdater.this.plugin, listener);
                }
            }
        }
    }
}
