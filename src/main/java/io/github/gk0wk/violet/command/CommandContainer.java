package io.github.gk0wk.violet.command;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * 同时也可以解析plugin.yml的内容
 */
@Deprecated
class CommandContainer {
    public final String token;
    public final String permission;
    public final String usageSuggestion;
    public final String permissionMessage;
    public final String description;
    public final String[] aliases;
    public final boolean hidden;
    public final boolean consoleAllowable;
    public final CommandHandler handler;

    public CommandContainer(@NotNull String token, @Nullable String permission, @NotNull String usageSuggestion,
                            @NotNull String description, @Nullable String permissionMessage, boolean hidden,
                            boolean consoleAllowable, @Nullable String[] aliases, @Nullable CommandHandler handler) {
        this.token = token;
        this.permission = permission;
        this.usageSuggestion = usageSuggestion;
        this.permissionMessage = permissionMessage;
        this.description = description;
        this.aliases = aliases;
        this.hidden = hidden;
        this.consoleAllowable = consoleAllowable;
        this.handler = handler;
    }
}
