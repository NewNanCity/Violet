import city.newnan.violet.config.Configure2
import com.fasterxml.jackson.core.type.TypeReference

fun main() {
    println(object : TypeReference<Configure2>() {}.type.toString())
    println(object : TypeReference<HashMap<String, Configure2>>() {}.type.toString())
    println(object : TypeReference<HashMap<String, HashMap<String, HashMap<String, Configure2>>>>() {}.type.toString())
}