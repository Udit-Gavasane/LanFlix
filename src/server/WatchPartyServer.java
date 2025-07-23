package server;

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
        PartySession session = new PartySession(partyCode, hostId, videoUrl);
        activeSessions.put(partyCode, session);
        return partyCode;
    }

    public boolean joinParty(String partyCode, String clientId) {
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
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}