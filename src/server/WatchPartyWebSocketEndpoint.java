package server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WatchPartyWebSocketEndpoint extends WebSocketServer {
    private static final Map<WebSocket, String> partyConnections = new ConcurrentHashMap<>();
    
    public WatchPartyWebSocketEndpoint(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String partyCode = handshake.getResourceDescriptor().replace("/party/", "");
        System.out.println("WebSocket connection opened for party: " + partyCode); // Debug log
        partyConnections.put(conn, partyCode);
        WatchPartyServer.getInstance().handleConnection(partyCode, conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String partyCode = partyConnections.get(conn);
        if (partyCode != null) {
            WatchPartyServer.getInstance().handleDisconnection(partyCode, conn);
            partyConnections.remove(conn);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            PartyEvent event = PartyEvent.fromJson(message);
            String partyCode = partyConnections.get(conn);
            System.out.println("Received message for party: " + partyCode);

            if (event.getType() == PartyEvent.EventType.JOIN_REQUEST) {
                handleJoinRequest(conn, partyCode, event.getSenderId());
                return;
            }

            if (partyCode != null) {
                WatchPartyServer.getInstance().broadcastEvent(partyCode, event);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            sendError(conn, "Invalid message format");
        }
    }

    private void handleJoinRequest(WebSocket conn, String partyCode, String clientId) {
        boolean success = WatchPartyServer.getInstance().joinParty(partyCode, clientId);
        PartyEvent response = new PartyEvent(
                PartyEvent.EventType.JOIN_RESPONSE,
                "server",
                0.0,
                success ? "success" : "Party not found"
        );
        conn.send(response.toJson());
    }


    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error occurred on connection: " + ex.getMessage());
        if (conn != null) {
            partyConnections.remove(conn);
        }
    }

    @Override
    public void onStart() {
        System.out.println("Watch Party WebSocket server started on port: " + getPort());
    }

    public static void broadcast(String partyCode, PartyEvent event) {
        String messageJson = event.toJson();
        partyConnections.forEach((conn, code) -> {
            if (code.equals(partyCode)) {
                conn.send(messageJson);
            }
        });
    }

    private void sendError(WebSocket conn, String message) {
        conn.send("{\"type\":\"ERROR\",\"message\":\"" + message + "\"}");
    }
}