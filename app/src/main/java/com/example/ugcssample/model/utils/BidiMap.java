package com.example.ugcssample.model.utils;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class BidiMap<K, V> {

    private Map<K, V> keyValue = new Hashtable<>();
    private Map<V, K> valueKey = new Hashtable<>();

    public BidiMap() {
    }

    public BidiMap(K[] keys, V[] values) {
        int length = keys.length;
        for (int i = 0; i < length; i += 1) {
            keyValue.put(keys[i], values[i]);
            valueKey.put(values[i], keys[i]);
        }
    }

    public V getByKey(K key) {
        return keyValue.get(key);
    }

    public K getByValue(V value) {
        return valueKey.get(value);
    }

    public boolean add(K key, V value) {
        if (keyValue.containsKey(key))
            throw new IllegalArgumentException("Key already exists");
        if (valueKey.containsKey(value))
            throw new IllegalArgumentException("Value already exists");
        keyValue.put(key, value);
        valueKey.put(value, key);
        return true;
    }

    public int size() {
        return keyValue.size();
    }

    public static final class Builder<K2, V2> {
        K2[] keys;
        V2[] values;

        private Builder() {
        }

        public static <K3, V3> Builder<K3, V3> start() {
            return new Builder<>();
        }

        public Builder<K2, V2> setKeyValues(K2[] keys, V2[] values) {
            Set<K2> lump1 = new HashSet<>();
            for (K2 i : keys) {
                if (lump1.contains(i))
                    throw new IllegalArgumentException("Duplicates in keys");
                lump1.add(i);
            }
            Set<V2> lump2 = new HashSet<>();
            for (V2 i : values) {
                if (lump2.contains(i))
                    throw new IllegalArgumentException("Duplicates in values");
                lump2.add(i);
            }
            if (keys.length != values.length)
                throw new IllegalArgumentException("Length keys and values must be equal");
            this.keys = keys;
            this.values = values;
            return this;
        }

        public BidiMap<K2, V2> build() {
            return new BidiMap<>(this.keys, this.values);
        }
    }
}




