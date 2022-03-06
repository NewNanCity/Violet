package city.newnan.violet.json

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.math.BigDecimal
import java.math.BigInteger

fun JsonObject.hasJsonObject(key: String): Boolean = has(key) && get(key).isJsonObject
fun JsonObject.hasJsonArray(key: String): Boolean = has(key) && get(key).isJsonArray
fun JsonObject.hasJsonNull(key: String): Boolean = has(key) && get(key).isJsonNull
fun JsonObject.hasJsonPrimitive(key: String): Boolean = has(key) && get(key).isJsonPrimitive


fun JsonObject.getElement(vararg keyPath: String): JsonElement? {
    if (keyPath.isEmpty())
        return this
    var currentNode: JsonObject = this
    for (i: Int in 0 until keyPath.size - 1) {
        val key = keyPath[i]
        if (!hasJsonObject(key))
            return null
        currentNode = currentNode.getAsJsonObject(key)
    }
    return currentNode.get(keyPath.last())
}
//
fun JsonObject.getJsonObject(defaultValue: JsonObject?, vararg keyPath: String): JsonObject? =
    getElement(*keyPath).run { if ((this == null) || !isJsonObject) defaultValue else asJsonObject }
fun JsonObject.getJsonArray(defaultValue: JsonArray?, vararg keyPath: String): JsonArray? =
    getElement(*keyPath).run { if ((this == null) || !isJsonArray) defaultValue else asJsonArray }
fun JsonObject.getJsonPrimitive(vararg keyPath: String): JsonPrimitive? =
    getElement(*keyPath).run { if ((this == null) || !isJsonPrimitive) null else asJsonPrimitive }
// Integer
fun JsonObject.getLong(defaultValue: Long?, vararg keyPath: String): Long? =
    getJsonPrimitive(*keyPath).run { this?.asLong ?: defaultValue }
fun JsonObject.getInt(defaultValue: Int?, vararg keyPath: String): Int? =
    getJsonPrimitive(*keyPath).run { this?.asInt ?: defaultValue }
fun JsonObject.getShort(defaultValue: Short?, vararg keyPath: String): Short? =
    getJsonPrimitive(*keyPath).run { this?.asShort ?: defaultValue }
fun JsonObject.getByte(defaultValue: Byte?, vararg keyPath: String): Byte? =
    getJsonPrimitive(*keyPath).run { this?.asByte ?: defaultValue }
fun JsonObject.getBigInteger(defaultValue: BigInteger?, vararg keyPath: String): BigInteger? =
    getJsonPrimitive(*keyPath).run { this?.asBigInteger ?: defaultValue }
// Real
fun JsonObject.getDouble(defaultValue: Double?, vararg keyPath: String): Double? =
    getJsonPrimitive(*keyPath).run { this?.asDouble ?: defaultValue }
fun JsonObject.getFloat(defaultValue: Float?, vararg keyPath: String): Float? =
    getJsonPrimitive(*keyPath).run { this?.asFloat ?: defaultValue }
fun JsonObject.getBigDecimal(defaultValue: BigDecimal?, vararg keyPath: String): BigDecimal? =
    getJsonPrimitive(*keyPath).run { this?.asBigDecimal ?: defaultValue }
// String
fun JsonObject.getCharacter(defaultValue: Char?, vararg keyPath: String): Char? =
    getJsonPrimitive(*keyPath).run { if ((this == null) || !isString) defaultValue else asCharacter }
fun JsonObject.getString(defaultValue: String?, vararg keyPath: String): String? =
    getJsonPrimitive(*keyPath).run { if ((this == null) || !isString) defaultValue else asString }
// Other
fun JsonObject.getBoolean(defaultValue: Boolean?, vararg keyPath: String): Boolean? =
    getJsonPrimitive(*keyPath).run { if ((this == null) || !isBoolean) defaultValue else asBoolean }
fun JsonObject.getNumber(defaultValue: Number?, vararg keyPath: String): Number? =
    getJsonPrimitive(*keyPath).run { if ((this == null) || !isNumber) defaultValue else asNumber }