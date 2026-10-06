package me.uc.hussein.ultrasclans.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * يدير الاتصال بقاعدة البيانات (SQLite أو MySQL) عبر HikariCP،
 * وينشئ الجداول اللازمة إذا لم تكن موجودة.
 *
 * قاعدة أساسية: لا يُسمح بأي استدعاء لـ Bukkit API من داخل هذه الفئة
 * أو من الـExecutorService الخاص بها (راجع قسم 54 من المواصفات).
 * كل استدعاءات JDBC تمر هنا، وكل استدعاء Bukkit API يعود لاحقًا
 * عبر BukkitScheduler من الطبقة التي تستدعي هذه الفئة.
 */
public final class DatabaseManager {

    private final UltrasClansPlugin plugin;
    private HikariDataSource dataSource;
    private DatabaseType type;
    private final ExecutorService executor;

    public DatabaseManager(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "UltrasClans-DB-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newFixedThreadPool(4, factory);
    }

    public void connect() {
        ConfigurationSection dbSection = plugin.getConfigManager().get().getConfigurationSection("database");
        String typeString = dbSection != null ? dbSection.getString("type", "SQLITE") : "SQLITE";
        this.type = DatabaseType.fromString(typeString);

        HikariConfig hikariConfig = new HikariConfig();
        ConfigurationSection pool = dbSection != null ? dbSection.getConfigurationSection("pool") : null;
        int maxPoolSize = pool != null ? pool.getInt("maximum-pool-size", 10) : 10;
        int minIdle = pool != null ? pool.getInt("minimum-idle", 2) : 2;
        long connTimeout = pool != null ? pool.getLong("connection-timeout-ms", 10000) : 10000;
        long idleTimeout = pool != null ? pool.getLong("idle-timeout-ms", 600000) : 600000;
        long maxLifetime = pool != null ? pool.getLong("max-lifetime-ms", 1800000) : 1800000;

        if (type == DatabaseType.SQLITE) {
            String fileName = dbSection != null ? dbSection.getString("sqlite-file", "data.db") : "data.db";
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, fileName);
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            // SQLite لا يدعم اتصالات متعددة بالكتابة بشكل جيد، لذا نحصر
            // الـpool على اتصال واحد لتفادي "database is locked".
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setMinimumIdle(1);
        } else {
            ConfigurationSection mysql = dbSection != null ? dbSection.getConfigurationSection("mysql") : null;
            String host = mysql != null ? mysql.getString("host", "localhost") : "localhost";
            int port = mysql != null ? mysql.getInt("port", 3306) : 3306;
            String database = mysql != null ? mysql.getString("database", "ultras_clans") : "ultras_clans";
            String params = mysql != null ? mysql.getString("parameters", "") : "";
            String username = mysql != null ? mysql.getString("username", "root") : "root";
            String password = mysql != null ? mysql.getString("password", "") : "";

            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?" + params);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(maxPoolSize);
            hikariConfig.setMinimumIdle(minIdle);
        }

        hikariConfig.setConnectionTimeout(connTimeout);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setPoolName("UltrasClans-Pool");

        this.dataSource = new HikariDataSource(hikariConfig);
        runMigrations();
    }

    private void runMigrations() {
        String schema = type == DatabaseType.SQLITE ? Schema.SQLITE_SCHEMA : Schema.MYSQL_SCHEMA;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
            plugin.getLogger().info("Database schema is up to date (" + type + ").");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to run database migrations: " + e.getMessage());
            throw new IllegalStateException("Database migration failed", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public DatabaseType getType() {
        return type;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            // ننتظر انتهاء عمليات الحفظ المعلقة (مثل حفظ المخزن) قبل إغلاق الاتصال
            if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Some database tasks did not finish before shutdown.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
