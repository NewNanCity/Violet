import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper

fun main() {
    val mapper = ObjectMapper()
    val mapper2 = CsvMapper().enable(SerializationFeature.INDENT_OUTPUT)
    val m = mapper.readValue("""{
        |"title": "title",
        |"created": 0,
        |"modified": 1,
        |"xxx": [1,2,3,4,5]
        |}""".trimMargin(), ObjectNode::class.java)
    val a = m["title",]
    println(a)
}