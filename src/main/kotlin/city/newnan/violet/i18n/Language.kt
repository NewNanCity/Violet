package city.newnan.violet.i18n

import city.newnan.violet.config.ConfigManager2
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import java.io.IOException
import java.util.*
import javax.annotation.Nonnull


class Language(private val languageFile: File, @get:Nonnull val language: Locale) {
    private val languageNodes = mutableMapOf<String, String>()

    init {
        reload()
    }

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
        visit(ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json].readTree(languageFile) as ObjectNode)
    }

    fun getNodeString(path: String): String? {
        return languageNodes[path]
    }
}