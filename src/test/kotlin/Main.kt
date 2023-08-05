import city.newnan.violet.config.ConfigManager2

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

    val a = hashMapOf(
        "a" to Area("a", 1),
        "b" to Area("b", 2)
    )
    ConfigManager2.stringify(a, ConfigManager2.ConfigFileType.Json).also(::println)
    ConfigManager2.parse<LinkedHashMap<String, Area>>(ConfigManager2.stringify(a, ConfigManager2.ConfigFileType.Json), ConfigManager2.ConfigFileType.Json).also(::println)
}