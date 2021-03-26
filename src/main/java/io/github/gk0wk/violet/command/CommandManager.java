package io.github.gk0wk.violet.command;

import io.github.gk0wk.violet.message.LanguageProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * 直接分担掉plugin.yml的功能
 */
@Deprecated
public class CommandManager implements CommandExecutor {
    private final JavaPlugin plugin;
    private final BiConsumer<CommandSender, String> messageSender;
    private final String prefix;
    private final ConfigurationSection commandsConfig;
    private final HashMap<String, CommandContainer> commandContainerHashMap = new HashMap<>();
    private final HashMap<String, CommandContainer> aliasCommandContainerHashMap = new HashMap<>();

    public CommandManager(JavaPlugin plugin, BiConsumer<CommandSender, String> messageSender, String prefix, FileConfiguration config) {
        this.plugin = plugin;
        this.messageSender = messageSender;
        this.prefix = prefix;
        this.commandsConfig = config.getConfigurationSection("commands");

        CommandExceptions.init(null);
        Objects.requireNonNull(plugin.getCommand(prefix)).setExecutor(this);
        register("help", null);
    }

    public CommandManager(JavaPlugin plugin, BiConsumer<CommandSender, String> messageSender, String prefix, FileConfiguration config, LanguageProvider provider) {
        this.plugin = plugin;
        this.messageSender = messageSender;
        this.prefix = prefix;
        this.commandsConfig = config.getConfigurationSection("commands");

        CommandExceptions.init(provider);
        Objects.requireNonNull(plugin.getCommand(prefix)).setExecutor(this);
        register("help", null);
    }

    public void register(String token, CommandHandler handler) {
        ConfigurationSection section = commandsConfig.getConfigurationSection(
                token.isEmpty() ? (prefix) : prefix + " " + token);
        assert section != null;

        String description = section.getString("description");
        assert description != null;
        String permissionMessage = section.getString("permission-message");
        String usage = section.getString("usage");
        assert usage != null;
        boolean console = section.getBoolean("console", true);
        boolean hidden = section.getBoolean("hidden", false);
        String permission = section.getString("permission-node");

        List<String> aliases;
        if (section.isList("aliases")) {
            aliases = section.getStringList("aliases");
        } else {
            aliases = new ArrayList<>();
            String alias = section.getString("aliases");
            if (alias != null)
                aliases.add(alias);
        }

        CommandContainer container = new CommandContainer(token, permission, usage, description,
                permissionMessage, hidden, console, aliases.toArray(new String[0]), handler);

        aliases.forEach(alias -> {
            Objects.requireNonNull(plugin.getCommand(alias)).setExecutor(this);
            aliasCommandContainerHashMap.put(alias, container);
        });

        commandContainerHashMap.put(token, container);
    }

    public void unregister(String token) {
        CommandContainer container = commandContainerHashMap.remove(token);
        if (container != null) {
            for (String alias : container.aliases) {
                aliasCommandContainerHashMap.remove(alias);
                // 好像没有办法直接注销一个命令，unregister又不敢乱用，先放着吧
            }
        }
    }

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String token;
        CommandContainer container;
        boolean isAlias;

        //是否是别名
        if (label.equals(prefix)) {
            // 不是别名
            isAlias = false;
            // 获取token
            token = (args.length == 0) ? "" : args[0];
            // 寻找对应的指令
            container = commandContainerHashMap.get(token);
        } else {
            // 是别名
            isAlias = true;
            container = aliasCommandContainerHashMap.get(label);
            token = container.token;
        }

        if (token.equals("help")) {
            printCommandHelp(sender);
            return true;
        }

        // 如果没有找到指令
        if (container == null) {
            messageSender.accept(sender, CommandExceptions.NoSuchCommandException.message);
            return true;
        }
        // 检查权限
        if (sender instanceof ConsoleCommandSender) {
            if (!container.consoleAllowable) {
                messageSender.accept(sender, CommandExceptions.RefuseConsoleException.message);
                return true;
            }
        }
        else if (container.permission != null && !sender.hasPermission(container.permission)) {
            messageSender.accept(sender, (container.permissionMessage == null) ?
                    CommandExceptions.NoPermissionException.message : container.permissionMessage);
            return true;
        }

        String[] newArgs = (args.length == 0 || isAlias) ? args : new String[args.length - 1];
        if (args.length >= 1) System.arraycopy(args, 1, newArgs, 0, args.length - 1);

        try {
            container.handler.executeCommand(sender, command, token, newArgs);
        }
        catch (Exception e) {
            if (e instanceof CommandExceptions.NoPermissionException)
                messageSender.accept(sender, CommandExceptions.NoPermissionException.message);
            else if (e instanceof CommandExceptions.BadUsageException)
                messageSender.accept(sender, MessageFormat.format(CommandExceptions.BadUsageException.message, container.usageSuggestion));
            else if (e instanceof CommandExceptions.NoSuchCommandException)
                messageSender.accept(sender, CommandExceptions.NoSuchCommandException.message);
            else if (e instanceof CommandExceptions.OnlyConsoleException)
                messageSender.accept(sender, CommandExceptions.OnlyConsoleException.message);
            else if (e instanceof CommandExceptions.PlayerOfflineException)
                messageSender.accept(sender, CommandExceptions.PlayerOfflineException.message);
            else if (e instanceof CommandExceptions.PlayerNotFountException)
                messageSender.accept(sender, CommandExceptions.PlayerNotFountException.message);
            else if (e instanceof CommandExceptions.PlayerMoreThanOneException)
                messageSender.accept(sender, CommandExceptions.PlayerMoreThanOneException.message);
            else if (e instanceof CommandExceptions.RefuseConsoleException)
                messageSender.accept(sender, CommandExceptions.RefuseConsoleException.message);
            else if (e instanceof CommandExceptions.CustomCommandException)
                messageSender.accept(sender, MessageFormat.format(CommandExceptions.CustomCommandException.message,
                        ((CommandExceptions.CustomCommandException) e).reason));
            else if (e instanceof  CommandExceptions.AccessFileErrorException)
                messageSender.accept(sender, MessageFormat.format(CommandExceptions.AccessFileErrorException.message,
                        ((CommandExceptions.AccessFileErrorException)e).who));
            else e.printStackTrace();
        }

        return true;
    }

    public void printCommandHelp(CommandSender sender) {
        messageSender.accept(sender, "NewNanPlus Commands:");
        commandContainerHashMap.forEach((token, command) -> {
            if (command.hidden)
                return;
            if (!command.consoleAllowable && sender instanceof ConsoleCommandSender)
                return;
            if (command.permission != null && sender instanceof Player && !sender.hasPermission(command.permission))
                return;
            messageSender.accept(sender, "/nnp " + command.token + " " + command.description);
            messageSender.accept(sender, "  Usage: " + command.usageSuggestion);
            StringBuilder aliasBuffer = new StringBuilder();
            Arrays.stream(command.aliases).forEach(alias -> aliasBuffer.append(alias).append(' '));
            if (aliasBuffer.length() > 0)
                messageSender.accept(sender, "  Alias: " + aliasBuffer.toString());
        });
    }

    static class CommandExceptions extends Exception {
        public static class NoPermissionException extends CommandExceptions {
            public static String message;
        }
        public static class PlayerOfflineException extends CommandExceptions {
            public static String message;
        }
        public static class PlayerNotFountException extends CommandExceptions {
            public static String message;
        }
        public static class PlayerMoreThanOneException extends CommandExceptions {
            public static String message;
        }
        public static class RefuseConsoleException extends CommandExceptions {
            public static String message;
        }
        public static class BadUsageException extends CommandExceptions {
            public static String message;
        }
        public static class NoSuchCommandException extends CommandExceptions {
            public static String message;
        }
        public static class OnlyConsoleException extends CommandExceptions {
            public static String message;
        }
        public static class AccessFileErrorException extends CommandExceptions {
            public static String message;
            public String who;
            public AccessFileErrorException(String who) { this.who = who; }
        }
        public static class CustomCommandException extends CommandExceptions {
            public String reason;
            public static String message;
            public CustomCommandException(String reason)
            {
                this.reason = reason;
            }
        }

        public static void init(LanguageProvider provider)
        {
            if (provider != null) {
                AccessFileErrorException.message = provider.provideLanguage("&c$violet-command.access-file-error$");
                BadUsageException.message = provider.provideLanguage("&c$violet-command.bad-usage$");
                CustomCommandException.message = provider.provideLanguage("&c$violet-command.custom-command-error$");
                NoSuchCommandException.message = provider.provideLanguage("&c$violet-command.no-such_command$");
                NoPermissionException.message = provider.provideLanguage("&c$violet-command.no-permission$");
                OnlyConsoleException.message = provider.provideLanguage("&c$violet-command.only-console$");
                PlayerMoreThanOneException.message = provider.provideLanguage("&c$violet-command.find-more-than-one-player$");
                PlayerNotFountException.message = provider.provideLanguage("&c$violet-command.player-not-found$");
                PlayerOfflineException.message = provider.provideLanguage("&c$violet-command.player-offline$");
                RefuseConsoleException.message = provider.provideLanguage("&c$violet-command.console-not-allowed$");
            } else {
                AccessFileErrorException.message = "&cFailed to access file: {0}";
                BadUsageException.message = "&cBad usage of this command! (Usage: {0})";
                CustomCommandException.message = "&cError: {0}";
                NoSuchCommandException.message = "&cCommand not found!";
                NoPermissionException.message = "&cSorry, but you don't have the permission to do so.";
                OnlyConsoleException.message = "&cThis command can only execute on console!";
                PlayerMoreThanOneException.message = "&cMore than one players matched!";
                PlayerNotFountException.message = "&cPlayer not found!";
                PlayerOfflineException.message = "&cPlayer is not online!";
                RefuseConsoleException.message = "&cYou cannot execute this command on console!";
            }
        }
    }
}

