package com.comphenix.net.sf.cglib.core;

public class ClassesKey
{
    private static final Key FACTORY;
    
    private ClassesKey() {
    }
    
    public static Object create(final Object[] array) {
        return ClassesKey.FACTORY.newInstance(classNames(array));
    }
    
    private static String[] classNames(final Object[] objects) {
        if (objects == null) {
            return null;
        }
        final String[] classNames = new String[objects.length];
        for (int i = 0; i < objects.length; ++i) {
            final Object object = objects[i];
            if (object != null) {
                final Class<?> aClass = object.getClass();
                classNames[i] = ((aClass == null) ? null : aClass.getName());
            }
        }
        return classNames;
    }
    
    static {
        FACTORY = (Key)KeyFactory.create(Key.class);
    }
    
    interface Key
    {
        Object newInstance(final Object[] p0);
    }
}
