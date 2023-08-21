package city.newnan.violet.cache

/**
 * LRU: Least Recently Used Cache
 */
class LRUCache<K, V>(override val capacity: Int) :
    LinkedHashMap<K, V>(capacity, 1.0f, true), Cache<K, V> {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > capacity
    }

    override fun forEach(action: (key: K, value: V) -> Unit) {
        for (entry in entries) action(entry.key, entry.value)
    }

    override fun getOrPut(key: K, defaultValue: () -> V): V {
        return get(key) ?: defaultValue().also { put(key, it) }
    }
}