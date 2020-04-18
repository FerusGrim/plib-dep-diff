package com.comphenix.net.sf.cglib.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class CollectionUtils
{
    private CollectionUtils() {
    }
    
    public static Map bucket(final Collection c, final Transformer t) {
        final Map buckets = new HashMap();
        for (final Object value : c) {
            final Object key = t.transform(value);
            List bucket = buckets.get(key);
            if (bucket == null) {
                buckets.put(key, bucket = new LinkedList());
            }
            bucket.add(value);
        }
        return buckets;
    }
    
    public static void reverse(final Map source, final Map target) {
        for (final Object key : source.keySet()) {
            target.put(source.get(key), key);
        }
    }
    
    public static Collection filter(final Collection c, final Predicate p) {
        final Iterator it = c.iterator();
        while (it.hasNext()) {
            if (!p.evaluate(it.next())) {
                it.remove();
            }
        }
        return c;
    }
    
    public static List transform(final Collection c, final Transformer t) {
        final List result = new ArrayList(c.size());
        final Iterator it = c.iterator();
        while (it.hasNext()) {
            result.add(t.transform(it.next()));
        }
        return result;
    }
    
    public static Map getIndexMap(final List list) {
        final Map indexes = new HashMap();
        int index = 0;
        final Iterator it = list.iterator();
        while (it.hasNext()) {
            indexes.put(it.next(), new Integer(index++));
        }
        return indexes;
    }
}
