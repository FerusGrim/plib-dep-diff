package com.comphenix.protocol;

import java.util.List;
import java.util.ArrayList;
import com.comphenix.protocol.reflect.FieldUtils;
import java.lang.reflect.Field;
import com.comphenix.protocol.error.Report;
import java.lang.reflect.Modifier;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.injector.spigot.SpigotPacketInjector;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import com.comphenix.protocol.injector.server.AbstractInputStreamLookup;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.ChunkPosition;
import com.comphenix.protocol.reflect.ObjectWriter;
import com.comphenix.protocol.reflect.compiler.StructureCompiler;
import com.comphenix.protocol.reflect.compiler.BackgroundCompiler;
import com.comphenix.protocol.reflect.MethodUtils;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.instances.PrimitiveGenerator;
import com.comphenix.protocol.reflect.instances.CollectionGenerator;
import com.comphenix.protocol.reflect.instances.DefaultInstances;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.error.ErrorReporter;
import com.comphenix.protocol.error.ReportType;

class CleanupStaticMembers
{
    public static final ReportType REPORT_CANNOT_RESET_FIELD;
    public static final ReportType REPORT_CANNOT_UNLOAD_CLASS;
    private ClassLoader loader;
    private ErrorReporter reporter;
    
    public CleanupStaticMembers(final ClassLoader loader, final ErrorReporter reporter) {
        this.loader = loader;
        this.reporter = reporter;
    }
    
    public void resetAll() {
        final Class<?>[] publicClasses = (Class<?>[])new Class[] { AsyncListenerHandler.class, ListeningWhitelist.class, PacketContainer.class, BukkitUnwrapper.class, DefaultInstances.class, CollectionGenerator.class, PrimitiveGenerator.class, FuzzyReflection.class, MethodUtils.class, BackgroundCompiler.class, StructureCompiler.class, ObjectWriter.class, Packets.Server.class, Packets.Client.class, ChunkPosition.class, WrappedDataWatcher.class, WrappedWatchableObject.class, AbstractInputStreamLookup.class, TemporaryPlayerFactory.class, SpigotPacketInjector.class, MinecraftReflection.class, NbtBinarySerializer.class };
        final String[] internalClasses = { "com.comphenix.protocol.events.SerializedOfflinePlayer", "com.comphenix.protocol.injector.player.InjectedServerConnection", "com.comphenix.protocol.injector.player.NetworkFieldInjector", "com.comphenix.protocol.injector.player.NetworkObjectInjector", "com.comphenix.protocol.injector.player.NetworkServerInjector", "com.comphenix.protocol.injector.player.PlayerInjector", "com.comphenix.protocol.injector.EntityUtilities", "com.comphenix.protocol.injector.packet.PacketRegistry", "com.comphenix.protocol.injector.packet.PacketInjector", "com.comphenix.protocol.injector.packet.ReadPacketModifier", "com.comphenix.protocol.injector.StructureCache", "com.comphenix.protocol.reflect.compiler.BoxingHelper", "com.comphenix.protocol.reflect.compiler.MethodDescriptor", "com.comphenix.protocol.wrappers.nbt.WrappedElement" };
        this.resetClasses(publicClasses);
        this.resetClasses(this.getClasses(this.loader, internalClasses));
    }
    
    private void resetClasses(final Class<?>[] classes) {
        for (final Class<?> clazz : classes) {
            this.resetClass(clazz);
        }
    }
    
    private void resetClass(final Class<?> clazz) {
        for (final Field field : clazz.getFields()) {
            final Class<?> type = field.getType();
            if (Modifier.isStatic(field.getModifiers()) && !type.isPrimitive() && !type.equals(String.class) && !type.equals(ReportType.class)) {
                try {
                    setFinalStatic(field, null);
                }
                catch (IllegalAccessException e) {
                    this.reporter.reportWarning(this, Report.newBuilder(CleanupStaticMembers.REPORT_CANNOT_RESET_FIELD).error(e).messageParam(field.getName(), e.getMessage()));
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static void setFinalStatic(final Field field, final Object newValue) throws IllegalAccessException {
        final int modifier = field.getModifiers();
        final boolean isFinal = Modifier.isFinal(modifier);
        final Field modifiersField = isFinal ? FieldUtils.getField(Field.class, "modifiers", true) : null;
        if (isFinal) {
            FieldUtils.writeField(modifiersField, field, modifier & 0xFFFFFFEF, true);
        }
        FieldUtils.writeStaticField(field, newValue, true);
        if (isFinal) {
            FieldUtils.writeField(modifiersField, field, modifier, true);
        }
    }
    
    private Class<?>[] getClasses(final ClassLoader loader, final String[] names) {
        final List<Class<?>> output = new ArrayList<Class<?>>();
        for (final String name : names) {
            try {
                output.add(loader.loadClass(name));
            }
            catch (ClassNotFoundException e) {
                this.reporter.reportWarning(this, Report.newBuilder(CleanupStaticMembers.REPORT_CANNOT_UNLOAD_CLASS).error(e).messageParam(name));
            }
        }
        return output.toArray(new Class[0]);
    }
    
    static {
        REPORT_CANNOT_RESET_FIELD = new ReportType("Unable to reset field %s: %s");
        REPORT_CANNOT_UNLOAD_CLASS = new ReportType("Unable to unload class %s.");
    }
}
