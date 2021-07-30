package io.github.gk0wk.violet.config;

import me.lucko.helper.config.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class ConfigUtil {
    @Nonnull
    public static ConfigurationNode setListIfNull(@NotNull ConfigurationNode node) {
        if (node.isEmpty() && !node.isList()) {
            node.setValue(new ArrayList<>());
        }
        return node;
    }
}
