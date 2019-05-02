package org.iota.ict.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MultiHashMap<K, V> {

    private Map<K, List<V>> implementation = new HashMap<>();

    public void add(K key, V value) {
        implementation.putIfAbsent(key, new LinkedList<V>());
        List<V> list = get(key);
        if(!list.contains(value))
            list.add(value);
    }

    public void clear() {
        implementation = new HashMap<>();
    }

    public List<V> get(K key) {
        return implementation.getOrDefault(key, new LinkedList<V>());
    }

    public void remove(K key) {
        implementation.remove(key);
    }

    public int size() {
        return implementation.size();
    }

    public boolean containsKey(K key) {
        return implementation.containsKey(key);
    }
}