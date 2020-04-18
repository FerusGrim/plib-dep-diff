package com.comphenix.protocol.reflect.compiler;

import java.util.HashMap;
import com.comphenix.net.sf.cglib.asm.$Type;
import java.util.Map;

class MethodDescriptor
{
    private final String name;
    private final String desc;
    private static final Map<String, String> DESCRIPTORS;
    
    public MethodDescriptor(final String name, final String desc) {
        this.name = name;
        this.desc = desc;
    }
    
    public MethodDescriptor(final String name, final $Type returnType, final $Type[] argumentTypes) {
        this(name, $Type.getMethodDescriptor(returnType, argumentTypes));
    }
    
    public static MethodDescriptor getMethod(final String method) throws IllegalArgumentException {
        return getMethod(method, false);
    }
    
    public static MethodDescriptor getMethod(final String method, final boolean defaultPackage) throws IllegalArgumentException {
        final int space = method.indexOf(32);
        int start = method.indexOf(40, space) + 1;
        final int end = method.indexOf(41, start);
        if (space == -1 || start == -1 || end == -1) {
            throw new IllegalArgumentException();
        }
        final String returnType = method.substring(0, space);
        final String methodName = method.substring(space + 1, start - 1).trim();
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        int p;
        do {
            p = method.indexOf(44, start);
            String s;
            if (p == -1) {
                s = map(method.substring(start, end).trim(), defaultPackage);
            }
            else {
                s = map(method.substring(start, p).trim(), defaultPackage);
                start = p + 1;
            }
            sb.append(s);
        } while (p != -1);
        sb.append(')');
        sb.append(map(returnType, defaultPackage));
        return new MethodDescriptor(methodName, sb.toString());
    }
    
    private static String map(final String type, final boolean defaultPackage) {
        if ("".equals(type)) {
            return type;
        }
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        while ((index = type.indexOf("[]", index) + 1) > 0) {
            sb.append('[');
        }
        final String t = type.substring(0, type.length() - sb.length() * 2);
        final String desc = MethodDescriptor.DESCRIPTORS.get(t);
        if (desc != null) {
            sb.append(desc);
        }
        else {
            sb.append('L');
            if (t.indexOf(46) < 0) {
                if (!defaultPackage) {
                    sb.append("java/lang/");
                }
                sb.append(t);
            }
            else {
                sb.append(t.replace('.', '/'));
            }
            sb.append(';');
        }
        return sb.toString();
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDescriptor() {
        return this.desc;
    }
    
    public $Type getReturnType() {
        return $Type.getReturnType(this.desc);
    }
    
    public $Type[] getArgumentTypes() {
        return $Type.getArgumentTypes(this.desc);
    }
    
    @Override
    public String toString() {
        return this.name + this.desc;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof MethodDescriptor)) {
            return false;
        }
        final MethodDescriptor other = (MethodDescriptor)o;
        return this.name.equals(other.name) && this.desc.equals(other.desc);
    }
    
    @Override
    public int hashCode() {
        return this.name.hashCode() ^ this.desc.hashCode();
    }
    
    static {
        (DESCRIPTORS = new HashMap<String, String>()).put("void", "V");
        MethodDescriptor.DESCRIPTORS.put("byte", "B");
        MethodDescriptor.DESCRIPTORS.put("char", "C");
        MethodDescriptor.DESCRIPTORS.put("double", "D");
        MethodDescriptor.DESCRIPTORS.put("float", "F");
        MethodDescriptor.DESCRIPTORS.put("int", "I");
        MethodDescriptor.DESCRIPTORS.put("long", "J");
        MethodDescriptor.DESCRIPTORS.put("short", "S");
        MethodDescriptor.DESCRIPTORS.put("boolean", "Z");
    }
}
