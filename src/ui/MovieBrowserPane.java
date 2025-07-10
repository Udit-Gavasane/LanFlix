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
//import player.MoviePlayer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import server.MovieMetadata;

public class MovieBrowserPane extends VBox {

    public MovieBrowserPane(String serverIp, String username) {
        setSpacing(20);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_CENTER);

        String serverUrl = "http://" + serverIp + ":8080";

        Label title = new Label("Welcome, " + username + "!");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#FFD500"));
        setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));


        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search movies...");
        searchField.setMaxWidth(300);
        //title.setTextFill(Color.web("#FFD500"));
        //searchField.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
        searchField.setStyle(
                "-fx-background-radius: 8; " +
                        "-fx-padding: 10; " +
                        "-fx-text-fill: white; " +  // text color
                        "-fx-font-size: 16px;" +   // bigger font
                        "-fx-background-color: rgba(255, 255, 255, 0.1);" // subtle transparent glow
        );



        FlowPane movieFlow = new FlowPane();
        movieFlow.setHgap(30);
        movieFlow.setVgap(30);
        movieFlow.setPadding(new Insets(20));
        movieFlow.setAlignment(Pos.TOP_CENTER);
        movieFlow.setPrefWrapLength(1200);

        ScrollPane scrollPane = new ScrollPane(movieFlow);
        scrollPane.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 5;
            scrollPane.setVvalue(scrollPane.getVvalue() - deltaY / scrollPane.getContent().getBoundsInLocal().getHeight());
        });
        scrollPane.getContent().setStyle("-fx-background-color: black;");
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background: black; " +
                        "-fx-background-color: black; " +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0;"
        );
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        movieFlow.setStyle("-fx-background-color: black;");

        //List<String> movieList = fetchMovieList(serverUrl + "/list");
        List<MovieMetadata> movieList = fetchMovieList(serverUrl + "/list");
        List<MovieMetadata> allMovies = new ArrayList<>(movieList);
        allMovies.sort((m1, m2) -> m1.filename.compareToIgnoreCase(m2.filename));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal.toLowerCase();
            List<MovieMetadata> filtered = allMovies.stream()
                    .filter(m -> m.filename.toLowerCase().contains(query))
                    .toList();

            displayMovies(movieFlow, filtered, serverUrl);
        });

        if (movieList.isEmpty()) {
            Label errorLabel = new Label("No movies found or unable to connect.");
            errorLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #FFD500;"); // optional color
            getChildren().add(errorLabel);
            return;
        }

        displayMovies(movieFlow, allMovies, serverUrl);



        getChildren().addAll(title, searchField, scrollPane);
    }

    private List<MovieMetadata> fetchMovieList(String listUrl) {
        List<MovieMetadata> movies = new ArrayList<>();

        try {
            URL url = new URL(listUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            JSONArray array = new JSONArray(jsonBuilder.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String filename = obj.getString("filename");
                long size = obj.getLong("size");
                movies.add(new MovieMetadata(filename, size, 0));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return movies;

    }

    private String cleanMovieTitle(String rawName) {
        // Remove extension
        String nameWithoutExt = rawName.replaceAll("\\.[^.]+$", "");

        // Remove bracketed parts
        nameWithoutExt = nameWithoutExt.replaceAll("\\(.*?\\)", "")
                .replaceAll("\\[.*?\\]", "");

        // Remove extra dots, underscores, hyphens
        nameWithoutExt = nameWithoutExt.replaceAll("[._-]", " ");

        // Trim excess whitespace
        return nameWithoutExt.trim();
    }


    private void displayMovies(FlowPane flow, List<MovieMetadata> movies, String serverUrl) {
        flow.getChildren().clear();

        for (int i = 0; i < movies.size(); i++) {
            MovieMetadata movie = movies.get(i);
            String movieName = movie.filename;
            long size = movie.size;

            VBox movieBox = new VBox(10);
            movieBox.setAlignment(Pos.CENTER);
            movieBox.setSpacing(12);
            movieBox.setPrefWidth(220); // ensure VBox doesn’t shrink too much
            movieBox.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.05); " +
                            "-fx-background-radius: 10; " +
                            "-fx-padding: 10;"
            );

            // Try fetching a movie poster
            ImageView posterView = new ImageView();
            posterView.setFitWidth(150);
            posterView.setFitHeight(225);
            posterView.setPreserveRatio(true);

            try {
                String cleanedTitle = cleanMovieTitle(movieName);
                String encodedTitle = java.net.URLEncoder.encode(cleanedTitle, "UTF-8");
                String omdbApiKey = "f918882c";
                String apiUrl = "http://www.omdbapi.com/?t=" + encodedTitle + "&apikey=" + omdbApiKey;


                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(jsonBuilder.toString());
                if (json.has("Poster") && !json.getString("Poster").equals("N/A")) {
                    String posterUrl = json.getString("Poster");
                    posterView.setImage(new Image(posterUrl, true));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


            Label movieLabel = new Label(movieName);
            movieLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
            movieLabel.setTextFill(Color.WHITE);
            movieLabel.setWrapText(true);
            movieLabel.setMaxWidth(200); // actual wrap width
            movieLabel.setStyle(
                    "-fx-text-alignment: center;" +
                            "-fx-alignment: center;" +
                            "-fx-padding: 6 8 4 8;"
            );

            try {
                String encoded = java.net.URLEncoder.encode(movieName, "UTF-8");
                String movieUrl = serverUrl + "/" + encoded;

                Button playButton = new Button("▶ Play");
                playButton.setStyle(
                        "-fx-background-color: #FF9500; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-radius: 6;"
                );

                playButton.setOnAction(e -> {
                    MainSceneManager.setView(new VideoPlayerPane(movieUrl, movieName));

                });

                movieBox.getChildren().addAll(posterView, movieLabel, playButton);
                flow.getChildren().add(movieBox);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
