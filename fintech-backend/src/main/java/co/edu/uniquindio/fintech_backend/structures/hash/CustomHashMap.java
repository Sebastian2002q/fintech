package co.edu.uniquindio.fintech_backend.structures.hash;

import java.util.Objects;
import java.util.function.Consumer;

public class CustomHashMap<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;
    private Node<K, V>[] buckets;
    private int size;

    private static class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> next;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }
    }

    @SuppressWarnings("unchecked")
    public CustomHashMap() {
        this.buckets = (Node<K, V>[]) new Node[DEFAULT_CAPACITY];
        this.size = 0;
    }

    private int indexFor(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return Math.floorMod(key.hashCode(), buckets.length);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    int capacity() {
        return buckets.length;
    }

    public V put(K key, V value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        int index = indexFor(key);
        Node<K, V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(key)) {
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            }
            current = current.next;
        }
        if ((size + 1.0) / buckets.length > LOAD_FACTOR) {
            resize();
            index = indexFor(key);
        }
        Node<K, V> newNode = new Node<>(key, value);
        newNode.next = buckets[index];
        buckets[index] = newNode;
        size++;
        return null;
    }

    public V get(K key) {
        int index = indexFor(key);
        Node<K, V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public V remove(K key) {
        int index = indexFor(key);
        Node<K, V> current = buckets[index];
        Node<K, V> previous = null;
        while (current != null) {
            if (current.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return current.value;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Node<K, V>[] oldBuckets = buckets;
        int newCapacity = oldBuckets.length * 2;
        Node<K, V>[] newBuckets = (Node<K, V>[]) new Node[newCapacity];
        for (Node<K, V> bucket : oldBuckets) {
            Node<K, V> current = bucket;
            while (current != null) {
                Node<K, V> next = current.next;
                int newIndex = Math.floorMod(current.key.hashCode(), newCapacity);
                current.next = newBuckets[newIndex];
                newBuckets[newIndex] = current;
                current = next;
            }
        }
        buckets = newBuckets;
    }

    public void forEachValue(Consumer<? super V> action) {
        Objects.requireNonNull(action, "Action cannot be null");
        for (Node<K, V> bucket : buckets) {
            Node<K, V> current = bucket;
            while (current != null) {
                action.accept(current.value);
                current = current.next;
            }
        }
    }
}
