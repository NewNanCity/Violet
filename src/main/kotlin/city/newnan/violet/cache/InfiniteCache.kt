package city.newnan.violet.cache

class InfiniteCache<K, V>(override val capacity: Int) : HashMap<K, V>(), Cache<K, V> {
    override fun forEach(action: (key: K, value: V) -> Unit) {
        for (entry in entries) action(entry.key, entry.value)
    }

    override fun getOrPut(key: K, defaultValue: () -> V): V {
        return get(key) ?: defaultValue().also { put(key, it) }
    }


}