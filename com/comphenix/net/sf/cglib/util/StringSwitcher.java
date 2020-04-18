package com.comphenix.net.sf.cglib.util;

import com.comphenix.net.sf.cglib.core.ReflectUtils;
import com.comphenix.net.sf.cglib.asm.$Label;
import java.util.List;
import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.ObjectSwitchCallback;
import java.util.Arrays;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.AbstractClassGenerator;
import com.comphenix.net.sf.cglib.core.KeyFactory;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.asm.$Type;

public abstract class StringSwitcher
{
    private static final $Type STRING_SWITCHER;
    private static final Signature INT_VALUE;
    private static final StringSwitcherKey KEY_FACTORY;
    
    public static StringSwitcher create(final String[] strings, final int[] ints, final boolean fixedInput) {
        final Generator gen = new Generator();
        gen.setStrings(strings);
        gen.setInts(ints);
        gen.setFixedInput(fixedInput);
        return gen.create();
    }
    
    protected StringSwitcher() {
    }
    
    public abstract int intValue(final String p0);
    
    static {
        STRING_SWITCHER = TypeUtils.parseType("com.comphenix.net.sf.cglib.util.StringSwitcher");
        INT_VALUE = TypeUtils.parseSignature("int intValue(String)");
        KEY_FACTORY = (StringSwitcherKey)KeyFactory.create(StringSwitcherKey.class);
    }
    
    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE;
        private String[] strings;
        private int[] ints;
        private boolean fixedInput;
        
        public Generator() {
            super(Generator.SOURCE);
        }
        
        public void setStrings(final String[] strings) {
            this.strings = strings;
        }
        
        public void setInts(final int[] ints) {
            this.ints = ints;
        }
        
        public void setFixedInput(final boolean fixedInput) {
            this.fixedInput = fixedInput;
        }
        
        @Override
        protected ClassLoader getDefaultClassLoader() {
            return this.getClass().getClassLoader();
        }
        
        public StringSwitcher create() {
            this.setNamePrefix(StringSwitcher.class.getName());
            final Object key = StringSwitcher.KEY_FACTORY.newInstance(this.strings, this.ints, this.fixedInput);
            return (StringSwitcher)super.create(key);
        }
        
        public void generateClass(final $ClassVisitor v) throws Exception {
            final ClassEmitter ce = new ClassEmitter(v);
            ce.begin_class(46, 1, this.getClassName(), StringSwitcher.STRING_SWITCHER, null, "<generated>");
            EmitUtils.null_constructor(ce);
            final CodeEmitter e = ce.begin_method(1, StringSwitcher.INT_VALUE, null);
            e.load_arg(0);
            final List stringList = Arrays.asList(this.strings);
            final int style = this.fixedInput ? 2 : 1;
            EmitUtils.string_switch(e, this.strings, style, new ObjectSwitchCallback() {
                public void processCase(final Object key, final $Label end) {
                    e.push(Generator.this.ints[stringList.indexOf(key)]);
                    e.return_value();
                }
                
                public void processDefault() {
                    e.push(-1);
                    e.return_value();
                }
            });
            e.end_method();
            ce.end_class();
        }
        
        @Override
        protected Object firstInstance(final Class type) {
            return ReflectUtils.newInstance(type);
        }
        
        @Override
        protected Object nextInstance(final Object instance) {
            return instance;
        }
        
        static {
            SOURCE = new Source(StringSwitcher.class.getName());
        }
    }
    
    interface StringSwitcherKey
    {
        Object newInstance(final String[] p0, final int[] p1, final boolean p2);
    }
}
