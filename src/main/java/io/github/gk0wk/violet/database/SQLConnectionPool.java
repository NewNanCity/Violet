package io.github.gk0wk.violet.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lucko.helper.terminable.Terminable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SQLConnectionPool implements Terminable {
    private String dbType = "mysql";
    private String host = "localhost";
    private int port = 3306;
    private String database = null;
    private final HikariConfig config;

    public SQLConnectionPool() {
        this.config = new HikariConfig();
    }
    public SQLConnectionPool(@NotNull HikariConfig config) {
        this.config = config;
    }

    @Nonnull
    public SQLConnectionPool dbType(@NotNull String dbType) {
        this.dbType = dbType.toLowerCase(Locale.ROOT);
        return this;
    }
    @Nonnull
    public SQLConnectionPool host(@NotNull String host) {
        this.host = host;
        return this;
    }
    @Nonnull
    public SQLConnectionPool port(int port) {
        this.port = port;
        return this;
    }
    @Nonnull
    public SQLConnectionPool database(@NotNull String database) {
        this.database = database;
        return this;
    }
    @Nonnull
    public SQLConnectionPool username(@NotNull String username) {
        config.setUsername(username);
        return this;
    }
    @Nonnull
    public SQLConnectionPool password(@NotNull String password) {
        config.setPassword(password);
        return this;
    }
    @Nonnull
    public SQLConnectionPool setProperty(@NotNull String key, @NotNull String value) {
        config.addDataSourceProperty(key, value);
        return this;
    }
    @Nullable
    public String getProperty(@NotNull String key) {
        return config.getDataSourceProperties().getProperty(key);
    }

    private static final AtomicInteger POOL_COUNTER = new AtomicInteger(0);

    // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
    private static final int MAXIMUM_POOL_SIZE = (Runtime.getRuntime().availableProcessors() * 2) + 1;
    private static final int MINIMUM_IDLE = Math.min(MAXIMUM_POOL_SIZE, 10);
    private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(10);
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final long LEAK_DETECTION_THRESHOLD = TimeUnit.SECONDS.toMillis(10);

    private int maximumPoolSize = MAXIMUM_POOL_SIZE;
    @Nonnull
    public SQLConnectionPool maximumPoolSize(int size) {
        this.maximumPoolSize = size;
        return this;
    }

    private int minimumIdle = MINIMUM_IDLE;
    @Nonnull
    public SQLConnectionPool minimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
        return this;
    }

    private long maxLifeTime = MAX_LIFETIME;
    @Nonnull
    public SQLConnectionPool maxLifeTime(long milliseconds) {
        this.maxLifeTime = milliseconds;
        return this;
    }

    private long connectionTimeout = CONNECTION_TIMEOUT;
    @Nonnull
    public SQLConnectionPool connectionTimeout(long milliseconds) {
        this.connectionTimeout = milliseconds;
        return this;
    }

    private long leakDetectionThreshold = LEAK_DETECTION_THRESHOLD;
    @Nonnull
    public SQLConnectionPool leakDetectionThreshold(long milliseconds) {
        this.leakDetectionThreshold = milliseconds;
        return this;
    }

    private long keepAlive = KEEP_ALIVE;
    @Nonnull
    public SQLConnectionPool keepAlive(long milliseconds) {
        this.keepAlive = milliseconds;
        return this;
    }

    private void CheckProperties(Map<String, String> defaultProperties) {
        Properties tmp = config.getDataSourceProperties();
        defaultProperties.forEach((key, value) -> {
            if (!tmp.containsKey(key)) config.addDataSourceProperty(key, value);
        });
    }

    private HikariDataSource dataSource;
    @Nonnull
    public SQLConnectionPool build() {
        if (dbType.equals("mysql")) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            CheckProperties(DefaultProperties.MySQL);
        }

        config.setPoolName("VioletSQL(HikariCP)-Pool" + POOL_COUNTER.getAndIncrement());
        StringBuilder sb = new StringBuilder();
        config.setJdbcUrl(sb
                .append("jdbc:").append(this.dbType).append("://")
                .append(this.host).append(':')
                .append(this.port).append('/')
                .append(this.database)
                .toString());

        // https://github.com/brettwooldridge/HikariCP
        // More: https://emacsist.github.io/2019/09/11/hikaricp-%E6%95%B0%E6%8D%AE%E5%BA%93%E8%BF%9E%E6%8E%A5%E6%B1%A0%E5%AE%9E%E8%B7%B5%E7%AC%94%E8%AE%B0/
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setMaxLifetime(maxLifeTime);
        config.setConnectionTimeout(connectionTimeout);
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        config.setKeepaliveTime(keepAlive);

        dataSource = new HikariDataSource(config);

        return this;
    }

    @Nonnull
    public HikariDataSource getHikari() {
        return this.dataSource;
    }

    @Nonnull
    public Connection getConnection() throws SQLException {
        return Objects.requireNonNull(this.dataSource.getConnection(), "connection is null");
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
