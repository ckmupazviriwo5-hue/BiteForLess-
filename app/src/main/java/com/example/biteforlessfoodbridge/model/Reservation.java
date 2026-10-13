package com.example.biteforlessfoodbridge.model;

/** Local reservation record (demo only). */
public class Reservation {
    private final String id;
    private final String listingId;
    private final String userId;
    private final int quantity;
    private final long createdAtEpoch;
    private ReservationStatus status;
    /** Demo pickup code shown to the customer; NOT secure proof of pickup. */
    private final String pickupCode;

    public Reservation(String id, String listingId, String userId, int quantity,
                       long createdAtEpoch, ReservationStatus status, String pickupCode) {
        this.id = id;
        this.listingId = listingId;
        this.userId = userId;
        this.quantity = quantity;
        this.createdAtEpoch = createdAtEpoch;
        this.status = status;
        this.pickupCode = pickupCode;
    }

    public String getId() { return id; }
    public String getListingId() { return listingId; }
    public String getUserId() { return userId; }
    public int getQuantity() { return quantity; }
    public long getCreatedAtEpoch() { return createdAtEpoch; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public String getPickupCode() { return pickupCode; }

    public boolean isActive() { return status == ReservationStatus.RESERVED; }
}
