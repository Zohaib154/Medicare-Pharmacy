package com.medicare;

import javafx.stage.Stage;

/**
 * Shares the embedded server's dynamic loopback port with the JavaFX desktop shell.
 * The port is never shown to the end user; it is used only inside the WebView.
 */
public final class DesktopServerBridge {

    private static volatile int port = -1;

    private DesktopServerBridge() {
    }

    public static void setPort(int serverPort) {
        port = serverPort;
    }

    public static int getPort() {
        return port;
    }

    public static boolean isReady() {
        return port > 0;
    }

    public static String apiBaseUrl() {
        return "http://127.0.0.1:" + port + "/api";
    }

    public static String indexUrl() {
        return apiBaseUrl() + "/index.html";
    }

    public static String healthUrl() {
        return apiBaseUrl() + "/actuator/health";
    }

    // Window Management Helpers
    private static Stage mainStage;
    public static void setMainStage(Stage stage) { mainStage = stage; }
    public static Stage getMainStage() { return mainStage; }
}


