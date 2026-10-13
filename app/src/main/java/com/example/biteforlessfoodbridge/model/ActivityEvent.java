package com.example.biteforlessfoodbridge.model;

/** Local admin activity record (in-memory, this session + seed). */
public class ActivityEvent {
    private final String id;
    private final String type;
    private final String text;
    private final long atEpoch;
    private final String actor;

    public ActivityEvent(String id, String type, String text, long atEpoch, String actor) {
        this.id = id;
        this.type = type;
        this.text = text;
        this.atEpoch = atEpoch;
        this.actor = actor;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getText() { return text; }
    public long getAtEpoch() { return atEpoch; }
    public String getActor() { return actor; }
}
