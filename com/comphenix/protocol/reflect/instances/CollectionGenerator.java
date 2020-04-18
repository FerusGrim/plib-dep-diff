package com.comphenix.protocol.reflect.instances;

import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import javax.annotation.Nullable;

public class CollectionGenerator implements InstanceProvider
{
    public static final CollectionGenerator INSTANCE;
    
    @Override
    public Object create(@Nullable final Class<?> type) {
        if (type != null && type.isInterface()) {
            if (type.equals(Collection.class) || type.equals(List.class)) {
                return new ArrayList();
            }
            if (type.equals(Set.class)) {
                return new HashSet();
            }
            if (type.equals(Map.class)) {
                return new HashMap();
            }
            if (type.equals(SortedSet.class)) {
                return new TreeSet();
            }
            if (type.equals(SortedMap.class)) {
                return new TreeMap();
            }
            if (type.equals(Queue.class)) {
                return new LinkedList();
            }
        }
        return null;
    }
    
    static {
        INSTANCE = new CollectionGenerator();
    }
}
