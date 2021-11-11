package com.lgh.widgetanalysis;

import java.util.ArrayList;

public class SimpleKeyValue<K, V> {
    private final ArrayList<K> keys = new ArrayList<>();
    private final ArrayList<V> values = new ArrayList<>();

    public boolean put(K key, V value) {
        boolean keyAddSuccess = keys.add(key);
        boolean valueAddSuccess = values.add(value);
        if (keyAddSuccess && valueAddSuccess) {
            return true;
        }
        if (keyAddSuccess) {
            keys.remove(key);
        }
        if (valueAddSuccess) {
            values.remove(value);
        }
        return false;
    }

    public boolean put(Entry<K, V> entry) {
        return put(entry.getKey(), entry.getValue());
    }

    public Entry<K, V> get(int index) {
        if (size() > index) {
            return new Entry<>(keys.get(index), values.get(index));
        }
        return null;
    }

    public void clear() {
        keys.clear();
        values.clear();
    }

    public boolean isEmpty() {
        return keys.isEmpty() || values.isEmpty();
    }

    public int size() {
        return Math.min(keys.size(), values.size());
    }

    public V getValueByKey(K key) {
        int keyIndex = keys.indexOf(key);
        if (keyIndex != -1 && values.size() > keyIndex) {
            return values.get(keyIndex);
        }
        return null;
    }

    public K getKeyByValue(V value) {
        int valueIndex = values.indexOf(value);
        if (valueIndex != -1 && keys.size() > valueIndex) {
            return keys.get(valueIndex);
        }
        return null;
    }

    public boolean removeKey(K key) {
        int keyIndex = keys.indexOf(key);
        if (keyIndex != -1) {
            keys.remove(keyIndex);
        }
        if (values.size() > keyIndex) {
            values.remove(keyIndex);
        }
        return true;
    }

    public boolean removeValue(V value) {
        int valueIndex = values.indexOf(value);
        if (valueIndex != -1) {
            values.remove(valueIndex);
        }
        if (keys.size() > valueIndex) {
            keys.remove(valueIndex);
        }
        return true;
    }

    public boolean containsKey(K key) {
        return keys.contains(key);
    }

    public boolean containsValue(V value) {
        return values.contains(value);
    }

    public static class Entry<K, V> {
        public K key;
        public V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}