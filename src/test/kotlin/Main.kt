import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.config.Configure2
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.DateDeserializers
import com.fasterxml.jackson.databind.ser.std.DateSerializer
import java.util.*

data class A(
    @JsonSerialize(using = DateSerializer::class)
    @JsonDeserialize(using = DateDeserializers.TimestampDeserializer::class)
    val date: Date
)

fun main() {
    val a = A(Date())
    val s = ConfigManager2.stringify(a, ConfigManager2.ConfigFileType.Yaml)
    println(s)
    val b = ConfigManager2.parse<A>(s, ConfigManager2.ConfigFileType.Yaml)
    println(a)
    println(b)
}