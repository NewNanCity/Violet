package io.github.gk0wk.violet.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MySQLConnectionBuilder {
    private String host = "localhost";
    private int port = 3306;
    private String database = null;
    private String username = null;
    private String password = null;
    private final Map<String, String> params = new HashMap<>();

    public MySQLConnectionBuilder host(String host) {
        this.host = host;
        return this;
    }

    public MySQLConnectionBuilder port(int port) {
        this.port = port;
        return this;
    }

    public MySQLConnectionBuilder database(String database) {
        this.database = database;
        return this;
    }

    public MySQLConnectionBuilder username(String username) {
        this.username = username;
        return this;
    }

    public MySQLConnectionBuilder password(String password) {
        this.password = password;
        return this;
    }

    public MySQLConnectionBuilder setParam(String param, String value) {
        this.params.put(param, value);
        return this;
    }

    public String getParam(String param) {
        return this.params.get(param);
    }

    public Connection build() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // "com.mysql.jdbc.Driver" is deprecated over JDBC6.
            if (!this.params.containsKey("serverTimezone")) {
                this.params.put("serverTimezone", "Asia/Shanghai");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("jdbc:mysql://").append(this.host).append(':').append(this.port).append('/').append(this.database);
            if (!this.params.isEmpty()) {
                sb.append('?');
                this.params.forEach((key, value) -> sb.append(key).append('=').append(value).append('&'));
                sb.deleteCharAt(sb.length() - 1);
            }

            return DriverManager.getConnection(sb.toString(), this.username, this.password);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
