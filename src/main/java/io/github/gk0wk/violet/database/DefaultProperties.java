package io.github.gk0wk.violet.database;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultProperties {
    static final Map<String, String> MySQL = ImmutableMap.<String, String> builder()
            // Set timezone
            .put("serverTimezone", "Asia/Shanghai")
            // Ensure we use utf8 encoding
            .put("useUnicode", "true")
            .put("characterEncoding", "utf8")
            // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
            .put("cachePrepStmts", "true")
            .put("prepStmtCacheSize", "250")
            .put("prepStmtCacheSqlLimit", "2048")
            .put("useServerPrepStmts", "true")
            .put("useLocalSessionState", "true")
            .put("rewriteBatchedStatements", "true")
            .put("cacheResultSetMetadata", "true")
            .put("cacheServerConfiguration", "true")
            .put("elideSetAutoCommits", "true")
            .put("maintainTimeStats", "false")
            .put("alwaysSendSetIsolation", "false")
            .put("cacheCallableStmts", "true")
            // Set the driver level TCP socket timeout
            // See: https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery
            .put("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)))
            .build();
}
