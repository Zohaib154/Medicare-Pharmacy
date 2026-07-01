package com.medicare;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.Taskbar;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;

public class DesktopApp extends Application {

    private static final String APP_ICON_RESOURCE = "/static/assets/logo.png";

    private static ConfigurableApplicationContext springContext;

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }

    private volatile String allowedOriginPrefix;

    @Override
    public void init() throws Exception {
        System.setProperty("java.awt.headless", "false");

        // Enable lazy initialization to reduce peak CPU/RAM usage during startup
        if (System.getProperty("spring.main.lazy-initialization") == null) {
            System.setProperty("spring.main.lazy-initialization", "false");
        }
        if (System.getProperty("spring.jmx.enabled") == null) {
            System.setProperty("spring.jmx.enabled", "false");
        }
        if (System.getProperty("spring.main.banner-mode") == null) {
            System.setProperty("spring.main.banner-mode", "off");
        }

        if (System.getProperty("server.port") == null) {
            System.setProperty("server.port", "0");
        }
        if (System.getProperty("server.address") == null) {
            System.setProperty("server.address", "127.0.0.1");
        }
    }

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);

        try {
            java.io.File userDataDir = new java.io.File(System.getProperty("user.home") + "/.medicare/webview");
            if (!userDataDir.exists()) {
                userDataDir.mkdirs();
            }
            webView.getEngine().setUserDataDirectory(userDataDir);
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to set WebEngine UserDataDirectory: " + e.getMessage());
        }

        webView.getEngine().setCreatePopupHandler(popupFeatures -> null);
        webView.getEngine().setUserAgent("MediCareDesktop/1.0");

        webView.getEngine().locationProperty().addListener((observable, oldLocation, newLocation) -> {
            if (newLocation == null || allowedOriginPrefix == null) {
                return;
            }
            if (newLocation.startsWith("about:") || newLocation.startsWith("data:")) {
                return;
            }
            if (!newLocation.startsWith(allowedOriginPrefix)) {
                Platform.runLater(() -> {
                    if (oldLocation != null && !oldLocation.isBlank()) {
                        webView.getEngine().load(oldLocation);
                    }
                });
            }
        });

        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
                injectConsoleBridge(webView);
            }
        });

        webView.getEngine().loadContent(buildLoadingHtml());

        String[] args = getParameters().getRaw().toArray(new String[0]);
        Thread springThread = new Thread(() -> {
            try {
                PharmacyApplication.checkAndConfigureDatabase();
                springContext = new SpringApplicationBuilder(PharmacyApplication.class)
                        .headless(false)
                        .run(args);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to start MediCare background services: " + e.getMessage());
            }
        });
        springThread.setDaemon(true);
        springThread.start();

        Thread monitorThread = new Thread(() -> waitForServerAndLoadApp(webView));
        monitorThread.setDaemon(true);
        monitorThread.start();

        Scene scene = new Scene(webView, 1366, 768);
        DesktopServerBridge.setMainStage(stage);
        stage.setScene(scene);
        stage.setTitle("MediCare Pharmacy Management System");
        stage.setMinWidth(1024);
        stage.setMinHeight(600);
        stage.setResizable(true);

        applyAppIcon(stage);

        stage.setOnCloseRequest(e -> {
            e.consume();
            shutdownApplication();
        });

        stage.show();
    }

    private static void shutdownApplication() {
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
        System.exit(0);
    }

    private void waitForServerAndLoadApp(WebView webView) {
        boolean serverReady = false;
        for (int i = 0; i < 600; i++) {
            if (!DesktopServerBridge.isReady()) {
                sleepQuietly(100);
                continue;
            }

            allowedOriginPrefix = DesktopServerBridge.apiBaseUrl();
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(DesktopServerBridge.healthUrl()).openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                if (conn.getResponseCode() == 200) {
                    serverReady = true;
                    break;
                }
            } catch (Exception ignored) {
            }
            sleepQuietly(100);
        }

        if (serverReady) {
            String frontendUrl = DesktopServerBridge.indexUrl();
            Platform.runLater(() -> webView.getEngine().load(frontendUrl));
        } else {
            Platform.runLater(() -> webView.getEngine().loadContent(
                "<html><body style='background-color:#f4f8f4;color:#1a2e1a;font-family:Segoe UI,sans-serif;text-align:center;padding-top:120px;'>" +
                "<h2 style='color:#c0392b;margin-bottom:10px;'>Failed to start MediCare</h2>" +
                "<p style='color:#4a5f4a;margin-bottom:6px;'>Please close other running instances or restart your computer, then try again.</p>" +
                "<p style='color:#6b7f6b;font-size:12px;'>If the issue persists, contact support.</p>" +
                "</body></html>"
            ));
        }
    }

    public static class JavaBridge {
        private final WebView webView;

        public JavaBridge(WebView webView) {
            this.webView = webView;
        }

        public void log(String text) {
            System.out.println("[JS LOG] " + text);
        }

        public void error(String text) {
            System.err.println("[JS ERROR] " + text);
        }

        public void minimize() {
            Platform.runLater(() -> {
                Stage s = DesktopServerBridge.getMainStage();
                if (s != null) {
                    s.setIconified(true);
                }
            });
        }

        public void maximize() {
            Platform.runLater(() -> {
                Stage s = DesktopServerBridge.getMainStage();
                if (s != null) {
                    s.setMaximized(!s.isMaximized());
                }
            });
        }

        public void close() {
            Platform.runLater(DesktopApp::shutdownApplication);
        }

        public void savePdf(String base64Data, String filename) {
            Platform.runLater(() -> {
                try {
                    Stage s = DesktopServerBridge.getMainStage();
                    if (s == null) {
                        System.err.println("[JS ERROR] Cannot save PDF: main stage unavailable");
                        return;
                    }
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Save Invoice PDF");
                    chooser.setInitialFileName(filename != null && !filename.isBlank() ? filename : "RxPro-Invoice.pdf");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                    java.io.File file = chooser.showSaveDialog(s);
                    if (file != null) {
                        String path = file.getAbsolutePath();
                        if (!path.toLowerCase().endsWith(".pdf")) {
                            path += ".pdf";
                            file = new java.io.File(path);
                        }
                        Files.write(file.toPath(), Base64.getDecoder().decode(base64Data));
                        System.out.println("[JS LOG] PDF saved: " + path);

                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.initOwner(s);
                        alert.setTitle("Success");
                        alert.setHeaderText(null);
                        alert.setContentText("PDF invoice saved successfully:\n" + path);
                        alert.showAndWait();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println("[JS ERROR] Failed to save PDF: " + ex.getMessage());

                    Stage s = DesktopServerBridge.getMainStage();
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    if (s != null) alert.initOwner(s);
                    alert.setTitle("Save Error");
                    alert.setHeaderText("Failed to save PDF");
                    alert.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                    alert.showAndWait();
                }
            });
        }

        public void saveFile(String base64Data, String filename, String extension, String filterDescription) {
            Platform.runLater(() -> {
                try {
                    Stage s = DesktopServerBridge.getMainStage();
                    if (s == null) {
                        System.err.println("[JS ERROR] Cannot save file: main stage unavailable");
                        return;
                    }
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Save File");
                    chooser.setInitialFileName(filename != null && !filename.isBlank() ? filename : "export" + extension);
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterDescription, "*" + extension));
                    java.io.File file = chooser.showSaveDialog(s);
                    if (file != null) {
                        String path = file.getAbsolutePath();
                        if (!path.toLowerCase().endsWith(extension.toLowerCase())) {
                            path += extension;
                            file = new java.io.File(path);
                        }
                        Files.write(file.toPath(), Base64.getDecoder().decode(base64Data));
                        System.out.println("[JS LOG] File saved: " + path);

                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.initOwner(s);
                        alert.setTitle("Success");
                        alert.setHeaderText(null);
                        alert.setContentText("File saved successfully:\n" + path);
                        alert.showAndWait();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println("[JS ERROR] Failed to save file: " + ex.getMessage());

                    Stage s = DesktopServerBridge.getMainStage();
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    if (s != null) alert.initOwner(s);
                    alert.setTitle("Save Error");
                    alert.setHeaderText("Failed to save file");
                    alert.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                    alert.showAndWait();
                }
            });
        }
    }

    private static void injectConsoleBridge(WebView webView) {
        try {
            netscape.javascript.JSObject window = (netscape.javascript.JSObject) webView.getEngine().executeScript("window");
            window.setMember("javaBridge", new JavaBridge(webView));
            webView.getEngine().executeScript(
                "console.log = function(message) { window.javaBridge.log(String(message)); };" +
                "console.error = function(message) { window.javaBridge.error(String(message)); };" +
                "window.onerror = function(message, source, lineno, colno, error) {" +
                "   window.javaBridge.error(String(message) + ' at ' + String(source) + ':' + lineno);" +
                "   return false;" +
                "};"
            );
        } catch (Exception ex) {
            System.err.println("[WARNING] Failed to inject javaBridge console redirect: " + ex.getMessage());
        }
    }

    private void applyAppIcon(Stage stage) {
        try (InputStream fxIconStream = getClass().getResourceAsStream(APP_ICON_RESOURCE)) {
            if (fxIconStream == null) {
                System.err.println("[WARNING] Application icon resource not found: " + APP_ICON_RESOURCE);
                return;
            }
            stage.getIcons().add(new javafx.scene.image.Image(fxIconStream));
        } catch (Exception e) {
            System.err.println("[WARNING] Failed to load application window icon: " + e.getMessage());
        }

        try (InputStream awtIconStream = getClass().getResourceAsStream(APP_ICON_RESOURCE)) {
            if (awtIconStream == null) {
                return;
            }
            if (Taskbar.isTaskbarSupported()) {
                Taskbar.getTaskbar().setIconImage(javax.imageio.ImageIO.read(awtIconStream));
            }
        } catch (Exception e) {
            System.err.println("[WARNING] Failed to apply taskbar icon: " + e.getMessage());
        }
    }

    private String buildLoadingHtml() {
        String logoDataUri = loadLogoDataUri();
        String logoBlock = logoDataUri == null
            ? "<div style='color:#3d8b37; font-size:42px; font-family:sans-serif;'>Rx</div>"
            : "<img class='logo-img' src='" + logoDataUri + "' alt='Rx'/>";

        return "<!DOCTYPE html><html><head>" +
            "<style>" +
            "body { background-color: #f4f8f4; color: #1a2e1a; font-family: Segoe UI, sans-serif; " +
            "display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }" +
            ".container { text-align: center; }" +
            ".logo-img { width: 80px; height: 80px; margin-bottom: 20px; }" +
            ".title { font-size: 28px; font-weight: 600; color: #3d8b37; margin-top: 10px; }" +
            ".subtitle { font-size: 14px; color: #6b7f6b; margin-top: 4px; }" +
            ".progress { width: 180px; height: 3px; background: #d4e8d4; border-radius: 2px; margin: 40px auto 0; overflow: hidden; }" +
            ".bar { width: 40%; height: 100%; background: #3d8b37; }" +
            "</style></head><body><div class='container'>" +
            logoBlock +
            "<div class='title'>MediCare</div>" +
            "<div class='subtitle'>Pharmacy Management System</div>" +
            "<div class='progress'><div class='bar'></div></div>" +
            "</div></body></html>";
    }

    private String loadLogoDataUri() {
        try (InputStream in = getClass().getResourceAsStream(APP_ICON_RESOURCE)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(buffer.toByteArray());
        } catch (Exception e) {
            System.err.println("[WARNING] Failed to read logo resource: " + e.getMessage());
            return null;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() throws Exception {
        if (springContext != null) {
            springContext.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
