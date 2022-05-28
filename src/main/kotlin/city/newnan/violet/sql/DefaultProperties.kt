package city.newnan.violet.sql

import java.util.concurrent.TimeUnit

object DefaultProperties {
    val MySQL = mapOf(
        // Set timezone
        "serverTimezone" to "Asia/Shanghai",
        // Ensure we use utf8 encoding
        "useUnicode" to "true",
        "characterEncoding" to "utf8",
        // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
        "cachePrepStmts" to "true",
        "prepStmtCacheSize" to "250",
        "prepStmtCacheSqlLimit" to "2048",
        "useServerPrepStmts" to "true",
        "useLocalSessionState" to "true",
        "rewriteBatchedStatements" to "true",
        "cacheResultSetMetadata" to "true",
        "cacheServerConfiguration" to "true",
        "elideSetAutoCommits" to "true",
        "maintainTimeStats" to "false",
        "alwaysSendSetIsolation" to "false",
        "cacheCallableStmts" to "true",
        // Set the driver level TCP socket timeout
        // See: https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery
        "socketTimeout" to TimeUnit.SECONDS.toMillis(30).toString()
    )
}
