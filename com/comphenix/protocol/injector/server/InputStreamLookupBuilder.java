package com.comphenix.protocol.injector.server;

import com.comphenix.protocol.error.ErrorReporter;
import org.bukkit.Server;

public class InputStreamLookupBuilder
{
    private Server server;
    private ErrorReporter reporter;
    
    public static InputStreamLookupBuilder newBuilder() {
        return new InputStreamLookupBuilder();
    }
    
    protected InputStreamLookupBuilder() {
    }
    
    public InputStreamLookupBuilder server(final Server server) {
        this.server = server;
        return this;
    }
    
    public InputStreamLookupBuilder reporter(final ErrorReporter reporter) {
        this.reporter = reporter;
        return this;
    }
    
    public AbstractInputStreamLookup build() {
        return new InputStreamReflectLookup(this.reporter, this.server);
    }
}
