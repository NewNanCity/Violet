package city.newnan.violet.i18n

import city.newnan.violet.config.ConfigManager.UnknownConfigFileFormatException
import city.newnan.violet.config.loadConfigurationTree
import me.lucko.helper.config.ConfigurationNode
import java.io.File
import java.io.IOException
import java.util.*
import javax.annotation.Nonnull


class Language(private val languageFile: File, @get:Nonnull val language: Locale) {
    private var languageRoot: ConfigurationNode? = null
    private val pathCache: MutableMap<String, String?> = HashMap()

    init {
        reload()
    }

    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    fun reload() {
        languageRoot = languageFile.loadConfigurationTree()
        pathCache.clear()
    }

    fun getNodeString(path: String): String? {
        var str = pathCache[path]
        if (str == null) {
            str = languageRoot!!.getNode(*path.split("\\.").toTypedArray()).string
            pathCache[path] = str
        }
        return str
    }
}