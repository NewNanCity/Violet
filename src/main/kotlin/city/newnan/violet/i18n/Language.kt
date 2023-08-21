package city.newnan.violet.i18n

import city.newnan.violet.config.ConfigManager2
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import java.io.IOException
import java.util.*


class Language(val language: Locale, private val getObjectNode: () -> ObjectNode) {
    private val languageNodes = mutableMapOf<String, String>()

    constructor(language: Locale, languageFile: File, type: ConfigManager2.ConfigFileType? = null)
        : this(language, { ConfigManager2.parse<ObjectNode>(languageFile, type) })

    init { reload() }

    @Throws(IOException::class, ConfigManager2.UnknownConfigFileFormatException::class)
    fun reload() {
        languageNodes.clear()
        fun visit(node: ObjectNode, path: String = "") {
            for ((key, value) in node.fields()) {
                val newPath = if (path.isEmpty()) key else "$path.$key"
                if (value.isObject) {
                    visit(value as ObjectNode, newPath)
                } else {
                    languageNodes[newPath] = value.asText()
                }
            }
        }
        visit(getObjectNode())
    }

    fun getNodeString(path: String): String? {
        return languageNodes[path]
    }
}