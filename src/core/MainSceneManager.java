package core;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MainSceneManager {

    private static Stage primaryStage;
    private static Scene mainScene;

    // Initialize from Main.java
    public static void init(Stage stage) {
        primaryStage = stage;
        mainScene = new Scene(new Pane(), 800, 600);
        primaryStage.setTitle("LanFlix - Your Personal Movie Streaming App");
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    // Set a new screen
    public static void setView(Pane newView) {
        mainScene.setRoot(newView);
    }

}
