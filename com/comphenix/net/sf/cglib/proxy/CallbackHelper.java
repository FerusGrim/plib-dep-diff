package com.comphenix.net.sf.cglib.proxy;

import com.comphenix.net.sf.cglib.core.ReflectUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CallbackHelper implements CallbackFilter
{
    private Map methodMap;
    private List callbacks;
    
    public CallbackHelper(final Class superclass, final Class[] interfaces) {
        this.methodMap = new HashMap();
        this.callbacks = new ArrayList();
        final List methods = new ArrayList();
        Enhancer.getMethods(superclass, interfaces, methods);
        final Map indexes = new HashMap();
        for (int i = 0, size = methods.size(); i < size; ++i) {
            final Method method = methods.get(i);
            final Object callback = this.getCallback(method);
            if (callback == null) {
                throw new IllegalStateException("getCallback cannot return null");
            }
            final boolean isCallback = callback instanceof Callback;
            if (!isCallback && !(callback instanceof Class)) {
                throw new IllegalStateException("getCallback must return a Callback or a Class");
            }
            if (i > 0 && (this.callbacks.get(i - 1) instanceof Callback ^ isCallback)) {
                throw new IllegalStateException("getCallback must return a Callback or a Class consistently for every Method");
            }
            Integer index = indexes.get(callback);
            if (index == null) {
                index = new Integer(this.callbacks.size());
                indexes.put(callback, index);
            }
            this.methodMap.put(method, index);
            this.callbacks.add(callback);
        }
    }
    
    protected abstract Object getCallback(final Method p0);
    
    public Callback[] getCallbacks() {
        if (this.callbacks.size() == 0) {
            return new Callback[0];
        }
        if (this.callbacks.get(0) instanceof Callback) {
            return this.callbacks.toArray(new Callback[this.callbacks.size()]);
        }
        throw new IllegalStateException("getCallback returned classes, not callbacks; call getCallbackTypes instead");
    }
    
    public Class[] getCallbackTypes() {
        if (this.callbacks.size() == 0) {
            return new Class[0];
        }
        if (this.callbacks.get(0) instanceof Callback) {
            return ReflectUtils.getClasses(this.getCallbacks());
        }
        return this.callbacks.toArray(new Class[this.callbacks.size()]);
    }
    
    public int accept(final Method method) {
        return this.methodMap.get(method);
    }
    
    @Override
    public int hashCode() {
        return this.methodMap.hashCode();
    }
    
    @Override
    public boolean equals(final Object o) {
        return o != null && o instanceof CallbackHelper && this.methodMap.equals(((CallbackHelper)o).methodMap);
    }
}
