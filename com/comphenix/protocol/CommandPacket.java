package com.comphenix.protocol;

import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.utility.HexDumper;
import com.comphenix.protocol.reflect.PrettyPrinter;
import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.events.PacketContainer;
import java.util.logging.Level;
import com.google.common.collect.Sets;
import com.comphenix.protocol.events.ListeningWhitelist;
import java.util.Iterator;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Set;
import java.util.Deque;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.Arrays;
import org.bukkit.ChatColor;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.error.Report;
import com.google.common.collect.MapMaker;
import java.util.WeakHashMap;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.concurrency.PacketTypeSet;
import java.util.List;
import org.bukkit.command.CommandSender;
import java.util.Map;
import com.comphenix.protocol.utility.ChatExtensions;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import com.comphenix.protocol.error.ReportType;

class CommandPacket extends CommandBase
{
    public static final ReportType REPORT_CANNOT_SEND_MESSAGE;
    public static final String NAME = "packet";
    public static final int PAGE_LINE_COUNT = 9;
    private static final int HEX_DUMP_THRESHOLD = 256;
    private Plugin plugin;
    private Logger logger;
    private ProtocolManager manager;
    private ChatExtensions chatter;
    private PacketTypeParser typeParser;
    private Map<CommandSender, List<String>> pagedMessage;
    private PacketTypeSet packetTypes;
    private PacketTypeSet extendedTypes;
    private PacketTypeSet compareTypes;
    private Map<PacketEvent, String> originalPackets;
    private PacketListener listener;
    private PacketListener compareListener;
    private CommandFilter filter;
    
    public CommandPacket(final ErrorReporter reporter, final Plugin plugin, final Logger logger, final CommandFilter filter, final ProtocolManager manager) {
        super(reporter, "protocol.admin", "packet", 1);
        this.typeParser = new PacketTypeParser();
        this.pagedMessage = new WeakHashMap<CommandSender, List<String>>();
        this.packetTypes = new PacketTypeSet();
        this.extendedTypes = new PacketTypeSet();
        this.compareTypes = new PacketTypeSet();
        this.originalPackets = (Map<PacketEvent, String>)new MapMaker().weakKeys().makeMap();
        this.plugin = plugin;
        this.logger = logger;
        this.manager = manager;
        this.filter = filter;
        this.chatter = new ChatExtensions(manager);
    }
    
    public void sendMessageSilently(final CommandSender receiver, final String message) {
        try {
            this.chatter.sendMessageSilently(receiver, message);
        }
        catch (InvocationTargetException e) {
            this.reporter.reportDetailed(this, Report.newBuilder(CommandPacket.REPORT_CANNOT_SEND_MESSAGE).error(e).callerParam(receiver, message));
        }
    }
    
    public void broadcastMessageSilently(final String message, final String permission) {
        try {
            this.chatter.broadcastMessageSilently(message, permission);
        }
        catch (InvocationTargetException e) {
            this.reporter.reportDetailed(this, Report.newBuilder(CommandPacket.REPORT_CANNOT_SEND_MESSAGE).error(e).callerParam(message, permission));
        }
    }
    
    private void printPage(final CommandSender sender, final int pageIndex) {
        final List<String> paged = this.pagedMessage.get(sender);
        if (paged != null) {
            final int lastPage = (paged.size() - 1) / 9 + 1;
            for (int i = 9 * (pageIndex - 1); i < 9 * pageIndex; ++i) {
                if (i < paged.size()) {
                    this.sendMessageSilently(sender, " " + paged.get(i));
                }
            }
            if (pageIndex < lastPage) {
                this.sendMessageSilently(sender, "Send /packet page " + (pageIndex + 1) + " for the next page.");
            }
        }
        else {
            this.sendMessageSilently(sender, ChatColor.RED + "No pages found.");
        }
    }
    
    @Override
    protected boolean handleCommand(final CommandSender sender, final String[] args) {
        try {
            final Deque<String> arguments = new ArrayDeque<String>(Arrays.asList(args));
            final SubCommand subCommand = this.parseCommand(arguments);
            if (subCommand == SubCommand.PAGE) {
                if (args.length <= 1) {
                    this.sendMessageSilently(sender, ChatColor.RED + "Must specify a page index.");
                    return true;
                }
                final int page = Integer.parseInt(args[1]);
                if (page > 0) {
                    this.printPage(sender, page);
                }
                else {
                    this.sendMessageSilently(sender, ChatColor.RED + "Page index must be greater than zero.");
                }
                return true;
            }
            else {
                final Set<PacketType> types = this.typeParser.parseTypes(arguments, PacketTypeParser.DEFAULT_MAX_RANGE);
                Boolean detailed = this.parseBoolean(arguments, "detailed");
                Boolean compare = this.parseBoolean(arguments, "compare");
                if (this.typeParser.getLastProtocol() == null) {
                    sender.sendMessage(ChatColor.YELLOW + "Warning: Missing protocol (PLAY, etc) - assuming legacy IDs.");
                }
                if (arguments.size() > 0) {
                    throw new IllegalArgumentException("Cannot parse " + arguments);
                }
                if (detailed == null) {
                    detailed = false;
                }
                if (compare == null) {
                    compare = false;
                }
                else {
                    detailed = true;
                }
                if (subCommand == SubCommand.ADD) {
                    if (args.length == 1) {
                        sender.sendMessage(ChatColor.RED + "Please specify a connection side.");
                        return false;
                    }
                    this.executeAddCommand(sender, types, detailed, compare);
                }
                else if (subCommand == SubCommand.REMOVE) {
                    this.executeRemoveCommand(sender, types);
                }
                else if (subCommand == SubCommand.NAMES) {
                    this.executeNamesCommand(sender, types);
                }
            }
        }
        catch (NumberFormatException e) {
            this.sendMessageSilently(sender, ChatColor.RED + "Cannot parse number: " + e.getMessage());
        }
        catch (IllegalArgumentException e2) {
            this.sendMessageSilently(sender, ChatColor.RED + e2.getMessage());
        }
        return true;
    }
    
    private void executeAddCommand(final CommandSender sender, final Set<PacketType> addition, final boolean detailed, final boolean compare) {
        this.packetTypes.addAll(addition);
        if (detailed) {
            this.extendedTypes.addAll(addition);
        }
        if (compare) {
            this.compareTypes.addAll(addition);
        }
        this.updatePacketListener();
        this.sendMessageSilently(sender, ChatColor.YELLOW + "Added listener " + this.getWhitelistInfo(this.listener));
    }
    
    private void executeRemoveCommand(final CommandSender sender, final Set<PacketType> removal) {
        this.packetTypes.removeAll(removal);
        this.extendedTypes.removeAll(removal);
        this.compareTypes.removeAll(removal);
        this.updatePacketListener();
        this.sendMessageSilently(sender, ChatColor.YELLOW + "Removing packet types.");
    }
    
    private void executeNamesCommand(final CommandSender sender, final Set<PacketType> types) {
        final List<String> messages = new ArrayList<String>();
        for (final PacketType type : types) {
            messages.add(ChatColor.YELLOW + type.toString());
        }
        if (sender instanceof Player && messages.size() > 0 && messages.size() > 9) {
            this.pagedMessage.put(sender, messages);
            this.printPage(sender, 1);
        }
        else {
            for (final String message : messages) {
                this.sendMessageSilently(sender, message);
            }
        }
    }
    
    private String getWhitelistInfo(final PacketListener listener) {
        final boolean sendingEmpty = ListeningWhitelist.isEmpty(listener.getSendingWhitelist());
        final boolean receivingEmpty = ListeningWhitelist.isEmpty(listener.getReceivingWhitelist());
        if (!sendingEmpty && !receivingEmpty) {
            return String.format("Sending: %s, Receiving: %s", listener.getSendingWhitelist(), listener.getReceivingWhitelist());
        }
        if (!sendingEmpty) {
            return listener.getSendingWhitelist().toString();
        }
        if (!receivingEmpty) {
            return listener.getReceivingWhitelist().toString();
        }
        return "[None]";
    }
    
    private Set<PacketType> filterTypes(final Set<PacketType> types, final PacketType.Sender sender) {
        final Set<PacketType> result = (Set<PacketType>)Sets.newHashSet();
        for (final PacketType type : types) {
            if (type.getSender() == sender) {
                result.add(type);
            }
        }
        return result;
    }
    
    public PacketListener createPacketListener(final Set<PacketType> type) {
        final ListeningWhitelist serverList = ListeningWhitelist.newBuilder().types(this.filterTypes(type, PacketType.Sender.SERVER)).gamePhaseBoth().monitor().build();
        final ListeningWhitelist clientList = ListeningWhitelist.newBuilder(serverList).types(this.filterTypes(type, PacketType.Sender.CLIENT)).monitor().build();
        return new PacketListener() {
            @Override
            public void onPacketSending(final PacketEvent event) {
                if (CommandPacket.this.filter.filterEvent(event)) {
                    this.printInformation(event);
                }
            }
            
            @Override
            public void onPacketReceiving(final PacketEvent event) {
                if (CommandPacket.this.filter.filterEvent(event)) {
                    this.printInformation(event);
                }
            }
            
            private void printInformation(final PacketEvent event) {
                final String verb = event.isServerPacket() ? "Sent" : "Received";
                final String format = event.isServerPacket() ? "%s %s to %s" : "%s %s from %s";
                final String shortDescription = String.format(format, event.isCancelled() ? "Cancelled" : verb, event.getPacketType(), event.getPlayer().getName());
                if (CommandPacket.this.extendedTypes.contains(event.getPacketType())) {
                    try {
                        final String original = CommandPacket.this.originalPackets.remove(event);
                        if (original != null) {
                            CommandPacket.this.logger.info("Initial packet:\n" + original + " -> ");
                        }
                        CommandPacket.this.logger.info(shortDescription + ":\n" + CommandPacket.this.getPacketDescription(event.getPacket()));
                    }
                    catch (IllegalAccessException e) {
                        CommandPacket.this.logger.log(Level.WARNING, "Unable to use reflection.", e);
                    }
                }
                else {
                    CommandPacket.this.logger.info(shortDescription + ".");
                }
            }
            
            @Override
            public ListeningWhitelist getSendingWhitelist() {
                return serverList;
            }
            
            @Override
            public ListeningWhitelist getReceivingWhitelist() {
                return clientList;
            }
            
            @Override
            public Plugin getPlugin() {
                return CommandPacket.this.plugin;
            }
        };
    }
    
    public PacketListener createCompareListener(final Set<PacketType> type) {
        final ListeningWhitelist serverList = ListeningWhitelist.newBuilder().types(this.filterTypes(type, PacketType.Sender.SERVER)).gamePhaseBoth().lowest().build();
        final ListeningWhitelist clientList = ListeningWhitelist.newBuilder(serverList).types(this.filterTypes(type, PacketType.Sender.CLIENT)).lowest().build();
        return new PacketListener() {
            @Override
            public void onPacketSending(final PacketEvent event) {
                this.savePacketState(event);
            }
            
            @Override
            public void onPacketReceiving(final PacketEvent event) {
                this.savePacketState(event);
            }
            
            private void savePacketState(final PacketEvent event) {
                try {
                    CommandPacket.this.originalPackets.put(event, CommandPacket.this.getPacketDescription(event.getPacket()));
                }
                catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot read packet.", e);
                }
            }
            
            @Override
            public ListeningWhitelist getSendingWhitelist() {
                return serverList;
            }
            
            @Override
            public ListeningWhitelist getReceivingWhitelist() {
                return clientList;
            }
            
            @Override
            public Plugin getPlugin() {
                return CommandPacket.this.plugin;
            }
        };
    }
    
    public String getPacketDescription(final PacketContainer packetContainer) throws IllegalAccessException {
        final Object packet = packetContainer.getHandle();
        Class<?> clazz;
        for (clazz = packet.getClass(); clazz != null && clazz != Object.class && (!MinecraftReflection.isMinecraftClass(clazz) || Factory.class.isAssignableFrom(clazz)); clazz = clazz.getSuperclass()) {}
        return PrettyPrinter.printObject(packet, clazz, MinecraftReflection.getPacketClass(), 3, new PrettyPrinter.ObjectPrinter() {
            @Override
            public boolean print(final StringBuilder output, final Object value) {
                if (value instanceof byte[]) {
                    final byte[] data = (byte[])value;
                    if (data.length > 256) {
                        output.append("[");
                        HexDumper.defaultDumper().appendTo(output, data);
                        output.append("]");
                        return true;
                    }
                }
                else if (value != null) {
                    final EquivalentConverter<Object> converter = CommandPacket.this.findConverter(value.getClass());
                    if (converter != null) {
                        output.append(converter.getSpecific(value));
                        return true;
                    }
                }
                return false;
            }
        });
    }
    
    private EquivalentConverter<Object> findConverter(Class<?> clazz) {
        final Map<Class<?>, EquivalentConverter<Object>> converters = BukkitConverters.getConvertersForGeneric();
        while (clazz != null) {
            final EquivalentConverter<Object> result = converters.get(clazz);
            if (result != null) {
                return result;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
    
    public PacketListener updatePacketListener() {
        if (this.listener != null) {
            this.manager.removePacketListener(this.listener);
        }
        if (this.compareListener != null) {
            this.manager.removePacketListener(this.compareListener);
        }
        this.listener = this.createPacketListener(this.packetTypes.values());
        this.compareListener = this.createCompareListener(this.compareTypes.values());
        this.manager.addPacketListener(this.listener);
        this.manager.addPacketListener(this.compareListener);
        return this.listener;
    }
    
    private SubCommand parseCommand(final Deque<String> arguments) {
        final String text = arguments.poll().toLowerCase();
        if ("add".startsWith(text)) {
            return SubCommand.ADD;
        }
        if ("remove".startsWith(text)) {
            return SubCommand.REMOVE;
        }
        if ("names".startsWith(text)) {
            return SubCommand.NAMES;
        }
        if ("page".startsWith(text)) {
            return SubCommand.PAGE;
        }
        throw new IllegalArgumentException(text + " is not a valid sub command. Must be add or remove.");
    }
    
    static {
        REPORT_CANNOT_SEND_MESSAGE = new ReportType("Cannot send chat message.");
    }
    
    private enum SubCommand
    {
        ADD, 
        REMOVE, 
        NAMES, 
        PAGE;
    }
}
