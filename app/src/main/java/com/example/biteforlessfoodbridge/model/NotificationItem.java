package com.example.biteforlessfoodbridge.model;

/** In-app activity/notification entry for reservation events and reminders. */
public class NotificationItem {
    private final String id;
    private final String title;
    private final String message;
    private final long createdAtEpoch;
    private boolean read;

    public NotificationItem(String id, String title, String message, long createdAtEpoch, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.createdAtEpoch = createdAtEpoch;
        this.read = read;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public long getCreatedAtEpoch() { return createdAtEpoch; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
