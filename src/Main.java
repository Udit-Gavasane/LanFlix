import core.AppConfig;
import core.MainSceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import server.WatchPartyWebSocketEndpoint;
import ui.LoginPane;
import ui.ServerIpPane;

import java.net.HttpURLConnection;
import java.net.URL;

public class Main extends Application {

    private WatchPartyWebSocketEndpoint wsServer;

    @Override
    public void start(Stage primaryStage) {
        // Set up stage first
//        wsServer = new WatchPartyWebSocketEndpoint(8090);
//        wsServer.start();

        MainSceneManager.init(primaryStage);
        primaryStage.setMaximized(true);

        // Load icon immediately
        try {
            Image icon = new Image(getClass().getResourceAsStream("/app_logo_final.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Failed to load application icon: " + e.getMessage());
        }

        // Show initial view immediately
        MainSceneManager.setView(new ServerIpPane());

        // Check server connection in background
        AppConfig.loadLastServerIp();
        new Thread(() -> {
            if (isServerReachable(AppConfig.getServerIp())) {
                Platform.runLater(() -> MainSceneManager.setView(new LoginPane()));
            }
        }).start();
    }

    private boolean isServerReachable(String ip) {
        try {
            if (ip == null || ip.isBlank()) return false;
            URL url = new URL("http://" + ip + ":8080/list");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        // Clean up WebSocket server on application shutdown
        if (wsServer != null) {
            try {
                wsServer.stop();
            } catch (Exception e) {
                System.err.println("Error stopping WebSocket server: " + e.getMessage());
            }
        }
    }

}