package com.medicare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.awt.GraphicsEnvironment;
import java.net.Socket;
import java.net.InetSocketAddress;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class PharmacyApplication {

    public static void main(String[] args) {
        boolean headless = false;
        for (String arg : args) {
            if ("--headless".equalsIgnoreCase(arg)) {
                headless = true;
                break;
            }
        }

        // Windows: always open the native desktop window unless --headless is passed.
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (!headless && !isWindows) {
            try {
                if (GraphicsEnvironment.isHeadless()) {
                    headless = true;
                }
            } catch (Throwable t) {
                headless = true;
            }
        }

        if (headless) {
            // Detect and configure database profile (MySQL vs H2 fallback)
            checkAndConfigureDatabase();
            System.setProperty("java.awt.headless", "true");
            SpringApplication.run(PharmacyApplication.class, args);
            System.out.println("""

                    ====================================================================
                             MediCare Pharmacy System  v1.0.0 (Server)
                             Running on http://localhost:8080/api  
                             Swagger UI: /api/swagger-ui.html     
                    ====================================================================
                    """);
        } else {
            // Boot the JavaFX application window, which in turn boots Spring Boot
            DesktopApp.main(args);
        }
    }

    public static void checkAndConfigureDatabase() {
        // First: apply any config explicitly saved via the in-app Database Settings panel
        com.medicare.service.impl.DatabaseConfigService.applyStoredConfig();

        if (System.getProperty("spring.profiles.active") != null) {
            // Profile already decided — either by saved config or -D flag
            return;
        }

        // Auto-detect: probe MySQL (3306) first, then SQL Server (1433), else H2 fallback
        // MySQL check first — most common case, no auth needed to probe
        if (probePort("127.0.0.1", 3306, 200)) {
            System.out.println("[DB-AUTO] MySQL detected on port 3306. Verifying default credentials...");
            // Test connection with default passwords ("your_password_here" or "")
            if (testMysqlConnection("your_password_here") || testMysqlConnection("")) {
                System.out.println("[DB-AUTO] Default credentials verified. Using MySQL configuration.");
                return; // default application.properties handles MySQL
            } else {
                System.out.println("[DB-AUTO] MySQL port 3306 is open, but connection failed (Access Denied).");
                System.out.println("[DB-AUTO] Falling back to portable H2 database. Please configure custom credentials in settings.");
            }
        }

        // SQL Server — try Windows Authentication (same as SSMS default login)
        if (probePort("127.0.0.1", 1433, 200)) {
            System.out.println("[DB-AUTO] SQL Server detected on port 1433. Attempting Windows Authentication...");
            try {
                String url = "jdbc:sqlserver://localhost:1433;databaseName=PharmacyDB"
                           + ";integratedSecurity=true;trustServerCertificate=true;encrypt=true";
                java.sql.Connection c = java.sql.DriverManager.getConnection(url);
                c.close();
                // Windows auth works — use it
                System.setProperty("spring.datasource.url",                  url);
                System.setProperty("spring.datasource.username",             "");
                System.setProperty("spring.datasource.password",             "");
                System.setProperty("spring.datasource.driver-class-name",    "com.microsoft.sqlserver.jdbc.SQLServerDriver");
                System.setProperty("spring.jpa.properties.hibernate.dialect","org.hibernate.dialect.SQLServerDialect");
                System.setProperty("spring.profiles.active",                 "sqlserver");
                System.out.println("[DB-AUTO] Connected to SQL Server (PharmacyDB) via Windows Authentication.");
                return;
            } catch (Exception e) {
                System.out.println("[DB-AUTO] SQL Server found but Windows Auth failed: " + e.getMessage());
                System.out.println("[DB-AUTO] Use the Database tab inside the app to configure SQL Server credentials.");
            }
        }

        // Fallback
        System.out.println("[DB-AUTO] No local database detected. Activating portable H2 fallback.");
        System.setProperty("spring.profiles.active", "h2");
    }

    private static boolean probePort(String host, int port, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean testMysqlConnection(String password) {
        String url = "jdbc:mysql://127.0.0.1:3306/medicare_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, "root", password)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}


