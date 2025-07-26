package server;

import org.java_websocket.WebSocket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WatchPartyServer {
    private static WatchPartyServer instance;
    private final Map<String, PartySession> activeSessions;
    private final Random random;

    private WatchPartyServer() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.random = new Random();
    }

    public static synchronized WatchPartyServer getInstance() {
        if (instance == null) {
            instance = new WatchPartyServer();
        }
        return instance;
    }

    public String createParty(String hostId, String videoUrl) {
        String partyCode = generatePartyCode();
        System.out.println("Creating party with code: " + partyCode); // Debug log
        PartySession session = new PartySession(partyCode, hostId, videoUrl);
        activeSessions.put(partyCode, session);
        return partyCode;
    }

    public boolean joinParty(String partyCode, String clientId) {
        System.out.println("Attempting to join party: " + partyCode); // Debug log
        //System.out.println("Available parties: " + parties.keySet()); // Debug log

        PartySession session = activeSessions.get(partyCode);
        if (session != null) {
            session.addParticipant(clientId);
            return true;
        }
        return false;
    }

    public void broadcastEvent(String partyCode, PartyEvent event) {
        PartySession session = activeSessions.get(partyCode);
        if (session != null) {
            session.handleEvent(event);
        }
    }

    private String generatePartyCode() {
//        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//        StringBuilder code = new StringBuilder();
//        for (int i = 0; i < 6; i++) {
//            code.append(chars.charAt(random.nextInt(chars.length())));
//        }
        return "1234";
    }

    public void handleConnection(String partyCode, WebSocket conn) {
        PartySession session = activeSessions.get(partyCode);
        if (session != null) {
            session.handleConnection(conn);
            // Send current state to new participant
            broadcastEvent(partyCode, new PartyEvent(
                PartyEvent.EventType.STATE_UPDATE,
                "server",
                session.getCurrentPosition()
            ));
        }
    }

    public void handleDisconnection(String partyCode, WebSocket conn) {
        PartySession session = activeSessions.get(partyCode);
        if (session != null) {
            session.handleDisconnection(conn);
        }
    }
}