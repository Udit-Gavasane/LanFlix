package ui;

import core.AppConfig;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import server.PartyEvent;
import server.WatchPartyServer;
import server.WatchPartyWebSocketEndpoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class WatchPartyController {
    private final WatchPartyDialog dialog;
    private final String videoUrl;
    private String currentPartyCode;
    private String clientId;
    private final WatchPartyServer server;
    private PartyWebSocketClient wsClient;
    private final VideoPlayerPane player;

    public String getClientId() {
        return clientId;
    }



    public WatchPartyController(WatchPartyDialog dialog, String videoUrl, VideoPlayerPane player) {
        this.dialog = dialog;
        this.videoUrl = videoUrl;
        this.player = player;

        this.clientId = generateClientId();
        this.server = WatchPartyServer.getInstance();
        player.setPartyController(this);  // Set the controller in the player

    }

    public void createParty() {
        try {
            // Start WebSocket server
//            WatchPartyWebSocketEndpoint wsServer = new WatchPartyWebSocketEndpoint(8090);
//            wsServer.start();
//            System.out.println("WebSocket server started on port 8090");

            // Create party
            //currentPartyCode = server.createParty(clientId, videoUrl);
            String serverIp = AppConfig.getServerIp();
            URL url = new URL("http://" + serverIp + ":8080/createParty?clientId=" + clientId + "&videoUrl=" + URLEncoder.encode(videoUrl, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            currentPartyCode = reader.readLine();
            reader.close();

            // Create WebSocket client for host as well
            wsClient = new PartyWebSocketClient(currentPartyCode, this);
            wsClient.connect();
            player.setWebSocketClient(wsClient);

            // Show party view
            dialog.showPartyView(currentPartyCode);

            // Send join request as host
//            PartyEvent joinRequest = new PartyEvent(
//                    PartyEvent.EventType.JOIN_REQUEST,
//                    clientId,
//                    0.0
//            );
//            wsClient.send(joinRequest.toJson());
        } catch (Exception e) {
            e.printStackTrace();
            dialog.showError("Failed to create party: " + e.getMessage());
        }
    }

    public void onWebSocketReady() {
        // Now that connection is open, we can safely send the join request
        PartyEvent joinRequest = new PartyEvent(
                PartyEvent.EventType.JOIN_REQUEST,
                clientId,
                0.0
        );
        wsClient.send(joinRequest.toJson());
    }


    public void joinParty(String code) {
        try {
            System.out.println("Attempting to join party with code: " + code);
            if (code == null || code.trim().isEmpty()) {
                dialog.showError("Party code cannot be empty");
                return;
            }

            currentPartyCode = code.trim();
            // First establish WebSocket connection
            wsClient = new PartyWebSocketClient(currentPartyCode, this);
            wsClient.connect();
            player.setWebSocketClient(wsClient);

            // The rest will happen in handleConnectionEstablished()
        } catch (Exception e) {
            e.printStackTrace();
            dialog.showError("Failed to join party: " + e.getMessage());
        }
    }




    public void leaveParty() {
        try {
            if (currentPartyCode != null) {
                if (wsClient != null) {
                    wsClient.close();
                    wsClient = null;
                }
                server.broadcastEvent(currentPartyCode,
                        new PartyEvent(PartyEvent.EventType.LEAVE, clientId, 0));
                currentPartyCode = null;
                dialog.showJoinView();
            }
        } catch (Exception e) {
            e.printStackTrace();
            dialog.showError("Failed to leave party");
        }
    }


    public void copyPartyCode() {
        if (currentPartyCode != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(currentPartyCode);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private String generateClientId() {
        return "Client-" + System.currentTimeMillis();
    }

    public void handleConnectionEstablished() {
        if (currentPartyCode != null) {
            // Send join request through WebSocket
            PartyEvent joinRequest = new PartyEvent(
                    PartyEvent.EventType.JOIN_REQUEST,
                    clientId,
                    0.0
            );
            wsClient.send(joinRequest.toJson());
        }
    }


    public void handlePartyEvent(PartyEvent event) {
        switch (event.getType()) {
            case JOIN:
                // Handle new participant joined
                break;
            case LEAVE:
                // Handle participant left
                break;
            case STATE_UPDATE:
                // Update participants list if message contains it
                if (event.getMessage() != null) {
                    updateParticipantsList(event.getMessage());
                }
                break;
            case ERROR:
                dialog.showError(event.getMessage());
                break;
            case JOIN_RESPONSE:
                if ("success".equals(event.getMessage())) {
                    dialog.showPartyView(currentPartyCode);
                } else {
                    dialog.showError("Failed to join party: " + event.getMessage());
                    wsClient.close();
                    currentPartyCode = null;
                }
                break;
            case PLAY:

            case PAUSE:

            case SEEK:
                player.handlePartyEvent(event);  // Call handlePartyEvent instead of individual methods
                break;



        }
    }

    public void handlePlayerEvent(PartyEvent.EventType type, double position) {
        if (wsClient != null && wsClient.isOpen()) {
            PartyEvent event = new PartyEvent(type, clientId, position);
            wsClient.send(event.toJson());
        }
    }


    public void handleDisconnection(boolean remote) {
        if (remote) {
            dialog.showError("Disconnected from server");
        }
        dialog.showJoinView();
    }

    public void handleError(Exception ex) {
        dialog.showError("Connection error: " + ex.getMessage());
    }

    private void updateParticipantsList(String participantsData) {
        // Update the UI with the new participants list
        // This will be called when receiving a STATE_UPDATE event
    }

}