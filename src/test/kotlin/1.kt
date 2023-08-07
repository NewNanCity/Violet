import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

data class MyClass(
    val name: String,
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    val values: IntArray
)

fun main() {
    val objectMapper = ObjectMapper(YAMLFactory())

    // 序列化对象
    val obj = MyClass("example", intArrayOf(1, 2, 3))
    val yaml = objectMapper.writeValueAsString(obj)
    println(yaml)
}