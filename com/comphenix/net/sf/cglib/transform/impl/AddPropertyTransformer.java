package com.comphenix.net.sf.cglib.transform.impl;

import com.comphenix.net.sf.cglib.core.ClassEmitter;
import com.comphenix.net.sf.cglib.core.EmitUtils;
import com.comphenix.net.sf.cglib.core.TypeUtils;
import java.util.Map;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.transform.ClassEmitterTransformer;

public class AddPropertyTransformer extends ClassEmitterTransformer
{
    private final String[] names;
    private final $Type[] types;
    
    public AddPropertyTransformer(final Map props) {
        final int size = props.size();
        this.names = props.keySet().toArray(new String[size]);
        this.types = new $Type[size];
        for (int i = 0; i < size; ++i) {
            this.types[i] = props.get(this.names[i]);
        }
    }
    
    public AddPropertyTransformer(final String[] names, final $Type[] types) {
        this.names = names;
        this.types = types;
    }
    
    @Override
    public void end_class() {
        if (!TypeUtils.isAbstract(this.getAccess())) {
            EmitUtils.add_properties(this, this.names, this.types);
        }
        super.end_class();
    }
}
