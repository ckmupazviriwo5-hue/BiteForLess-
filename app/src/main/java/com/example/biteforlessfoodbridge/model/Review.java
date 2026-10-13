package com.example.biteforlessfoodbridge.model;

/** Local review (never presented as verified unless demo data supports it). */
public class Review {
    private final String id;
    private final String listingId;
    private final String userId;
    private final int rating; // 1..5
    private final String text;
    private final long createdAtEpoch;
    private ReviewStatus status;

    public Review(String id, String listingId, String userId, int rating,
                  String text, long createdAtEpoch, ReviewStatus status) {
        this.id = id;
        this.listingId = listingId;
        this.userId = userId;
        this.rating = rating;
        this.text = text;
        this.createdAtEpoch = createdAtEpoch;
        this.status = status;
    }

    public String getId() { return id; }
    public String getListingId() { return listingId; }
    public String getUserId() { return userId; }
    public int getRating() { return rating; }
    public String getText() { return text; }
    public long getCreatedAtEpoch() { return createdAtEpoch; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
}
