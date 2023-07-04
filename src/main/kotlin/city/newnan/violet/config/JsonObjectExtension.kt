@file:Suppress("unused")

package city.newnan.violet.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import me.lucko.helper.serialize.InventorySerialization
import org.bukkit.Material
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID


/**
 * 使用索引器路径获取节点
 *
 * ```
 * val node1 = node["foo", 2, "bar"]
 * ```
 *
 * @receiver [ObjectNode]
 * @param paths Paths 字符串或整数，分别用于索引Object和Array
 * @return [ObjectNode]
 * @throws Exception 未知路径类型
 */
operator fun ObjectNode.get(vararg paths: Any): ObjectNode {
    var node: ObjectNode = this
    for (path in paths) {
        node = when (path) {
            is String -> node[path] as ObjectNode
            is Int -> node[path] as ObjectNode
            else -> throw Exception("Unknown path type: ${path::class.java} (value: $path)")
        }
    }
    return node
}

infix fun ObjectNode.get(key: String) = get(key) as ObjectNode

fun <T> JsonNode.asType(): T = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json]
    .convertValue(this, object : com.fasterxml.jackson.core.type.TypeReference<T>() {})
fun JsonNode.asMap() = asType<LinkedHashMap<String, Any>>()
fun Map<*, *>.toObjectNode(): ObjectNode = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json]
    .valueToTree(this)
fun ObjectNode.put(key: String, value: Map<*, *>) {
    replace(key, value.toObjectNode())
}
fun JsonNode.asList() = asType<ArrayList<Any>>()
fun List<*>.toObjectNode(): ObjectNode = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json]
    .valueToTree(this)
fun ObjectNode.put(key: String, value: List<*>) {
    replace(key, value.toObjectNode())
}
fun <T> T.toObjectNode(): ObjectNode = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json]
    .valueToTree(this)
fun <T> ObjectNode.put(key: String, value: T) {
    replace(key, value.toObjectNode())
}
fun JsonNode.asUUID(): UUID = UUID.fromString(asText())
fun ObjectNode.put(key: String, value: UUID): ObjectNode = put(key, value.toString())
fun JsonNode.asMaterial(): Material? = Material.matchMaterial(asText())
fun ObjectNode.put(key: String, value: Material): ObjectNode = put(key, value.key.toString())
fun ObjectNode.put(key: String, value: ConfigurationSerializable): ObjectNode
    = replace(key,
    ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json].valueToTree<ObjectNode>(value.serialize())) as ObjectNode
fun <T : ConfigurationSerializable> JsonNode.asConfigurationSerializable(): T?
    = ConfigurationSerialization.deserializeObject(asMap()) as T?
fun JsonNode.asBase64Inventory(): Inventory {
    val map = asMap()
    return InventorySerialization.decodeInventory(map["data"] as String, map["title"] as String)
}
fun JsonNode.asBase64ItemStacks(): Array<ItemStack>
    = InventorySerialization.decodeItemStacks(asText())
fun JsonNode.asBase64ItemStack(): ItemStack
    = InventorySerialization.decodeItemStack(asText())
fun ObjectNode.putBase64(key: String, value: Inventory, title: String): ObjectNode
    = replace(key, linkedMapOf(
        "title" to title,
        "data" to InventorySerialization.encodeInventory(value)
    ).toObjectNode()) as ObjectNode

fun ObjectNode.putBase64(key: String, value: Array<ItemStack>): ObjectNode
    = put(key, InventorySerialization.encodeItemStacks(value))
fun ObjectNode.putBase64(key: String, value: ItemStack): ObjectNode
    = put(key, InventorySerialization.encodeItemStack(value))