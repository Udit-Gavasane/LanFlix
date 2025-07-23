
package server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PartySession {
    private final String partyCode;
    private final String hostId;
    private final String videoUrl;
    private final Set<String> participants;
    private boolean isPlaying;
    private double currentPosition;

    public PartySession(String partyCode, String hostId, String videoUrl) {
        this.partyCode = partyCode;
        this.hostId = hostId;
        this.videoUrl = videoUrl;
        this.participants = ConcurrentHashMap.newKeySet();
        this.participants.add(hostId);
        this.isPlaying = false;
        this.currentPosition = 0.0;
    }

    public void addParticipant(String clientId) {
        participants.add(clientId);
    }

    public void removeParticipant(String clientId) {
        participants.remove(clientId);
    }

    public void handleEvent(PartyEvent event) {
        switch (event.getType()) {
            case PLAY -> isPlaying = true;
            case PAUSE -> isPlaying = false;
            case SEEK -> currentPosition = event.getPosition();
        }
        // Broadcast to all participants except sender
        broadcastEventToParticipants(event);
    }

    private void broadcastEventToParticipants(PartyEvent event) {
        // In a real implementation, this would use WebSocket or similar to broadcast
        // For now, we'll implement the networking part later
    }
}