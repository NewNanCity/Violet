package city.newnan.violet.cache

import java.util.*

class GreedyDualSizeCache<K, V>(override val capacity: Int) : Cache<K, V> {
    private val cache = mutableMapOf<K, V>()
    private val freq = mutableMapOf<K, Int>()
    private val queue = PriorityQueue<Pair<K, Int>>(compareBy { it.second })

    override fun put(key: K, value: V): V? {
        val oldValue = cache[key]
        if (oldValue == null) {
            if (cache.size >= capacity) {
                val (k, _) = queue.poll()
                cache.remove(k)
                freq.remove(k)
            }
            freq[key] = 1
        } else {
            freq[key] = (freq[key]?.also { queue.remove(key to it) } ?: 0) + 1
        }
        cache[key] = value
        queue.add(key to freq[key]!!)
        return oldValue
    }

    override fun get(key: K): V? {
        if (cache.containsKey(key)) {
            freq[key] = (freq[key]?.also { queue.remove(key to it) } ?: 0) + 1
            queue.add(key to freq[key]!!)
            return cache[key]
        }
        return null
    }

    override fun remove(key: K): V? {
        freq.remove(key)?.also { queue.remove(key to it) }
        return cache.remove(key)
    }

    override fun clear() {
        cache.clear()
        freq.clear()
        queue.clear()
    }

    override val entries: Set<Map.Entry<K, V>>
        get() = cache.entries

    override val keys: Set<K>
        get() = cache.keys

    override val values: Collection<V>
        get() = cache.values

    override val size: Int
        get() = cache.size

    override fun containsKey(key: K): Boolean =
        cache.containsKey(key)

    override fun containsValue(value: V): Boolean =
        cache.containsValue(value)

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) put(k, v)
    }

    override fun forEach(action: (key: K, value: V) -> Unit) {
        for ((k, v) in cache) action(k, v)
    }

    override fun getOrDefault(key: K, defaultValue: V): V =
        get(key) ?: defaultValue

    override fun getOrPut(key: K, defaultValue: () -> V): V =
        get(key) ?: defaultValue().also { put(key, it) }

    override fun toString(): String {
        return cache.toString()
    }
}