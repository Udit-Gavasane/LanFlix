package ui;

import core.MainSceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SignupPane extends VBox {

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final Label messageLabel = new Label();

    public SignupPane() {
        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);

        Label title = new Label("🔐 Create New Account");
        setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        title.setTextFill(Color.web("#FFD500"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));



        usernameField.setPromptText("Choose a username");
        usernameField.setMaxWidth(300);
        usernameField.setStyle(
                "-fx-background-radius: 8; " +
                        "-fx-padding: 10; " +
                        "-fx-text-fill: white; " +  // text color
                        "-fx-font-size: 16px;" +   // bigger font
                        "-fx-background-color: rgba(255, 255, 255, 0.1);" // subtle transparent glow
        );
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);
        passwordField.setStyle(
                "-fx-background-radius: 8; " +
                        "-fx-padding: 10; " +
                        "-fx-text-fill: white; " +  // text color
                        "-fx-font-size: 16px;" +   // bigger font
                        "-fx-background-color: rgba(255, 255, 255, 0.1);" // subtle transparent glow
        );
        confirmField.setPromptText("Confirm Password");
        confirmField.setMaxWidth(300);
        confirmField.setStyle(
                "-fx-background-radius: 8; " +
                        "-fx-padding: 10; " +
                        "-fx-text-fill: white; " +  // text color
                        "-fx-font-size: 16px;" +   // bigger font
                        "-fx-background-color: rgba(255, 255, 255, 0.1);" // subtle transparent glow
        );

        Button createButton = new Button("Create Account");
        createButton.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");
        createButton.setOnAction(e -> createAccount());

        Button backToLogin = new Button("← Back to Login");
        backToLogin.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");
        backToLogin.setOnAction(e -> MainSceneManager.setView(new LoginPane()));

        messageLabel.setStyle("-fx-text-fill: red;");

        getChildren().addAll(
                title,
                usernameField,
                passwordField,
                confirmField,
                createButton,
                backToLogin,
                messageLabel
        );
    }

    private void createAccount() {
        String username = usernameField.getText().trim();
        String pass = passwordField.getText();
        String confirm = confirmField.getText();

        if (username.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }

        if (!pass.equals(confirm)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        try {
            URL url = new URL("http://" + core.AppConfig.getServerIp() + ":8080/signup");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            String jsonInputString = "{\"username\":\"" + username + "\",\"password\":\"" + pass + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                messageLabel.setStyle("-fx-text-fill: green;");
                messageLabel.setText(" Account created successfully! You can now login.");
            } else if (responseCode == 409) {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText(" Username already exists!");
            } else {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText(" Something went wrong. Try again.");
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(" Could not connect to server.");
        }
    }

}
