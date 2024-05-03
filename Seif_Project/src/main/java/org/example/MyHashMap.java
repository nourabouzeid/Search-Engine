package org.example;

import java.util.*;

public class MyHashMap<K, K2, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private LinkedList<Entry<K, HashMap<K2, V>>>[] buckets;
    private int size;

    public MyHashMap() {
        this(DEFAULT_CAPACITY);
    }

    public MyHashMap(int initialCapacity) {
        this.buckets = new LinkedList[initialCapacity];
        this.size = 0;
    }

    public void put(K key, K2 nestedKey, V value) {
        if (key == null || nestedKey == null) {
            throw new IllegalArgumentException("Keys cannot be null");
        }
        int index = getIndex(key);
        if (buckets[index] == null) {
            buckets[index] = new LinkedList<>();
        }
        for (Entry<K, HashMap<K2, V>> entry : buckets[index]) {
            if (entry.key.equals(key)) {
                entry.value.put(nestedKey, value);
                return;
            }
        }
        HashMap<K2, V> nestedMap = new HashMap<>();
        nestedMap.put(nestedKey, value);
        buckets[index].add(new Entry<>(key, nestedMap));
        size++;
        if ((float) size / buckets.length > LOAD_FACTOR) {
            resizeAndRehash();
        }
    }

    public void update(K key, K2 nestedKey, V newValue) {
        if (key == null || nestedKey == null) {
            throw new IllegalArgumentException("Keys cannot be null");
        }
        int index = getIndex(key);
        LinkedList<Entry<K, HashMap<K2, V>>> bucket = buckets[index];
        if (bucket != null) {
            for (Entry<K, HashMap<K2, V>> entry : bucket) {
                if (entry.key.equals(key)) {
                    entry.value.put(nestedKey, newValue);
                    return;
                }
            }
        }
        // If key not found, you can choose to throw an exception or handle accordingly.
        // Here, for simplicity, let's just do nothing or log a message.
        // You can customize this behavior based on your application's requirements.
    }

    public HashMap<K2, V> get(K key) {
        if (key == null) {
            return null;
        }
        int index = getIndex(key);
        LinkedList<Entry<K, HashMap<K2, V>>> bucket = buckets[index];
        if (bucket != null) {
            for (Entry<K, HashMap<K2, V>> entry : bucket) {
                if (entry.key.equals(key)) {
                    return entry.value;
                }
            }
        }
        return null;
    }

    public V getValueFromNestedMap(HashMap<K2, V> nestedMap, K2 nestedKey) {
        if (nestedMap == null || nestedKey == null) {
            return null;
        }
        return nestedMap.get(nestedKey);
    }

    public int size() {
        return size;
    }

    private int getIndex(K key) {
        int hashCode = key.hashCode();
        int index = hashCode % buckets.length;
        return index < 0 ? index + buckets.length : index;
    }

    private void resizeAndRehash() {
        int newCapacity = buckets.length * 2;
        LinkedList<Entry<K, HashMap<K2, V>>>[] newBuckets = new LinkedList[newCapacity];
        for (LinkedList<Entry<K, HashMap<K2, V>>> bucket : buckets) {
            if (bucket != null) {
                for (Entry<K, HashMap<K2, V>> entry : bucket) {
                    int newIndex = entry.key.hashCode() % newCapacity;
                    if (newIndex < 0) {
                        newIndex += newCapacity;
                    }
                    if (newBuckets[newIndex] == null) {
                        newBuckets[newIndex] = new LinkedList<>();
                    }
                    newBuckets[newIndex].add(entry);
                }
            }
        }
        buckets = newBuckets;
    }

    public void forEachEntry(EntryConsumer<K, K2, V> consumer) {
        for (LinkedList<Entry<K, HashMap<K2, V>>> bucket : buckets) {
            if (bucket != null) {
                for (Entry<K, HashMap<K2, V>> entry : bucket) {
                    consumer.accept(entry.key, entry.value);
                }
            }
        }
    }

    @FunctionalInterface
    public interface EntryConsumer<K, K2, V> {
        void accept(K key, HashMap<K2, V> nestedMap);
    }

    private static class Entry<K, V> {
        K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    public static void main(String[] args) {
        MyHashMap<String, String, Float> map = new MyHashMap<>();
        map.put("fruit", "apple", 5.3f);
        map.put("fruit", "banana", 3f);
        map.put("color", "red", 10f);
        map.put("color", "blue", 7f);

        map.update("fruit", "apple", 6.0f); // Update value of "apple" under "fruit" key

        map.forEachEntry((key, nestedMap) -> {
            System.out.println("Key: " + key);
            nestedMap.forEach((nestedKey, value) -> {
                System.out.println("  Nested Key: " + nestedKey + ", Value: " + value);
            });
        });

        System.out.println("Size of map: " + map.size());
    }
}