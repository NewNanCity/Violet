@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package city.newnan.violet.config

import city.newnan.violet.cache.Cache
import city.newnan.violet.cache.GreedyDualSizeCache
import city.newnan.violet.cache.InfiniteCache
import city.newnan.violet.cache.LRUCache
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import me.lucko.helper.terminable.Terminable
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.plugin.Plugin
import java.io.*


/**
 * 配置文件对象
 *
 * @property type 类型
 * @property manager 配置管理器实例
 * @constructor Create [Configure2]
 *
 * @param path 配置文件路径
 * @param node 配置文件的根节点
 */
class Configure2(
    path: File?,
    val configPath: String?,
    val cached: Boolean,
    /**
     * 配置文件类型
     */
    var type: ConfigManager2.ConfigFileType,
    node: ObjectNode,
    /**
     * 配置文件绑定的ConfigManager2
     */
    val manager: ConfigManager2
) {
    /**
     * 配置文件路径
     */
    var path: File? = path?.canonicalFile

    /**
     * 配置文件的Jackson根节点
     */
    var node = node
        private set

    /**
     * 获取根节点
     *
     * @param block 根节点修改器
     * @return [Configure2]
     */
    fun root(block: (ObjectNode) -> Unit): Configure2 {
        block(node)
        return this
    }

    /**
     * 获取某个路径的节点
     *
     * @param paths 路径
     * @return 节点
     */
    fun path(vararg paths: Any, block: (ObjectNode) -> Unit): Configure2 {
        block(node[paths])
        return this
    }

    /**
     * 放弃内存中的配置，从磁盘重新加载，获得一个新实例，如果磁盘中不存在则返回自己
     */
    @Throws(IOException::class, ConfigManager2.UnknownConfigFileFormatException::class)
    fun reload(saveToCache: Boolean = cached): Configure2 {
        if (path == null || configPath == null) return this
        return manager.get(configPath, type, saveToCache = saveToCache, useCacheIfPossible = false)
    }

    /**
     * 保存配置文件
     * @param block 配置文件修改器
     */
    fun save(block: (ObjectNode) -> Unit) {
        block(node)
        save()
    }

    /**
     * 保存配置文件
     */
    @Throws(ConfigManager2.UnknownConfigFileFormatException::class, IOException::class)
    fun save(path: File? = this.path, type: ConfigManager2.ConfigFileType? = this.type, saveToCache: Boolean = cached) {
        if (path == null) return
        manager.save(this, path, type, saveToCache)
    }

    /**
     * 克隆一个新的配置文件
     * @param file 要保存的文件，注意路径是相对于插件数据目录的
     * @param type 配置文件类型
     * @return 新的配置文件实例
     */
    fun clone(file: String, type: ConfigManager2.ConfigFileType? = null): Configure2 {
        val path = File(manager.plugin.dataFolder, file)
        return Configure2(path, file, cached, type ?: ConfigManager2.guessConfigType(path), node.deepCopy(), manager)
    }

    /**
     * 保存为另一个文件
     * @param file 要保存的文件，注意路径是相对于插件数据目录的
     * @param clone 是否克隆一个新的配置文件
     * @param type 配置文件类型
     * @return 新的配置文件实例
     */
    fun saveAs(file: String, clone: Boolean = true,
               type: ConfigManager2.ConfigFileType? = null, block: ((ObjectNode) -> Unit)?): Configure2 {
        val path = File(manager.plugin.dataFolder, file)
        val c = if (clone) clone(file, type) else this.also {
            it.path = path
            it.type = type ?: ConfigManager2.guessConfigType(path)
        }
        block?.invoke(c.node)
        c.save()
        return c
    }

    /**
     * 删除配置文件
     */
    fun remove() {
        if (path == null) return
        manager.remove(path!!)
    }

    /**
     * 转换为字符串
     *
     * @param type Type 类型
     * @return
     */
    fun toString(type: ConfigManager2.ConfigFileType = this.type): String {
        return ConfigManager2.mapper[type].writeValueAsString(node)
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
    val plugin: Plugin
) : Terminable {
    companion object {
        var DEFAULT_CACHE_CAPACITY = 8
        var DEFAULT_CACHE_TYPE = CacheType.None

        private val MapperBuilders = mapOf(
            ConfigFileType.Json to { decorateMapper(ObjectMapper()) },
            ConfigFileType.Yaml to { decorateMapper(YAMLMapper()) },
            ConfigFileType.Toml to { decorateMapper(TomlMapper()) },
            ConfigFileType.Properties to { decorateMapper(JavaPropsMapper()) },
            ConfigFileType.Csv to { decorateMapper(CsvMapper()) },
            ConfigFileType.Xml to { decorateMapper(XmlMapper()) },
            ConfigFileType.Hocon to { decorateMapper(ObjectMapper(HoconFactory())) }
        )
        private val Mappers = mutableMapOf<ConfigFileType, ObjectMapper>()
        private fun decorateMapper(mapper: ObjectMapper): ObjectMapper {
            mapper
                // 序列化时使用缩进
                .enable(SerializationFeature.INDENT_OUTPUT)
                // 解析时忽略未知的属性
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 支持 Kotlin 类
                // https://github.com/FasterXML/jackson-module-kotlin
                .registerKotlinModule()
            if (mapper is YAMLMapper) {
                // 不需要写入 YAML 文档开始标记 ---
                mapper.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            }
            return mapper
        }


        /**
         * 获取一个 mapper
         *
         * @param type 配置文集类型
         * @return
         */
        @Throws(Exception::class)
        fun getMapper(type: ConfigFileType) =
            Mappers.getOrPut(type) { MapperBuilders[type]?.invoke() ?: throw Exception("Unknown config file type: $type") }

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

        /**
         * 使用指定类型解析配置文件
         *
         * @param T 指定类型
         * @param path 配置文件路径
         * @param type 配置文件类型，如果为null则自动检测
         * @return [T] 实例
         */
        @Throws(IOException::class, UnknownConfigFileFormatException::class)
        inline fun <reified T> parse(path: File, type: ConfigFileType? = null): T {
            // 读取配置文件
            val typeReal = type ?: guessConfigType(path)
            return mapper[typeReal].readValue(path, object : TypeReference<T>() {})
        }

        /**
         * 使用指定类型解析文本
         *
         * @param T 指定类型
         * @param config 配置文本
         * @param type 配置文件类型
         * @return [T] 实例
         */
        @Throws(JsonProcessingException::class, UnknownConfigFileFormatException::class)
        inline fun <reified T> parse(config: String, type: ConfigFileType): T {
            return mapper[type].readValue(config, object : TypeReference<T>() {})
        }

        /**
         * 序列化为文本
         *
         * @param T 指定类型
         * @param type 配置文件类型
         * @return [String] 文本
         */
        @Throws(JsonProcessingException::class, UnknownConfigFileFormatException::class)
        fun <T> stringify(obj: T, type: ConfigFileType): String {
            return mapper[type].writeValueAsString(obj)
        }

        @Throws(IOException::class, UnknownConfigFileFormatException::class)
        fun <T> save(obj: T, path: File, type: ConfigFileType? = null) {
            val typeReal = type ?: guessConfigType(path)
            mapper[typeReal].writeValue(path, obj)
        }
    }

    init {
        if (plugin is TerminableConsumer) bindWith(plugin)
        setCache(DEFAULT_CACHE_TYPE, DEFAULT_CACHE_CAPACITY)
    }

    val configure2TypeReference = object : TypeReference<Configure2>() {}
    val configure2TypeReferenceString = configure2TypeReference.type.toString()
    var cache: Cache<Pair<String, String>, Any>? = null
        private set

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
     * 设置缓存
     * @param type 缓存类型
     * @param capacity 缓存容量
     */
    fun setCache(type: CacheType, capacity: Int = DEFAULT_CACHE_CAPACITY) {
        cache = when (type) {
            CacheType.None -> null
            CacheType.LRU -> LRUCache(capacity)
            CacheType.GreedyDualSize -> GreedyDualSizeCache(capacity)
            CacheType.Infinite -> InfiniteCache(capacity)
        }
    }

    /**
     * 设置缓存
     * @param cache 缓存实例
     */
    fun setCache(cache: Cache<Pair<String, String>, Any>) {
        this.cache = cache
    }

    /**
     * 检查这个配置文件是否存在，不存在就创建
     * @param configFile 配置文件路径
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     */
    infix fun touch(configFile: String): Boolean =
        touch(configFile, configFile)


    /**
     * 获取某个配置文件(支持YAML,JSON,TOML,HOCON,XML,Properties和CSV)，如果不存在就加载默认
     * @param configFile 配置文件路径
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    infix operator fun get(configFile: String): Configure2 =
        get(configFile, null, saveToCache = true, useCacheIfPossible = true)

    /**
     * 检查这个资源文件是否存在，如果不存在就从指定的模板复制一份
     * @param targetFile 要检查的配置文件路径
     * @param templateFile 模板配置文件路径
     * @param overrideExist 如果文件已经存在是否覆盖
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     * @throws IOException 文件读写出错
     */
    @Throws(IOException::class)
    fun touch(targetFile: String, templateFile: String, overrideExist: Boolean = false): Boolean {
        val file = File(plugin.dataFolder, targetFile)
        // 如果文件不存在
        if (overrideExist || !file.exists()) {
            // 检查父目录
            if (!file.parentFile.exists()) file.parentFile.mkdirs()

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
     * 检查这个资源文件是否存在，如果不存在就用模板来初始化
     * @param targetFile 要检查的配置文件路径
     * @param templateValue 模板，一个可序列化的类
     * @param overrideExist 如果文件已经存在是否覆盖
     * @return 如果文件之前存在就返回true，如果现在新创建就返回false
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    fun touch(targetFile: String, templateValue: Any, type: ConfigFileType? = null, overrideExist: Boolean = false): Boolean {
        val file = File(plugin.dataFolder, targetFile)
        // 如果文件不存在
        if (overrideExist || !file.exists()) {
            // 检查父目录
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            save(templateValue, file, type)
            return false
        }
        return true
    }

    /**
     * 从文件获取某个配置文件(支持YAML,JSON,TOML,HOCON,XML,Properties和CSV)
     * @param configFile 配置文件路径
     * @param type 配置文件类型，如果为null则自动检测
     * @return 配置实例
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    fun get(configFile: String,
            type: ConfigFileType? = null,
            templateFile: String = configFile,
            saveToCache: Boolean = true,
            useCacheIfPossible: Boolean = true,
            templateData: Any? = null): Configure2 {
        // 查找缓存
        val path = File(plugin.dataFolder, configFile)
        val cacheKey = Pair(path.canonicalPath, configure2TypeReferenceString)
        if (useCacheIfPossible) {
            val cached = cache?.get(cacheKey)
            if (cached != null) return cached as Configure2
        }
        // 未缓存则加载
        if (templateData != null) touch(configFile, templateData, type) else touch(configFile, templateFile)
        // 读取配置文件
        val typeReal = type ?: guessConfigType(path)
        val config = Configure2(path, configFile, saveToCache, typeReal, mapper[typeReal].readTree(path) as ObjectNode, this)
        if (saveToCache) cache?.set(cacheKey, config)
        return config
    }

    /**
     * 使用指定类型解析配置文件
     *
     * @param T 指定类型
     * @param configFile 配置文件路径
     * @param type 配置文件类型，如果为null则自动检测
     * @return [T] 实例
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    inline fun <reified T : Any> parse(configFile: String,
                                       type: ConfigFileType? = null,
                                       saveToCache: Boolean = true,
                                       useCacheIfPossible: Boolean = true): T {
        // 查找缓存
        val typeR = object : TypeReference<T>() {}
        val path = File(plugin.dataFolder, configFile)
        val cacheKey = Pair(path.canonicalPath, typeR.type.toString())
        if (useCacheIfPossible) {
            val cached = cache?.get(cacheKey)
            if (cached != null) return cached as T
        }
        /// 未缓存则加载
        if (typeR == configure2TypeReference) return get(configFile, type, saveToCache=saveToCache, useCacheIfPossible=false) as T
        touch(configFile)
        val obj = mapper[type ?: guessConfigType(path)].readValue(path, typeR)
        if (saveToCache) cache?.set(cacheKey, obj)
        return obj as T
    }


    /**
     * 序列化对象到指定的配置文件
     *
     * @param T 指定类型
     * @param obj 对象实例
     * @param configFile 配置文件路径
     * @param type 配置文件类型，如果为null则自动检测
     * @return [T] 实例
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    inline fun <reified T : Any> save(obj: T,
                                      configFile: String,
                                      type: ConfigFileType? = null,
                                      saveToCache: Boolean = true) =
        save(obj, File(plugin.dataFolder, configFile), type, saveToCache)


    /**
     * 序列化对象到指定的配置文件
     *
     * @param T 指定类型
     * @param obj 对象实例
     * @param path 配置文件路径
     * @param type 配置文件类型，如果为null则自动检测
     * @return [T] 实例
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    inline fun <reified T : Any> save(obj: T,
                                      path: File,
                                      type: ConfigFileType? = null,
                                      saveToCache: Boolean = true) {
        if (obj is Configure2) {
            mapper[type ?: guessConfigType(path)].writeValue(path, obj.node)
            if (saveToCache) this.cache?.also {
                it[path.canonicalPath to configure2TypeReferenceString] = obj
            }
        } else {
            ConfigManager2.save(obj, path, type)
            if (saveToCache) this.cache?.also {
                it[path.canonicalPath to object : TypeReference<T>() {}.type.toString()] = obj
            }
        }
    }


    /**
     * 从文件系统删除配置文件
     * @param configFile 配置文件路径
     * @param removeFromCache 是否从缓存中删除
     */
    fun remove(configFile: String, removeFromCache: Boolean = true) =
        remove(File(plugin.dataFolder, configFile), removeFromCache)


    /**
     * 从文件系统删除配置文件
     * @param path 配置文件路径
     * @param removeFromCache 是否从缓存中删除
     */
    fun remove(path: File, removeFromCache: Boolean = true) {
        if (removeFromCache && cache != null) {
            val f = path.canonicalPath
            cache!!.keys.filter { it.first == f }.forEach { cache!!.remove(it) }
        }
        path.delete()
    }


    /**
     * 重置配置文件，恢复为默认配置文件
     * @param configFile 配置文件路径
     * @param templateFile 模板配置文件资源路径
     * @param templateData 模板数据
     * @param type 配置文件类型，如果为null则自动检测
     * @throws IOException 文件读写出错
     * @throws UnknownConfigFileFormatException 未知的配置文件格式
     */
    @Throws(IOException::class, UnknownConfigFileFormatException::class)
    fun reset(configFile: String, templateFile: String = configFile, templateData: Any? = null, type: ConfigFileType? = null) {
        if(templateData != null) touch(configFile, templateData) else touch(configFile, templateFile, type)
    }


    /**
     * 关闭、清理服务
     */
    override fun close() {
        cache?.clear()
    }

    /**
     * 从文本中加载配置文件
     * @param text 文本
     * @param type 配置文件类型
     * @return 配置文件实例
     */
    fun parseText(text: String, type: ConfigFileType): Configure2 {
        val node = mapper[type].readTree(text) as ObjectNode
        return Configure2(null, null, false, type, node, this)
    }

    /**
     * 未知的配置文件格式异常
     */
    class UnknownConfigFileFormatException(fileName: String) : Exception("Unknown Config File Format: $fileName")

    /**
     * 支持的配置文件类型
     */
    enum class ConfigFileType {
        Properties, Json, Yaml, Toml, Hocon, Xml, Csv
    }

    /**
     * 缓存类型
     */
    enum class CacheType {
        None, LRU, GreedyDualSize, Infinite
    }
}