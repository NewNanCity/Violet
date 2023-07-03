@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package city.newnan.violet.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import me.lucko.helper.Schedulers
import me.lucko.helper.scheduler.Task
import me.lucko.helper.terminable.Terminable
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import java.io.*
import java.util.*
import kotlin.collections.HashMap

/**
 * 使用索引器路径获取节点
 *
 * ```
 * val node1 = node["foo", 2, "bar"]
 * ```
 *
 * @receiver [ObjectNode]
 * @param paths Paths 字符串或整数，分别用于索引Object和Array
 * @return [ObjectNode]
 * @throws Exception 未知路径类型
 */
operator fun ObjectNode.get(vararg paths: Any): ObjectNode {
    var node: ObjectNode = this
    for (path in paths) {
        node = when (path) {
            is String -> node[path] as ObjectNode
            is Int -> node[path] as ObjectNode
            else -> throw Exception("Unknown path type: ${path::class.java} (value: $path)")
        }
    }
    return node
}

infix fun ObjectNode.get(key: String) = get(key) as ObjectNode

fun <T> ObjectNode.asType(): T = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json]
    .convertValue(this, object : com.fasterxml.jackson.core.type.TypeReference<T>() {})
fun ObjectNode.asMap() = asType<LinkedHashMap<String, Any>>()
fun Map<*, *>.toObjectNode(): ObjectNode = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json]
    .valueToTree(this)
fun ObjectNode.asList() = asType<ArrayList<Any>>()
fun List<*>.toObjectNode(): ObjectNode = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json]
    .valueToTree(this)
fun <T> T.asObjectNode(): ObjectNode = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Json]
    .valueToTree(this)
fun JsonNode.asUUID(): UUID = UUID.fromString(asText())
fun ObjectNode.put(key: String, value: UUID): ObjectNode = put(key, value.toString())
fun JsonNode.asPlayer(): OfflinePlayer = Bukkit.getOfflinePlayer(asUUID())
fun ObjectNode.put(key: String, value: OfflinePlayer): ObjectNode = put(key, value.uniqueId.toString())
fun JsonNode.asMaterial(): Material? = Material.matchMaterial(asText())
fun ObjectNode.put(key: String, value: Material): ObjectNode = put(key, value.key.toString())

/**
 * 配置文件对象
 *
 * @param plugin 插件实例
 * @param dataFolder 配置文件目录
 * @param mapper Jackson mapper
 */
class Configure2(
    rawPath: File,
    /**
     * 配置文件类型
     */
    var type: ConfigManager2.ConfigFileType,
    nodeInit: ObjectNode,
    /**
     * 配置文件绑定的ConfigManager2
     */
    val manager: ConfigManager2
) {
    private var _enableCache = false
    private var _enablePersistence = false
    /**
     * 配置文件启用或关闭持久化保存，即不会因为长久未访问就从内存中卸载，适合经常访问的配置文件。
     */
    fun setCache(enable: Boolean) {
        if (enable == _enableCache) return
        _enableCache = enable
        val p = path.canonicalPath
        if (enable) {
            manager.configCache[p] = this
            manager.configTimestampMap[p] = System.currentTimeMillis()
            if (_enablePersistence) manager.persistentConfigSet.add(p)
        } else {
            manager.configCache.remove(p)
            manager.configTimestampMap.remove(p)
            manager.persistentConfigSet.remove(p)
            save()
        }
    }
    /**
     * 配置文件启用或关闭持久化保存，即不会因为长久未访问就从内存中卸载，适合经常访问的配置文件。
     */
    infix fun cache(enable: Boolean) = setCache(enable)
    /**
     * 启动缓存
     */
    fun load() = setCache(true)
    /**
     * 将配置文件从内存中卸载并保存
     */
    fun unload() = setCache(false)

    /**
     * 将某个文件设置为持久化保存的，即不会因为长久未访问就从内存中卸载；或者取消。以缓存为前提。
     *
     * @param enable 是否启动持久化
     */
    fun setPersistence(enable: Boolean) {
        if (enable == _enablePersistence) return
        _enablePersistence = enable
        if (enable) manager.persistentConfigSet.add(path.canonicalPath)
    }
    /**
     * 将某个文件设置为持久化保存的，即不会因为长久未访问就从内存中卸载；或者取消。以缓存为前提。
     *
     * @param enable 是否启动持久化
     */
    infix fun persistence(enable: Boolean) = setPersistence(enable)

    /**
     * 配置文件路径
     */
    var path: File = rawPath.canonicalFile

    /**
     * 配置文件的Jackson根节点
     */
    var node = nodeInit
        private set

    /**
     * 获取根节点
     *
     * @param block 根节点修改器
     * @return [Configure2]
     */
    inline fun root(block: (ObjectNode) -> Unit): Configure2 {
        block(node)
        return this
    }

    /**
     * 获取某个路径的节点
     *
     * @param paths 路径
     * @return 节点
     */
    inline fun path(vararg paths: Any, block: (ObjectNode) -> Unit): Configure2 {
        block(node[paths])
        return this
    }

    /**
     * 放弃内存中的配置，从磁盘重新加载，如果磁盘中不存在就会加载默认配置
     */
    @Throws(IOException::class, ConfigManager2.UnknownConfigFileFormatException::class)
    fun reload() {
        node = ConfigManager2.mapper[type].readTree(path) as ObjectNode
    }

    /**
     * 保存配置文件
     * @param block 配置文件修改器
     */
    inline fun save(block: (ObjectNode) -> Unit) {
        block(node)
        save()
    }

    /**
     * 保存配置文件
     */
    @Throws(ConfigManager2.UnknownConfigFileFormatException::class, IOException::class)
    fun save() {
        val mapper: ObjectMapper
        try {
            mapper = ConfigManager2.mapper[type]
        } catch (e: Exception) {
            throw ConfigManager2.UnknownConfigFileFormatException(path.canonicalPath)
        }
        val writer = BufferedWriter(FileWriter(path))
        mapper.writeValue(writer, node)
        writer.close()
        if (_enableCache) {
            manager.configTimestampMap[path.canonicalPath] = System.currentTimeMillis()
        }
    }

    /**
     * 克隆一个新的配置文件
     * @param file 要保存的文件
     * @param type 配置文件类型
     * @return 新的配置文件实例
     */
    fun clone(file: String, type: ConfigManager2.ConfigFileType? = null): Configure2
        = clone(File(file), type)

    /**
     * 克隆一个新的配置文件
     * @param file 要保存的文件
     * @param type 配置文件类型
     * @return 新的配置文件实例
     */
    fun clone(file: File, type: ConfigManager2.ConfigFileType? = null): Configure2
        = Configure2(path, type ?: ConfigManager2.guessConfigType(file), node.deepCopy(), manager)

    /**
     * 保存为另一个文件
     * @param file 要保存的文件
     * @param clone 是否克隆一个新的配置文件
     * @param type 配置文件类型
     * @return 新的配置文件实例
     */
    fun saveAs(file: File, clone: Boolean = true, type: ConfigManager2.ConfigFileType? = null): Configure2 {
        val typeReal = type ?: ConfigManager2.guessConfigType(file)
        val c = if (clone) clone(file, type) else this.also {
            it.path = file
            it.type = typeReal
        }
        c.save()
        return c
    }

    /**
     * 保存为另一个文件
     * @param file 要保存的文件
     * @param clone 是否克隆一个新的配置文件
     * @param type 配置文件类型
     * @return 新的配置文件实例
     */
    fun saveAs(file: String, clone: Boolean = true, type: ConfigManager2.ConfigFileType? = null): Configure2
        = saveAs(File(file), clone, type)

    /**
     * 保存为另一个文件
     * @param file 要保存的文件
     * @param clone 是否克隆一个新的配置文件
     * @param type 配置文件类型
     * @param block 保存之前的操作
     * @return 新的配置文件实例
     */
    inline fun saveAs(file: File, clone: Boolean = true, type: ConfigManager2.ConfigFileType? = null,
                      block: (ObjectNode) -> Unit): Configure2 {
        val typeReal = type ?: ConfigManager2.guessConfigType(file)
        val c = if (clone) clone(file, type) else this.also {
            it.path = file
            it.type = typeReal
        }
        block(c.node)
        c.save()
        return c
    }

    /**
     * 保存为另一个文件
     * @param file 要保存的文件
     * @param clone 是否克隆一个新的配置文件
     * @param type 配置文件类型
     * @param block 保存之前的操作
     * @return 新的配置文件实例
     */
    inline fun saveAs(file: String, clone: Boolean = true, type: ConfigManager2.ConfigFileType? = null,
                      block: (ObjectNode) -> Unit): Configure2
        = saveAs(File(file), clone, type, block)

    /**
     * 删除配置文件
     */
    fun remove() {
        unload()
        path.delete()
    }
}

class ConfigManager2
/**
 * 构造函数
 * @param plugin 要绑定的插件
 */(
    /**
     * 绑定的插件实例
     */
    private val plugin: Plugin
) : Terminable {
    companion object {
        private val MapperBuilders = hashMapOf(
            ConfigFileType.Json to { ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT) },
            ConfigFileType.Yaml to { YAMLMapper().enable(SerializationFeature.INDENT_OUTPUT) },
            ConfigFileType.Toml to { TomlMapper().enable(SerializationFeature.INDENT_OUTPUT) },
            ConfigFileType.Properties to { JavaPropsMapper().enable(SerializationFeature.INDENT_OUTPUT) },
            ConfigFileType.Csv to { CsvMapper().enable(SerializationFeature.INDENT_OUTPUT) },
            ConfigFileType.Xml to { XmlMapper().enable(SerializationFeature.INDENT_OUTPUT) },
            ConfigFileType.Hocon to { ObjectMapper(HoconFactory()).enable(SerializationFeature.INDENT_OUTPUT) }
        )
        private val Mappers = HashMap<ConfigFileType, ObjectMapper>()

        /**
         * 获取一个 mapper
         *
         * @param type 配置文集类型
         * @return
         */
        @Throws(Exception::class)
        fun getMapper(type: ConfigFileType) =
            Mappers.getOrPut(type) { MapperBuilders[type]?.invoke() ?: throw Exception("Unknown config file type") }

        /**
         * 根据路径推断配置文件类型
         *
         * @param path 文件路径
         * @return
         */
        @Throws(UnknownConfigFileFormatException::class)
        fun guessConfigType(path: File) = when (path.name.substringAfterLast('.').lowercase()) {
            "json" -> ConfigFileType.Json
            "yml", "yaml" -> ConfigFileType.Yaml
            "toml" -> ConfigFileType.Toml
            "properties" -> ConfigFileType.Properties
            "csv" -> ConfigFileType.Csv
            "xml" -> ConfigFileType.Xml
            "conf", "hocon" -> ConfigFileType.Hocon
            else -> throw UnknownConfigFileFormatException(path.path)
        }
    }

    /**
     * 支持的配置文件类型
     */
    enum class ConfigFileType {
        Properties, Json, Yaml, Toml, Hocon, Xml, Csv
    }

    init { if (plugin is TerminableConsumer) bindWith(plugin) }

    /**
     * 配置文件缓冲
     */
    internal val configCache = HashMap<String, Configure2>()

    /**
     * 配置文件访问时间戳
     */
    internal val configTimestampMap = HashMap<String, Long>()

    /**
     * 持久化保存的配置文件
     */
    internal val persistentConfigSet: HashSet<String> = hashSetOf("config.yml")

    private var cleanTask: Task? = null

    /**
     * 缓存过期的时长(毫秒)
     */
    var fileCacheTimeout = 1800000L
        set(value) { if (value > 1000L) field = value }

    object mapper {
        /**
         * 获取一个 mapper
         *
         * @param type Type
         * @return
         */
        @Throws(Exception::class)
        operator fun get(type: ConfigFileType) = getMapper(type)
    }

    /**
     * 启动周期性的配置缓存清理
     * @return ConfigManager2实例
     */
    fun startCleanService(): ConfigManager2 {
        if (cleanTask != null) {
            return this
        }
        // 自动卸载长时间未使用的配置文件
        cleanTask = Schedulers.sync().runRepeating({ _: Task? ->
            val outdatedTime = System.currentTimeMillis() - fileCacheTimeout
            // 先存起来再修改
            val outdatedConfig = ArrayList<Configure2>()
            val additionalRemoveConfigPath = ArrayList<String>()
            configTimestampMap.forEach { (config: String, time: Long) ->
                if (time <= outdatedTime && !persistentConfigSet.contains(config)) {
                    if (configCache.containsKey(config)) {
                        outdatedConfig.add(configCache[config]!!)
                    } else {
                        additionalRemoveConfigPath.add(config)
                    }
                }
            }
            for (config in outdatedConfig) {
                try {
                    config.unload()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            for (key in additionalRemoveConfigPath) {
                persistentConfigSet.remove(key)
                configTimestampMap.remove(key)
            }
        }, 36000L, 36000L)
        return this
    }


    /**
     * 关闭周期性的配置缓存清理
     * @return ConfigManager2实例
     */
    fun stopCleanService(): ConfigManager2 = this.also { cleanTask?.close() }

    /**
     * 检查这个配置文件是否存在，不存在就创建
     * @param configFile 配置文件路径
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     */
    infix fun touch(configFile: String): Boolean {
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
    fun touchOrCopyTemplate(targetFile: String, templateFile: String): Boolean {
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
     * 获取某个配置文件(支持YAML,JSON,TOML,HOCON,XML,Properties和CSV)，如果不存在就加载默认
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    infix operator fun get(configFile: String): Configure2 {
        // 已缓存则返回
        return if (configCache.containsKey(configFile)) configCache[configFile]!!
        // 读取配置文件
        else getWithoutCache(configFile)
    }

    /**
     * 直接从文件获取某个配置文件(支持YAML,JSON,TOML,HOCON,XML,Properties和CSV)，不使用缓存机制
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    fun getWithoutCache(configFile: String, type: ConfigFileType? = null): Configure2 {
        // 未缓存则加载
        touch(configFile)
        // 读取配置文件
        val path = File(plugin.dataFolder, configFile)
        val typeReal = type ?: guessConfigType(path)
        return Configure2(path, typeReal, mapper[typeReal].readTree(path) as ObjectNode, this)
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
    fun getOrCopyTemplate(targetFile: String, templateFile: String, withoutCache: Boolean = false): Configure2 {
        // 未缓存则加载
        touchOrCopyTemplate(targetFile, templateFile)
        return if (withoutCache && configCache.containsKey(targetFile)) configCache[targetFile]!!
        else this[targetFile]
    }

    /**
     * 重置配置文件，恢复为默认配置文件
     * @param configFile 配置文件路径
     * @param templateFile 模板配置文件资源路径
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    fun reset(configFile: String, templateFile: String? = null) {
        if (templateFile == null) plugin.saveResource(configFile, true)
        else touchOrCopyTemplate(configFile, templateFile)
    }

    /**
     * 保存所有配置文件
     */
    fun saveAll() {
        for (c : Configure2 in configCache.values) {
            try {
                c.save()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 关闭、清理服务
     */
    override fun close() {
        stopCleanService()
        saveAll()
        configCache.clear()
        configTimestampMap.clear()
        persistentConfigSet.clear()
    }

    /**
     * 未知的配置文件格式异常
     */
    class UnknownConfigFileFormatException(fileName: String) : Exception("Unknown Config File Format: $fileName")
}