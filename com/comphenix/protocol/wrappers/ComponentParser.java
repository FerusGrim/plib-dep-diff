package com.comphenix.protocol.wrappers;

import java.io.IOException;
import com.google.gson.Gson;
import java.io.Reader;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

public class ComponentParser
{
    private static Constructor readerConstructor;
    private static Method setLenient;
    private static Method getAdapter;
    private static Method read;
    
    private ComponentParser() {
    }
    
    public static Object deserialize(final Object gson, final Class<?> component, final StringReader str) {
        try {
            final JsonReader reader = new JsonReader((Reader)str);
            reader.setLenient(true);
            return ((Gson)gson).getAdapter((Class)component).read(reader);
        }
        catch (IOException ex) {
            throw new RuntimeException("Failed to read JSON", ex);
        }
        catch (LinkageError er) {
            return deserializeLegacy(gson, component, str);
        }
    }
    
    private static Object deserializeLegacy(final Object gson, final Class<?> component, final StringReader str) {
        try {
            if (ComponentParser.readerConstructor == null) {
                final Class<?> readerClass = Class.forName("org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonReader");
                (ComponentParser.readerConstructor = readerClass.getDeclaredConstructor(Reader.class)).setAccessible(true);
                (ComponentParser.setLenient = readerClass.getDeclaredMethod("setLenient", Boolean.TYPE)).setAccessible(true);
                (ComponentParser.getAdapter = gson.getClass().getDeclaredMethod("getAdapter", Class.class)).setAccessible(true);
                final Object adapter = ComponentParser.getAdapter.invoke(gson, component);
                (ComponentParser.read = adapter.getClass().getDeclaredMethod("read", readerClass)).setAccessible(true);
            }
            final Object reader = ComponentParser.readerConstructor.newInstance(str);
            ComponentParser.setLenient.invoke(reader, true);
            final Object adapter = ComponentParser.getAdapter.invoke(gson, component);
            return ComponentParser.read.invoke(adapter, reader);
        }
        catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to read JSON", ex);
        }
    }
}
