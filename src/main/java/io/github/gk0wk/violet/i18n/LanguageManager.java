package io.github.gk0wk.violet.i18n;

import io.github.gk0wk.violet.config.ConfigManager;
import io.github.gk0wk.violet.message.LanguageProvider;
import me.lucko.helper.terminable.Terminable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager implements LanguageProvider, Terminable {
    private Language majorLanguage;
    private Language defaultLanguage;
    private final Map<Locale, Language> languageMap = new HashMap<>();
    private final Plugin plugin;

    public LanguageManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    public LanguageManager register(@NotNull Locale locale, @NotNull String filePath)
            throws FileNotFoundException, IOException, ConfigManager.UnknownConfigFileFormatException {
        File file = new File(plugin.getDataFolder(), filePath);
        if (file.exists() || file.isFile()) {
            Language language = new Language(file, locale);
            languageMap.put(locale, language);
        } else {
            throw new FileNotFoundException(filePath);
        }
        return this;
    }

    @Nonnull
    public LanguageManager unregister(@NotNull Locale locale) {
        languageMap.remove(locale);
        return this;
    }

    @Nullable
    public Language getLanguage(@NotNull Locale locale) {
        return languageMap.get(locale);
    }

    @Nonnull
    public LanguageManager setMajorLanguage(@NotNull Locale locale) {
        Language language = languageMap.get(locale);
        if (language != null) {
            majorLanguage = language;
        }
        return this;
    }

    @Nullable
    public Language getMajorLanguage() {
        return majorLanguage;
    }

    public void guessMajorLanguage() {
        Locale locale = Locale.getDefault();
        if (languageMap.containsKey(locale)) {
            setMajorLanguage(locale);
        }
    }

    @Nonnull
    public LanguageManager setDefaultLanguage(@NotNull Locale locale) {
        Language language = languageMap.get(locale);
        if (language != null) {
            defaultLanguage = language;
        }
        return this;
    }

    @Nullable
    public Language getDefaultLanguage() {
        return defaultLanguage;
    }

    @Nonnull
    public LanguageManager reloadAll() {
        languageMap.forEach((locale, language) -> {
            try {
                language.reload();
            } catch (IOException | ConfigManager.UnknownConfigFileFormatException e) {
                e.printStackTrace();
            }
        });
        return this;
    }

    private static final Pattern pattern = Pattern.compile("[$][^$]+[$]");

    @Override
    @Nonnull
    public String provideLanguage(@NotNull String rawText) {
        Matcher matcher = pattern.matcher(rawText);
        while (matcher.find()) {
            String key = matcher.group(0);
            String path = key.replace("$", "");
            String replaced;
            if (majorLanguage == null || (replaced = majorLanguage.getNodeString(path)) == null) {
                if (defaultLanguage == null || (replaced = defaultLanguage.getNodeString(path)) == null)
                    continue;
            }
            rawText = rawText.replace(key, replaced);
        }

        return rawText.replaceAll("[$][$]", "$");
    }

    @Override
    public void close() {
        languageMap.clear();
    }

    public static class FileNotFoundException extends Exception {
        public FileNotFoundException(@NotNull String path) {
            super("File not found: " + path);
        }
    }
}
