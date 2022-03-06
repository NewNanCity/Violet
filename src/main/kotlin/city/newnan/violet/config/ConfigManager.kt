package city.newnan.violet.config

import me.lucko.helper.Schedulers
import me.lucko.helper.config.ConfigFactory
import me.lucko.helper.config.ConfigurationNode
import me.lucko.helper.scheduler.Task
import me.lucko.helper.terminable.Terminable
import org.bukkit.plugin.Plugin
import java.io.*
import java.util.*
import java.util.function.Consumer


fun String.getConfigFileType(): ConfigManager.ConfigFileType {
    val splits = split("\\.").toTypedArray()
    when (splits.last().uppercase(Locale.getDefault())) {
        "YML", "YAML" -> return ConfigManager.ConfigFileType.YAML
        "JSON" -> return ConfigManager.ConfigFileType.JSON
        "CONF" -> return ConfigManager.ConfigFileType.HOCON
    }
    return ConfigManager.ConfigFileType.UNKNOWN
}

fun ConfigurationNode.setListIfNull(): ConfigurationNode =
    this.also {
        if (isEmpty && !isList) {
            value = ArrayList<Any>()
        }
    }

fun File.loadConfigurationTree(): ConfigurationNode = when (path.getConfigFileType()) {
    ConfigManager.ConfigFileType.YAML -> {
        ConfigFactory.yaml().loader(this).load()
    }
    ConfigManager.ConfigFileType.JSON -> {
        ConfigFactory.gson().loader(this).load()
    }
    ConfigManager.ConfigFileType.HOCON -> {
        ConfigFactory.hocon().loader(this).load()
    }
    else -> {
        throw ConfigManager.UnknownConfigFileFormatException(path)
    }
}

fun File.saveConfigurationTree(config: ConfigurationNode): Unit = when (path.getConfigFileType()) {
    ConfigManager.ConfigFileType.YAML -> {
        ConfigFactory.yaml().loader(this).save(config)
    }
    ConfigManager.ConfigFileType.JSON -> {
        ConfigFactory.gson().loader(this).save(config)
    }
    ConfigManager.ConfigFileType.HOCON -> {
        ConfigFactory.hocon().loader(this).save(config)
    }
    else -> {
        throw ConfigManager.UnknownConfigFileFormatException(path)
    }
}

open class ConfigManager
/**
 * 构造函数
 * @param plugin 要绑定的插件
 */(
    /**
     * 绑定的插件实例
     */
    private val plugin: Plugin
) : Terminable {
    /**
     * 配置文件缓冲
     */
    private val configMap = HashMap<String, ConfigurationNode>()

    /**
     * 配置文件访问时间戳
     */
    private val configTimestampMap = HashMap<String, Long>()

    /**
     * 持久化保存的配置文件
     */
    private val persistentConfigSet: HashSet<String> = hashSetOf("config.yml")

    private var cleanTask: Task? = null

    /**
     * 启动周期性的配置缓存清理
     * @return ConfigManager实例
     */
    fun startCleanService(): ConfigManager {
        if (cleanTask == null) {
            // 自动卸载长时间未使用的配置文件
            cleanTask = Schedulers.sync().runRepeating({ _: Task? ->
                val outdatedTime = System.currentTimeMillis() - 1800000
                val outdatedConfigFiles: MutableList<String> = ArrayList()
                configTimestampMap.forEach { (config: String, time: Long) ->
                    if (time <= outdatedTime && !persistentConfigSet.contains(config)) {
                        outdatedConfigFiles.add(config)
                    }
                }
                outdatedConfigFiles.forEach(Consumer { config: String ->
                    try {
                        unload(config, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
            }, 36000L, 36000L)
        }
        return this
    }

    /**
     * 关闭周期性的配置缓存清理
     * @return ConfigManager实例
     */
    open fun stopCleanService(): ConfigManager = this.also { cleanTask?.close() }

    /**
     * 检查这个配置文件是否存在，不存在就创建
     * @param configFile 配置文件路径
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     */
    open infix fun touch(configFile: String): Boolean {
        // 不存在就创建
        if (!File(plugin.dataFolder, configFile).exists()) {
            try {
                plugin.saveResource(configFile, false)
            } catch (e: IllegalArgumentException) {
                // jar包中没有这个资源文件
            }
            return false
        }
        return true
    }

    /**
     * 检查这个资源文件是否存在，如果不存在就从指定的模板复制一份
     * @param targetFile 要检查的配置文件路径
     * @param templateFile 模板配置文件路径
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     * @throws IOException 文件读写出错
     */
    @Throws(IOException::class)
    open fun touchOrCopyTemplate(targetFile: String, templateFile: String): Boolean {
        val file = File(plugin.dataFolder, targetFile)
        // 如果文件不存在
        if (!file.exists()) {
            // 检查父目录
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }

            // 创建文件
            file.createNewFile()

            // 拷贝内容
            val input = BufferedInputStream(plugin.getResource(templateFile) ?: return true)
            val output = BufferedOutputStream(FileOutputStream(file))
            var len: Int
            val bytes = ByteArray(1024)
            while (input.read(bytes).also { len = it } != -1) {
                output.write(bytes, 0, len)
            }
            input.close()
            output.close()
            return false
        }
        return true
    }

    /**
     * 获取某个配置文件(支持YAML,JSON和HOCON)，如果不存在就加载默认
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    infix operator fun get(configFile: String): ConfigurationNode? {
        // 已缓存则返回
        if (configMap.containsKey(configFile)) return configMap[configFile]
        // 未缓存则加载
        touch(configFile)

        // 读取配置文件
        val config: ConfigurationNode = File(plugin.dataFolder, configFile).loadConfigurationTree()

        // 添加缓存和时间戳
        configMap[configFile] = config
        configTimestampMap[configFile] = System.currentTimeMillis()
        return config
    }

    enum class ConfigFileType {
        YAML, JSON, HOCON, UNKNOWN
    }

    /**
     * 获取某个配置文件(支持YAML,JSON和HOCON)，如果不存在就从指定的模板复制一份
     * @param targetFile 要获取的配置文件路径
     * @param templateFile 模板配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    fun getOrCopyTemplate(targetFile: String, templateFile: String): ConfigurationNode? {
        // 已缓存则返回
        if (configMap.containsKey(targetFile)) return configMap[targetFile]
        // 未缓存则加载
        touchOrCopyTemplate(targetFile, templateFile)
        return get(targetFile)
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
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    protected fun saveOrUnload(configFile: String, unload: Boolean, save: Boolean): ConfigurationNode? {
        val config: ConfigurationNode?
        if (unload) {
            config = configMap.remove(configFile)
            configTimestampMap.remove(configFile)
            persistentConfigSet.remove(configFile)
        } else {
            config = configMap[configFile]
        }
        if (config != null && save) {
            File(plugin.dataFolder, configFile).saveConfigurationTree(config)
            // 更新时间戳
            if (!unload) {
                configTimestampMap[configFile] = System.currentTimeMillis()
            }
        }
        return config
    }

    /**
     * 保存某个配置文件，如果之前没有加载过且磁盘中不存在，则不会有任何动作
     * @param configFile 配置文件路径
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    infix fun save(configFile: String): ConfigurationNode? = saveOrUnload(configFile, unload=false, save=true)

    /**
     * 卸载配置文件，并选择是否保存配置文件
     * @param configFile 配置文件路径
     * @param save 是否保存内存中的配置文件信息到硬盘
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    fun unload(configFile: String, save: Boolean): ConfigurationNode? = saveOrUnload(configFile, true, save)

    /**
     * 放弃内存中的配置，从磁盘重新加载，如果磁盘中不存在就会加载默认配置
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    infix fun reload(configFile: String): ConfigurationNode? {
        unload(configFile, false)
        return get(configFile)
    }

    /**
     * 重置配置文件，恢复为默认配置文件
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    infix fun reset(configFile: String): ConfigurationNode? {
        plugin.saveResource(configFile, true)
        return reload(configFile)
    }

    /**
     * 保存所有配置文件
     */
    open fun saveAll() {
        configMap.forEach { (name: String, _: ConfigurationNode?) ->
            try {
                save(name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 将某个文件设置为持久化保存的，即不会因为长久未访问就从内存中卸载
     * @param configFile 要持久化保存的配置文件路径
     */
    infix fun setPersistent(configFile: String) = persistentConfigSet.add(configFile)

    /**
     * 取消持久化保存
     * @param configFile 要取消持久化保存的配置文件路径
     */
    infix fun unsetPersistent(configFile: String) = persistentConfigSet.remove(configFile)

    override fun close() {
        stopCleanService()
        saveAll()
        configMap.clear()
        configTimestampMap.clear()
        persistentConfigSet.clear()
    }

    /**
     * 未知的配置文件格式异常
     */
    class UnknownConfigFileFormatException(fileName: String) : Exception("Unknown Config File Format: $fileName")
}