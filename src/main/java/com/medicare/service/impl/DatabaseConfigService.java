package com.medicare.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

@Service
@Slf4j
public class DatabaseConfigService {

    // Config file stored in the user's home dir so it persists across restarts
    private static final Path CONFIG_FILE = Paths.get(
        System.getProperty("user.home"), ".medicare", "db-config.properties"
    );

    @Value("${spring.datasource.url}")
    private String activeUrl;

    @Value("${spring.datasource.username}")
    private String activeUser;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    // -------------------------------------------------------
    //  Public API
    // -------------------------------------------------------

    public Map<String, Object> getCurrentStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeProfile", resolveProfileLabel(activeProfile, activeUrl));
        result.put("url", activeUrl);
        result.put("username", activeUser);
        result.put("savedConfig", loadSavedConfig());
        return result;
    }

    public Map<String, Object> testConnection(Map<String, String> config) {
        String db   = config.getOrDefault("db", "mysql");
        String host = config.getOrDefault("host", "localhost");
        String port = config.getOrDefault("port", defaultPort(db));
        String name = config.getOrDefault("dbName", defaultDbName(db));
        String user = config.getOrDefault("username", "root");
        String pass = config.getOrDefault("password", "");

        String jdbcUrl = buildUrl(db, host, port, name);
        String driver  = driverClass(db);

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Class.forName(driver);
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
                result.put("success", true);
                result.put("message", "Connection successful — " + conn.getMetaData().getDatabaseProductName()
                    + " " + conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (ClassNotFoundException e) {
            result.put("success", false);
            result.put("message", "JDBC driver not found for: " + db);
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> saveAndRestart(Map<String, String> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            writeConfigFile(config);
            result.put("success", true);
            result.put("message", "Configuration saved. The application will restart in 2 seconds to apply the new database.");
            scheduleRestart();
        } catch (Exception e) {
            log.error("Failed to save DB config", e);
            result.put("success", false);
            result.put("message", "Failed to save config: " + e.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------
    //  Static helper — called from PharmacyApplication on boot
    // -------------------------------------------------------

    public static void applyStoredConfig() {
        if (!Files.exists(CONFIG_FILE)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("[DB-CONFIG] Could not read " + CONFIG_FILE + ": " + e.getMessage());
            return;
        }

        String db     = props.getProperty("db", "mysql");
        String host   = props.getProperty("host", "localhost");
        String port   = props.getProperty("port", defaultPortStatic(db));
        String name   = props.getProperty("dbName", defaultDbNameStatic(db));
        String user   = props.getProperty("username", "root");
        String pass   = props.getProperty("password", "");

        String url    = buildUrlStatic(db, host, port, name);
        String driver = driverClassStatic(db);

        System.setProperty("spring.datasource.url",                 url);
        System.setProperty("spring.datasource.username",            user);
        System.setProperty("spring.datasource.password",            pass);
        System.setProperty("spring.datasource.driver-class-name",   driver);
        System.setProperty("spring.jpa.properties.hibernate.dialect", dialectStatic(db));

        // Suppress the profile auto-detection in PharmacyApplication
        System.setProperty("spring.profiles.active", profileForDb(db));

        System.out.println("[DB-CONFIG] Applying stored config: " + db.toUpperCase()
            + " @ " + host + ":" + port + "/" + name);
    }

    // -------------------------------------------------------
    //  Private helpers
    // -------------------------------------------------------

    private void writeConfigFile(Map<String, String> config) throws IOException {
        Files.createDirectories(CONFIG_FILE.getParent());
        Properties props = new Properties();
        props.putAll(config);
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "MediCare DB Config — written by DatabaseConfigService");
        }
        log.info("DB config written to {}", CONFIG_FILE);
    }

    private Map<String, String> loadSavedConfig() {
        if (!Files.exists(CONFIG_FILE)) return Collections.emptyMap();
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
            Map<String, String> map = new LinkedHashMap<>();
            props.forEach((k, v) -> map.put(k.toString(),
                k.toString().equalsIgnoreCase("password") ? "••••••" : v.toString()));
            return map;
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    private void scheduleRestart() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(2000);
                log.info("Restarting application to apply new database configuration...");
                // Restart by exiting — the launcher (Launch-MediCare.bat / EXE) should restart automatically.
                // We flush logs first, then exit with code 3 (custom restart code).
                System.exit(3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.setDaemon(false);
        t.setName("db-restart-thread");
        t.start();
    }

    private String resolveProfileLabel(String profile, String url) {
        if (profile != null && !profile.equals("default")) return profile;
        if (url != null && url.contains("sqlserver")) return "sqlserver";
        if (url != null && url.contains("mysql"))     return "mysql";
        if (url != null && url.contains("h2"))        return "h2";
        return "mysql";
    }

    // ---- Instance versions ----
    private String buildUrl(String db, String host, String port, String name) {
        return buildUrlStatic(db, host, port, name);
    }

    private String driverClass(String db) {
        return driverClassStatic(db);
    }

    private String defaultPort(String db) {
        return defaultPortStatic(db);
    }

    private String defaultDbName(String db) {
        return defaultDbNameStatic(db);
    }

    // ---- Static versions (usable before bean init) ----
    private static String buildUrlStatic(String db, String host, String port, String name) {
        return switch (db.toLowerCase()) {
            case "sqlserver" ->
                "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + name
                + ";trustServerCertificate=true;encrypt=true";
            case "sqlserver-winauth" ->
                "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + name
                + ";integratedSecurity=true;trustServerCertificate=true;encrypt=true";
            case "h2" ->
                "jdbc:h2:file:~/.medicare/medicare_db"
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL";
            default -> // mysql
                "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?createDatabaseIfNotExist=true&useSSL=false"
                + "&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        };
    }

    private static String driverClassStatic(String db) {
        return switch (db.toLowerCase()) {
            case "sqlserver", "sqlserver-winauth" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "h2"        -> "org.h2.Driver";
            default          -> "com.mysql.cj.jdbc.Driver";
        };
    }

    private static String dialectStatic(String db) {
        return switch (db.toLowerCase()) {
            case "sqlserver", "sqlserver-winauth" -> "org.hibernate.dialect.SQLServerDialect";
            case "h2"        -> "org.hibernate.dialect.H2Dialect";
            default          -> "org.hibernate.dialect.MySQLDialect";
        };
    }

    private static String profileForDb(String db) {
        return switch (db.toLowerCase()) {
            case "sqlserver", "sqlserver-winauth" -> "sqlserver";
            case "h2"        -> "h2";
            default          -> "default";
        };
    }

    private static String defaultPortStatic(String db) {
        return switch (db.toLowerCase()) {
            case "sqlserver", "sqlserver-winauth" -> "1433";
            case "h2"        -> "";
            default          -> "3306";
        };
    }

    private static String defaultDbNameStatic(String db) {
        return switch (db.toLowerCase()) {
            case "sqlserver", "sqlserver-winauth" -> "PharmacyDB";
            case "h2"        -> "medicare_db";
            default          -> "medicare_db";
        };
    }
}
