package ui;

import core.AppConfig;
import core.MainSceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.HttpURLConnection;
import java.net.URL;

public class ServerIpPane extends VBox {

    private final TextField ipField = new TextField();
    private final Label errorLabel = new Label();

    public ServerIpPane() {
        setSpacing(10);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        ImageView brandText = new ImageView(new Image(getClass().getResource("/lanflix.png").toExternalForm()));
        VBox.setMargin(brandText, new Insets(0, 0, -10, 0));
        brandText.setFitWidth(400);
        brandText.setPreserveRatio(true);

        ipField.setPromptText("Enter Server IP");
        ipField.setMaxWidth(300);
        ipField.setStyle(
                "-fx-background-radius: 8; -fx-padding: 10;" +
                        "-fx-text-fill: white; -fx-font-size: 16px;" +
                        "-fx-background-color: rgba(255,255,255,0.1);"
        );

        Button connectButton = new Button("Connect");
        connectButton.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");
        connectButton.setOnAction(e -> attemptConnection(ipField.getText().trim()));

        errorLabel.setStyle("-fx-text-fill: red;");

        getChildren().addAll(brandText, ipField, connectButton, errorLabel);
    }

    private void attemptConnection(String ip) {
        if (ip.isEmpty()) {
            errorLabel.setText("Please enter a server IP address.");
            return;
        }

        if (isServerReachable(ip)) {
            AppConfig.setServerIp(ip);
            AppConfig.saveServerIp(ip);
            MainSceneManager.setView(new LoginPane());
        } else {
            errorLabel.setText("Server not reachable at " + ip);
        }
    }

    private boolean isServerReachable(String ip) {
        try {
            URL url = new URL("http://" + ip + ":8080/list");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
