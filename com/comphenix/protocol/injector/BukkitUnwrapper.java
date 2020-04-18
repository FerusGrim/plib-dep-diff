package com.comphenix.protocol.injector;

import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.FieldUtils;
import org.bukkit.Bukkit;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.entity.Player;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.error.Report;
import java.lang.reflect.Method;
import java.util.Iterator;
import com.comphenix.protocol.reflect.instances.DefaultInstances;
import com.google.common.primitives.Primitives;
import java.util.Collection;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.error.ErrorReporter;
import java.util.Map;
import com.comphenix.protocol.error.ReportType;

public class BukkitUnwrapper implements PacketConstructor.Unwrapper
{
    private static BukkitUnwrapper DEFAULT;
    public static final ReportType REPORT_ILLEGAL_ARGUMENT;
    public static final ReportType REPORT_SECURITY_LIMITATION;
    public static final ReportType REPORT_CANNOT_FIND_UNWRAP_METHOD;
    public static final ReportType REPORT_CANNOT_READ_FIELD_HANDLE;
    private static Map<Class<?>, PacketConstructor.Unwrapper> unwrapperCache;
    private final ErrorReporter reporter;
    
    public static BukkitUnwrapper getInstance() {
        final ErrorReporter currentReporter = ProtocolLibrary.getErrorReporter();
        if (BukkitUnwrapper.DEFAULT == null || BukkitUnwrapper.DEFAULT.reporter != currentReporter) {
            BukkitUnwrapper.DEFAULT = new BukkitUnwrapper(currentReporter);
        }
        return BukkitUnwrapper.DEFAULT;
    }
    
    public BukkitUnwrapper() {
        this(ProtocolLibrary.getErrorReporter());
    }
    
    public BukkitUnwrapper(final ErrorReporter reporter) {
        this.reporter = reporter;
    }
    
    @Override
    public Object unwrapItem(final Object wrappedObject) {
        if (wrappedObject == null) {
            return null;
        }
        final Class<?> currentClass = PacketConstructor.getClass(wrappedObject);
        if (currentClass.isPrimitive() || currentClass.equals(String.class)) {
            return null;
        }
        if (wrappedObject instanceof Collection) {
            return this.handleCollection((Collection<Object>)wrappedObject);
        }
        if (Primitives.isWrapperType((Class)currentClass) || wrappedObject instanceof String) {
            return null;
        }
        final PacketConstructor.Unwrapper specificUnwrapper = this.getSpecificUnwrapper(currentClass);
        if (specificUnwrapper != null) {
            return specificUnwrapper.unwrapItem(wrappedObject);
        }
        return null;
    }
    
    private Object handleCollection(final Collection<Object> wrappedObject) {
        final Collection<Object> copy = DefaultInstances.DEFAULT.getDefault(wrappedObject.getClass());
        if (copy != null) {
            for (final Object element : wrappedObject) {
                copy.add(this.unwrapItem(element));
            }
            return copy;
        }
        return null;
    }
    
    private PacketConstructor.Unwrapper getSpecificUnwrapper(final Class<?> type) {
        if (BukkitUnwrapper.unwrapperCache.containsKey(type)) {
            return BukkitUnwrapper.unwrapperCache.get(type);
        }
        try {
            final Method find = type.getMethod("getHandle", (Class<?>[])new Class[0]);
            final PacketConstructor.Unwrapper methodUnwrapper = new PacketConstructor.Unwrapper() {
                @Override
                public Object unwrapItem(final Object wrappedObject) {
                    try {
                        if (wrappedObject instanceof Class) {
                            return checkClass((Class)wrappedObject, type, find.getReturnType());
                        }
                        return find.invoke(wrappedObject, new Object[0]);
                    }
                    catch (IllegalArgumentException e) {
                        BukkitUnwrapper.this.reporter.reportDetailed(this, Report.newBuilder(BukkitUnwrapper.REPORT_ILLEGAL_ARGUMENT).error(e).callerParam(wrappedObject, find));
                    }
                    catch (IllegalAccessException e3) {
                        return null;
                    }
                    catch (InvocationTargetException e2) {
                        throw new RuntimeException("Minecraft error.", e2);
                    }
                    return null;
                }
            };
            BukkitUnwrapper.unwrapperCache.put(type, methodUnwrapper);
            return methodUnwrapper;
        }
        catch (SecurityException e) {
            this.reporter.reportDetailed(this, Report.newBuilder(BukkitUnwrapper.REPORT_SECURITY_LIMITATION).error(e).callerParam(type));
        }
        catch (NoSuchMethodException e2) {
            final PacketConstructor.Unwrapper proxyUnwrapper = this.getProxyUnwrapper(type);
            if (proxyUnwrapper != null) {
                return proxyUnwrapper;
            }
            final PacketConstructor.Unwrapper fieldUnwrapper = this.getFieldUnwrapper(type);
            if (fieldUnwrapper != null) {
                return fieldUnwrapper;
            }
            this.reporter.reportDetailed(this, Report.newBuilder(BukkitUnwrapper.REPORT_CANNOT_FIND_UNWRAP_METHOD).error(e2).callerParam(type));
        }
        return null;
    }
    
    private PacketConstructor.Unwrapper getProxyUnwrapper(final Class<?> type) {
        try {
            if (Player.class.isAssignableFrom(type)) {
                final Method getHandle = MinecraftReflection.getCraftPlayerClass().getMethod("getHandle", (Class<?>[])new Class[0]);
                final PacketConstructor.Unwrapper unwrapper = new PacketConstructor.Unwrapper() {
                    @Override
                    public Object unwrapItem(final Object wrapped) {
                        try {
                            return getHandle.invoke(((Player)wrapped).getPlayer(), new Object[0]);
                        }
                        catch (Throwable ex) {
                            try {
                                return getHandle.invoke(Bukkit.getPlayer(((Player)wrapped).getUniqueId()), new Object[0]);
                            }
                            catch (ReflectiveOperationException ex2) {
                                throw new RuntimeException("Failed to unwrap proxy " + wrapped, ex);
                            }
                        }
                    }
                };
                BukkitUnwrapper.unwrapperCache.put(type, unwrapper);
                return unwrapper;
            }
        }
        catch (Throwable t) {}
        return null;
    }
    
    private PacketConstructor.Unwrapper getFieldUnwrapper(final Class<?> type) {
        final Field find = FieldUtils.getField(type, "handle", true);
        if (find != null) {
            final PacketConstructor.Unwrapper fieldUnwrapper = new PacketConstructor.Unwrapper() {
                @Override
                public Object unwrapItem(final Object wrappedObject) {
                    try {
                        if (wrappedObject instanceof Class) {
                            return checkClass((Class)wrappedObject, type, find.getType());
                        }
                        return FieldUtils.readField(find, wrappedObject, true);
                    }
                    catch (IllegalAccessException e) {
                        BukkitUnwrapper.this.reporter.reportDetailed(this, Report.newBuilder(BukkitUnwrapper.REPORT_CANNOT_READ_FIELD_HANDLE).error(e).callerParam(wrappedObject, find));
                        return null;
                    }
                }
            };
            BukkitUnwrapper.unwrapperCache.put(type, fieldUnwrapper);
            return fieldUnwrapper;
        }
        this.reporter.reportDetailed(this, Report.newBuilder(BukkitUnwrapper.REPORT_CANNOT_READ_FIELD_HANDLE).callerParam(find));
        return null;
    }
    
    private static Class<?> checkClass(final Class<?> input, final Class<?> expected, final Class<?> result) {
        if (expected.isAssignableFrom(input)) {
            return result;
        }
        return null;
    }
    
    static {
        REPORT_ILLEGAL_ARGUMENT = new ReportType("Illegal argument.");
        REPORT_SECURITY_LIMITATION = new ReportType("Security limitation.");
        REPORT_CANNOT_FIND_UNWRAP_METHOD = new ReportType("Cannot find method.");
        REPORT_CANNOT_READ_FIELD_HANDLE = new ReportType("Cannot read field 'handle'.");
        BukkitUnwrapper.unwrapperCache = new ConcurrentHashMap<Class<?>, PacketConstructor.Unwrapper>();
    }
}
