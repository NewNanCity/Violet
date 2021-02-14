package city.newnan.api.config;

import me.lucko.helper.Schedulers;
import me.lucko.helper.config.ConfigFactory;
import me.lucko.helper.config.ConfigurationNode;
import me.lucko.helper.scheduler.Task;
import me.lucko.helper.terminable.Terminable;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;

public class ConfigManager implements Terminable {
    /**
     * 绑定的插件实例
     */
    protected final Plugin plugin;

    /**
     * 配置文件缓冲
     */
    protected final HashMap<String, ConfigurationNode> configMap = new HashMap<>();

    /**
     * 配置文件访问时间戳
     */
    protected final HashMap<String, Long> configTimestampMap = new HashMap<>();

    /**
     * 持久化保存的配置文件
     */
    protected final HashSet<String> persistentConfigSet = new HashSet<String>() {{
        add("config.yml");
    }};

    protected Task cleanTask;

    /**
     * 构造函数
     * @param plugin 要绑定的插件
     */
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 启动周期性的配置缓存清理
     * @return ConfigManager实例
     */
    public ConfigManager startCleanService() {
        if (cleanTask == null) {
            // 自动卸载长时间未使用的配置文件
            this.cleanTask = Schedulers.sync().runRepeating(task -> {
                long outdatedTime = System.currentTimeMillis() - 1800000;
                List<String> outdatedConfigFiles = new ArrayList<>();
                configTimestampMap.forEach((config, time) -> {
                    if (time <= outdatedTime && !persistentConfigSet.contains(config)) {
                        outdatedConfigFiles.add(config);
                    }
                });
                outdatedConfigFiles.forEach(config -> {
                    try {
                        unload(config, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }, 36000L, 36000L);
        }
        return this;
    }

    /**
     * 关闭周期性的配置缓存清理
     * @return ConfigManager实例
     */
    public ConfigManager stopCleanService() {
        if (cleanTask != null) {
            cleanTask.close();
        }
        return this;
    }

    /**
     * 检查这个配置文件是否存在，不存在就创建
     * @param configFile 配置文件路径
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     */
    public boolean touch(String configFile) {
        // 不存在就创建
        if (!new File(plugin.getDataFolder(), configFile).exists()) {
            try {
                plugin.saveResource(configFile, false);
            } catch (IllegalArgumentException e) {
                // jar包中没有这个资源文件
                return false;
            }
            return false;
        }
        return true;
    }

    /**
     * 检查这个资源文件是否存在，如果不存在就从指定的模板复制一份
     * @param targetFile 要检查的配置文件路径
     * @param templateFile 模板配置文件路径
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     * @throws IOException 文件读写出错
     */
    public boolean touchOrCopyTemplate(String targetFile, String templateFile) throws IOException {
        File file = new File(plugin.getDataFolder(), targetFile);
        // 如果文件不存在
        if (!file.exists()) {
            // 检查父目录
            if (!file.getParentFile().exists()) {
                boolean result = file.getParentFile().mkdirs();
            }

            // 创建文件
            boolean result = file.createNewFile();

            // 拷贝内容
            BufferedInputStream input = new BufferedInputStream(Objects.requireNonNull(plugin.getResource(templateFile)));
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
            int len;
            byte[] bytes = new byte[1024];
            while ((len = input.read(bytes)) != -1) {
                output.write(bytes, 0, len);
            }
            input.close();
            output.close();
            return false;
        }
        return true;
    }

    /**
     * 获取某个配置文件(支持YAML,JSON和HOCON)，如果不存在就加载默认
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    public ConfigurationNode get(String configFile) throws IOException, UnknownConfigFileFormatException {
        // 已缓存则返回
        if (this.configMap.containsKey(configFile))
            return this.configMap.get(configFile);
        // 未缓存则加载
        touch(configFile);

        // 读取配置文件
        ConfigurationNode config;
        switch (getType(configFile)) {
            case YAML: {
                config = ConfigFactory.yaml().loader((new File(plugin.getDataFolder(), configFile))).load();
                break;
            }
            case JSON: {
                config = ConfigFactory.gson().loader((new File(plugin.getDataFolder(), configFile))).load();
                break;
            }
            case HOCON: {
                config = ConfigFactory.hocon().loader((new File(plugin.getDataFolder(), configFile))).load();
                break;
            }
            default: {
                throw new UnknownConfigFileFormatException(configFile);
            }
        }

        // 添加缓存和时间戳
        this.configMap.put(configFile, config);
        this.configTimestampMap.put(configFile, System.currentTimeMillis());
        return config;
    }

    protected static ConfigFileType getType(String filePath) {
        String[] splits = ".".split(filePath);
        switch (splits[splits.length - 1].toUpperCase()) {
            case "YML":
            case "YAML":
                return ConfigFileType.YAML;
            case "JSON":
                return ConfigFileType.JSON;
            case "CONF":
                return ConfigFileType.HOCON;
        }
        return ConfigFileType.UNKNOWN;
    }

    protected enum ConfigFileType {YAML, JSON, HOCON, UNKNOWN}

    /**
     * 获取某个配置文件(支持YAML,JSON和HOCON)，如果不存在就从指定的模板复制一份
     * @param targetFile 要获取的配置文件路径
     * @param templateFile 模板配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    public ConfigurationNode getOrCopyTemplate(String targetFile, String templateFile) throws IOException, UnknownConfigFileFormatException {
        // 已缓存则返回
        if (this.configMap.containsKey(targetFile))
            return this.configMap.get(targetFile);
        // 未缓存则加载
        touchOrCopyTemplate(targetFile, templateFile);
        return get(targetFile);
    }

    /**
     * 卸载/保存某个配置文件
     * @param configFile 配置文件路径
     * @param unload 是否从缓存中卸载
     * @param save 是否保存
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    protected ConfigurationNode saveOrUnload(String configFile, boolean unload, boolean save) throws IOException, UnknownConfigFileFormatException {
        ConfigurationNode config;
        if (unload) {
            config = this.configMap.remove(configFile);
            this.configTimestampMap.remove(configFile);
            this.persistentConfigSet.remove(configFile);
        } else {
            config = this.configMap.get(configFile);
        }

        if (config != null && save) {
            switch (getType(configFile)) {
                case YAML: {
                    ConfigFactory.yaml().loader((new File(plugin.getDataFolder(), configFile))).save(config);
                    break;
                }
                case JSON: {
                    ConfigFactory.gson().loader((new File(plugin.getDataFolder(), configFile))).save(config);
                    break;
                }
                case HOCON: {
                    ConfigFactory.hocon().loader((new File(plugin.getDataFolder(), configFile))).save(config);
                    break;
                }
                default: {
                    throw new UnknownConfigFileFormatException(configFile);
                }
            }
            // 更新时间戳
            if (!unload) {
                this.configTimestampMap.put(configFile, System.currentTimeMillis());
            }
        }
        return config;
    }

    /**
     * 保存某个配置文件，如果之前没有加载过且磁盘中不存在，则不会有任何动作
     * @param configFile 配置文件路径
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    public ConfigurationNode save(String configFile) throws IOException, UnknownConfigFileFormatException {
        return saveOrUnload(configFile, false, true);
    }

    /**
     * 卸载配置文件，并选择是否保存配置文件
     * @param configFile 配置文件路径
     * @param save 是否保存内存中的配置文件信息到硬盘
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    public ConfigurationNode unload(String configFile, boolean save) throws IOException, UnknownConfigFileFormatException {
        return saveOrUnload(configFile, true, save);
    }

    /**
     * 放弃内存中的配置，从磁盘重新加载，如果磁盘中不存在就会加载默认配置
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     *  @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    public ConfigurationNode reload(String configFile) throws IOException, UnknownConfigFileFormatException {
        unload(configFile, false);
        return get(configFile);
    }

    /**
     * 重置配置文件，恢复为默认配置文件
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    public ConfigurationNode reset(String configFile) throws IOException, UnknownConfigFileFormatException {
        plugin.saveResource(configFile, true);
        return reload(configFile);
    }

    /**
     * 保存所有配置文件
     */
    public void saveAll() {
        configMap.forEach((name, config) -> {
            try {
                save(name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 将某个文件设置为持久化保存的，即不会因为长久未访问就从内存中卸载
     * @param configFile 要持久化保存的配置文件路径
     */
    public void setPersistent(String configFile) {
        persistentConfigSet.add(configFile);
    }

    /**
     * 取消持久化保存
     * @param configFile 要取消持久化保存的配置文件路径
     */
    public void unsetPersistent(String configFile) {
        persistentConfigSet.remove(configFile);
    }

    @Override
    public void close() {
        stopCleanService();
        saveAll();
        configMap.clear();
        configTimestampMap.clear();
        persistentConfigSet.clear();
    }

    /**
     * 未知的配置文件格式异常
     */
    public static class UnknownConfigFileFormatException extends Exception {
        public UnknownConfigFileFormatException(String fileName) {
            super("Unknown Config File Format: " + fileName);
        }
    }
}