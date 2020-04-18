package com.comphenix.protocol.reflect;

import java.util.Arrays;
import java.util.Collections;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.lang.ref.Reference;
import org.bukkit.Bukkit;
import java.util.logging.Logger;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class MethodUtils
{
    private static boolean loggedAccessibleWarning;
    private static boolean CACHE_METHODS;
    private static final Class[] EMPTY_CLASS_PARAMETERS;
    private static final Object[] EMPTY_OBJECT_ARRAY;
    private static final Map cache;
    
    public static synchronized void setCacheMethods(final boolean cacheMethods) {
        if (!(MethodUtils.CACHE_METHODS = cacheMethods)) {
            clearCache();
        }
    }
    
    public static synchronized int clearCache() {
        final int size = MethodUtils.cache.size();
        MethodUtils.cache.clear();
        return size;
    }
    
    public static Object invokeMethod(final Object object, final String methodName, final Object arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Object[] args = { arg };
        return invokeMethod(object, methodName, args);
    }
    
    public static Object invokeMethod(final Object object, final String methodName, Object[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = MethodUtils.EMPTY_OBJECT_ARRAY;
        }
        final int arguments = args.length;
        final Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return invokeMethod(object, methodName, args, parameterTypes);
    }
    
    public static Object invokeMethod(final Object object, final String methodName, Object[] args, Class[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (parameterTypes == null) {
            parameterTypes = MethodUtils.EMPTY_CLASS_PARAMETERS;
        }
        if (args == null) {
            args = MethodUtils.EMPTY_OBJECT_ARRAY;
        }
        final Method method = getMatchingAccessibleMethod(object.getClass(), methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on object: " + object.getClass().getName());
        }
        return method.invoke(object, args);
    }
    
    public static Object invokeExactMethod(final Object object, final String methodName, final Object arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Object[] args = { arg };
        return invokeExactMethod(object, methodName, args);
    }
    
    public static Object invokeExactMethod(final Object object, final String methodName, Object[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = MethodUtils.EMPTY_OBJECT_ARRAY;
        }
        final int arguments = args.length;
        final Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return invokeExactMethod(object, methodName, args, parameterTypes);
    }
    
    public static Object invokeExactMethod(final Object object, final String methodName, Object[] args, Class[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = MethodUtils.EMPTY_OBJECT_ARRAY;
        }
        if (parameterTypes == null) {
            parameterTypes = MethodUtils.EMPTY_CLASS_PARAMETERS;
        }
        final Method method = getAccessibleMethod(object.getClass(), methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on object: " + object.getClass().getName());
        }
        return method.invoke(object, args);
    }
    
    public static Object invokeExactStaticMethod(final Class objectClass, final String methodName, Object[] args, Class[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = MethodUtils.EMPTY_OBJECT_ARRAY;
        }
        if (parameterTypes == null) {
            parameterTypes = MethodUtils.EMPTY_CLASS_PARAMETERS;
        }
        final Method method = getAccessibleMethod(objectClass, methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on class: " + objectClass.getName());
        }
        return method.invoke(null, args);
    }
    
    public static Object invokeStaticMethod(final Class objectClass, final String methodName, final Object arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Object[] args = { arg };
        return invokeStaticMethod(objectClass, methodName, args);
    }
    
    public static Object invokeStaticMethod(final Class objectClass, final String methodName, Object[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = MethodUtils.EMPTY_OBJECT_ARRAY;
        }
        final int arguments = args.length;
        final Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return invokeStaticMethod(objectClass, methodName, args, parameterTypes);
    }
    
    public static Object invokeStaticMethod(final Class objectClass, final String methodName, Object[] args, Class[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (parameterTypes == null) {
            parameterTypes = MethodUtils.EMPTY_CLASS_PARAMETERS;
        }
        if (args == null) {
            args = MethodUtils.EMPTY_OBJECT_ARRAY;
        }
        final Method method = getMatchingAccessibleMethod(objectClass, methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on class: " + objectClass.getName());
        }
        return method.invoke(null, args);
    }
    
    public static Object invokeExactStaticMethod(final Class objectClass, final String methodName, final Object arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Object[] args = { arg };
        return invokeExactStaticMethod(objectClass, methodName, args);
    }
    
    public static Object invokeExactStaticMethod(final Class objectClass, final String methodName, Object[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (args == null) {
            args = MethodUtils.EMPTY_OBJECT_ARRAY;
        }
        final int arguments = args.length;
        final Class[] parameterTypes = new Class[arguments];
        for (int i = 0; i < arguments; ++i) {
            parameterTypes[i] = args[i].getClass();
        }
        return invokeExactStaticMethod(objectClass, methodName, args, parameterTypes);
    }
    
    public static Method getAccessibleMethod(final Class clazz, final String methodName, final Class[] parameterTypes) {
        try {
            final MethodDescriptor md = new MethodDescriptor(clazz, methodName, parameterTypes, true);
            Method method = getCachedMethod(md);
            if (method != null) {
                return method;
            }
            method = getAccessibleMethod(clazz, clazz.getMethod(methodName, (Class[])parameterTypes));
            cacheMethod(md, method);
            return method;
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }
    
    public static Method getAccessibleMethod(final Method method) {
        if (method == null) {
            return null;
        }
        return getAccessibleMethod(method.getDeclaringClass(), method);
    }
    
    public static Method getAccessibleMethod(Class clazz, Method method) {
        if (method == null) {
            return null;
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            return null;
        }
        boolean sameClass = true;
        if (clazz == null) {
            clazz = method.getDeclaringClass();
        }
        else {
            sameClass = clazz.equals(method.getDeclaringClass());
            if (!method.getDeclaringClass().isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(clazz.getName() + " is not assignable from " + method.getDeclaringClass().getName());
            }
        }
        if (Modifier.isPublic(clazz.getModifiers())) {
            if (!sameClass && !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                setMethodAccessible(method);
            }
            return method;
        }
        final String methodName = method.getName();
        final Class[] parameterTypes = method.getParameterTypes();
        method = getAccessibleMethodFromInterfaceNest(clazz, methodName, parameterTypes);
        if (method == null) {
            method = getAccessibleMethodFromSuperclass(clazz, methodName, parameterTypes);
        }
        return method;
    }
    
    private static Method getAccessibleMethodFromSuperclass(final Class clazz, final String methodName, final Class[] parameterTypes) {
        for (Class parentClazz = clazz.getSuperclass(); parentClazz != null; parentClazz = parentClazz.getSuperclass()) {
            if (Modifier.isPublic(parentClazz.getModifiers())) {
                try {
                    return parentClazz.getMethod(methodName, (Class[])parameterTypes);
                }
                catch (NoSuchMethodException e) {
                    return null;
                }
            }
        }
        return null;
    }
    
    private static Method getAccessibleMethodFromInterfaceNest(Class clazz, final String methodName, final Class[] parameterTypes) {
        Method method = null;
        while (clazz != null) {
            final Class[] interfaces = clazz.getInterfaces();
            for (int i = 0; i < interfaces.length; ++i) {
                if (Modifier.isPublic(interfaces[i].getModifiers())) {
                    try {
                        method = interfaces[i].getDeclaredMethod(methodName, (Class[])parameterTypes);
                    }
                    catch (NoSuchMethodException ex) {}
                    if (method != null) {
                        return method;
                    }
                    method = getAccessibleMethodFromInterfaceNest(interfaces[i], methodName, parameterTypes);
                    if (method != null) {
                        return method;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
    
    public static Method getMatchingAccessibleMethod(final Class clazz, final String methodName, final Class[] parameterTypes) {
        final MethodDescriptor md = new MethodDescriptor(clazz, methodName, parameterTypes, false);
        final Logger log = tryGetLogger();
        try {
            Method method = getCachedMethod(md);
            if (method != null) {
                return method;
            }
            method = clazz.getMethod(methodName, (Class[])parameterTypes);
            setMethodAccessible(method);
            cacheMethod(md, method);
            return method;
        }
        catch (NoSuchMethodException ex) {
            final int paramSize = parameterTypes.length;
            Method bestMatch = null;
            final Method[] methods = clazz.getMethods();
            float bestMatchCost = Float.MAX_VALUE;
            float myCost = Float.MAX_VALUE;
            for (int i = 0, size = methods.length; i < size; ++i) {
                if (methods[i].getName().equals(methodName)) {
                    final Class[] methodsParams = methods[i].getParameterTypes();
                    final int methodParamSize = methodsParams.length;
                    if (methodParamSize == paramSize) {
                        boolean match = true;
                        for (int n = 0; n < methodParamSize; ++n) {
                            if (!isAssignmentCompatible(methodsParams[n], parameterTypes[n])) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            final Method method2 = getAccessibleMethod(clazz, methods[i]);
                            if (method2 != null) {
                                setMethodAccessible(method2);
                                myCost = getTotalTransformationCost(parameterTypes, method2.getParameterTypes());
                                if (myCost < bestMatchCost) {
                                    bestMatch = method2;
                                    bestMatchCost = myCost;
                                }
                            }
                            if (log != null) {}
                        }
                    }
                }
            }
            if (bestMatch != null) {
                cacheMethod(md, bestMatch);
            }
            else if (log != null) {
                log.severe("No match found.");
            }
            return bestMatch;
        }
    }
    
    private static Logger tryGetLogger() {
        try {
            return Bukkit.getLogger();
        }
        catch (Exception e) {
            return null;
        }
    }
    
    private static void setMethodAccessible(final Method method) {
        try {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
        }
        catch (SecurityException se) {
            if (!MethodUtils.loggedAccessibleWarning) {
                boolean vulnerableJVM = false;
                try {
                    final String specVersion = System.getProperty("java.specification.version");
                    if (specVersion.charAt(0) == '1' && (specVersion.charAt(2) == '0' || specVersion.charAt(2) == '1' || specVersion.charAt(2) == '2' || specVersion.charAt(2) == '3')) {
                        vulnerableJVM = true;
                    }
                }
                catch (SecurityException e) {
                    vulnerableJVM = true;
                }
                if (vulnerableJVM && tryGetLogger() != null) {
                    tryGetLogger().info("Vulnerable JVM!");
                }
                MethodUtils.loggedAccessibleWarning = true;
            }
        }
    }
    
    private static float getTotalTransformationCost(final Class[] srcArgs, final Class[] destArgs) {
        float totalCost = 0.0f;
        for (int i = 0; i < srcArgs.length; ++i) {
            final Class srcClass = srcArgs[i];
            final Class destClass = destArgs[i];
            totalCost += getObjectTransformationCost(srcClass, destClass);
        }
        return totalCost;
    }
    
    private static float getObjectTransformationCost(final Class srcClass, Class destClass) {
        float cost = 0.0f;
        while (destClass != null && !destClass.equals(srcClass)) {
            if (destClass.isInterface() && isAssignmentCompatible(destClass, srcClass)) {
                cost += 0.25f;
                break;
            }
            ++cost;
            destClass = destClass.getSuperclass();
        }
        if (destClass == null) {
            cost += 1.5f;
        }
        return cost;
    }
    
    public static final boolean isAssignmentCompatible(final Class parameterType, final Class parameterization) {
        if (parameterType.isAssignableFrom(parameterization)) {
            return true;
        }
        if (parameterType.isPrimitive()) {
            final Class parameterWrapperClazz = getPrimitiveWrapper(parameterType);
            if (parameterWrapperClazz != null) {
                return parameterWrapperClazz.equals(parameterization);
            }
        }
        return false;
    }
    
    public static Class getPrimitiveWrapper(final Class primitiveType) {
        if (Boolean.TYPE.equals(primitiveType)) {
            return Boolean.class;
        }
        if (Float.TYPE.equals(primitiveType)) {
            return Float.class;
        }
        if (Long.TYPE.equals(primitiveType)) {
            return Long.class;
        }
        if (Integer.TYPE.equals(primitiveType)) {
            return Integer.class;
        }
        if (Short.TYPE.equals(primitiveType)) {
            return Short.class;
        }
        if (Byte.TYPE.equals(primitiveType)) {
            return Byte.class;
        }
        if (Double.TYPE.equals(primitiveType)) {
            return Double.class;
        }
        if (Character.TYPE.equals(primitiveType)) {
            return Character.class;
        }
        return null;
    }
    
    public static Class getPrimitiveType(final Class wrapperType) {
        if (Boolean.class.equals(wrapperType)) {
            return Boolean.TYPE;
        }
        if (Float.class.equals(wrapperType)) {
            return Float.TYPE;
        }
        if (Long.class.equals(wrapperType)) {
            return Long.TYPE;
        }
        if (Integer.class.equals(wrapperType)) {
            return Integer.TYPE;
        }
        if (Short.class.equals(wrapperType)) {
            return Short.TYPE;
        }
        if (Byte.class.equals(wrapperType)) {
            return Byte.TYPE;
        }
        if (Double.class.equals(wrapperType)) {
            return Double.TYPE;
        }
        if (Character.class.equals(wrapperType)) {
            return Character.TYPE;
        }
        return null;
    }
    
    public static Class toNonPrimitiveClass(final Class clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        }
        final Class primitiveClazz = getPrimitiveWrapper(clazz);
        if (primitiveClazz != null) {
            return primitiveClazz;
        }
        return clazz;
    }
    
    private static Method getCachedMethod(final MethodDescriptor md) {
        if (MethodUtils.CACHE_METHODS) {
            final Reference methodRef = MethodUtils.cache.get(md);
            if (methodRef != null) {
                return methodRef.get();
            }
        }
        return null;
    }
    
    private static void cacheMethod(final MethodDescriptor md, final Method method) {
        if (MethodUtils.CACHE_METHODS && method != null) {
            MethodUtils.cache.put(md, new WeakReference<Method>(method));
        }
    }
    
    static {
        MethodUtils.loggedAccessibleWarning = false;
        MethodUtils.CACHE_METHODS = true;
        EMPTY_CLASS_PARAMETERS = new Class[0];
        EMPTY_OBJECT_ARRAY = new Object[0];
        cache = Collections.synchronizedMap(new WeakHashMap<Object, Object>());
    }
    
    private static class MethodDescriptor
    {
        private Class cls;
        private String methodName;
        private Class[] paramTypes;
        private boolean exact;
        private int hashCode;
        
        public MethodDescriptor(final Class cls, final String methodName, Class[] paramTypes, final boolean exact) {
            if (cls == null) {
                throw new IllegalArgumentException("Class cannot be null");
            }
            if (methodName == null) {
                throw new IllegalArgumentException("Method Name cannot be null");
            }
            if (paramTypes == null) {
                paramTypes = MethodUtils.EMPTY_CLASS_PARAMETERS;
            }
            this.cls = cls;
            this.methodName = methodName;
            this.paramTypes = paramTypes;
            this.exact = exact;
            this.hashCode = methodName.length();
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof MethodDescriptor)) {
                return false;
            }
            final MethodDescriptor md = (MethodDescriptor)obj;
            return this.exact == md.exact && this.methodName.equals(md.methodName) && this.cls.equals(md.cls) && Arrays.equals(this.paramTypes, md.paramTypes);
        }
        
        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
