package com.comphenix.protocol.reflect.instances;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import java.util.LinkedList;
import com.google.common.collect.Lists;
import java.util.Iterator;
import com.comphenix.protocol.reflect.FieldUtils;
import java.lang.reflect.Field;
import com.comphenix.protocol.reflect.FuzzyReflection;

public class ExistingGenerator implements InstanceProvider
{
    private Node root;
    
    private ExistingGenerator() {
        this.root = new Node(null, null, 0);
    }
    
    public static ExistingGenerator fromObjectFields(final Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be NULL.");
        }
        return fromObjectFields(object, object.getClass());
    }
    
    public static ExistingGenerator fromObjectFields(final Object object, final Class<?> type) {
        final ExistingGenerator generator = new ExistingGenerator();
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be NULL.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be NULL.");
        }
        if (!type.isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("Type must be a superclass or be the same type.");
        }
        for (final Field field : FuzzyReflection.fromClass(type, true).getFields()) {
            try {
                final Object value = FieldUtils.readField(field, object, true);
                if (value == null) {
                    continue;
                }
                generator.addObject(field.getType(), value);
            }
            catch (Exception ex) {}
        }
        return generator;
    }
    
    public static ExistingGenerator fromObjectArray(final Object[] values) {
        final ExistingGenerator generator = new ExistingGenerator();
        for (final Object value : values) {
            generator.addObject(value);
        }
        return generator;
    }
    
    private void addObject(final Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be NULL.");
        }
        this.addObject(value.getClass(), value);
    }
    
    private void addObject(final Class<?> type, final Object value) {
        final Node node = this.getLeafNode(this.root, type, false);
        node.setValue(value);
    }
    
    private Node getLeafNode(final Node start, final Class<?> type, final boolean readOnly) {
        final Class<?>[] path = this.getHierachy(type);
        Node current = start;
        for (int i = 0; i < path.length; ++i) {
            final Node next = this.getNext(current, path[i], readOnly);
            if (next == null && readOnly) {
                current = null;
                break;
            }
            current = next;
        }
        return current;
    }
    
    private Node getNext(final Node current, final Class<?> clazz, final boolean readOnly) {
        Node next = current.getChild(clazz);
        if (next == null && !readOnly) {
            next = current.addChild(new Node(clazz, null, current.getLevel() + 1));
        }
        if (next != null && !readOnly && !clazz.isInterface()) {
            for (final Class<?> clazzInterface : clazz.getInterfaces()) {
                this.getLeafNode(this.root, clazzInterface, readOnly).addChild(next);
            }
        }
        return next;
    }
    
    private Node getLowestLeaf(final Node current) {
        Node candidate = current;
        for (final Node child : current.getChildren()) {
            final Node subtree = this.getLowestLeaf(child);
            if (subtree.getValue() != null && candidate.getLevel() < subtree.getLevel()) {
                candidate = subtree;
            }
        }
        return candidate;
    }
    
    private Class<?>[] getHierachy(Class<?> type) {
        final LinkedList<Class<?>> levels = (LinkedList<Class<?>>)Lists.newLinkedList();
        while (type != null) {
            levels.addFirst(type);
            type = type.getSuperclass();
        }
        return levels.toArray(new Class[0]);
    }
    
    @Override
    public Object create(@Nullable final Class<?> type) {
        Node node = this.getLeafNode(this.root, type, true);
        if (node != null) {
            node = this.getLowestLeaf(node);
        }
        if (node != null) {
            return node.getValue();
        }
        return null;
    }
    
    private static final class Node
    {
        private Map<Class<?>, Node> children;
        private Class<?> key;
        private Object value;
        private int level;
        
        public Node(final Class<?> key, final Object value, final int level) {
            this.children = new HashMap<Class<?>, Node>();
            this.key = key;
            this.value = value;
            this.level = level;
        }
        
        public Node addChild(final Node node) {
            this.children.put(node.key, node);
            return node;
        }
        
        public int getLevel() {
            return this.level;
        }
        
        public Collection<Node> getChildren() {
            return this.children.values();
        }
        
        public Object getValue() {
            return this.value;
        }
        
        public void setValue(final Object value) {
            this.value = value;
        }
        
        public Node getChild(final Class<?> clazz) {
            return this.children.get(clazz);
        }
    }
}
