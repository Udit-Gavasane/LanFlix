package ui;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import server.PartyEvent;
import server.WatchPartyServer;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;


public class WatchPartyController {
    private final WatchPartyDialog dialog;
    private final String videoUrl;
    private String currentPartyCode;
    private String clientId;
    private final WatchPartyServer server;

    public WatchPartyController(WatchPartyDialog dialog, String videoUrl) {
        this.dialog = dialog;
        this.videoUrl = videoUrl;
        this.clientId = generateClientId();
        this.server = WatchPartyServer.getInstance();
    }

    public void createParty() {
        try {
            currentPartyCode = server.createParty(clientId, videoUrl);
            dialog.showPartyView(currentPartyCode);
        } catch (Exception e) {
            e.printStackTrace();
            // Show error dialog
            dialog.showError("Failed to create party");
        }
    }

    public void joinParty(String code) {
        try {
            if (server.joinParty(code, clientId)) {
                currentPartyCode = code;
                dialog.showPartyView(code);
            } else {
                dialog.showError("Invalid party code");
            }
        } catch (Exception e) {
            e.printStackTrace();
            dialog.showError("Failed to join party");
        }
    }

    public void leaveParty() {
        try {
            if (currentPartyCode != null) {
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
}