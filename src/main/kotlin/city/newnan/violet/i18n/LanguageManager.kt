package city.newnan.violet.i18n

import city.newnan.violet.config.ConfigManager.UnknownConfigFileFormatException
import city.newnan.violet.message.LanguageProvider
import me.lucko.helper.terminable.Terminable
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

private val pattern = Pattern.compile("[$][^$]+[$]")

class LanguageManager(private val plugin: Plugin) : LanguageProvider, Terminable {
    var majorLanguage: Language? = null
        private set
    var defaultLanguage: Language? = null
        private set
    private val languageMap: MutableMap<Locale, Language> = HashMap()

    @Throws(FileNotFoundException::class, IOException::class, UnknownConfigFileFormatException::class)
    fun register(locale: Locale, filePath: String): LanguageManager {
        val file = File(plugin.dataFolder, filePath)
        if (file.exists() || file.isFile) {
            languageMap[locale] = Language(file, locale)
        } else {
            throw FileNotFoundException(filePath)
        }
        return this
    }

    infix fun unregister(locale: Locale): LanguageManager = this.also { languageMap.remove(locale) }

    infix fun getLanguage(locale: Locale): Language? = languageMap[locale]

    infix fun setMajorLanguage(locale: Locale): LanguageManager = this.also { languageMap[locale]?.run { majorLanguage = this } }

    fun guessMajorLanguage() {
        Locale.getDefault().run {
            if (languageMap.containsKey(this)) {
                setMajorLanguage(this)
            }
        }
    }

    infix fun setDefaultLanguage(locale: Locale): LanguageManager = this.also { languageMap[locale]?.run {defaultLanguage = this  } }

    fun reloadAll(): LanguageManager {
        languageMap.forEach { (locale: Locale?, language: Language) ->
            try {
                language.reload()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: UnknownConfigFileFormatException) {
                e.printStackTrace()
            }
        }
        return this
    }

    override infix fun provideLanguage(rawText: String): String {
        var rawTextShadow = rawText
        val matcher = pattern.matcher(rawTextShadow)
        while (matcher.find()) {
            val key = matcher.group(0)
            val path = key.replace("$", "")
            var replaced: String? = null
            if (majorLanguage == null || majorLanguage!!.getNodeString(path).also { replaced = it } == null) {
                if (defaultLanguage == null || defaultLanguage!!.getNodeString(path)
                        .also { replaced = it } == null
                ) continue
            }
            rawTextShadow = rawTextShadow.replace(key, replaced!!)
        }
        return rawTextShadow.replace("[$][$]".toRegex(), "$")
    }

    override fun close() = languageMap.clear()

    class FileNotFoundException(path: String) : Exception("File not found: $path")
}