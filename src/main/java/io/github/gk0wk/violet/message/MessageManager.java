package io.github.gk0wk.violet.message;

import me.lucko.helper.terminable.Terminable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.text.MessageFormat;

public class MessageManager implements Terminable {

    public static final java.util.logging.Logger consoleLogger = Bukkit.getLogger();

    private LanguageProvider languageProvider = null;

    protected String playerPrefixString = "";

    protected final String consolePrefixString;

    public MessageManager(@NotNull Plugin plugin) {
        this.consolePrefixString = "[" + plugin.getDescription().getName() + "] ";
    }

    @Nonnull
    public MessageManager setPlayerPrefix(@NotNull String prefix) {
        this.playerPrefixString = prefix;
        return this;
    }

    @Nonnull
    public MessageManager setLanguageProvider(@NotNull LanguageProvider languageProvider) {
        this.languageProvider = languageProvider;
        return this;
    }

    /**
     * 向控制台(其实是向JAVA日志)输出INFO日志
     * @param msg 要发送的消息
     */
    @Nonnull
    public MessageManager info(@NotNull String msg) {
        consoleLogger.info(consolePrefixString + msg);
        return this;
    }

    /**
     * 向控制台(其实是向JAVA日志)输出WARN日志
     * @param msg 要发送的消息
     */
    @Nonnull
    public MessageManager warn(@NotNull String msg) {
        consoleLogger.warning(consolePrefixString + msg);
        return this;
    }

    /**
     * 向控制台(其实是向JAVA日志)输出ERROR日志
     * @param msg 要发送的消息
     */
    @Nonnull
    public MessageManager error(@NotNull String msg) {
        consoleLogger.severe(consolePrefixString + msg);
        return this;
    }

    @Nonnull
    public String sprintf(boolean provider, boolean color, @NotNull String formatText, Object ...params) {
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

    @Nonnull
    public String sprintf(boolean provider, @NotNull String formatText, Object ...params) {
        return sprintf(provider, true, formatText, params);
    }

    @Nonnull
    public String sprintf(@NotNull String formatText, Object ...params) {
        return sprintf(true, true, formatText, params);
    }

    @Nonnull
    public MessageManager printf(@Nullable CommandSender sendTo, boolean prefix,
                                 boolean provider, @NotNull String formatText, Object ...params) {
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

    @Nonnull
    public MessageManager printf(boolean prefix, boolean provider, @NotNull String formatText, Object ...params) {
        return printf(null, prefix, provider, formatText, params);
    }

    @Nonnull
    public MessageManager printf(CommandSender sendTo, boolean prefix, @NotNull String formatText, Object ...params) {
        return printf(sendTo, prefix, true, formatText, params);
    }

    @Nonnull
    public MessageManager printf(CommandSender sendTo, @NotNull String formatText, Object ...params) {
        return printf(sendTo, true, true, formatText, params);
    }

    @Nonnull
    public MessageManager printf(@NotNull String formatText, Object ...params) {
        return printf(null, true, true, formatText, params);
    }

    @Override
    public void close() {

    }

}
