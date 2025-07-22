package ui;

import core.AppConfig;
import core.MainSceneManager;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;



public class VideoPlayerPane extends BorderPane {

    private int retryCount = 1;
    private static final int MAX_RETRIES = 1;
    private boolean userExitedFullscreen = false;
    private final Label overlayTitleLabel = new Label();
    private PauseTransition hideControlsTimer = new PauseTransition(Duration.seconds(3));


    public VideoPlayerPane(String movieUrl, String movieTitle) {
        this(movieUrl, movieTitle, 0);  // default retryCount = 0
    }


    public VideoPlayerPane(String movieUrl, String movieTitle, int retryCount) {
        this.retryCount = retryCount;
        setStyle("-fx-background-color: black;");

        //Media media = new Media(movieUrl);
        Media media = new Media(java.net.URI.create(movieUrl).toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnError(() -> {
            System.err.println("MediaPlayer error: " + mediaPlayer.getError());

            // Exit fullscreen before showing dialog
            Stage stage = (Stage) getScene().getWindow();
            if (stage != null && stage.isFullScreen()) {
                stage.setFullScreen(false);
            }

            if (this.retryCount <= MAX_RETRIES) {
                showPlaybackErrorDialog(stage, movieUrl, movieTitle);
            } else {
                Alert failAlert = new Alert(Alert.AlertType.ERROR);
                failAlert.setTitle("Playback Failed");
                failAlert.setHeaderText("Something went wrong on the server.");
                failAlert.setContentText("Please try another movie.");
                failAlert.showAndWait();
                MainSceneManager.setView(new MovieBrowserPane(extractIpFromUrl(movieUrl), core.AppConfig.getUsername()));
            }
        });

        MediaView mediaView = new MediaView(mediaPlayer);
        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);
        mediaView.fitWidthProperty().bind(widthProperty());
        mediaView.fitHeightProperty().bind(heightProperty().subtract(80));

        // Playback controls
        Button playPauseBtn = new Button("⏸ Pause");
        playPauseBtn.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");

        playPauseBtn.setOnAction(e -> {
            MediaPlayer.Status status = mediaPlayer.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPauseBtn.setText("▶ Play");
            } else {
                mediaPlayer.play();
                playPauseBtn.setText("⏸ Pause");
            }
        });

        ProgressBar volumeProgress = new ProgressBar(1.0); // initial volume
        volumeProgress.setPrefHeight(10);
        volumeProgress.setPrefWidth(160); // adjust size as needed
        volumeProgress.setStyle(
                "-fx-accent: #FF9500; " +
                        "-fx-control-inner-background: #222; " +
                        "-fx-background-color: transparent;"
        );

        Slider volumeSlider = new Slider(0, 1, 1.0);
        volumeSlider.getStyleClass().add("custom-slider");
        mediaPlayer.setVolume(1.0);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            mediaPlayer.setVolume(newVal.doubleValue());
            volumeProgress.setProgress(newVal.doubleValue());
        });

        Label currentTimeLabel = new Label("00:00");
        currentTimeLabel.setStyle("-fx-text-fill: white;");

        Label totalTimeLabel = new Label("00:00");
        totalTimeLabel.setStyle("-fx-text-fill: white;");

        // Create the progress bar (fill background)
        ProgressBar seekProgress = new ProgressBar(0);
        seekProgress.setMaxWidth(Double.MAX_VALUE);
        seekProgress.setPrefHeight(10);
        seekProgress.setPrefWidth(300);
        seekProgress.setStyle(
                "-fx-accent: #FF9500; " +
                        "-fx-control-inner-background: #222; " +
                        "-fx-background-color: transparent;"
                        //"-fx-background-insets: -10;"
        );

        Slider seekSlider = new Slider();
        seekSlider.getStyleClass().add("custom-slider");
        seekSlider.setPrefWidth(300);
        seekSlider.setMin(0);

        // Stack them together
        StackPane seekBarStack = new StackPane(seekSlider, seekProgress);
        StackPane.setAlignment(seekProgress, Pos.CENTER_LEFT);
        StackPane.setAlignment(seekSlider, Pos.CENTER);
        seekBarStack.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(seekBarStack, Priority.ALWAYS);

        StackPane volumeBarStack = new StackPane(volumeSlider, volumeProgress);
        StackPane.setAlignment(volumeProgress, Pos.CENTER_LEFT);
        StackPane.setAlignment(volumeSlider, Pos.CENTER);

        Label timePopup = new Label("00:00");
        timePopup.setStyle(
                "-fx-background-color: #FF9500; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 4 8 4 8; " +
                        "-fx-background-radius: 10; " +
                        "-fx-font-weight: bold;"
        );
        timePopup.setVisible(false);
        timePopup.setMouseTransparent(true);

        // Update slider + time
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            Duration total = mediaPlayer.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                if (!seekSlider.isValueChanging()) {
                    //seekSlider.setValue(newTime.toSeconds() / total.toSeconds() * 100);
                    double progress = newTime.toSeconds() / total.toSeconds();
                    seekSlider.setValue(progress * 100);
                    seekProgress.setProgress(progress);
                }
                currentTimeLabel.setText(formatTime(newTime));
                totalTimeLabel.setText(formatTime(total));
            }
        });

        seekSlider.setOnMousePressed(e -> mediaPlayer.pause());
        seekSlider.setOnMouseDragged(e -> {
            Duration total = mediaPlayer.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                double seekTime = seekSlider.getValue() / 100.0 * total.toSeconds();
                timePopup.setText(formatTime(Duration.seconds(seekTime)));
                timePopup.setVisible(true);

                // Optional: position near thumb/mouse
                timePopup.setTranslateX(e.getSceneX() - getScene().getWidth() / 2);
                timePopup.setTranslateY(5); // 40px above slider
            }
        });


        seekSlider.setOnMouseReleased(e -> {
            Duration total = mediaPlayer.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                double seekTime = seekSlider.getValue() / 100.0 * total.toSeconds();
                mediaPlayer.seek(Duration.seconds(seekTime));
            }
            mediaPlayer.play();
            timePopup.setVisible(false);
        });

        // Back button
        Button backBtn = new Button("🔙 Back");
        backBtn.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");
        backBtn.setOnAction(e -> {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            // Exit fullscreen explicitly before switching panes
            Stage stage = (Stage) getScene().getWindow();
            if (stage != null) {
                stage.setFullScreen(false);
            }
            MainSceneManager.setView(new MovieBrowserPane(extractIpFromUrl(movieUrl), core.AppConfig.getUsername()));
        });

        // Control layout
        HBox controls = new HBox(15, playPauseBtn, currentTimeLabel, seekBarStack, totalTimeLabel, volumeBarStack, backBtn);
        controls.setStyle("-fx-background-color: rgba(20, 20, 20, 0.8); -fx-border-color: #444;");
        HBox.setHgrow(seekSlider, Priority.ALWAYS);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: #222; -fx-border-color: #444;");


        VBox controlContainer = new VBox(5);
        controlContainer.setAlignment(Pos.BOTTOM_CENTER);
        controlContainer.setPadding(new Insets(10));
        controlContainer.getChildren().addAll(timePopup, controls);

        //setCenter(mediaView);
        //setBottom(controls);


        mediaPlayer.play();


        // Now Playing Banner
        Label nowPlayingLabel = new Label("🎬 Now Playing: " + movieTitle);
        nowPlayingLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 20));
        nowPlayingLabel.setTextFill(Color.web("#FFD500")); // bright yellow
        nowPlayingLabel.setPadding(new Insets(10));
        nowPlayingLabel.setVisible(false);

        // Frosted glass effect using blur and semi-transparent background
        nowPlayingLabel.setBackground(new Background(new BackgroundFill(
                Color.rgb(30, 30, 30, 0.15), new CornerRadii(12), Insets.EMPTY
        )));
        nowPlayingLabel.setEffect(new javafx.scene.effect.BoxBlur(10, 10, 3));
        nowPlayingLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 10, 0.5, 0, 0);");

        // Position it top center
        StackPane.setAlignment(nowPlayingLabel, Pos.CENTER);


        StackPane overlay = new StackPane();
        // Show controls when mouse moves
        overlay.setOnMouseMoved(e -> {
            controls.setVisible(true);
            controls.setManaged(true);

            hideControlsTimer.stop();
            hideControlsTimer.setOnFinished(event -> {
                controls.setVisible(false);
                controls.setManaged(false);
            });
            hideControlsTimer.play();
        });

        overlay.getChildren().addAll(mediaView, nowPlayingLabel, controlContainer);  // Media + overlay label
        StackPane.setAlignment(controlContainer, Pos.TOP_CENTER);
        setCenter(overlay);



        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getStylesheets().add(getClass().getResource("/SeekSlider.css").toExternalForm());
                Stage stage = (Stage) newScene.getWindow();
                if (stage != null) {
                    // Go fullscreen initially
                    stage.setFullScreenExitHint(""); // Hide "Press ESC to exit"
                    stage.setFullScreen(true);

                    // Listen for ESC key press to toggle fullscreen
                    newScene.setOnKeyPressed(event -> {
                        if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                            boolean nowFullScreen = !stage.isFullScreen();
                            stage.setFullScreen(nowFullScreen);
                        }
                    });
                }

                // Request focus so ESC key works
                this.setFocusTraversable(true);
                this.requestFocus();
            }
        });

        overlay.setOnMouseClicked(e -> {
            nowPlayingLabel.setVisible(true);

            PauseTransition hideLabel = new PauseTransition(Duration.seconds(3));
            hideLabel.setOnFinished(evt -> nowPlayingLabel.setVisible(false));
            hideLabel.play();
        });


    }

    private void showPlaybackErrorDialog(Stage ownerStage, String movieUrl, String movieTitle) {
        Stage dialog = new Stage();
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.setResizable(false);
        //dialog.setTitle("Oops...!");

        // App icon
        dialog.getIcons().add(new Image(getClass().getResource("/app_logo_final.png").toExternalForm()));

        // Styling the layout
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);
        layout.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        Label title = new Label("OOPS...!");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 26));
        title.setTextFill(Color.web("#FFD500")); // Yellow
        title.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 0.5, 0, 0);");

        Label line1 = new Label("Looks like we threaded the reel onto the wrong projector — this one's not compatible with the film format.");
        line1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        line1.setTextFill(Color.WHITE);
        line1.setWrapText(true);
        line1.setTextAlignment(TextAlignment.CENTER);

        Label line2 = new Label("Would you like us to try loading it onto a different projector?");
        line2.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        line2.setTextFill(Color.WHITE);
        line2.setWrapText(true);
        line2.setTextAlignment(TextAlignment.LEFT);

        // Custom styled buttons
        Button yesBtn = new Button("Yes");
        Button noBtn = new Button("No");

        yesBtn.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");
        noBtn.setStyle("-fx-background-color: #FF9500; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox buttonBox = new HBox(20, yesBtn, noBtn);
        buttonBox.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(title, line1, line2, buttonBox);

        Scene dialogScene = new Scene(layout);
        dialog.setScene(dialogScene);

        // Button logic
        yesBtn.setOnAction(e -> {
            dialog.close();
            MainSceneManager.setView(new VideoPlayerPane(movieUrl, movieTitle, retryCount + 1));
        });

        noBtn.setOnAction(e -> {
            dialog.close();
            MainSceneManager.setView(new MovieBrowserPane(extractIpFromUrl(movieUrl), AppConfig.getUsername()));
        });

        dialog.show();
    }


    private static String formatTime(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String extractIpFromUrl(String url) {
        try {
            return url.split("//")[1].split(":")[0];
        } catch (Exception e) {
            return "localhost";
        }
    }
}
