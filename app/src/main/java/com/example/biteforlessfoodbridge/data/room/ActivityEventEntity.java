package com.example.biteforlessfoodbridge.data.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_events")
public class ActivityEventEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String type;
    private String text;
    private long timestamp;
    private String actor;

    public ActivityEventEntity(@NonNull String id, String type, String text, long timestamp, String actor) {
        this.id = id;
        this.type = type;
        this.text = text;
        this.timestamp = timestamp;
        this.actor = actor;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
}
