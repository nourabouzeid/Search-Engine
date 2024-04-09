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
        // Ensure the bucket at the index is initialized
        if (buckets[index] == null) {
            buckets[index] = new LinkedList<>();
        }
        // Check if key already exists in the bucket
        for (Entry<K, HashMap<K2, V>> entry : buckets[index]) {
            if (entry.key.equals(key)) {
                entry.value.put(nestedKey, value); // Add or update value in nested hashmap
                return;
            }
        }
        // Key doesn't exist, add new entry with a new nested hashmap
        HashMap<K2, V> nestedMap = new HashMap<>();
        nestedMap.put(nestedKey, value);
        buckets[index].add(new Entry<>(key, nestedMap));
        size++;
        // Check load factor and resize if necessary
        if ((float) size / buckets.length > LOAD_FACTOR) {
            resizeAndRehash();
        }
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
                    return entry.value; // Return the nested hashmap associated with the key
                }
            }
        }
        return null; // Key not found
    }
    public V getValueFromNestedMap(HashMap<K2, V> nestedMap, K2 nestedKey) {
        if (nestedMap == null || nestedKey == null) {
            return null;
        }
        return nestedMap.get(nestedKey); // Retrieve value from the nested hashmap
    }
    public int size() {
        return size;
    }
    private int getIndex(K key) {
        int hashCode = key.hashCode();
        int index = hashCode % buckets.length;
        return index < 0 ? index + buckets.length : index; // Handle negative index
    }
    private void resizeAndRehash() {
        int newCapacity = buckets.length * 2;
        LinkedList<Entry<K, HashMap<K2, V>>>[] newBuckets = new LinkedList[newCapacity];
        // Rehash all entries into the new buckets
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
        // Update buckets array and capacity
        buckets = newBuckets;
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
        // Add key-value pairs with nested hashmaps
        map.put("fruit", "apple", 5.3f);
        map.put("fruit", "banana", 3f);
        map.put("color", "red" , 10f);
        map.put("color", "blue", 7f);
        // Retrieve and print the nested hashmap associated with 'fruit'
        HashMap<String, Float> fruitMap = map.get("fruit");
        if (fruitMap != null) {
            // Retrieve and print values associated with keys in the nested hashmap
            System.out.println("Value for 'apple' in 'fruit' map: " + map.getValueFromNestedMap(fruitMap, "apple"));
            System.out.println("Value for 'banana' in 'fruit' map: " + map.getValueFromNestedMap(fruitMap, "banana"));
        }
        // Print size of the map
        System.out.println("Size of map: " + map.size());
    }
}
