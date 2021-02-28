package city.newnan.api.config;

import me.lucko.helper.config.ConfigurationNode;
import me.lucko.helper.config.ValueType;

import java.util.ArrayList;

public class ConfigUtil {
    public static ConfigurationNode setListIfNull(ConfigurationNode node) {
        if (node != null && node.getValueType() == ValueType.NULL) {
            node.setValue(new ArrayList<>());
        }
        return node;
    }
}
