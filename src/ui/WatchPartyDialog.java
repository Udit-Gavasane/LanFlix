package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import server.PartyEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;


public class WatchPartyDialog extends Stage {
    private final WatchPartyController controller;
    private VBox mainContainer;
    private VBox partyView;
    private VBox joinView;

    public WatchPartyDialog(Stage owner, String videoUrl) {
        this.controller = new WatchPartyController(this, videoUrl);

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Watch Party");

        createUI();

        Scene scene = new Scene(mainContainer, 400, 500);
        setScene(scene);
    }

    private void createUI() {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setAlignment(Pos.TOP_CENTER);

        createJoinView();
        createPartyView();

        showJoinView(); // Start with join view
    }

    private void createJoinView() {
        joinView = new VBox(15);
        joinView.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("Watch Party");
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");

        Button createButton = new Button("Create New Party");
        createButton.setOnAction(e -> controller.createParty());

        Label orLabel = new Label("- OR -");

        TextField codeField = new TextField();
        codeField.setPromptText("Enter Party Code");
        codeField.setPrefWidth(200);

        Button joinButton = new Button("Join Party");
        joinButton.setOnAction(e -> controller.joinParty(codeField.getText()));

        joinView.getChildren().addAll(
                titleLabel,
                createButton,
                orLabel,
                codeField,
                joinButton
        );
    }

    private void createPartyView() {
        partyView = new VBox(15);
        partyView.setAlignment(Pos.TOP_CENTER);

        Label partyCodeLabel = new Label();
        partyCodeLabel.setStyle("-fx-font-size: 18;");

        Button copyButton = new Button("Copy Code");
        copyButton.setOnAction(e -> controller.copyPartyCode());

        ListView<String> participantsList = new ListView<>();
        participantsList.setPrefHeight(200);

        Button leaveButton = new Button("Leave Party");
        leaveButton.setOnAction(e -> controller.leaveParty());

        partyView.getChildren().addAll(
                partyCodeLabel,
                copyButton,
                new Label("Participants:"),
                participantsList,
                leaveButton
        );
    }

    public void showJoinView() {
        mainContainer.getChildren().clear();
        mainContainer.getChildren().add(joinView);
    }

    public void showPartyView(String partyCode) {
        mainContainer.getChildren().clear();
        ((Label)partyView.getChildren().get(0)).setText("Party Code: " + partyCode);
        mainContainer.getChildren().add(partyView);
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}