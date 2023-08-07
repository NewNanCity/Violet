import city.newnan.violet.config.ConfigManager2
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RailArea(
    @JsonFormat(shape = JsonFormat.Shape.BINARY)
    val from: IntArray,
    val to: IntArray,
    val title: String,
    val subTitle: String?,
    val actionBar: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RailArea

        if (!from.contentEquals(other.from)) return false
        if (!to.contentEquals(other.to)) return false
        if (title != other.title) return false
        if (subTitle != other.subTitle) return false
        return actionBar == other.actionBar
    }

    override fun hashCode(): Int {
        var result = from.contentHashCode()
        result = 31 * result + to.contentHashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (subTitle?.hashCode() ?: 0)
        result = 31 * result + (actionBar?.hashCode() ?: 0)
        return result
    }
}

data class Area(val a: String, val b: Int)

fun main() {
//    val mapper2 = CsvMapper().enable(SerializationFeature.INDENT_OUTPUT)
//    val m = mapper.readValue("""{
//        |"title": "title",
//        |"created": 0,
//        |"modified": 1,
//        |"xxx": [1,2,3,4,5]
//        |}""".trimMargin(), ObjectNode::class.java)
//
//    val x = hashMapOf(
//        "title" to "title",
//        "created" to 0,
//        "modified" to 1,
//        "xxx" to listOf(1,2,3,4,5),
//        "c" to hashMapOf(
//            "a" to 1,
//            "b" to 2
//        )
//    )
//    // println(mapper.writeValueAsString(x))
//    println(mapper.valueToTree<ObjectNode>(x).toPrettyString())
//
//    val k = m.asMap()
//    println(k)

//    val a = hashMapOf(
//        "a" to Area("a", 1),
//        "b" to Area("b", 2)
//    )
    // ConfigManager2.stringify(a, ConfigManager2.ConfigFileType.Json).also(::println)
    // ConfigManager2.parse<LinkedHashMap<String, Area>>(ConfigManager2.stringify(a, ConfigManager2.ConfigFileType.Json), ConfigManager2.ConfigFileType.Json).also(::println)

    val a = hashMapOf(
        "world" to hashMapOf(
            "a" to RailArea(intArrayOf(1,2,3), intArrayOf(4,5,6), "a", null, null),
            "b" to RailArea(intArrayOf(1,2,3), intArrayOf(4,5,6), "b", null, null)
        )
    )
    val ass = ConfigManager2.stringify(a, ConfigManager2.ConfigFileType.Yaml).also(::println)
    ConfigManager2.parse<LinkedHashMap<String, LinkedHashMap<String, RailArea>>>(ConfigManager2.stringify(a, ConfigManager2.ConfigFileType.Yaml), ConfigManager2.ConfigFileType.Yaml).also(::println)

    println(ConfigManager2.parse<JsonNode>(ass, ConfigManager2.ConfigFileType.Yaml).toString())
}