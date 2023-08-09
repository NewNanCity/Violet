package city.newnan.violet.cache

interface Cache<K, V> {
    fun put(key: K, value: V): V?
    fun remove(key: K): V?
    operator fun get(key: K): V?
    fun clear()
    val size: Int
    val capacity: Int
    val keys: Set<K>
    val values: Collection<V>
    val entries: Set<Map.Entry<K, V>>
    fun containsKey(key: K): Boolean
    fun containsValue(value: V): Boolean
    fun putAll(from: Map<out K, V>)
    fun forEach(action: (key: K, value: V) -> Unit)
    fun getOrDefault(key: K, defaultValue: V): V
    fun getOrPut(key: K, defaultValue: () -> V): V
    operator fun set(key: K, value: V) {
        put(key, value)
    }
}