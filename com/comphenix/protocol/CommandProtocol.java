package com.comphenix.protocol;

import java.util.Set;
import java.util.logging.Level;
import java.util.HashSet;
import com.comphenix.protocol.error.DetailedErrorReporter;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Date;
import com.comphenix.protocol.utility.Closer;
import org.bukkit.plugin.PluginDescriptionFile;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import com.comphenix.protocol.timing.TimingReportGenerator;
import java.io.File;
import com.comphenix.protocol.timing.TimedListenerManager;
import java.util.Iterator;
import com.google.common.collect.UnmodifiableIterator;
import com.comphenix.protocol.events.PacketListener;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.comphenix.protocol.error.ErrorReporter;
import java.text.SimpleDateFormat;
import com.comphenix.protocol.updater.Updater;
import org.bukkit.plugin.Plugin;

class CommandProtocol extends CommandBase
{
    public static final String NAME = "protocol";
    private Plugin plugin;
    private Updater updater;
    private ProtocolConfig config;
    private static SimpleDateFormat FILE_FORMAT;
    private static SimpleDateFormat TIMESTAMP_FORMAT;
    
    public CommandProtocol(final ErrorReporter reporter, final Plugin plugin, final Updater updater, final ProtocolConfig config) {
        super(reporter, "protocol.admin", "protocol", 1);
        this.plugin = plugin;
        this.updater = updater;
        this.config = config;
    }
    
    @Override
    protected boolean handleCommand(final CommandSender sender, final String[] args) {
        final String subCommand = args[0];
        if (subCommand.equalsIgnoreCase("config") || subCommand.equalsIgnoreCase("reload")) {
            this.reloadConfiguration(sender);
        }
        else if (subCommand.equalsIgnoreCase("check")) {
            this.checkVersion(sender, true);
        }
        else if (subCommand.equalsIgnoreCase("update")) {
            this.updateVersion(sender, true);
        }
        else if (subCommand.equalsIgnoreCase("timings")) {
            this.toggleTimings(sender, args);
        }
        else if (subCommand.equalsIgnoreCase("listeners")) {
            this.printListeners(sender);
        }
        else if (subCommand.equalsIgnoreCase("version")) {
            this.printVersion(sender);
        }
        else {
            if (!subCommand.equalsIgnoreCase("dump")) {
                return false;
            }
            this.dump(sender);
        }
        return true;
    }
    
    public void checkVersion(final CommandSender sender, final boolean command) {
        this.performUpdate(sender, Updater.UpdateType.NO_DOWNLOAD, command);
    }
    
    public void updateVersion(final CommandSender sender, final boolean command) {
        this.performUpdate(sender, Updater.UpdateType.DEFAULT, command);
    }
    
    private void printListeners(final CommandSender sender) {
        final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        sender.sendMessage(ChatColor.GOLD + "Packet listeners:");
        for (final PacketListener listener : manager.getPacketListeners()) {
            sender.sendMessage(ChatColor.GOLD + " - " + listener);
        }
        sender.sendMessage(ChatColor.GOLD + "Asynchronous listeners:");
        for (final PacketListener listener : manager.getAsynchronousManager().getAsyncHandlers()) {
            sender.sendMessage(ChatColor.GOLD + " - " + listener);
        }
    }
    
    private void performUpdate(final CommandSender sender, final Updater.UpdateType type, final boolean command) {
        if (this.updater.isChecking()) {
            sender.sendMessage(ChatColor.RED + "Already checking for an update.");
            return;
        }
        final Runnable notify = new Runnable() {
            @Override
            public void run() {
                if (command) {
                    sender.sendMessage(ChatColor.YELLOW + "[ProtocolLib] " + CommandProtocol.this.updater.getResult());
                    final String remoteVersion = CommandProtocol.this.updater.getRemoteVersion();
                    if (remoteVersion != null) {
                        sender.sendMessage(ChatColor.YELLOW + "Remote version: " + remoteVersion);
                        sender.sendMessage(ChatColor.YELLOW + "Current version: " + CommandProtocol.this.plugin.getDescription().getVersion());
                    }
                }
                else if (CommandProtocol.this.updater.shouldNotify() || CommandProtocol.this.config.isDebug()) {
                    sender.sendMessage(ChatColor.YELLOW + "[ProtocolLib] " + CommandProtocol.this.updater.getResult());
                }
                CommandProtocol.this.updater.removeListener(this);
                CommandProtocol.this.updateFinished();
            }
        };
        this.updater.start(type);
        this.updater.addListener(notify);
    }
    
    private void toggleTimings(final CommandSender sender, final String[] args) {
        final TimedListenerManager manager = TimedListenerManager.getInstance();
        boolean state = !manager.isTiming();
        if (args.length == 2) {
            final Boolean parsed = this.parseBoolean(this.toQueue(args, 2), "start");
            if (parsed == null) {
                sender.sendMessage(ChatColor.RED + "Specify a state: ON or OFF.");
                return;
            }
            state = parsed;
        }
        else if (args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Too many parameters.");
            return;
        }
        if (state) {
            if (manager.startTiming()) {
                sender.sendMessage(ChatColor.GOLD + "Started timing packet listeners.");
            }
            else {
                sender.sendMessage(ChatColor.RED + "Packet timing already started.");
            }
        }
        else if (manager.stopTiming()) {
            this.saveTimings(manager);
            sender.sendMessage(ChatColor.GOLD + "Stopped and saved result in plugin folder.");
        }
        else {
            sender.sendMessage(ChatColor.RED + "Packet timing already stopped.");
        }
    }
    
    private void saveTimings(final TimedListenerManager manager) {
        try {
            final File destination = new File(this.plugin.getDataFolder(), "Timings - " + System.currentTimeMillis() + ".txt");
            final TimingReportGenerator generator = new TimingReportGenerator();
            generator.saveTo(destination, manager);
            manager.clear();
        }
        catch (IOException e) {
            this.reporter.reportMinimal(this.plugin, "saveTimings()", e);
        }
    }
    
    public void updateFinished() {
        final long currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        this.config.setAutoLastTime(currentTime);
        this.config.saveAll();
    }
    
    public void reloadConfiguration(final CommandSender sender) {
        this.plugin.reloadConfig();
        sender.sendMessage(ChatColor.YELLOW + "Reloaded configuration!");
    }
    
    private void printVersion(final CommandSender sender) {
        final PluginDescriptionFile desc = this.plugin.getDescription();
        sender.sendMessage(ChatColor.GREEN + desc.getName() + ChatColor.WHITE + " v" + ChatColor.GREEN + desc.getVersion());
        sender.sendMessage(ChatColor.WHITE + "Authors: " + ChatColor.GREEN + "dmulloy2" + ChatColor.WHITE + " and " + ChatColor.GREEN + "Comphenix");
        sender.sendMessage(ChatColor.WHITE + "Issues: " + ChatColor.GREEN + "https://github.com/dmulloy2/ProtocolLib/issues");
    }
    
    private void dump(final CommandSender sender) {
        final Closer closer = Closer.create();
        if (CommandProtocol.FILE_FORMAT == null) {
            CommandProtocol.FILE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        }
        if (CommandProtocol.TIMESTAMP_FORMAT == null) {
            CommandProtocol.TIMESTAMP_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
        }
        try {
            final Date date = new Date();
            final File file = new File(this.plugin.getDataFolder(), "dump-" + CommandProtocol.FILE_FORMAT.format(date) + ".txt");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            final FileWriter fw = closer.register(new FileWriter(file));
            final PrintWriter pw = closer.register(new PrintWriter(fw));
            pw.println("ProtocolLib Dump");
            pw.println("Timestamp: " + CommandProtocol.TIMESTAMP_FORMAT.format(date));
            pw.println();
            pw.println("ProtocolLib Version: " + this.plugin.toString());
            pw.println("Bukkit Version: " + this.plugin.getServer().getBukkitVersion());
            pw.println("Server Version: " + this.plugin.getServer().getVersion());
            pw.println("Java Version: " + System.getProperty("java.version"));
            pw.println();
            final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            pw.println("ProtocolLib: " + DetailedErrorReporter.getStringDescription(this.plugin));
            pw.println("Manager: " + DetailedErrorReporter.getStringDescription(manager));
            pw.println();
            final Set<PacketListener> listeners = (Set<PacketListener>)manager.getPacketListeners();
            final Set<Plugin> plugins = new HashSet<Plugin>();
            if (!listeners.isEmpty()) {
                pw.println("Listeners:");
                for (final PacketListener listener : listeners) {
                    pw.println(DetailedErrorReporter.getStringDescription(listener));
                    final Plugin plugin = listener.getPlugin();
                    if (plugin != null) {
                        plugins.add(plugin);
                    }
                    else {
                        pw.println("(Missing plugin!)");
                    }
                }
                pw.println();
            }
            else {
                pw.println("No listeners");
            }
            if (!plugins.isEmpty()) {
                pw.println("Plugins Using ProtocolLib:");
                for (final Plugin plugin2 : plugins) {
                    pw.println(plugin2.getName() + " by " + plugin2.getDescription().getAuthors());
                }
            }
            sender.sendMessage("Data dump written to " + file.getAbsolutePath());
        }
        catch (IOException ex) {
            ProtocolLogger.log(Level.SEVERE, "Failed to create dump:", ex);
            sender.sendMessage(ChatColor.RED + "Failed to create dump! Check console!");
        }
        finally {
            closer.close();
        }
    }
}
