package io.github.gk0wk.violet.i18n;

import io.github.gk0wk.violet.config.ConfigManager;
import me.lucko.helper.config.ConfigFactory;
import me.lucko.helper.config.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Language {
    private ConfigurationNode languageRoot;
    private final File languageFile;
    private final Locale language;
    private final Map<String, String> pathCache = new HashMap<>();

    public Language(File languageFile, Locale language) throws IOException, ConfigManager.UnknownConfigFileFormatException {
        this.languageFile = languageFile;
        this.language = language;
        reload();
    }

    public Locale getLanguage() {return language;}

    public void reload() throws IOException, ConfigManager.UnknownConfigFileFormatException {
        String[] splits = languageFile.getPath().split("\\.");
        switch (splits[splits.length - 1].toUpperCase()) {
            case "YML":
            case "YAML":
                languageRoot = ConfigFactory.yaml().loader(languageFile).load();
                break;
            case "JSON":
                languageRoot = ConfigFactory.gson().loader(languageFile).load();
                break;
            case "CONF":
                languageRoot = ConfigFactory.hocon().loader(languageFile).load();
                break;
            default:
                throw new ConfigManager.UnknownConfigFileFormatException(languageFile.getCanonicalPath());
        }
    }

    public String getNodeString(String path) {
        String str = pathCache.get(path);
        if (str == null) {
            str = languageRoot.getNode((Object[]) path.split("\\.")).getString();
            pathCache.put(path, str);
        }
        return str;
    }
}
