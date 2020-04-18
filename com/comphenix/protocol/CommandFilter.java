package com.comphenix.protocol;

import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import javax.script.Invocable;
import com.google.common.collect.Sets;
import java.util.Deque;
import org.bukkit.conversations.ConversationAbandonedEvent;
import java.util.Set;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Conversable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import java.util.Iterator;
import com.comphenix.protocol.events.PacketEvent;
import javax.script.ScriptEngineManager;
import com.comphenix.protocol.error.Report;
import javax.script.ScriptException;
import java.util.ArrayList;
import com.comphenix.protocol.error.ErrorReporter;
import javax.script.ScriptEngine;
import org.bukkit.plugin.Plugin;
import java.util.List;
import com.comphenix.protocol.error.ReportType;

public class CommandFilter extends CommandBase
{
    public static final ReportType REPORT_FALLBACK_ENGINE;
    public static final ReportType REPORT_CANNOT_LOAD_FALLBACK_ENGINE;
    public static final ReportType REPORT_PACKAGES_UNSUPPORTED_IN_ENGINE;
    public static final ReportType REPORT_FILTER_REMOVED_FOR_ERROR;
    public static final ReportType REPORT_CANNOT_HANDLE_CONVERSATION;
    public static final String NAME = "filter";
    private FilterFailedHandler defaultFailedHandler;
    private List<Filter> filters;
    private final Plugin plugin;
    private ProtocolConfig config;
    private ScriptEngine engine;
    private boolean uninitialized;
    
    public CommandFilter(final ErrorReporter reporter, final Plugin plugin, final ProtocolConfig config) {
        super(reporter, "protocol.admin", "filter", 2);
        this.filters = new ArrayList<Filter>();
        this.plugin = plugin;
        this.config = config;
        this.uninitialized = true;
    }
    
    private void initalizeScript() {
        try {
            this.initializeEngine();
            if (!this.isInitialized()) {
                throw new ScriptException("A JavaScript engine could not be found.");
            }
            this.plugin.getLogger().info("Loaded command filter engine.");
        }
        catch (ScriptException e1) {
            this.printPackageWarning(e1);
            if (!this.config.getScriptEngineName().equals("rhino")) {
                this.reporter.reportWarning(this, Report.newBuilder(CommandFilter.REPORT_FALLBACK_ENGINE));
                this.config.setScriptEngineName("rhino");
                this.config.saveAll();
                try {
                    this.initializeEngine();
                    if (!this.isInitialized()) {
                        this.reporter.reportWarning(this, Report.newBuilder(CommandFilter.REPORT_CANNOT_LOAD_FALLBACK_ENGINE));
                    }
                }
                catch (ScriptException e2) {
                    this.printPackageWarning(e2);
                }
            }
        }
    }
    
    private void printPackageWarning(final ScriptException e) {
        this.reporter.reportWarning(this, Report.newBuilder(CommandFilter.REPORT_PACKAGES_UNSUPPORTED_IN_ENGINE).error(e));
    }
    
    private void initializeEngine() throws ScriptException {
        final ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName(this.config.getScriptEngineName());
        if (this.engine != null) {
            this.engine.eval("importPackage(org.bukkit);");
            this.engine.eval("importPackage(com.comphenix.protocol.reflect);");
        }
    }
    
    public boolean isInitialized() {
        return this.engine != null;
    }
    
    private FilterFailedHandler getDefaultErrorHandler() {
        if (this.defaultFailedHandler == null) {
            this.defaultFailedHandler = new FilterFailedHandler() {
                @Override
                public boolean handle(final PacketEvent event, final Filter filter, final Exception ex) {
                    CommandFilter.this.reporter.reportMinimal(CommandFilter.this.plugin, "filterEvent(PacketEvent)", ex, event);
                    CommandFilter.this.reporter.reportWarning(this, Report.newBuilder(CommandFilter.REPORT_FILTER_REMOVED_FOR_ERROR).messageParam(filter.getName(), ex.getClass().getSimpleName()));
                    return false;
                }
            };
        }
        return this.defaultFailedHandler;
    }
    
    public boolean filterEvent(final PacketEvent event) {
        return this.filterEvent(event, this.getDefaultErrorHandler());
    }
    
    public boolean filterEvent(final PacketEvent event, final FilterFailedHandler handler) {
        final Iterator<Filter> it = this.filters.iterator();
        while (it.hasNext()) {
            final Filter filter = it.next();
            try {
                if (!filter.evaluate(this.engine, event)) {
                    return false;
                }
                continue;
            }
            catch (Exception ex) {
                if (handler.handle(event, filter, ex)) {
                    continue;
                }
                it.remove();
            }
        }
        return true;
    }
    
    private void checkScriptStatus() {
        if (this.uninitialized) {
            this.uninitialized = false;
            this.initalizeScript();
        }
    }
    
    @Override
    protected boolean handleCommand(final CommandSender sender, final String[] args) {
        this.checkScriptStatus();
        if (!this.config.isDebug()) {
            sender.sendMessage(ChatColor.RED + "Debug mode must be enabled in the configuration first!");
            return true;
        }
        if (!this.isInitialized()) {
            sender.sendMessage(ChatColor.RED + "JavaScript engine was not present. Filter system is disabled.");
            return true;
        }
        final SubCommand command = this.parseCommand(args, 0);
        final String name = args[1];
        switch (command) {
            case ADD: {
                if (this.findFilter(name) != null) {
                    sender.sendMessage(ChatColor.RED + "Filter " + name + " already exists. Remove it first.");
                    return true;
                }
                final Deque<String> rangeArguments = this.toQueue(args, 2);
                final PacketTypeParser parser = new PacketTypeParser();
                final Set<PacketType> packets = parser.parseTypes(rangeArguments, PacketTypeParser.DEFAULT_MAX_RANGE);
                sender.sendMessage("Enter filter program ('}' to complete or CANCEL):");
                if (sender instanceof Conversable) {
                    final MultipleLinesPrompt prompt = new MultipleLinesPrompt(new CompilationSuccessCanceller(), "function(event, packet) {");
                    new ConversationFactory(this.plugin).withFirstPrompt((Prompt)prompt).withEscapeSequence("CANCEL").withLocalEcho(false).addConversationAbandonedListener((ConversationAbandonedListener)new ConversationAbandonedListener() {
                        public void conversationAbandoned(final ConversationAbandonedEvent event) {
                            try {
                                final Conversable whom = event.getContext().getForWhom();
                                if (event.gracefulExit()) {
                                    final String predicate = prompt.removeAccumulatedInput(event.getContext());
                                    final Filter filter = new Filter(name, predicate, packets);
                                    whom.sendRawMessage(prompt.getPromptText(event.getContext()));
                                    try {
                                        filter.compile(CommandFilter.this.engine);
                                        CommandFilter.this.filters.add(filter);
                                        whom.sendRawMessage(ChatColor.GOLD + "Added filter " + name);
                                    }
                                    catch (ScriptException e) {
                                        e.printStackTrace();
                                        whom.sendRawMessage(ChatColor.GOLD + "Compilation error: " + e.getMessage());
                                    }
                                }
                                else {
                                    whom.sendRawMessage(ChatColor.RED + "Cancelled filter.");
                                }
                            }
                            catch (Exception e2) {
                                CommandFilter.this.reporter.reportDetailed(this, Report.newBuilder(CommandFilter.REPORT_CANNOT_HANDLE_CONVERSATION).error(e2).callerParam(event));
                            }
                        }
                    }).buildConversation((Conversable)sender).begin();
                    break;
                }
                sender.sendMessage(ChatColor.RED + "Only console and players are supported!");
                break;
            }
            case REMOVE: {
                final Filter filter = this.findFilter(name);
                if (filter != null) {
                    filter.close(this.engine);
                    this.filters.remove(filter);
                    sender.sendMessage(ChatColor.GOLD + "Removed filter " + name);
                    break;
                }
                sender.sendMessage(ChatColor.RED + "Unable to find a filter by the name " + name);
                break;
            }
        }
        return true;
    }
    
    private Filter findFilter(final String name) {
        for (final Filter filter : this.filters) {
            if (filter.getName().equalsIgnoreCase(name)) {
                return filter;
            }
        }
        return null;
    }
    
    private SubCommand parseCommand(final String[] args, final int index) {
        final String text = args[index].toUpperCase();
        try {
            return SubCommand.valueOf(text);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(text + " is not a valid sub command. Must be add or remove.", e);
        }
    }
    
    static {
        REPORT_FALLBACK_ENGINE = new ReportType("Falling back to the Rhino engine.");
        REPORT_CANNOT_LOAD_FALLBACK_ENGINE = new ReportType("Could not load Rhino either. Please upgrade your JVM or OS.");
        REPORT_PACKAGES_UNSUPPORTED_IN_ENGINE = new ReportType("Unable to initialize packages for JavaScript engine.");
        REPORT_FILTER_REMOVED_FOR_ERROR = new ReportType("Removing filter %s for causing %s.");
        REPORT_CANNOT_HANDLE_CONVERSATION = new ReportType("Cannot handle conversation.");
    }
    
    private enum SubCommand
    {
        ADD, 
        REMOVE;
    }
    
    public static class Filter
    {
        private final String name;
        private final String predicate;
        private final Set<PacketType> packets;
        
        public Filter(final String name, final String predicate, final Set<PacketType> packets) {
            this.name = name;
            this.predicate = predicate;
            this.packets = (Set<PacketType>)Sets.newHashSet((Iterable)packets);
        }
        
        public String getName() {
            return this.name;
        }
        
        public String getPredicate() {
            return this.predicate;
        }
        
        public Set<PacketType> getRanges() {
            return (Set<PacketType>)Sets.newHashSet((Iterable)this.packets);
        }
        
        private boolean isApplicable(final PacketEvent event) {
            return this.packets.contains(event.getPacketType());
        }
        
        public boolean evaluate(final ScriptEngine context, final PacketEvent event) throws ScriptException {
            if (!this.isApplicable(event)) {
                return true;
            }
            this.compile(context);
            try {
                final Object result = ((Invocable)context).invokeFunction(this.name, event, event.getPacket().getHandle());
                if (result instanceof Boolean) {
                    return (boolean)result;
                }
                throw new ScriptException("Filter result wasn't a boolean: " + result);
            }
            catch (NoSuchMethodException e) {
                throw new IllegalStateException("Unable to compile " + this.name + " into current script engine.", e);
            }
        }
        
        public void compile(final ScriptEngine context) throws ScriptException {
            if (context.get(this.name) == null) {
                context.eval("var " + this.name + " = function(event, packet) {\n" + this.predicate);
            }
        }
        
        public void close(final ScriptEngine context) {
            context.put(this.name, null);
        }
    }
    
    private class CompilationSuccessCanceller implements MultipleLinesPrompt.MultipleConversationCanceller
    {
        @Override
        public boolean cancelBasedOnInput(final ConversationContext context, final String in) {
            throw new UnsupportedOperationException("Cannot cancel on the last line alone.");
        }
        
        public void setConversation(final Conversation conversation) {
        }
        
        @Override
        public boolean cancelBasedOnInput(final ConversationContext context, final String currentLine, final StringBuilder lines, final int lineCount) {
            try {
                CommandFilter.this.engine.eval("function(event, packet) {\n" + lines.toString());
                return true;
            }
            catch (ScriptException e) {
                final int realLineCount = lineCount + 1;
                return e.getLineNumber() < realLineCount;
            }
        }
        
        public CompilationSuccessCanceller clone() {
            return new CompilationSuccessCanceller();
        }
    }
    
    public interface FilterFailedHandler
    {
        boolean handle(final PacketEvent p0, final Filter p1, final Exception p2);
    }
}
