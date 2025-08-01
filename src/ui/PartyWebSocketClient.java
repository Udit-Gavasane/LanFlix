package ui;

import core.AppConfig;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import server.PartyEvent;

import java.net.URI;
import java.net.URISyntaxException;

public class PartyWebSocketClient extends WebSocketClient {
    private final WatchPartyController controller;

    public PartyWebSocketClient(String partyCode, WatchPartyController controller) throws URISyntaxException {
        super(new URI("ws://" + AppConfig.getServerIp() + ":8090/party/" + partyCode));
        System.out.println("Connecting to: " + getURI()); // Debug log
        this.controller = controller;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("WebSocket connected to party!");
        controller.onWebSocketReady();
        Platform.runLater(() -> controller.handleConnectionEstablished());
    }

    @Override
    public void onMessage(String message) {
        Platform.runLater(() -> controller.handlePartyEvent(PartyEvent.fromJson(message)));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Platform.runLater(() -> controller.handleDisconnection(remote));
    }

    @Override
    public void onError(Exception ex) {
        Platform.runLater(() -> controller.handleError(ex));
    }
}