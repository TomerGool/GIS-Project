package com.example.gisapp1.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AppNotification {
    private String id;
    private String userId;
    private String title;
    private String message;
    private Date timestamp;
    private boolean isRead;
    private NotificationType type;
    private String relatedEntityId;
    private Map<String, String> extraData;

    public enum NotificationType {
        PARKING_AVAILABLE,
        RESERVATION_CONFIRMED,
        RESERVATION_CANCELED,
        SPOT_BOOKED,
        SPOT_UNBOOKED,
        SYSTEM_MESSAGE
    }

    // Default constructor for Firestore
    public AppNotification() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date();
        this.isRead = false;
        this.extraData = new HashMap<>();
    }

    // Comprehensive constructor
    public AppNotification(String userId, String title, String message,
                           NotificationType type, String relatedEntityId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.timestamp = new Date();
        this.isRead = false;
        this.type = type;
        this.relatedEntityId = relatedEntityId;
        this.extraData = new HashMap<>();
    }

    // Add extra data to notification
    public void addExtraData(String key, String value) {
        if (extraData == null) {
            extraData = new HashMap<>();
        }
        extraData.put(key, value);
    }

    // Get extra data
    public String getExtraData(String key) {
        if (extraData == null) {
            return null;
        }
        return extraData.get(key);
    }

    // Get full extra data map
    public Map<String, String> getExtraData() {
        return extraData;
    }

    // Set full extra data map
    public void setExtraData(Map<String, String> extraData) {
        this.extraData = extraData;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(String relatedEntityId) { this.relatedEntityId = relatedEntityId; }
}