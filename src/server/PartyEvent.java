package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class PartyEvent {
    private static final Gson gson = new Gson();

    public enum EventType {
        PLAY, PAUSE, SEEK, JOIN, LEAVE, STATE_UPDATE, ERROR, JOIN_REQUEST, JOIN_RESPONSE

    }

    private final EventType type;
    private final String senderId;
    private final double position;
    private final long timestamp;
    private final String message; // For error messages

    public PartyEvent(EventType type, String senderId, double position) {
        this(type, senderId, position, null);
    }

    public PartyEvent(EventType type, String senderId, double position, String message) {
        this.type = type;
        this.senderId = senderId;
        this.position = position;
        this.timestamp = System.currentTimeMillis();
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static PartyEvent fromJson(String json) {
        return gson.fromJson(json, PartyEvent.class);
    }
}