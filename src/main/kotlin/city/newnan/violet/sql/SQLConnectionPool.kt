package city.newnan.violet.sql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

fun buildSQLCP(config: HikariConfig, block: SQLConnectionPool.() -> Unit): SQLConnectionPool
    = SQLConnectionPool(config).apply { block(this) }.build()
fun buildSQLCP(block: SQLConnectionPool.() -> Unit): SQLConnectionPool
    = SQLConnectionPool().apply { block(this) }.build()

class SQLConnectionPool {
    private val config: HikariConfig
    constructor() {
        config = HikariConfig()
        init()
    }
    constructor(config: HikariConfig) {
        this.config = config
        init()
    }

    var dbType = "mysql"
    infix fun dbType(dbType: String) = this.also { it.dbType = dbType.lowercase(Locale.ROOT) }

    var host = "localhost"
    infix fun host(host: String) = this.also { it.host = host }

    var port = 3306
    infix fun port(port: Int) = this.also { it.port = port }

    var database: String = ""
    infix fun database(database: String) = this.also { it.database = database }

    var username: String
        get() = config.username
        set(value) { config.username = value }
    infix fun username(username: String) = this.also { it.username = username }

    var password: String
        get() = config.password
        set(value) { config.password = value }
    infix fun password(password: String) = this.also { it.password = password }

    infix fun set(pair: Pair<String, String>)
        = this.also { it.config.addDataSourceProperty(pair.first, pair.second) }

    fun set(key: String, value: String)
        = this.also { it.config.addDataSourceProperty(key, value) }

    infix fun get(key: String): String?
        = config.dataSourceProperties.getProperty(key)

    private fun init() {
        maximumPoolSize = MAXIMUM_POOL_SIZE
        minimumIdle = MINIMUM_IDLE
        maxLifetime = MAX_LIFETIME
        connectionTimeout = CONNECTION_TIMEOUT
        leakDetectionThreshold = LEAK_DETECTION_THRESHOLD
        keepaliveTime = KEEP_ALIVE
    }

    var maximumPoolSize
        get() = config.maximumPoolSize
        set(value) { config.maximumPoolSize = value }
    infix fun maximumPoolSize(size: Int) = this.also { it.maximumPoolSize = size }

    var minimumIdle
        get() = config.minimumIdle
        set(value) { config.minimumIdle = value }
    infix fun minimumIdle(minimumIdle: Int) = this.also { it.minimumIdle = minimumIdle }

    var maxLifetime
        get() = config.maxLifetime
        set(value) { config.maxLifetime = value }
    infix fun maxLifetime(milliseconds: Long) = this.also { it.maxLifetime = milliseconds }

    var connectionTimeout
        get() = config.connectionTimeout
        set(value) { config.connectionTimeout = value }
    infix fun connectionTimeout(milliseconds: Long) = this.also { it.connectionTimeout = milliseconds }

    var leakDetectionThreshold
        get() = config.leakDetectionThreshold
        set(value) { config.leakDetectionThreshold = value }
    infix fun leakDetectionThreshold(milliseconds: Long) = this.also { it.leakDetectionThreshold = milliseconds }

    var keepaliveTime
        get() = config.keepaliveTime
        set(value) { config.keepaliveTime = value }
    infix fun keepaliveTime(milliseconds: Long) = this.also { it.keepaliveTime = milliseconds }

    private fun checkProperties(defaultProperties: Map<String, String>) {
        val tmp = config.dataSourceProperties
        defaultProperties.forEach { (key: String, value: String) ->
            if (!tmp.containsKey(key)) config.addDataSourceProperty(key, value)
        }
    }

    private lateinit var dataSource: HikariDataSource
    val hikari: HikariDataSource
        get() = dataSource

    fun build(): SQLConnectionPool {
        if (dbType == "mysql") {
            config.driverClassName = "com.mysql.cj.jdbc.Driver"
            checkProperties(DefaultProperties.MySQL)
        }
        config.poolName = "VioletSQL(HikariCP)-Pool" + POOL_COUNTER.getAndIncrement()
        config.jdbcUrl = "jdbc:$dbType://$host:$port/$database"
        // https://github.com/brettwooldridge/HikariCP
        // More: https://emacsist.github.io/2019/09/11/hikaricp-%E6%95%B0%E6%8D%AE%E5%BA%93%E8%BF%9E%E6%8E%A5%E6%B1%A0%E5%AE%9E%E8%B7%B5%E7%AC%94%E8%AE%B0/
        dataSource = HikariDataSource(config)
        return this
    }

    infix fun call(block: Connection.() -> Unit): SQLConnectionPool {
        var oneConnection: Connection? = null
        try {
            oneConnection = connection
            oneConnection?.let { block(it) }
        } finally {
            oneConnection?.close()
        }
        return this
    }

    @get:Throws(SQLException::class)
    val connection: Connection?
        get() = dataSource.connection

    fun close() = dataSource.close()

    companion object {
        private val POOL_COUNTER = AtomicInteger(0)
        // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
        private val MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2 + 1
        private val MINIMUM_IDLE = MAXIMUM_POOL_SIZE.coerceAtMost(10)
        private val MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30)
        private val CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10)
        private val LEAK_DETECTION_THRESHOLD = TimeUnit.SECONDS.toMillis(10)
        private val KEEP_ALIVE = TimeUnit.MINUTES.toMillis(60)
    }
}