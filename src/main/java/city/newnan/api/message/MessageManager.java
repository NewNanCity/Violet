package city.newnan.api.message;

import me.lucko.helper.terminable.Terminable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

import java.text.MessageFormat;

public class MessageManager implements Terminable {

    public static final java.util.logging.Logger consoleLogger = Bukkit.getLogger();

    private LanguageProvider languageProvider = null;

    protected String playerPrefixString = "";

    protected final String consolePrefixString;

    public MessageManager(Plugin plugin) {
        this.consolePrefixString = "[" + plugin.getDescription().getName() + "] ";
    }

    public MessageManager setPlayerPrefix(String prefix) {
        this.playerPrefixString = prefix;
        return this;
    }

    public MessageManager setLanguageProvider(LanguageProvider languageProvider) {
        this.languageProvider = languageProvider;
        return this;
    }

    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    public MessageManager info(String msg) {
        consoleLogger.info(consolePrefixString + msg);
        return this;
    }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    public MessageManager warn(String msg) {
        consoleLogger.warning(consolePrefixString + msg);
        return this;
    }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    public MessageManager error(String msg) {
        consoleLogger.severe(consolePrefixString + msg);
        return this;
    }

    public String sprintf(boolean provider, boolean color, String formatText, Object ...params) {
        // 语言映射处理
        if (provider && languageProvider != null) {
            formatText = languageProvider.provideLanguage(formatText);
        }
        // 数据格式化
        formatText = MessageFormat.format(formatText, params);
        if (color) {
            formatText = ChatColor.translateAlternateColorCodes('&', formatText);
        }
        return formatText;
    }

    public String sprintf(boolean provider, String formatText, Object ...params) {
        return sprintf(provider, true, formatText, params);
    }

    public String sprintf(String formatText, Object ...params) {
        return sprintf(true, true, formatText, params);
    }

    public MessageManager printf(CommandSender sendTo, boolean prefix, boolean provider, String formatText, Object ...params) {
        // 语言映射处理
        if (provider && languageProvider != null) {
            formatText = languageProvider.provideLanguage(formatText);
        }
        // 数据格式化
        formatText = MessageFormat.format(formatText, params);
        // 分对象输出
        if (sendTo == null || sendTo instanceof ConsoleCommandSender) {
            // 前缀添加
            if (prefix) {
                formatText = consolePrefixString + formatText;
            }
            // 样式码转换+输出
            consoleLogger.info(ChatColor.translateAlternateColorCodes('&', formatText));
        } else {
            // 前缀添加
            if (prefix) {
                formatText = playerPrefixString + formatText;
            }
            sendTo.sendMessage(ChatColor.translateAlternateColorCodes('&', formatText));
        }
        return this;
    }

    public MessageManager printf(boolean prefix, boolean provider, String formatText, Object ...params) {
        return printf(null, prefix, provider, formatText, params);
    }

    public MessageManager printf(CommandSender sendTo, boolean prefix, String formatText, Object ...params) {
        return printf(sendTo, prefix, true, formatText, params);
    }

    public MessageManager printf(CommandSender sendTo, String formatText, Object ...params) {
        return printf(sendTo, true, true, formatText, params);
    }

    public MessageManager printf(String formatText, Object ...params) {
        return printf(null, true, true, formatText, params);
    }

    @Override
    public void close() {

    }

    public interface LanguageProvider {
        String provideLanguage(String rawText);
    }
}
