package com.comphenix.protocol.injector.player;

import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.reflect.ObjectWriter;
import java.util.Iterator;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.util.ArrayList;
import com.comphenix.protocol.error.ErrorReporter;
import org.bukkit.Server;
import com.comphenix.protocol.injector.server.AbstractInputStreamLookup;
import com.comphenix.protocol.reflect.VolatileField;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import com.comphenix.protocol.error.ReportType;

public class InjectedServerConnection
{
    public static final ReportType REPORT_CANNOT_FIND_MINECRAFT_SERVER;
    public static final ReportType REPORT_CANNOT_INJECT_SERVER_CONNECTION;
    public static final ReportType REPORT_CANNOT_FIND_LISTENER_THREAD;
    public static final ReportType REPORT_CANNOT_READ_LISTENER_THREAD;
    public static final ReportType REPORT_CANNOT_FIND_SERVER_CONNECTION;
    public static final ReportType REPORT_UNEXPECTED_THREAD_COUNT;
    public static final ReportType REPORT_CANNOT_FIND_NET_HANDLER_THREAD;
    public static final ReportType REPORT_INSUFFICENT_THREAD_COUNT;
    public static final ReportType REPORT_CANNOT_COPY_OLD_TO_NEW;
    private static Field listenerThreadField;
    private static Field minecraftServerField;
    private static Field listField;
    private static Field dedicatedThreadField;
    private static Method serverConnectionMethod;
    private List<VolatileField> listFields;
    private List<ReplacedArrayList<Object>> replacedLists;
    private NetLoginInjector netLoginInjector;
    private AbstractInputStreamLookup socketInjector;
    private ServerSocketType socketType;
    private Server server;
    private ErrorReporter reporter;
    private boolean hasAttempted;
    private boolean hasSuccess;
    private Object minecraftServer;
    
    public InjectedServerConnection(final ErrorReporter reporter, final AbstractInputStreamLookup socketInjector, final Server server, final NetLoginInjector netLoginInjector) {
        this.minecraftServer = null;
        this.listFields = new ArrayList<VolatileField>();
        this.replacedLists = new ArrayList<ReplacedArrayList<Object>>();
        this.reporter = reporter;
        this.server = server;
        this.socketInjector = socketInjector;
        this.netLoginInjector = netLoginInjector;
    }
    
    public static Object getServerConnection(final ErrorReporter reporter, final Server server) {
        try {
            final InjectedServerConnection inspector = new InjectedServerConnection(reporter, null, server, null);
            return inspector.getServerConnection();
        }
        catch (IllegalAccessException e) {
            throw new FieldAccessException("Reflection error.", e);
        }
        catch (IllegalArgumentException e2) {
            throw new FieldAccessException("Corrupt data.", e2);
        }
        catch (InvocationTargetException e3) {
            throw new FieldAccessException("Minecraft error.", e3);
        }
    }
    
    public void initialize() {
        if (!this.hasAttempted) {
            this.hasAttempted = true;
            if (InjectedServerConnection.minecraftServerField == null) {
                InjectedServerConnection.minecraftServerField = FuzzyReflection.fromObject(this.server, true).getFieldByType("MinecraftServer", MinecraftReflection.getMinecraftServerClass());
            }
            try {
                this.minecraftServer = FieldUtils.readField(InjectedServerConnection.minecraftServerField, this.server, true);
            }
            catch (IllegalAccessException e2) {
                this.reporter.reportWarning(this, Report.newBuilder(InjectedServerConnection.REPORT_CANNOT_FIND_MINECRAFT_SERVER));
                return;
            }
            try {
                if (InjectedServerConnection.serverConnectionMethod == null) {
                    InjectedServerConnection.serverConnectionMethod = FuzzyReflection.fromClass(InjectedServerConnection.minecraftServerField.getType()).getMethodByParameters("getServerConnection", MinecraftReflection.getServerConnectionClass(), new Class[0]);
                }
                this.socketType = ServerSocketType.SERVER_CONNECTION;
            }
            catch (IllegalArgumentException e3) {
                this.socketType = ServerSocketType.LISTENER_THREAD;
            }
            catch (Exception e) {
                this.reporter.reportDetailed(this, Report.newBuilder(InjectedServerConnection.REPORT_CANNOT_INJECT_SERVER_CONNECTION).error(e));
            }
        }
    }
    
    public ServerSocketType getServerSocketType() {
        return this.socketType;
    }
    
    public void injectList() {
        this.initialize();
        if (this.socketType == ServerSocketType.SERVER_CONNECTION) {
            this.injectServerConnection();
        }
        else {
            if (this.socketType != ServerSocketType.LISTENER_THREAD) {
                throw new IllegalStateException("Unable to detected server connection.");
            }
            this.injectListenerThread();
        }
    }
    
    private void initializeListenerField() {
        if (InjectedServerConnection.listenerThreadField == null) {
            InjectedServerConnection.listenerThreadField = FuzzyReflection.fromObject(this.minecraftServer).getFieldByType("networkListenThread", MinecraftReflection.getNetworkListenThreadClass());
        }
    }
    
    public Object getListenerThread() throws RuntimeException, IllegalAccessException {
        this.initialize();
        if (this.socketType == ServerSocketType.LISTENER_THREAD) {
            this.initializeListenerField();
            return InjectedServerConnection.listenerThreadField.get(this.minecraftServer);
        }
        return null;
    }
    
    public Object getServerConnection() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.initialize();
        if (this.socketType == ServerSocketType.SERVER_CONNECTION) {
            return InjectedServerConnection.serverConnectionMethod.invoke(this.minecraftServer, new Object[0]);
        }
        return null;
    }
    
    private void injectListenerThread() {
        try {
            this.initializeListenerField();
        }
        catch (RuntimeException e) {
            this.reporter.reportDetailed(this, Report.newBuilder(InjectedServerConnection.REPORT_CANNOT_FIND_LISTENER_THREAD).callerParam(this.minecraftServer).error(e));
            return;
        }
        Object listenerThread = null;
        try {
            listenerThread = this.getListenerThread();
        }
        catch (Exception e2) {
            this.reporter.reportWarning(this, Report.newBuilder(InjectedServerConnection.REPORT_CANNOT_READ_LISTENER_THREAD).error(e2));
            return;
        }
        this.injectServerSocket(listenerThread);
        this.injectEveryListField(listenerThread, 1);
        this.hasSuccess = true;
    }
    
    private void injectServerConnection() {
        Object serverConnection = null;
        try {
            serverConnection = this.getServerConnection();
        }
        catch (Exception e) {
            this.reporter.reportDetailed(this, Report.newBuilder(InjectedServerConnection.REPORT_CANNOT_FIND_SERVER_CONNECTION).callerParam(this.minecraftServer).error(e));
            return;
        }
        if (InjectedServerConnection.listField == null) {
            InjectedServerConnection.listField = FuzzyReflection.fromClass(InjectedServerConnection.serverConnectionMethod.getReturnType(), true).getFieldByType("netServerHandlerList", List.class);
        }
        if (InjectedServerConnection.dedicatedThreadField == null) {
            final List<Field> matches = FuzzyReflection.fromObject(serverConnection, true).getFieldListByType(Thread.class);
            if (matches.size() != 1) {
                this.reporter.reportWarning(this, Report.newBuilder(InjectedServerConnection.REPORT_UNEXPECTED_THREAD_COUNT).messageParam(serverConnection.getClass(), matches.size()));
            }
            else {
                InjectedServerConnection.dedicatedThreadField = matches.get(0);
            }
        }
        try {
            if (InjectedServerConnection.dedicatedThreadField != null) {
                final Object dedicatedThread = FieldUtils.readField(InjectedServerConnection.dedicatedThreadField, serverConnection, true);
                this.injectServerSocket(dedicatedThread);
                this.injectEveryListField(dedicatedThread, 1);
            }
        }
        catch (IllegalAccessException e2) {
            this.reporter.reportWarning(this, Report.newBuilder(InjectedServerConnection.REPORT_CANNOT_FIND_NET_HANDLER_THREAD).error(e2));
        }
        this.injectIntoList(serverConnection, InjectedServerConnection.listField);
        this.hasSuccess = true;
    }
    
    private void injectServerSocket(final Object container) {
        this.socketInjector.inject(container);
    }
    
    private void injectEveryListField(final Object container, final int minimum) {
        final List<Field> lists = FuzzyReflection.fromObject(container, true).getFieldListByType(List.class);
        for (final Field list : lists) {
            this.injectIntoList(container, list);
        }
        if (lists.size() < minimum) {
            this.reporter.reportWarning(this, Report.newBuilder(InjectedServerConnection.REPORT_INSUFFICENT_THREAD_COUNT).messageParam(minimum, container.getClass()));
        }
    }
    
    private void injectIntoList(final Object instance, final Field field) {
        final VolatileField listFieldRef = new VolatileField(field, instance, true);
        final List<Object> list = (List<Object>)listFieldRef.getValue();
        if (list instanceof ReplacedArrayList) {
            this.replacedLists.add((ReplacedArrayList)list);
        }
        else {
            final ReplacedArrayList<Object> injectedList = this.createReplacement(list);
            this.replacedLists.add(injectedList);
            listFieldRef.setValue(injectedList);
            this.listFields.add(listFieldRef);
        }
    }
    
    private ReplacedArrayList<Object> createReplacement(final List<Object> list) {
        return new ReplacedArrayList<Object>(list) {
            private static final long serialVersionUID = 2070481080950500367L;
            private final ObjectWriter writer = new ObjectWriter();
            
            @Override
            protected void onReplacing(final Object inserting, final Object replacement) {
                if (!(inserting instanceof Factory)) {
                    try {
                        this.writer.copyTo(inserting, replacement, inserting.getClass());
                    }
                    catch (OutOfMemoryError e) {
                        throw e;
                    }
                    catch (ThreadDeath e2) {
                        throw e2;
                    }
                    catch (Throwable e3) {
                        InjectedServerConnection.this.reporter.reportDetailed(InjectedServerConnection.this, Report.newBuilder(InjectedServerConnection.REPORT_CANNOT_COPY_OLD_TO_NEW).messageParam(inserting).callerParam(inserting, replacement).error(e3));
                    }
                }
            }
            
            @Override
            protected void onInserting(final Object inserting) {
                if (MinecraftReflection.isLoginHandler(inserting)) {
                    final Object replaced = InjectedServerConnection.this.netLoginInjector.onNetLoginCreated(inserting);
                    if (inserting != replaced) {
                        this.addMapping(inserting, replaced, true);
                    }
                }
            }
            
            @Override
            protected void onRemoved(final Object removing) {
                if (MinecraftReflection.isLoginHandler(removing)) {
                    InjectedServerConnection.this.netLoginInjector.cleanup(removing);
                }
            }
        };
    }
    
    public void replaceServerHandler(final Object oldHandler, final Object newHandler) {
        if (!this.hasAttempted) {
            this.injectList();
        }
        if (this.hasSuccess) {
            for (final ReplacedArrayList<Object> replacedList : this.replacedLists) {
                replacedList.addMapping(oldHandler, newHandler);
            }
        }
    }
    
    public void revertServerHandler(final Object oldHandler) {
        if (this.hasSuccess) {
            for (final ReplacedArrayList<Object> replacedList : this.replacedLists) {
                replacedList.removeMapping(oldHandler);
            }
        }
    }
    
    public void cleanupAll() {
        if (this.replacedLists.size() > 0) {
            for (final ReplacedArrayList<Object> replacedList : this.replacedLists) {
                replacedList.revertAll();
            }
            for (final VolatileField field : this.listFields) {
                field.revertValue();
            }
            this.listFields.clear();
            this.replacedLists.clear();
        }
    }
    
    static {
        REPORT_CANNOT_FIND_MINECRAFT_SERVER = new ReportType("Cannot extract minecraft server from Bukkit.");
        REPORT_CANNOT_INJECT_SERVER_CONNECTION = new ReportType("Cannot inject into server connection. Bad things will happen.");
        REPORT_CANNOT_FIND_LISTENER_THREAD = new ReportType("Cannot find listener thread in MinecraftServer.");
        REPORT_CANNOT_READ_LISTENER_THREAD = new ReportType("Unable to read the listener thread.");
        REPORT_CANNOT_FIND_SERVER_CONNECTION = new ReportType("Unable to retrieve server connection");
        REPORT_UNEXPECTED_THREAD_COUNT = new ReportType("Unexpected number of threads in %s: %s");
        REPORT_CANNOT_FIND_NET_HANDLER_THREAD = new ReportType("Unable to retrieve net handler thread.");
        REPORT_INSUFFICENT_THREAD_COUNT = new ReportType("Unable to inject %s lists in %s.");
        REPORT_CANNOT_COPY_OLD_TO_NEW = new ReportType("Cannot copy old %s to new.");
    }
    
    public enum ServerSocketType
    {
        SERVER_CONNECTION, 
        LISTENER_THREAD;
    }
}
