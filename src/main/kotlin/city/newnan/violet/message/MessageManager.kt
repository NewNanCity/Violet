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
    internal var playerPrefixString = ""
    internal val consolePrefixString: String

    /**
     * 调试模式，默认为`false`
     */
    var debugMode: Boolean = false

    init {
        consolePrefixString = "[${plugin.description.name}] "
    }

    /**
     * 设置向玩家输出信息时的前缀
     * @param prefix 前缀字符串
     */
    infix fun setPlayerPrefix(prefix: String): MessageManager = this.also { playerPrefixString = prefix }

    /**
     * 设置多语言服务提供者
     * @param languageProvider 多语言服务提供者，需实现`LanguageProvider`接口
     */
    infix fun setLanguageProvider(languageProvider: LanguageProvider): MessageManager =
        this.also { this.languageProvider = languageProvider }

    /**
     * 向控制台(其实是向JAVA日志)输出调试日志，只有`debugMode`为`true`时才会输出
     * @param msg 要发送的消息
     */
    infix fun debug(msg: String): MessageManager =
        this.also { if (debugMode) consoleLogger.info("$consolePrefixString$msg") }

    /**
     * 向控制台(其实是向JAVA日志)输出调试日志，只有`debugMode`为`true`时才会输出
     * @param msg 要发送的消息
     */
    infix fun debug(msg: (MessageManager) -> String) =
        this.also { if (debugMode) consoleLogger.info("$consolePrefixString${msg(this)}") }

    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    infix fun info(msg: String): MessageManager = this.also { consoleLogger.info("$consolePrefixString$msg") }

    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    infix fun info(msg: (MessageManager) -> String) =
        this.also { consoleLogger.info("$consolePrefixString${msg(this)}") }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    infix fun warn(msg: String): MessageManager = this.also { consoleLogger.warning("$consolePrefixString$msg") }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    infix fun warn(msg: (MessageManager) -> String) =
        this.also { consoleLogger.warning("$consolePrefixString${msg(this)}") }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    infix fun error(msg: String): MessageManager = this.also { consoleLogger.severe("$consolePrefixString$msg") }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    infix fun error(msg: (MessageManager) -> String) =
        this.also { consoleLogger.severe("$consolePrefixString${msg(this)}") }

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