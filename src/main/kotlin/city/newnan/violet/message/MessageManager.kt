package city.newnan.violet.message

import me.lucko.helper.terminable.Terminable
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.plugin.Plugin
import java.text.MessageFormat

private val consoleLogger = Bukkit.getLogger()

class MessageManager(plugin: Plugin) : Terminable {
    init { if (plugin is TerminableConsumer) bindWith(plugin) }

    private var languageProvider: LanguageProvider? = null
    protected var playerPrefixString = ""
    protected val consolePrefixString: String

    init {
        consolePrefixString = "[${plugin.description.name}] "
    }

    infix fun setPlayerPrefix(prefix: String): MessageManager = this.also { playerPrefixString = prefix }

    infix fun setLanguageProvider(languageProvider: LanguageProvider): MessageManager =
        this.also { this.languageProvider = languageProvider }

    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    infix fun info(msg: String): MessageManager = this.also { consoleLogger.info("$consolePrefixString$msg") }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    infix fun warn(msg: String): MessageManager = this.also { consoleLogger.warning("$consolePrefixString$msg") }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    infix fun error(msg: String): MessageManager = this.also { consoleLogger.severe("$consolePrefixString$msg") }

    fun sprintf(provider: Boolean, color: Boolean, formatText: String, vararg params: Any?): String {
        // 语言映射处理
        var formatTextShadow = formatText
        if (provider && languageProvider != null) {
            formatTextShadow = languageProvider!!.provideLanguage(formatTextShadow)!!
        }
        // 数据格式化
        formatTextShadow = MessageFormat.format(formatTextShadow, *params)
        if (color) {
            formatTextShadow = ChatColor.translateAlternateColorCodes('&', formatTextShadow)
        }
        return formatTextShadow
    }

    fun sprintf(provider: Boolean, formatText: String, vararg params: Any?): String =
        sprintf(provider, true, formatText, *params)

    fun sprintf(formatText: String, vararg params: Any?): String =
        sprintf(provider=true, color=true, formatText, *params)

    fun printf(
        sendTo: CommandSender?, prefix: Boolean,
        provider: Boolean, formatText: String, vararg params: Any?
    ): MessageManager {
        // 语言映射处理
        var formatTextShadow = formatText
        if (provider && languageProvider != null) {
            formatTextShadow = languageProvider!!.provideLanguage(formatTextShadow)!!
        }
        // 数据格式化
        formatTextShadow = MessageFormat.format(formatTextShadow, *params)
        // 分对象输出
        if (sendTo == null || sendTo is ConsoleCommandSender) {
            // 前缀添加
            if (prefix) {
                formatTextShadow = consolePrefixString + formatTextShadow
            }
            // 样式码转换+输出
            consoleLogger.info(ChatColor.translateAlternateColorCodes('&', formatTextShadow))
        } else {
            // 前缀添加
            if (prefix) {
                formatTextShadow = playerPrefixString + formatTextShadow
            }
            sendTo.sendMessage(ChatColor.translateAlternateColorCodes('&', formatTextShadow))
        }
        return this
    }

    fun printf(prefix: Boolean, provider: Boolean, formatText: String, vararg params: Any?): MessageManager =
        printf(sendTo=null, prefix=prefix, provider=provider, formatText=formatText, *params)

    fun printf(sendTo: CommandSender?, prefix: Boolean, formatText: String, vararg params: Any?): MessageManager =
        printf(sendTo=sendTo, prefix=prefix, provider=true, formatText=formatText, *params)

    fun printf(sendTo: CommandSender?, formatText: String, vararg params: Any?): MessageManager =
        printf(sendTo=sendTo, prefix=true, provider=true, formatText=formatText, *params)

    fun printf(formatText: String, vararg params: Any?): MessageManager =
        printf(sendTo=null, prefix=true, provider=true, formatText=formatText, *params)

    override fun close() {}
}