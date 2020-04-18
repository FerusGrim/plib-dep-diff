package com.comphenix.net.sf.cglib.core;

import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import com.comphenix.net.sf.cglib.asm.$Attribute;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Set;
import java.util.HashSet;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.lang.reflect.Constructor;
import com.comphenix.net.sf.cglib.asm.$Type;
import java.lang.reflect.Member;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.security.ProtectionDomain;
import java.lang.reflect.Method;
import java.util.Map;

public class ReflectUtils
{
    private static final Map primitives;
    private static final Map transforms;
    private static final ClassLoader defaultLoader;
    private static Method DEFINE_CLASS;
    private static Method DEFINE_CLASS_UNSAFE;
    private static final ProtectionDomain PROTECTION_DOMAIN;
    private static final Object UNSAFE;
    private static final Throwable THROWABLE;
    private static final List<Method> OBJECT_METHODS;
    private static final String[] CGLIB_PACKAGES;
    
    private ReflectUtils() {
    }
    
    public static ProtectionDomain getProtectionDomain(final Class source) {
        if (source == null) {
            return null;
        }
        return AccessController.doPrivileged((PrivilegedAction<ProtectionDomain>)new PrivilegedAction() {
            public Object run() {
                return source.getProtectionDomain();
            }
        });
    }
    
    public static $Type[] getExceptionTypes(final Member member) {
        if (member instanceof Method) {
            return TypeUtils.getTypes(((Method)member).getExceptionTypes());
        }
        if (member instanceof Constructor) {
            return TypeUtils.getTypes(((Constructor)member).getExceptionTypes());
        }
        throw new IllegalArgumentException("Cannot get exception types of a field");
    }
    
    public static Signature getSignature(final Member member) {
        if (member instanceof Method) {
            return new Signature(member.getName(), $Type.getMethodDescriptor((Method)member));
        }
        if (member instanceof Constructor) {
            final $Type[] types = TypeUtils.getTypes(((Constructor)member).getParameterTypes());
            return new Signature("<init>", $Type.getMethodDescriptor($Type.VOID_TYPE, types));
        }
        throw new IllegalArgumentException("Cannot get signature of a field");
    }
    
    public static Constructor findConstructor(final String desc) {
        return findConstructor(desc, ReflectUtils.defaultLoader);
    }
    
    public static Constructor findConstructor(final String desc, final ClassLoader loader) {
        try {
            final int lparen = desc.indexOf(40);
            final String className = desc.substring(0, lparen).trim();
            return getClass(className, loader).getConstructor((Class[])parseTypes(desc, loader));
        }
        catch (ClassNotFoundException e) {
            throw new CodeGenerationException(e);
        }
        catch (NoSuchMethodException e2) {
            throw new CodeGenerationException(e2);
        }
    }
    
    public static Method findMethod(final String desc) {
        return findMethod(desc, ReflectUtils.defaultLoader);
    }
    
    public static Method findMethod(final String desc, final ClassLoader loader) {
        try {
            final int lparen = desc.indexOf(40);
            final int dot = desc.lastIndexOf(46, lparen);
            final String className = desc.substring(0, dot).trim();
            final String methodName = desc.substring(dot + 1, lparen).trim();
            return getClass(className, loader).getDeclaredMethod(methodName, (Class[])parseTypes(desc, loader));
        }
        catch (ClassNotFoundException e) {
            throw new CodeGenerationException(e);
        }
        catch (NoSuchMethodException e2) {
            throw new CodeGenerationException(e2);
        }
    }
    
    private static Class[] parseTypes(final String desc, final ClassLoader loader) throws ClassNotFoundException {
        final int lparen = desc.indexOf(40);
        final int rparen = desc.indexOf(41, lparen);
        final List params = new ArrayList();
        int start = lparen + 1;
        while (true) {
            final int comma = desc.indexOf(44, start);
            if (comma < 0) {
                break;
            }
            params.add(desc.substring(start, comma).trim());
            start = comma + 1;
        }
        if (start < rparen) {
            params.add(desc.substring(start, rparen).trim());
        }
        final Class[] types = new Class[params.size()];
        for (int i = 0; i < types.length; ++i) {
            types[i] = getClass(params.get(i), loader);
        }
        return types;
    }
    
    private static Class getClass(final String className, final ClassLoader loader) throws ClassNotFoundException {
        return getClass(className, loader, ReflectUtils.CGLIB_PACKAGES);
    }
    
    private static Class getClass(String className, final ClassLoader loader, final String[] packages) throws ClassNotFoundException {
        final String save = className;
        int dimensions = 0;
        int index = 0;
        while ((index = className.indexOf("[]", index) + 1) > 0) {
            ++dimensions;
        }
        final StringBuffer brackets = new StringBuffer(className.length() - dimensions);
        for (int i = 0; i < dimensions; ++i) {
            brackets.append('[');
        }
        className = className.substring(0, className.length() - 2 * dimensions);
        final String prefix = (dimensions > 0) ? ((Object)brackets + "L") : "";
        final String suffix = (dimensions > 0) ? ";" : "";
        try {
            return Class.forName(prefix + className + suffix, false, loader);
        }
        catch (ClassNotFoundException ex) {
            int j = 0;
            while (j < packages.length) {
                try {
                    return Class.forName(prefix + packages[j] + '.' + className + suffix, false, loader);
                }
                catch (ClassNotFoundException ex2) {
                    ++j;
                    continue;
                }
                break;
            }
            if (dimensions == 0) {
                final Class c = ReflectUtils.primitives.get(className);
                if (c != null) {
                    return c;
                }
            }
            else {
                final String transform = ReflectUtils.transforms.get(className);
                if (transform != null) {
                    try {
                        return Class.forName((Object)brackets + transform, false, loader);
                    }
                    catch (ClassNotFoundException ex3) {}
                }
            }
            throw new ClassNotFoundException(save);
        }
    }
    
    public static Object newInstance(final Class type) {
        return newInstance(type, Constants.EMPTY_CLASS_ARRAY, null);
    }
    
    public static Object newInstance(final Class type, final Class[] parameterTypes, final Object[] args) {
        return newInstance(getConstructor(type, parameterTypes), args);
    }
    
    public static Object newInstance(final Constructor cstruct, final Object[] args) {
        final boolean flag = cstruct.isAccessible();
        try {
            if (!flag) {
                cstruct.setAccessible(true);
            }
            final Object result = cstruct.newInstance(args);
            return result;
        }
        catch (InstantiationException e) {
            throw new CodeGenerationException(e);
        }
        catch (IllegalAccessException e2) {
            throw new CodeGenerationException(e2);
        }
        catch (InvocationTargetException e3) {
            throw new CodeGenerationException(e3.getTargetException());
        }
        finally {
            if (!flag) {
                cstruct.setAccessible(flag);
            }
        }
    }
    
    public static Constructor getConstructor(final Class type, final Class[] parameterTypes) {
        try {
            final Constructor constructor = type.getDeclaredConstructor((Class[])parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        }
        catch (NoSuchMethodException e) {
            throw new CodeGenerationException(e);
        }
    }
    
    public static String[] getNames(final Class[] classes) {
        if (classes == null) {
            return null;
        }
        final String[] names = new String[classes.length];
        for (int i = 0; i < names.length; ++i) {
            names[i] = classes[i].getName();
        }
        return names;
    }
    
    public static Class[] getClasses(final Object[] objects) {
        final Class[] classes = new Class[objects.length];
        for (int i = 0; i < objects.length; ++i) {
            classes[i] = objects[i].getClass();
        }
        return classes;
    }
    
    public static Method findNewInstance(final Class iface) {
        final Method m = findInterfaceMethod(iface);
        if (!m.getName().equals("newInstance")) {
            throw new IllegalArgumentException(iface + " missing newInstance method");
        }
        return m;
    }
    
    public static Method[] getPropertyMethods(final PropertyDescriptor[] properties, final boolean read, final boolean write) {
        final Set methods = new HashSet();
        for (int i = 0; i < properties.length; ++i) {
            final PropertyDescriptor pd = properties[i];
            if (read) {
                methods.add(pd.getReadMethod());
            }
            if (write) {
                methods.add(pd.getWriteMethod());
            }
        }
        methods.remove(null);
        return methods.toArray(new Method[methods.size()]);
    }
    
    public static PropertyDescriptor[] getBeanProperties(final Class type) {
        return getPropertiesHelper(type, true, true);
    }
    
    public static PropertyDescriptor[] getBeanGetters(final Class type) {
        return getPropertiesHelper(type, true, false);
    }
    
    public static PropertyDescriptor[] getBeanSetters(final Class type) {
        return getPropertiesHelper(type, false, true);
    }
    
    private static PropertyDescriptor[] getPropertiesHelper(final Class type, final boolean read, final boolean write) {
        try {
            final BeanInfo info = Introspector.getBeanInfo(type, Object.class);
            final PropertyDescriptor[] all = info.getPropertyDescriptors();
            if (read && write) {
                return all;
            }
            final List properties = new ArrayList(all.length);
            for (int i = 0; i < all.length; ++i) {
                final PropertyDescriptor pd = all[i];
                if ((read && pd.getReadMethod() != null) || (write && pd.getWriteMethod() != null)) {
                    properties.add(pd);
                }
            }
            return properties.toArray(new PropertyDescriptor[properties.size()]);
        }
        catch (IntrospectionException e) {
            throw new CodeGenerationException(e);
        }
    }
    
    public static Method findDeclaredMethod(final Class type, final String methodName, final Class[] parameterTypes) throws NoSuchMethodException {
        Class cl = type;
        while (cl != null) {
            try {
                return cl.getDeclaredMethod(methodName, (Class[])parameterTypes);
            }
            catch (NoSuchMethodException e) {
                cl = cl.getSuperclass();
                continue;
            }
            break;
        }
        throw new NoSuchMethodException(methodName);
    }
    
    public static List addAllMethods(final Class type, final List list) {
        if (type == Object.class) {
            list.addAll(ReflectUtils.OBJECT_METHODS);
        }
        else {
            list.addAll(Arrays.asList(type.getDeclaredMethods()));
        }
        final Class superclass = type.getSuperclass();
        if (superclass != null) {
            addAllMethods(superclass, list);
        }
        final Class[] interfaces = type.getInterfaces();
        for (int i = 0; i < interfaces.length; ++i) {
            addAllMethods(interfaces[i], list);
        }
        return list;
    }
    
    public static List addAllInterfaces(final Class type, final List list) {
        final Class superclass = type.getSuperclass();
        if (superclass != null) {
            list.addAll(Arrays.asList(type.getInterfaces()));
            addAllInterfaces(superclass, list);
        }
        return list;
    }
    
    public static Method findInterfaceMethod(final Class iface) {
        if (!iface.isInterface()) {
            throw new IllegalArgumentException(iface + " is not an interface");
        }
        final Method[] methods = iface.getDeclaredMethods();
        if (methods.length != 1) {
            throw new IllegalArgumentException("expecting exactly 1 method in " + iface);
        }
        return methods[0];
    }
    
    public static Class defineClass(final String className, final byte[] b, final ClassLoader loader) throws Exception {
        return defineClass(className, b, loader, ReflectUtils.PROTECTION_DOMAIN);
    }
    
    public static Class defineClass(final String className, final byte[] b, final ClassLoader loader, final ProtectionDomain protectionDomain) throws Exception {
        Class c;
        if (ReflectUtils.DEFINE_CLASS != null) {
            final Object[] args = { className, b, new Integer(0), new Integer(b.length), protectionDomain };
            c = (Class)ReflectUtils.DEFINE_CLASS.invoke(loader, args);
        }
        else {
            if (ReflectUtils.DEFINE_CLASS_UNSAFE == null) {
                throw new CodeGenerationException(ReflectUtils.THROWABLE);
            }
            final Object[] args = { className, b, new Integer(0), new Integer(b.length), loader, protectionDomain };
            c = (Class)ReflectUtils.DEFINE_CLASS_UNSAFE.invoke(ReflectUtils.UNSAFE, args);
        }
        Class.forName(className, true, loader);
        return c;
    }
    
    public static int findPackageProtected(final Class[] classes) {
        for (int i = 0; i < classes.length; ++i) {
            if (!Modifier.isPublic(classes[i].getModifiers())) {
                return i;
            }
        }
        return 0;
    }
    
    public static MethodInfo getMethodInfo(final Member member, final int modifiers) {
        final Signature sig = getSignature(member);
        return new MethodInfo() {
            private ClassInfo ci;
            
            @Override
            public ClassInfo getClassInfo() {
                if (this.ci == null) {
                    this.ci = ReflectUtils.getClassInfo(member.getDeclaringClass());
                }
                return this.ci;
            }
            
            @Override
            public int getModifiers() {
                return modifiers;
            }
            
            @Override
            public Signature getSignature() {
                return sig;
            }
            
            @Override
            public $Type[] getExceptionTypes() {
                return ReflectUtils.getExceptionTypes(member);
            }
            
            public $Attribute getAttribute() {
                return null;
            }
        };
    }
    
    public static MethodInfo getMethodInfo(final Member member) {
        return getMethodInfo(member, member.getModifiers());
    }
    
    public static ClassInfo getClassInfo(final Class clazz) {
        final $Type type = $Type.getType(clazz);
        final $Type sc = (clazz.getSuperclass() == null) ? null : $Type.getType(clazz.getSuperclass());
        return new ClassInfo() {
            @Override
            public $Type getType() {
                return type;
            }
            
            @Override
            public $Type getSuperType() {
                return sc;
            }
            
            @Override
            public $Type[] getInterfaces() {
                return TypeUtils.getTypes(clazz.getInterfaces());
            }
            
            @Override
            public int getModifiers() {
                return clazz.getModifiers();
            }
        };
    }
    
    public static Method[] findMethods(final String[] namesAndDescriptors, final Method[] methods) {
        final Map map = new HashMap();
        for (int i = 0; i < methods.length; ++i) {
            final Method method = methods[i];
            map.put(method.getName() + $Type.getMethodDescriptor(method), method);
        }
        final Method[] result = new Method[namesAndDescriptors.length / 2];
        for (int j = 0; j < result.length; ++j) {
            result[j] = map.get(namesAndDescriptors[j * 2] + namesAndDescriptors[j * 2 + 1]);
            if (result[j] == null) {}
        }
        return result;
    }
    
    static {
        primitives = new HashMap(8);
        transforms = new HashMap(8);
        defaultLoader = ReflectUtils.class.getClassLoader();
        OBJECT_METHODS = new ArrayList<Method>();
        Throwable throwable = null;
        ProtectionDomain protectionDomain;
        Method defineClass;
        Method defineClassUnsafe;
        Object unsafe;
        try {
            protectionDomain = getProtectionDomain(ReflectUtils.class);
            try {
                defineClass = AccessController.doPrivileged((PrivilegedExceptionAction<Method>)new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        final Class loader = Class.forName("java.lang.ClassLoader");
                        final Method defineClass = loader.getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE, ProtectionDomain.class);
                        defineClass.setAccessible(true);
                        return defineClass;
                    }
                });
                defineClassUnsafe = null;
                unsafe = null;
            }
            catch (Throwable t) {
                throwable = t;
                defineClass = null;
                unsafe = AccessController.doPrivileged((PrivilegedExceptionAction<Object>)new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        final Class u = Class.forName("sun.misc.Unsafe");
                        final Field theUnsafe = u.getDeclaredField("theUnsafe");
                        theUnsafe.setAccessible(true);
                        return theUnsafe.get(null);
                    }
                });
                final Class u = Class.forName("sun.misc.Unsafe");
                defineClassUnsafe = u.getMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE, ClassLoader.class, ProtectionDomain.class);
            }
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>)new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    final Method[] declaredMethods;
                    final Method[] methods = declaredMethods = Object.class.getDeclaredMethods();
                    for (final Method method : declaredMethods) {
                        if (!"finalize".equals(method.getName())) {
                            if ((method.getModifiers() & 0x18) <= 0) {
                                ReflectUtils.OBJECT_METHODS.add(method);
                            }
                        }
                    }
                    return null;
                }
            });
        }
        catch (Throwable t) {
            if (throwable == null) {
                throwable = t;
            }
            protectionDomain = null;
            defineClass = null;
            defineClassUnsafe = null;
            unsafe = null;
        }
        PROTECTION_DOMAIN = protectionDomain;
        ReflectUtils.DEFINE_CLASS = defineClass;
        ReflectUtils.DEFINE_CLASS_UNSAFE = defineClassUnsafe;
        UNSAFE = unsafe;
        THROWABLE = throwable;
        CGLIB_PACKAGES = new String[] { "java.lang" };
        ReflectUtils.primitives.put("byte", Byte.TYPE);
        ReflectUtils.primitives.put("char", Character.TYPE);
        ReflectUtils.primitives.put("double", Double.TYPE);
        ReflectUtils.primitives.put("float", Float.TYPE);
        ReflectUtils.primitives.put("int", Integer.TYPE);
        ReflectUtils.primitives.put("long", Long.TYPE);
        ReflectUtils.primitives.put("short", Short.TYPE);
        ReflectUtils.primitives.put("boolean", Boolean.TYPE);
        ReflectUtils.transforms.put("byte", "B");
        ReflectUtils.transforms.put("char", "C");
        ReflectUtils.transforms.put("double", "D");
        ReflectUtils.transforms.put("float", "F");
        ReflectUtils.transforms.put("int", "I");
        ReflectUtils.transforms.put("long", "J");
        ReflectUtils.transforms.put("short", "S");
        ReflectUtils.transforms.put("boolean", "Z");
    }
}
