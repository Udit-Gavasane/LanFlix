package ui;

import core.MainSceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import core.AppConfig;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginPane extends VBox {

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();

    public LoginPane() {
        setSpacing(10);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);

        //  Solid black background
        setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        // Large LanFlix word logo
        ImageView brandText = new ImageView(
                new Image(getClass().getResource("/lanflix.png").toExternalForm())
        );
        VBox.setMargin(brandText, new Insets(0, 0, -10, 0)); // slightly pulls it up
        brandText.setFitWidth(400); // Adjust as needed
        brandText.setPreserveRatio(true);


        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(300);
        //usernameField.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
        usernameField.setStyle(
                "-fx-background-radius: 8; " +
                        "-fx-padding: 10; " +
                        "-fx-text-fill: white; " +  // text color
                        "-fx-font-size: 16px;" +   // bigger font
                        "-fx-background-color: rgba(255, 255, 255, 0.1);" // subtle transparent glow
        );

        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);
        //passwordField.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
        passwordField.setStyle(
                "-fx-background-radius: 8; " +
                        "-fx-padding: 10; " +
                        "-fx-text-fill: white; " +  // text color
                        "-fx-font-size: 16px;" +   // bigger font
                        "-fx-background-color: rgba(255, 255, 255, 0.1);" // subtle transparent glow
        );


        Button loginButton = new Button("Login");
        Button signupButton = new Button("Sign Up");

        loginButton.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");
        signupButton.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox buttonRow = new HBox(15, loginButton, signupButton);
        buttonRow.setAlignment(Pos.CENTER);

        signupButton.setOnAction(e -> MainSceneManager.setView(new SignupPane()));
        loginButton.setOnAction(e -> attemptLogin());

        errorLabel.setStyle("-fx-text-fill: red;");

        getChildren().addAll(brandText, usernameField, passwordField, buttonRow, errorLabel);

    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter username and password.");
            return;
        }


        try {
            System.out.println("Attempting login at: http://" + AppConfig.getServerIp() + ":8080/login");
            URL url = new URL("http://" + AppConfig.getServerIp() + ":8080/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            String jsonInputString = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                errorLabel.setStyle("-fx-text-fill: green;");
                errorLabel.setText(" Login successful!");
                AppConfig.setUsername(username);
                MainSceneManager.setView(new MovieBrowserPane(AppConfig.getServerIp(), username));
            } else {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText(" Invalid username or password!");
            }

            conn.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
            errorLabel.setText(" Could not connect to server.");
        }
    }
}
