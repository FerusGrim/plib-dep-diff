package com.comphenix.net.sf.cglib.core;

import java.util.HashSet;
import com.comphenix.net.sf.cglib.core.internal.Function;
import com.comphenix.net.sf.cglib.core.internal.LoadingCache;
import java.util.Set;
import com.comphenix.net.sf.cglib.asm.$ClassReader;
import java.util.WeakHashMap;
import java.security.ProtectionDomain;
import java.lang.ref.WeakReference;
import java.util.Map;

public abstract class AbstractClassGenerator<T> implements ClassGenerator
{
    private static final ThreadLocal CURRENT;
    private static volatile Map<ClassLoader, ClassLoaderData> CACHE;
    private GeneratorStrategy strategy;
    private NamingPolicy namingPolicy;
    private Source source;
    private ClassLoader classLoader;
    private String namePrefix;
    private Object key;
    private boolean useCache;
    private String className;
    private boolean attemptLoad;
    
    protected T wrapCachedClass(final Class klass) {
        return (T)new WeakReference((T)klass);
    }
    
    protected Object unwrapCachedValue(final T cached) {
        return ((WeakReference)cached).get();
    }
    
    protected AbstractClassGenerator(final Source source) {
        this.strategy = DefaultGeneratorStrategy.INSTANCE;
        this.namingPolicy = DefaultNamingPolicy.INSTANCE;
        this.useCache = true;
        this.source = source;
    }
    
    protected void setNamePrefix(final String namePrefix) {
        this.namePrefix = namePrefix;
    }
    
    protected final String getClassName() {
        return this.className;
    }
    
    private void setClassName(final String className) {
        this.className = className;
    }
    
    private String generateClassName(final Predicate nameTestPredicate) {
        return this.namingPolicy.getClassName(this.namePrefix, this.source.name, this.key, nameTestPredicate);
    }
    
    public void setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    public void setNamingPolicy(NamingPolicy namingPolicy) {
        if (namingPolicy == null) {
            namingPolicy = DefaultNamingPolicy.INSTANCE;
        }
        this.namingPolicy = namingPolicy;
    }
    
    public NamingPolicy getNamingPolicy() {
        return this.namingPolicy;
    }
    
    public void setUseCache(final boolean useCache) {
        this.useCache = useCache;
    }
    
    public boolean getUseCache() {
        return this.useCache;
    }
    
    public void setAttemptLoad(final boolean attemptLoad) {
        this.attemptLoad = attemptLoad;
    }
    
    public boolean getAttemptLoad() {
        return this.attemptLoad;
    }
    
    public void setStrategy(GeneratorStrategy strategy) {
        if (strategy == null) {
            strategy = DefaultGeneratorStrategy.INSTANCE;
        }
        this.strategy = strategy;
    }
    
    public GeneratorStrategy getStrategy() {
        return this.strategy;
    }
    
    public static AbstractClassGenerator getCurrent() {
        return AbstractClassGenerator.CURRENT.get();
    }
    
    public ClassLoader getClassLoader() {
        ClassLoader t = this.classLoader;
        if (t == null) {
            t = this.getDefaultClassLoader();
        }
        if (t == null) {
            t = this.getClass().getClassLoader();
        }
        if (t == null) {
            t = Thread.currentThread().getContextClassLoader();
        }
        if (t == null) {
            throw new IllegalStateException("Cannot determine classloader");
        }
        return t;
    }
    
    protected abstract ClassLoader getDefaultClassLoader();
    
    protected ProtectionDomain getProtectionDomain() {
        return null;
    }
    
    protected Object create(final Object key) {
        try {
            final ClassLoader loader = this.getClassLoader();
            Map<ClassLoader, ClassLoaderData> cache = AbstractClassGenerator.CACHE;
            ClassLoaderData data = cache.get(loader);
            if (data == null) {
                synchronized (AbstractClassGenerator.class) {
                    cache = AbstractClassGenerator.CACHE;
                    data = cache.get(loader);
                    if (data == null) {
                        final Map<ClassLoader, ClassLoaderData> newCache = new WeakHashMap<ClassLoader, ClassLoaderData>(cache);
                        data = new ClassLoaderData(loader);
                        newCache.put(loader, data);
                        AbstractClassGenerator.CACHE = newCache;
                    }
                }
            }
            this.key = key;
            final Object obj = data.get(this, this.getUseCache());
            if (obj instanceof Class) {
                return this.firstInstance((Class)obj);
            }
            return this.nextInstance(obj);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Error e2) {
            throw e2;
        }
        catch (Exception e3) {
            throw new CodeGenerationException(e3);
        }
    }
    
    protected Class generate(final ClassLoaderData data) {
        final Object save = AbstractClassGenerator.CURRENT.get();
        AbstractClassGenerator.CURRENT.set(this);
        try {
            final ClassLoader classLoader = data.getClassLoader();
            if (classLoader == null) {
                throw new IllegalStateException("ClassLoader is null while trying to define class " + this.getClassName() + ". It seems that the loader has been expired from a weak reference somehow. Please file an issue at cglib's issue tracker.");
            }
            synchronized (classLoader) {
                final String name = this.generateClassName(data.getUniqueNamePredicate());
                data.reserveName(name);
                this.setClassName(name);
            }
            if (this.attemptLoad) {
                try {
                    final Class gen = classLoader.loadClass(this.getClassName());
                    return gen;
                }
                catch (ClassNotFoundException ex) {}
            }
            final byte[] b = this.strategy.generate(this);
            final String className = ClassNameReader.getClassName(new $ClassReader(b));
            final ProtectionDomain protectionDomain = this.getProtectionDomain();
            Class gen;
            synchronized (classLoader) {
                if (protectionDomain == null) {
                    gen = ReflectUtils.defineClass(className, b, classLoader);
                }
                else {
                    gen = ReflectUtils.defineClass(className, b, classLoader, protectionDomain);
                }
            }
            return gen;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Error e2) {
            throw e2;
        }
        catch (Exception e3) {
            throw new CodeGenerationException(e3);
        }
        finally {
            AbstractClassGenerator.CURRENT.set(save);
        }
    }
    
    protected abstract Object firstInstance(final Class p0) throws Exception;
    
    protected abstract Object nextInstance(final Object p0) throws Exception;
    
    static {
        CURRENT = new ThreadLocal();
        AbstractClassGenerator.CACHE = new WeakHashMap<ClassLoader, ClassLoaderData>();
    }
    
    protected static class ClassLoaderData
    {
        private final Set<String> reservedClassNames;
        private final LoadingCache<AbstractClassGenerator, Object, Object> generatedClasses;
        private final WeakReference<ClassLoader> classLoader;
        private final Predicate uniqueNamePredicate;
        private static final Function<AbstractClassGenerator, Object> GET_KEY;
        
        public ClassLoaderData(final ClassLoader classLoader) {
            this.reservedClassNames = new HashSet<String>();
            this.uniqueNamePredicate = new Predicate() {
                public boolean evaluate(final Object name) {
                    return ClassLoaderData.this.reservedClassNames.contains(name);
                }
            };
            if (classLoader == null) {
                throw new IllegalArgumentException("classLoader == null is not yet supported");
            }
            this.classLoader = new WeakReference<ClassLoader>(classLoader);
            final Function<AbstractClassGenerator, Object> load = new Function<AbstractClassGenerator, Object>() {
                public Object apply(final AbstractClassGenerator gen) {
                    final Class klass = gen.generate(ClassLoaderData.this);
                    return gen.wrapCachedClass(klass);
                }
            };
            this.generatedClasses = new LoadingCache<AbstractClassGenerator, Object, Object>(ClassLoaderData.GET_KEY, load);
        }
        
        public ClassLoader getClassLoader() {
            return this.classLoader.get();
        }
        
        public void reserveName(final String name) {
            this.reservedClassNames.add(name);
        }
        
        public Predicate getUniqueNamePredicate() {
            return this.uniqueNamePredicate;
        }
        
        public Object get(final AbstractClassGenerator gen, final boolean useCache) {
            if (!useCache) {
                return gen.generate(this);
            }
            final Object cachedValue = this.generatedClasses.get(gen);
            return gen.unwrapCachedValue(cachedValue);
        }
        
        static {
            GET_KEY = new Function<AbstractClassGenerator, Object>() {
                public Object apply(final AbstractClassGenerator gen) {
                    return gen.key;
                }
            };
        }
    }
    
    protected static class Source
    {
        String name;
        
        public Source(final String name) {
            this.name = name;
        }
    }
}
