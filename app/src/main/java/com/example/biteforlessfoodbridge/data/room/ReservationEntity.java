package com.example.biteforlessfoodbridge.data.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reservations")
public class ReservationEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String listingId;
    private String userId;
    private int quantity;
    private long createdAtEpoch;
    private String status; // RESERVED, COMPLETED, CANCELLED, EXPIRED
    private String pickupCode;

    public ReservationEntity(@NonNull String id, String listingId, String userId,
                             int quantity, long createdAtEpoch, String status, String pickupCode) {
        this.id = id;
        this.listingId = listingId;
        this.userId = userId;
        this.quantity = quantity;
        this.createdAtEpoch = createdAtEpoch;
        this.status = status;
        this.pickupCode = pickupCode;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public long getCreatedAtEpoch() { return createdAtEpoch; }
    public void setCreatedAtEpoch(long createdAtEpoch) { this.createdAtEpoch = createdAtEpoch; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }
}
