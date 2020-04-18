package com.comphenix.protocol.reflect.cloning;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

public class SerializableCloner implements Cloner
{
    @Override
    public boolean canClone(final Object source) {
        return source != null && source instanceof Serializable;
    }
    
    @Override
    public Object clone(final Object source) {
        return clone((Serializable)source);
    }
    
    public static <T extends Serializable> T clone(final T obj) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeObject(obj);
            final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
            return (T)in.readObject();
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to clone object " + obj + " (" + obj.getClass().getName() + ")", e);
        }
    }
}
