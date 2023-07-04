import city.newnan.violet.config.asConfigurationSerializable
import city.newnan.violet.config.asMap
import city.newnan.violet.config.put
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import me.lucko.helper.serialize.Position
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

fun main() {
    val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    val mapper2 = CsvMapper().enable(SerializationFeature.INDENT_OUTPUT)
    val m = mapper.readValue("""{
        |"title": "title",
        |"created": 0,
        |"modified": 1,
        |"xxx": [1,2,3,4,5]
        |}""".trimMargin(), ObjectNode::class.java)

    val x = hashMapOf(
        "title" to "title",
        "created" to 0,
        "modified" to 1,
        "xxx" to listOf(1,2,3,4,5),
        "c" to hashMapOf(
            "a" to 1,
            "b" to 2
        )
    )
    // println(mapper.writeValueAsString(x))
    println(mapper.valueToTree<ObjectNode>(x).toPrettyString())

    val k = m.asMap()
    println(k)
}