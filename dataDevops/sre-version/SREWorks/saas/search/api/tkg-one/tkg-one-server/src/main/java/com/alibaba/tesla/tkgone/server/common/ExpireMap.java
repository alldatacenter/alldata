package com.alibaba.tesla.tkgone.server.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yangjinghua
 */
public class ExpireMap<K, V> extends HashMap<K, V> {

    private static final long serialVersionUID = 1L;

    private long EXPIRY = 2000L;

    private HashMap<K, Long> expiryMap;
    private HashMap<K, V> valueMap;

    public ExpireMap() {
        expiryMap = new HashMap<>();
        valueMap = new HashMap<>();
    }

    public ExpireMap(long expiry) {
        this();
        this.EXPIRY = expiry;
    }

    public ExpireMap(int capacity, long expire) {
        expiryMap = new HashMap<>(capacity);
        valueMap = new HashMap<>(capacity);
        this.EXPIRY = expire;
    }

    @Override
    public V put(K key, V value) {
        if (value != null) {
            expiryMap.put(key, System.currentTimeMillis() + EXPIRY);
            return valueMap.put(key, value);
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        return !isInvalid(key) && valueMap.containsKey(key);
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = new HashSet<>();
        for (K key : valueMap.keySet()) {
            if (containsKey(key)) {
                set.add(key);
            }
        }
        return set;
    }

    @Override
    public V get(Object key) {
        if (key == null) { return null; }
        if (isInvalid(key)) { return null; }
        return valueMap.get(key);
    }

    private boolean isInvalid(Object key) {
        long expireTime = expiryMap.getOrDefault(key, 0L);
        boolean invalid = System.currentTimeMillis() > expireTime;

        if (invalid) {
            expiryMap.remove(key);
            valueMap.remove(key);
        }
        return invalid;
    }

}
