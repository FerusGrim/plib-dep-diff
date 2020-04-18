package com.comphenix.protocol.reflect;

import com.google.common.primitives.Primitives;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

public class PrettyPrinter
{
    public static final int RECURSE_DEPTH = 3;
    
    public static String printObject(final Object object) throws IllegalAccessException {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be NULL.");
        }
        return printObject(object, object.getClass(), Object.class);
    }
    
    public static String printObject(final Object object, final Class<?> start, final Class<?> stop) throws IllegalAccessException {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be NULL.");
        }
        return printObject(object, start, stop, 3);
    }
    
    public static String printObject(final Object object, final Class<?> start, final Class<?> stop, final int hierachyDepth) throws IllegalAccessException {
        return printObject(object, start, stop, hierachyDepth, ObjectPrinter.DEFAULT);
    }
    
    public static String printObject(final Object object, final Class<?> start, final Class<?> stop, final int hierachyDepth, final ObjectPrinter printer) throws IllegalAccessException {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be NULL.");
        }
        final StringBuilder output = new StringBuilder();
        final Set<Object> previous = new HashSet<Object>();
        output.append("{ ");
        printObject(output, object, start, stop, previous, hierachyDepth, true, printer);
        output.append(" }");
        return output.toString();
    }
    
    private static void printIterables(final StringBuilder output, final Iterable iterable, final Class<?> stop, final Set<Object> previous, final int hierachyIndex, final ObjectPrinter printer) throws IllegalAccessException {
        boolean first = true;
        output.append("(");
        for (final Object value : iterable) {
            if (first) {
                first = false;
            }
            else {
                output.append(", ");
            }
            printValue(output, value, stop, previous, hierachyIndex - 1, printer);
        }
        output.append(")");
    }
    
    private static void printMap(final StringBuilder output, final Map<Object, Object> map, final Class<?> current, final Class<?> stop, final Set<Object> previous, final int hierachyIndex, final ObjectPrinter printer) throws IllegalAccessException {
        boolean first = true;
        output.append("[");
        for (final Map.Entry<Object, Object> entry : map.entrySet()) {
            if (first) {
                first = false;
            }
            else {
                output.append(", ");
            }
            printValue(output, entry.getKey(), stop, previous, hierachyIndex - 1, printer);
            output.append(": ");
            printValue(output, entry.getValue(), stop, previous, hierachyIndex - 1, printer);
        }
        output.append("]");
    }
    
    private static void printArray(final StringBuilder output, final Object array, final Class<?> current, final Class<?> stop, final Set<Object> previous, final int hierachyIndex, final ObjectPrinter printer) throws IllegalAccessException {
        final Class<?> component = current.getComponentType();
        boolean first = true;
        if (!component.isArray()) {
            output.append(component.getName());
        }
        output.append("[");
        for (int i = 0; i < Array.getLength(array); ++i) {
            if (first) {
                first = false;
            }
            else {
                output.append(", ");
            }
            try {
                printValue(output, Array.get(array, i), component, stop, previous, hierachyIndex - 1, printer);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                break;
            }
            catch (IllegalArgumentException e2) {
                e2.printStackTrace();
                break;
            }
        }
        output.append("]");
    }
    
    private static void printObject(final StringBuilder output, final Object object, final Class<?> current, final Class<?> stop, final Set<Object> previous, final int hierachyIndex, boolean first, final ObjectPrinter printer) throws IllegalAccessException {
        if (current == null || current == Object.class || (stop != null && current.equals(stop))) {
            return;
        }
        previous.add(object);
        if (hierachyIndex < 0) {
            output.append("...");
            return;
        }
        for (final Field field : current.getDeclaredFields()) {
            final int mod = field.getModifiers();
            if (!Modifier.isTransient(mod) && !Modifier.isStatic(mod)) {
                final Class<?> type = field.getType();
                final Object value = FieldUtils.readField(field, object, true);
                if (first) {
                    first = false;
                }
                else {
                    output.append(", ");
                }
                output.append(field.getName());
                output.append(" = ");
                printValue(output, value, type, stop, previous, hierachyIndex - 1, printer);
            }
        }
        printObject(output, object, current.getSuperclass(), stop, previous, hierachyIndex, first, printer);
    }
    
    private static void printValue(final StringBuilder output, final Object value, final Class<?> stop, final Set<Object> previous, final int hierachyIndex, final ObjectPrinter printer) throws IllegalAccessException {
        printValue(output, value, (value != null) ? value.getClass() : null, stop, previous, hierachyIndex, printer);
    }
    
    private static void printValue(final StringBuilder output, final Object value, final Class<?> type, final Class<?> stop, final Set<Object> previous, final int hierachyIndex, final ObjectPrinter printer) throws IllegalAccessException {
        if (printer.print(output, value)) {
            return;
        }
        if (value == null) {
            output.append("NULL");
        }
        else if (type.isPrimitive() || Primitives.isWrapperType((Class)type)) {
            output.append(value);
        }
        else if (type == String.class || hierachyIndex <= 0) {
            output.append("\"" + value + "\"");
        }
        else if (type.isArray()) {
            printArray(output, value, type, stop, previous, hierachyIndex, printer);
        }
        else if (Iterable.class.isAssignableFrom(type)) {
            printIterables(output, (Iterable)value, stop, previous, hierachyIndex, printer);
        }
        else if (Map.class.isAssignableFrom(type)) {
            printMap(output, (Map<Object, Object>)value, type, stop, previous, hierachyIndex, printer);
        }
        else if (ClassLoader.class.isAssignableFrom(type) || previous.contains(value)) {
            output.append("\"" + value + "\"");
        }
        else {
            output.append("{ ");
            printObject(output, value, value.getClass(), stop, previous, hierachyIndex, true, printer);
            output.append(" }");
        }
    }
    
    public interface ObjectPrinter
    {
        public static final ObjectPrinter DEFAULT = new ObjectPrinter() {
            @Override
            public boolean print(final StringBuilder output, final Object value) {
                return false;
            }
        };
        
        boolean print(final StringBuilder p0, final Object p1);
    }
}
