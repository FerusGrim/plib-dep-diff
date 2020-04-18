package com.comphenix.protocol;

import java.util.Collection;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import org.bukkit.ChatColor;
import com.comphenix.protocol.error.Report;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.error.ReportType;
import org.bukkit.command.CommandExecutor;

abstract class CommandBase implements CommandExecutor
{
    public static final ReportType REPORT_COMMAND_ERROR;
    public static final ReportType REPORT_UNEXPECTED_COMMAND;
    public static final String PERMISSION_ADMIN = "protocol.admin";
    private String permission;
    private String name;
    private int minimumArgumentCount;
    protected ErrorReporter reporter;
    
    public CommandBase(final ErrorReporter reporter, final String permission, final String name) {
        this(reporter, permission, name, 0);
    }
    
    public CommandBase(final ErrorReporter reporter, final String permission, final String name, final int minimumArgumentCount) {
        this.reporter = reporter;
        this.name = name;
        this.permission = permission;
        this.minimumArgumentCount = minimumArgumentCount;
    }
    
    public final boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        try {
            if (!command.getName().equalsIgnoreCase(this.name)) {
                this.reporter.reportWarning(this, Report.newBuilder(CommandBase.REPORT_UNEXPECTED_COMMAND).messageParam(this));
                return false;
            }
            if (this.permission != null && !sender.hasPermission(this.permission)) {
                sender.sendMessage(ChatColor.RED + "You haven't got permission to run this command.");
                return true;
            }
            if (args != null && args.length >= this.minimumArgumentCount) {
                return this.handleCommand(sender, args);
            }
            sender.sendMessage(ChatColor.RED + "Insufficient arguments. You need at least " + this.minimumArgumentCount);
            return false;
        }
        catch (Throwable ex) {
            this.reporter.reportDetailed(this, Report.newBuilder(CommandBase.REPORT_COMMAND_ERROR).error(ex).messageParam(this.name).callerParam(sender, label, args));
            return true;
        }
    }
    
    protected Boolean parseBoolean(final Deque<String> arguments, final String parameterName) {
        Boolean result = null;
        if (!arguments.isEmpty()) {
            final String arg = arguments.peek();
            if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("on")) {
                result = true;
            }
            else if (arg.equalsIgnoreCase(parameterName)) {
                result = true;
            }
            else if (arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("off")) {
                result = false;
            }
        }
        if (result != null) {
            arguments.poll();
        }
        return result;
    }
    
    protected Deque<String> toQueue(final String[] args, final int start) {
        return new ArrayDeque<String>(Arrays.asList(args).subList(start, args.length));
    }
    
    public String getPermission() {
        return this.permission;
    }
    
    public String getName() {
        return this.name;
    }
    
    protected ErrorReporter getReporter() {
        return this.reporter;
    }
    
    protected abstract boolean handleCommand(final CommandSender p0, final String[] p1);
    
    static {
        REPORT_COMMAND_ERROR = new ReportType("Cannot execute command %s.");
        REPORT_UNEXPECTED_COMMAND = new ReportType("Incorrect command assigned to %s.");
    }
}
