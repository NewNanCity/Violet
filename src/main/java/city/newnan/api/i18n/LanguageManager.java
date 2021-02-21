package city.newnan.api.i18n;

import city.newnan.api.config.ConfigManager;
import city.newnan.api.message.MessageManager;
import me.lucko.helper.terminable.Terminable;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager implements MessageManager.LanguageProvider, Terminable {

    private Language majorLanguage;
    private Language fallbackLanguage;
    private final Map<Locale, Language> languageMap = new HashMap<>();
    private final Plugin plugin;

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public LanguageManager register(Locale locale, String filePath) throws FileNotFoundException, IOException, ConfigManager.UnknownConfigFileFormatException {
        File file = new File(plugin.getDataFolder(), filePath);
        if (file.exists() || file.isFile()) {
            Language language = new Language(file, locale);
            languageMap.put(locale, language);
            if (majorLanguage == null) {
                majorLanguage = language;
            }
            else if (fallbackLanguage == null) {
                fallbackLanguage = language;
            }
        } else {
            throw new FileNotFoundException(filePath);
        }
        return this;
    }

    public LanguageManager unregister(Locale locale) {
        languageMap.remove(locale);
        return this;
    }

    public Language getLanguage(Locale locale) {
        return languageMap.get(locale);
    }

    public LanguageManager setMajorLanguage(Locale locale) {
        Language language = languageMap.get(locale);
        if (language != null) {
            majorLanguage = language;
        }
        return this;
    }

    public LanguageManager setFallbackLanguage(Locale locale) {
        Language language = languageMap.get(locale);
        if (language != null) {
            fallbackLanguage = language;
        }
        return this;
    }

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

    private static final Pattern pattern = Pattern.compile("[$]([^$]+?)[$]");

    @Override
    public String provideLanguage(String rawText) {
        Matcher matcher = pattern.matcher(rawText);
        while (matcher.find()) {
            String key = matcher.group(0);
            String path = key.replace("$", "");
            String replaced;
            if (majorLanguage == null || (replaced = majorLanguage.getNodeString(path)) == null) {
                if (fallbackLanguage == null || (replaced = fallbackLanguage.getNodeString(path)) == null)
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
        public FileNotFoundException(String path) {
            super("File not found: " + path);
        }
    }
}
