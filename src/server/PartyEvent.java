
package server;

public class PartyEvent {
    public enum EventType {
        PLAY, PAUSE, SEEK, JOIN, LEAVE, STATE_UPDATE
    }

    private final EventType type;
    private final String senderId;
    private final double position;
    private final long timestamp;

    public PartyEvent(EventType type, String senderId, double position) {
        this.type = type;
        this.senderId = senderId;
        this.position = position;
        this.timestamp = System.currentTimeMillis();
    }

    public EventType getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public double getPosition() {
        return position;
    }

    public long getTimestamp() {
        return timestamp;
    }
}