package com.comphenix.net.sf.cglib.core;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import com.comphenix.net.sf.cglib.asm.$Type;
import java.util.Map;

public class TypeUtils
{
    private static final Map transforms;
    private static final Map rtransforms;
    
    private TypeUtils() {
    }
    
    public static $Type getType(final String className) {
        return $Type.getType("L" + className.replace('.', '/') + ";");
    }
    
    public static boolean isFinal(final int access) {
        return (0x10 & access) != 0x0;
    }
    
    public static boolean isStatic(final int access) {
        return (0x8 & access) != 0x0;
    }
    
    public static boolean isProtected(final int access) {
        return (0x4 & access) != 0x0;
    }
    
    public static boolean isPublic(final int access) {
        return (0x1 & access) != 0x0;
    }
    
    public static boolean isAbstract(final int access) {
        return (0x400 & access) != 0x0;
    }
    
    public static boolean isInterface(final int access) {
        return (0x200 & access) != 0x0;
    }
    
    public static boolean isPrivate(final int access) {
        return (0x2 & access) != 0x0;
    }
    
    public static boolean isSynthetic(final int access) {
        return (0x1000 & access) != 0x0;
    }
    
    public static boolean isBridge(final int access) {
        return (0x40 & access) != 0x0;
    }
    
    public static String getPackageName(final $Type type) {
        return getPackageName(getClassName(type));
    }
    
    public static String getPackageName(final String className) {
        final int idx = className.lastIndexOf(46);
        return (idx < 0) ? "" : className.substring(0, idx);
    }
    
    public static String upperFirst(final String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    
    public static String getClassName(final $Type type) {
        if (isPrimitive(type)) {
            return TypeUtils.rtransforms.get(type.getDescriptor());
        }
        if (isArray(type)) {
            return getClassName(getComponentType(type)) + "[]";
        }
        return type.getClassName();
    }
    
    public static $Type[] add(final $Type[] types, final $Type extra) {
        if (types == null) {
            return new $Type[] { extra };
        }
        final List list = Arrays.asList(types);
        if (list.contains(extra)) {
            return types;
        }
        final $Type[] copy = new $Type[types.length + 1];
        System.arraycopy(types, 0, copy, 0, types.length);
        copy[types.length] = extra;
        return copy;
    }
    
    public static $Type[] add(final $Type[] t1, final $Type[] t2) {
        final $Type[] all = new $Type[t1.length + t2.length];
        System.arraycopy(t1, 0, all, 0, t1.length);
        System.arraycopy(t2, 0, all, t1.length, t2.length);
        return all;
    }
    
    public static $Type fromInternalName(final String name) {
        return $Type.getType("L" + name + ";");
    }
    
    public static $Type[] fromInternalNames(final String[] names) {
        if (names == null) {
            return null;
        }
        final $Type[] types = new $Type[names.length];
        for (int i = 0; i < names.length; ++i) {
            types[i] = fromInternalName(names[i]);
        }
        return types;
    }
    
    public static int getStackSize(final $Type[] types) {
        int size = 0;
        for (int i = 0; i < types.length; ++i) {
            size += types[i].getSize();
        }
        return size;
    }
    
    public static String[] toInternalNames(final $Type[] types) {
        if (types == null) {
            return null;
        }
        final String[] names = new String[types.length];
        for (int i = 0; i < types.length; ++i) {
            names[i] = types[i].getInternalName();
        }
        return names;
    }
    
    public static Signature parseSignature(final String s) {
        final int space = s.indexOf(32);
        final int lparen = s.indexOf(40, space);
        final int rparen = s.indexOf(41, lparen);
        final String returnType = s.substring(0, space);
        final String methodName = s.substring(space + 1, lparen);
        final StringBuffer sb = new StringBuffer();
        sb.append('(');
        final Iterator it = parseTypes(s, lparen + 1, rparen).iterator();
        while (it.hasNext()) {
            sb.append(it.next());
        }
        sb.append(')');
        sb.append(map(returnType));
        return new Signature(methodName, sb.toString());
    }
    
    public static $Type parseType(final String s) {
        return $Type.getType(map(s));
    }
    
    public static $Type[] parseTypes(final String s) {
        final List names = parseTypes(s, 0, s.length());
        final $Type[] types = new $Type[names.size()];
        for (int i = 0; i < types.length; ++i) {
            types[i] = $Type.getType(names.get(i));
        }
        return types;
    }
    
    public static Signature parseConstructor(final $Type[] types) {
        final StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (int i = 0; i < types.length; ++i) {
            sb.append(types[i].getDescriptor());
        }
        sb.append(")");
        sb.append("V");
        return new Signature("<init>", sb.toString());
    }
    
    public static Signature parseConstructor(final String sig) {
        return parseSignature("void <init>(" + sig + ")");
    }
    
    private static List parseTypes(final String s, int mark, final int end) {
        final List types = new ArrayList(5);
        while (true) {
            final int next = s.indexOf(44, mark);
            if (next < 0) {
                break;
            }
            types.add(map(s.substring(mark, next).trim()));
            mark = next + 1;
        }
        types.add(map(s.substring(mark, end).trim()));
        return types;
    }
    
    private static String map(String type) {
        if (type.equals("")) {
            return type;
        }
        final String t = TypeUtils.transforms.get(type);
        if (t != null) {
            return t;
        }
        if (type.indexOf(46) < 0) {
            return map("java.lang." + type);
        }
        final StringBuffer sb = new StringBuffer();
        int index = 0;
        while ((index = type.indexOf("[]", index) + 1) > 0) {
            sb.append('[');
        }
        type = type.substring(0, type.length() - sb.length() * 2);
        sb.append('L').append(type.replace('.', '/')).append(';');
        return sb.toString();
    }
    
    public static $Type getBoxedType(final $Type type) {
        switch (type.getSort()) {
            case 2: {
                return Constants.TYPE_CHARACTER;
            }
            case 1: {
                return Constants.TYPE_BOOLEAN;
            }
            case 8: {
                return Constants.TYPE_DOUBLE;
            }
            case 6: {
                return Constants.TYPE_FLOAT;
            }
            case 7: {
                return Constants.TYPE_LONG;
            }
            case 5: {
                return Constants.TYPE_INTEGER;
            }
            case 4: {
                return Constants.TYPE_SHORT;
            }
            case 3: {
                return Constants.TYPE_BYTE;
            }
            default: {
                return type;
            }
        }
    }
    
    public static $Type getUnboxedType(final $Type type) {
        if (Constants.TYPE_INTEGER.equals(type)) {
            return $Type.INT_TYPE;
        }
        if (Constants.TYPE_BOOLEAN.equals(type)) {
            return $Type.BOOLEAN_TYPE;
        }
        if (Constants.TYPE_DOUBLE.equals(type)) {
            return $Type.DOUBLE_TYPE;
        }
        if (Constants.TYPE_LONG.equals(type)) {
            return $Type.LONG_TYPE;
        }
        if (Constants.TYPE_CHARACTER.equals(type)) {
            return $Type.CHAR_TYPE;
        }
        if (Constants.TYPE_BYTE.equals(type)) {
            return $Type.BYTE_TYPE;
        }
        if (Constants.TYPE_FLOAT.equals(type)) {
            return $Type.FLOAT_TYPE;
        }
        if (Constants.TYPE_SHORT.equals(type)) {
            return $Type.SHORT_TYPE;
        }
        return type;
    }
    
    public static boolean isArray(final $Type type) {
        return type.getSort() == 9;
    }
    
    public static $Type getComponentType(final $Type type) {
        if (!isArray(type)) {
            throw new IllegalArgumentException("Type " + type + " is not an array");
        }
        return $Type.getType(type.getDescriptor().substring(1));
    }
    
    public static boolean isPrimitive(final $Type type) {
        switch (type.getSort()) {
            case 9:
            case 10: {
                return false;
            }
            default: {
                return true;
            }
        }
    }
    
    public static String emulateClassGetName(final $Type type) {
        if (isArray(type)) {
            return type.getDescriptor().replace('/', '.');
        }
        return getClassName(type);
    }
    
    public static boolean isConstructor(final MethodInfo method) {
        return method.getSignature().getName().equals("<init>");
    }
    
    public static $Type[] getTypes(final Class[] classes) {
        if (classes == null) {
            return null;
        }
        final $Type[] types = new $Type[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            types[i] = $Type.getType(classes[i]);
        }
        return types;
    }
    
    public static int ICONST(final int value) {
        switch (value) {
            case -1: {
                return 2;
            }
            case 0: {
                return 3;
            }
            case 1: {
                return 4;
            }
            case 2: {
                return 5;
            }
            case 3: {
                return 6;
            }
            case 4: {
                return 7;
            }
            case 5: {
                return 8;
            }
            default: {
                return -1;
            }
        }
    }
    
    public static int LCONST(final long value) {
        if (value == 0L) {
            return 9;
        }
        if (value == 1L) {
            return 10;
        }
        return -1;
    }
    
    public static int FCONST(final float value) {
        if (value == 0.0f) {
            return 11;
        }
        if (value == 1.0f) {
            return 12;
        }
        if (value == 2.0f) {
            return 13;
        }
        return -1;
    }
    
    public static int DCONST(final double value) {
        if (value == 0.0) {
            return 14;
        }
        if (value == 1.0) {
            return 15;
        }
        return -1;
    }
    
    public static int NEWARRAY(final $Type type) {
        switch (type.getSort()) {
            case 3: {
                return 8;
            }
            case 2: {
                return 5;
            }
            case 8: {
                return 7;
            }
            case 6: {
                return 6;
            }
            case 5: {
                return 10;
            }
            case 7: {
                return 11;
            }
            case 4: {
                return 9;
            }
            case 1: {
                return 4;
            }
            default: {
                return -1;
            }
        }
    }
    
    public static String escapeType(final String s) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0, len = s.length(); i < len; ++i) {
            final char c = s.charAt(i);
            switch (c) {
                case '$': {
                    sb.append("$24");
                    break;
                }
                case '.': {
                    sb.append("$2E");
                    break;
                }
                case '[': {
                    sb.append("$5B");
                    break;
                }
                case ';': {
                    sb.append("$3B");
                    break;
                }
                case '(': {
                    sb.append("$28");
                    break;
                }
                case ')': {
                    sb.append("$29");
                    break;
                }
                case '/': {
                    sb.append("$2F");
                    break;
                }
                default: {
                    sb.append(c);
                    break;
                }
            }
        }
        return sb.toString();
    }
    
    static {
        transforms = new HashMap();
        rtransforms = new HashMap();
        TypeUtils.transforms.put("void", "V");
        TypeUtils.transforms.put("byte", "B");
        TypeUtils.transforms.put("char", "C");
        TypeUtils.transforms.put("double", "D");
        TypeUtils.transforms.put("float", "F");
        TypeUtils.transforms.put("int", "I");
        TypeUtils.transforms.put("long", "J");
        TypeUtils.transforms.put("short", "S");
        TypeUtils.transforms.put("boolean", "Z");
        CollectionUtils.reverse(TypeUtils.transforms, TypeUtils.rtransforms);
    }
}
