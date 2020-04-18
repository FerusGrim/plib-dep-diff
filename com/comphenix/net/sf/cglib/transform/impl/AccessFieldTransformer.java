package com.comphenix.net.sf.cglib.transform.impl;

import com.comphenix.net.sf.cglib.core.CodeEmitter;
import com.comphenix.net.sf.cglib.core.Signature;
import com.comphenix.net.sf.cglib.core.Constants;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.transform.ClassEmitterTransformer;

public class AccessFieldTransformer extends ClassEmitterTransformer
{
    private Callback callback;
    
    public AccessFieldTransformer(final Callback callback) {
        this.callback = callback;
    }
    
    @Override
    public void declare_field(final int access, final String name, final $Type type, final Object value) {
        super.declare_field(access, name, type, value);
        final String property = TypeUtils.upperFirst(this.callback.getPropertyName(this.getClassType(), name));
        if (property != null) {
            CodeEmitter e = this.begin_method(1, new Signature("get" + property, type, Constants.TYPES_EMPTY), null);
            e.load_this();
            e.getfield(name);
            e.return_value();
            e.end_method();
            e = this.begin_method(1, new Signature("set" + property, $Type.VOID_TYPE, new $Type[] { type }), null);
            e.load_this();
            e.load_arg(0);
            e.putfield(name);
            e.return_value();
            e.end_method();
        }
    }
    
    public interface Callback
    {
        String getPropertyName(final $Type p0, final String p1);
    }
}
