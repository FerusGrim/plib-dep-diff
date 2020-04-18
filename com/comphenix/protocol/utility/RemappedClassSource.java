package com.comphenix.protocol.utility;

import org.bukkit.Server;
import com.comphenix.protocol.reflect.MethodUtils;
import com.comphenix.protocol.reflect.FieldUtils;
import org.bukkit.Bukkit;
import java.lang.reflect.Method;

class RemappedClassSource extends ClassSource
{
    private Object classRemapper;
    private Method mapType;
    private ClassLoader loader;
    
    public RemappedClassSource() {
        this(RemappedClassSource.class.getClassLoader());
    }
    
    public RemappedClassSource(final ClassLoader loader) {
        this.loader = loader;
    }
    
    public RemappedClassSource initialize() {
        try {
            final Server server = Bukkit.getServer();
            if (server == null) {
                throw new IllegalStateException("Bukkit not initialized.");
            }
            final String version = server.getVersion();
            if (!version.contains("MCPC") && !version.contains("Cauldron")) {
                throw new RemapperUnavailableException(RemapperUnavailableException.Reason.MCPC_NOT_PRESENT);
            }
            this.classRemapper = FieldUtils.readField(this.getClass().getClassLoader(), "remapper", true);
            if (this.classRemapper == null) {
                throw new RemapperUnavailableException(RemapperUnavailableException.Reason.REMAPPER_DISABLED);
            }
            final Class<?> renamerClazz = this.classRemapper.getClass();
            this.mapType = MethodUtils.getAccessibleMethod(renamerClazz, "map", new Class[] { String.class });
            return this;
        }
        catch (RemapperUnavailableException e) {
            throw e;
        }
        catch (Exception e2) {
            throw new RuntimeException("Cannot access MCPC remapper.", e2);
        }
    }
    
    @Override
    public Class<?> loadClass(final String canonicalName) throws ClassNotFoundException {
        final String remapped = this.getClassName(canonicalName);
        try {
            return this.loader.loadClass(remapped);
        }
        catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Cannot find " + canonicalName + "(Remapped: " + remapped + ")");
        }
    }
    
    public String getClassName(final String path) {
        try {
            final String remapped = (String)this.mapType.invoke(this.classRemapper, path.replace('.', '/'));
            return remapped.replace('/', '.');
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot remap class name.", e);
        }
    }
    
    public static class RemapperUnavailableException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private final Reason reason;
        
        public RemapperUnavailableException(final Reason reason) {
            super(reason.getMessage());
            this.reason = reason;
        }
        
        public Reason getReason() {
            return this.reason;
        }
        
        public enum Reason
        {
            MCPC_NOT_PRESENT("The server is not running MCPC+/Cauldron"), 
            REMAPPER_DISABLED("Running an MCPC+/Cauldron server but the remapper is unavailable. Please turn it on!");
            
            private final String message;
            
            private Reason(final String message) {
                this.message = message;
            }
            
            public String getMessage() {
                return this.message;
            }
        }
    }
}
