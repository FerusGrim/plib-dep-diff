package com.comphenix.net.sf.cglib.core;

public class DefaultNamingPolicy implements NamingPolicy
{
    public static final DefaultNamingPolicy INSTANCE;
    private static final boolean STRESS_HASH_CODE;
    
    public String getClassName(String prefix, final String source, final Object key, final Predicate names) {
        if (prefix == null) {
            prefix = "com.comphenix.net.sf.cglib.empty.Object";
        }
        else if (prefix.startsWith("java")) {
            prefix = "$" + prefix;
        }
        String attempt;
        final String base = attempt = prefix + "$$" + source.substring(source.lastIndexOf(46) + 1) + this.getTag() + "$$" + Integer.toHexString(DefaultNamingPolicy.STRESS_HASH_CODE ? 0 : key.hashCode());
        for (int index = 2; names.evaluate(attempt); attempt = base + "_" + index++) {}
        return attempt;
    }
    
    protected String getTag() {
        return "ByCGLIB";
    }
    
    @Override
    public int hashCode() {
        return this.getTag().hashCode();
    }
    
    @Override
    public boolean equals(final Object o) {
        return o instanceof DefaultNamingPolicy && ((DefaultNamingPolicy)o).getTag().equals(this.getTag());
    }
    
    static {
        INSTANCE = new DefaultNamingPolicy();
        STRESS_HASH_CODE = Boolean.getBoolean("com.comphenix.net.sf.cglib.test.stressHashCodes");
    }
}
