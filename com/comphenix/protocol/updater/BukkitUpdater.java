package com.comphenix.protocol.updater;

import java.util.Iterator;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import java.net.URLConnection;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.net.MalformedURLException;
import java.io.IOException;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.net.URL;

public class BukkitUpdater extends Updater
{
    private URL url;
    private File file;
    private Thread thread;
    private int id;
    private String apiKey;
    private static final String TITLE_VALUE = "name";
    private static final String LINK_VALUE = "downloadUrl";
    private static final String TYPE_VALUE = "releaseType";
    private static final String VERSION_VALUE = "gameVersion";
    private static final Object FILE_NAME;
    private static final String QUERY = "/servermods/files?projectIds=";
    private static final String HOST = "https://api.curseforge.com";
    private static final int BYTE_SIZE = 1024;
    private YamlConfiguration config;
    private String updateFolder;
    
    public BukkitUpdater(final Plugin plugin, final int id, final File file, final UpdateType type, final boolean announce) {
        super(plugin, type, announce);
        this.id = -1;
        this.apiKey = null;
        this.file = file;
        this.id = id;
        this.updateFolder = plugin.getServer().getUpdateFolder();
        final File dataFolder = plugin.getDataFolder();
        if (dataFolder != null) {
            final File pluginFile = plugin.getDataFolder().getParentFile();
            final File updaterFile = new File(pluginFile, "Updater");
            final File updaterConfigFile = new File(updaterFile, "config.yml");
            if (!updaterFile.exists()) {
                updaterFile.mkdir();
            }
            if (!updaterConfigFile.exists()) {
                try {
                    updaterConfigFile.createNewFile();
                }
                catch (IOException e) {
                    plugin.getLogger().severe("The updater could not create a configuration in " + updaterFile.getAbsolutePath());
                    e.printStackTrace();
                }
            }
            this.config = YamlConfiguration.loadConfiguration(updaterConfigFile);
            this.config.options().header("This configuration file affects all plugins using the Updater system (version 2+ - http://forums.bukkit.org/threads/96681/ )\nIf you wish to use your API key, read http://wiki.bukkit.org/ServerMods_API and place it below.\nSome updating systems will not adhere to the disabled value, but these may be turned off in their plugin's configuration.");
            this.config.addDefault("api-key", (Object)"PUT_API_KEY_HERE");
            this.config.addDefault("disable", (Object)false);
            if (this.config.get("api-key", (Object)null) == null) {
                this.config.options().copyDefaults(true);
                try {
                    this.config.save(updaterConfigFile);
                }
                catch (IOException e) {
                    plugin.getLogger().severe("The updater could not save the configuration in " + updaterFile.getAbsolutePath());
                    e.printStackTrace();
                }
            }
            if (this.config.getBoolean("disable")) {
                this.result = UpdateResult.DISABLED;
                return;
            }
            String key = this.config.getString("api-key");
            if (key.equalsIgnoreCase("PUT_API_KEY_HERE") || key.equals("")) {
                key = null;
            }
            this.apiKey = key;
        }
        try {
            this.url = new URL("https://api.curseforge.com/servermods/files?projectIds=" + id);
        }
        catch (MalformedURLException e2) {
            plugin.getLogger().severe("The project ID provided for updating, " + id + " is invalid.");
            this.result = UpdateResult.FAIL_BADID;
            e2.printStackTrace();
        }
    }
    
    @Override
    public void start(final UpdateType type) {
        this.waitForThread();
        this.type = type;
        (this.thread = new Thread(new UpdateRunnable())).start();
    }
    
    private void saveFile(final File folder, final String file, final String u) {
        if (!folder.exists()) {
            folder.mkdir();
        }
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            final URL url = new URL(u);
            final int fileLength = url.openConnection().getContentLength();
            in = new BufferedInputStream(url.openStream());
            fout = new FileOutputStream(folder.getAbsolutePath() + "/" + file);
            final byte[] data = new byte[1024];
            if (this.announce) {
                this.plugin.getLogger().info("About to download a new update: " + this.versionName);
            }
            long downloaded = 0L;
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                downloaded += count;
                fout.write(data, 0, count);
                final int percent = (int)(downloaded * 100L / fileLength);
                if (this.announce && percent % 10 == 0) {
                    this.plugin.getLogger().info("Downloading update: " + percent + "% of " + fileLength + " bytes.");
                }
            }
            for (final File xFile : new File(this.plugin.getDataFolder().getParent(), this.updateFolder).listFiles()) {
                if (xFile.getName().endsWith(".zip")) {
                    xFile.delete();
                }
            }
            final File dFile = new File(folder.getAbsolutePath() + "/" + file);
            if (dFile.getName().endsWith(".zip")) {
                this.unzip(dFile.getCanonicalPath());
            }
            if (this.announce) {
                this.plugin.getLogger().info("Finished updating.");
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().warning("The auto-updater tried to download a new update, but was unsuccessful.");
            this.result = UpdateResult.FAIL_DOWNLOAD;
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (fout != null) {
                    fout.close();
                }
            }
            catch (Exception ex2) {}
        }
    }
    
    private void unzip(final String file) {
        try {
            final File fSourceZip = new File(file);
            final String zipPath = file.substring(0, file.length() - 4);
            ZipFile zipFile = new ZipFile(fSourceZip);
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)e.nextElement();
                File destinationFilePath = new File(zipPath, entry.getName());
                destinationFilePath.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    continue;
                }
                final BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                final byte[] buffer = new byte[1024];
                final FileOutputStream fos = new FileOutputStream(destinationFilePath);
                final BufferedOutputStream bos = new BufferedOutputStream(fos, 1024);
                int b;
                while ((b = bis.read(buffer, 0, 1024)) != -1) {
                    bos.write(buffer, 0, b);
                }
                bos.flush();
                bos.close();
                bis.close();
                final String name = destinationFilePath.getName();
                if (name.endsWith(".jar") && this.pluginFile(name)) {
                    destinationFilePath.renameTo(new File(this.plugin.getDataFolder().getParent(), this.updateFolder + "/" + name));
                }
                entry = null;
                destinationFilePath = null;
            }
            e = null;
            zipFile.close();
            zipFile = null;
            for (final File dFile : new File(zipPath).listFiles()) {
                if (dFile.isDirectory() && this.pluginFile(dFile.getName())) {
                    final File oFile = new File(this.plugin.getDataFolder().getParent(), dFile.getName());
                    final File[] contents = oFile.listFiles();
                    for (final File cFile : dFile.listFiles()) {
                        boolean found = false;
                        for (final File xFile : contents) {
                            if (xFile.getName().equals(cFile.getName())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            cFile.renameTo(new File(oFile.getCanonicalFile() + "/" + cFile.getName()));
                        }
                        else {
                            cFile.delete();
                        }
                    }
                }
                dFile.delete();
            }
            new File(zipPath).delete();
            fSourceZip.delete();
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("The auto-updater tried to unzip a new update file, but was unsuccessful.");
            this.result = UpdateResult.FAIL_DOWNLOAD;
            ex.printStackTrace();
        }
        new File(file).delete();
    }
    
    private boolean pluginFile(final String name) {
        for (final File file : new File("plugins").listFiles()) {
            if (file.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean read() {
        try {
            final URLConnection conn = this.url.openConnection();
            conn.setConnectTimeout(5000);
            if (this.apiKey != null) {
                conn.addRequestProperty("X-API-Key", this.apiKey);
            }
            conn.addRequestProperty("User-Agent", "Updater (by Gravity)");
            conn.setDoOutput(true);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            final String response = reader.readLine();
            final JSONArray array = (JSONArray)JSONValue.parse(response);
            if (array.size() == 0) {
                this.plugin.getLogger().warning("The updater could not find any files for the project id " + this.id);
                this.result = UpdateResult.FAIL_BADID;
                return false;
            }
            final JSONObject jsonObject = (JSONObject)array.get(array.size() - 1);
            this.versionFileName = (String)jsonObject.get(BukkitUpdater.FILE_NAME);
            this.versionName = (String)jsonObject.get((Object)"name");
            this.versionLink = (String)jsonObject.get((Object)"downloadUrl");
            this.versionType = (String)jsonObject.get((Object)"releaseType");
            this.versionGameVersion = (String)jsonObject.get((Object)"gameVersion");
            return true;
        }
        catch (IOException e) {
            if (e.getMessage().contains("HTTP response code: 403")) {
                this.plugin.getLogger().warning("dev.bukkit.org rejected the API key provided in plugins/Updater/config.yml");
                this.plugin.getLogger().warning("Please double-check your configuration to ensure it is correct.");
                this.result = UpdateResult.FAIL_APIKEY;
            }
            else {
                this.result = UpdateResult.FAIL_DBO;
            }
            return false;
        }
    }
    
    @Override
    public String getRemoteVersion() {
        return this.getLatestName();
    }
    
    static {
        FILE_NAME = "fileName";
    }
    
    private class UpdateRunnable implements Runnable
    {
        @Override
        public void run() {
            try {
                if (BukkitUpdater.this.url != null && BukkitUpdater.this.read() && BukkitUpdater.this.versionCheck(BukkitUpdater.this.versionName)) {
                    this.performUpdate();
                }
            }
            catch (Exception e) {
                ProtocolLibrary.getErrorReporter().reportDetailed(BukkitUpdater.this, Report.newBuilder(Updater.REPORT_CANNOT_UPDATE_PLUGIN).error(e).callerParam(this));
            }
            finally {
                for (final Runnable listener : BukkitUpdater.this.listeners) {
                    BukkitUpdater.this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(BukkitUpdater.this.plugin, listener);
                }
            }
        }
        
        private void performUpdate() {
            if (BukkitUpdater.this.versionLink != null && BukkitUpdater.this.type != UpdateType.NO_DOWNLOAD) {
                final File pluginFolder = BukkitUpdater.this.plugin.getDataFolder().getParentFile();
                final File destinationFolder = new File(pluginFolder, BukkitUpdater.this.updateFolder);
                String name = BukkitUpdater.this.file.getName();
                if (BukkitUpdater.this.versionLink.endsWith(".zip")) {
                    name = BukkitUpdater.this.versionFileName;
                }
                BukkitUpdater.this.saveFile(destinationFolder, name, BukkitUpdater.this.versionLink);
            }
            else {
                BukkitUpdater.this.result = UpdateResult.UPDATE_AVAILABLE;
            }
        }
    }
}
