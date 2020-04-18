package com.comphenix.protocol;

import java.text.MessageFormat;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import com.comphenix.protocol.injector.netty.WirePacket;
import com.google.common.base.Charsets;
import java.io.OutputStream;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.HexDump;
import java.io.ByteArrayOutputStream;
import com.comphenix.protocol.events.PacketEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.Formatter;
import java.util.logging.FileHandler;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.ArrayList;
import org.bukkit.plugin.Plugin;
import java.util.logging.Logger;
import com.comphenix.protocol.events.ListeningWhitelist;
import java.util.List;
import com.comphenix.protocol.events.PacketListener;
import org.bukkit.command.CommandExecutor;

public class PacketLogging implements CommandExecutor, PacketListener
{
    public static final String NAME = "packetlog";
    private List<PacketType> sendingTypes;
    private List<PacketType> receivingTypes;
    private ListeningWhitelist sendingWhitelist;
    private ListeningWhitelist receivingWhitelist;
    private Logger fileLogger;
    private LogLocation location;
    private final ProtocolManager manager;
    private final Plugin plugin;
    
    PacketLogging(final Plugin plugin, final ProtocolManager manager) {
        this.sendingTypes = new ArrayList<PacketType>();
        this.receivingTypes = new ArrayList<PacketType>();
        this.location = LogLocation.FILE;
        this.plugin = plugin;
        this.manager = manager;
    }
    
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        PacketType type = null;
        try {
            if (args.length <= 2) {
                sender.sendMessage(ChatColor.RED + "Invalid syntax: /packetlog <protocol> <sender> <packet> [location]");
                return true;
            }
            PacketType.Protocol protocol;
            try {
                protocol = PacketType.Protocol.valueOf(args[0].toUpperCase());
            }
            catch (IllegalArgumentException ex2) {
                sender.sendMessage(ChatColor.RED + "Unknown protocol " + args[0]);
                return true;
            }
            PacketType.Sender pSender;
            try {
                pSender = PacketType.Sender.valueOf(args[1].toUpperCase());
            }
            catch (IllegalArgumentException ex3) {
                sender.sendMessage(ChatColor.RED + "Unknown sender: " + args[1]);
                return true;
            }
            try {
                Label_0274: {
                    try {
                        final int id = Integer.parseInt(args[2]);
                        type = PacketType.findCurrent(protocol, pSender, id);
                    }
                    catch (NumberFormatException ex4) {
                        final String name = args[2];
                        Block_20: {
                            for (final PacketType packet : PacketType.values()) {
                                if (packet.getProtocol() == protocol && packet.getSender() == pSender) {
                                    if (packet.name().equalsIgnoreCase(name)) {
                                        break Block_20;
                                    }
                                    for (final String className : packet.getClassNames()) {
                                        if (className.equalsIgnoreCase(name)) {
                                            type = packet;
                                            break;
                                        }
                                    }
                                }
                            }
                            break Label_0274;
                        }
                        final PacketType packet;
                        type = packet;
                    }
                }
            }
            catch (IllegalArgumentException ex3) {
                type = null;
            }
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Unknown packet: " + args[2]);
                return true;
            }
            if (args.length > 3) {
                if (args[3].equalsIgnoreCase("console")) {
                    this.location = LogLocation.CONSOLE;
                }
                else {
                    this.location = LogLocation.FILE;
                }
            }
            if (pSender == PacketType.Sender.CLIENT) {
                if (this.receivingTypes.contains(type)) {
                    this.receivingTypes.remove(type);
                }
                else {
                    this.receivingTypes.add(type);
                }
            }
            else if (this.sendingTypes.contains(type)) {
                this.sendingTypes.remove(type);
            }
            else {
                this.sendingTypes.add(type);
            }
            this.startLogging();
            sender.sendMessage(ChatColor.GREEN + "Now logging " + type.getPacketClass().getSimpleName());
            return true;
        }
        catch (Throwable ex) {
            sender.sendMessage(ChatColor.RED + "Failed to parse command: " + ex.toString());
            return true;
        }
    }
    
    private void startLogging() {
        this.manager.removePacketListener(this);
        if (this.sendingTypes.isEmpty() && this.receivingTypes.isEmpty()) {
            return;
        }
        this.sendingWhitelist = ListeningWhitelist.newBuilder().types(this.sendingTypes).build();
        this.receivingWhitelist = ListeningWhitelist.newBuilder().types(this.receivingTypes).build();
        if (this.location == LogLocation.FILE && this.fileLogger == null) {
            this.fileLogger = Logger.getLogger("ProtocolLib-FileLogging");
            for (final Handler handler : this.fileLogger.getHandlers()) {
                this.fileLogger.removeHandler(handler);
            }
            this.fileLogger.setUseParentHandlers(false);
            try {
                final File logFile = new File(this.plugin.getDataFolder(), "log.log");
                final FileHandler handler2 = new FileHandler(logFile.getAbsolutePath(), true);
                handler2.setFormatter(new LogFormatter());
                this.fileLogger.addHandler(handler2);
            }
            catch (IOException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to obtain log file:", ex);
                return;
            }
        }
        this.manager.addPacketListener(this);
    }
    
    public void onPacketSending(final PacketEvent event) {
        this.log(event);
    }
    
    public void onPacketReceiving(final PacketEvent event) {
        this.log(event);
    }
    
    private static String hexDump(final byte[] bytes) throws IOException {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            HexDump.dump(bytes, 0L, (OutputStream)output, 0);
            return new String(output.toByteArray(), Charsets.UTF_8);
        }
    }
    
    private void log(final PacketEvent event) {
        try {
            final byte[] bytes = WirePacket.bytesFromPacket(event.getPacket());
            final String hexDump = hexDump(bytes);
            if (this.location == LogLocation.FILE) {
                this.fileLogger.log(Level.INFO, event.getPacketType() + ":");
                this.fileLogger.log(Level.INFO, hexDump);
                this.fileLogger.log(Level.INFO, "");
            }
            else {
                System.out.println(event.getPacketType() + ":");
                System.out.println(hexDump);
                System.out.println();
            }
        }
        catch (Throwable ex) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to log packet " + event.getPacketType() + ":", ex);
            this.plugin.getLogger().log(Level.WARNING, "Clearing packet logger...");
            this.sendingTypes.clear();
            this.receivingTypes.clear();
            this.startLogging();
        }
    }
    
    public ListeningWhitelist getSendingWhitelist() {
        return this.sendingWhitelist;
    }
    
    public ListeningWhitelist getReceivingWhitelist() {
        return this.receivingWhitelist;
    }
    
    public Plugin getPlugin() {
        return this.plugin;
    }
    
    private enum LogLocation
    {
        CONSOLE, 
        FILE;
    }
    
    private static class LogFormatter extends Formatter
    {
        private static final SimpleDateFormat DATE;
        private static final String LINE_SEPARATOR;
        private static final String FORMAT = "[{0}] {1}";
        
        @Override
        public String format(final LogRecord record) {
            final String string = this.formatMessage(record);
            if (string.isEmpty()) {
                return LogFormatter.LINE_SEPARATOR;
            }
            final StringBuilder message = new StringBuilder();
            message.append(MessageFormat.format("[{0}] {1}", LogFormatter.DATE.format(record.getMillis()), string));
            message.append(LogFormatter.LINE_SEPARATOR);
            return message.toString();
        }
        
        static {
            DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            LINE_SEPARATOR = System.getProperty("line.separator");
        }
    }
}
