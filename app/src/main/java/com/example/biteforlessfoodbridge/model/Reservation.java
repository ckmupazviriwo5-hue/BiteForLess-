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

    /** 4-digit pickup PIN (local API); empty when the backend does not return one. */
    private String pickupPin = "";

    public String getPickupPin() { return pickupPin; }
    public void setPickupPin(String pin) { this.pickupPin = pin == null ? "" : pin; }

    /** Customer display name when the backend provides it (vendor views). */
    private String customerName = "";

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String name) { this.customerName = name == null ? "" : name; }

    /** Listing/shop names served with the reservation row (local API). */
    private String foodName = "";
    private String shopName = "";

    public String getFoodName() { return foodName; }
    public void setFoodName(String name) { this.foodName = name == null ? "" : name; }
    public String getShopName() { return shopName; }
    public void setShopName(String name) { this.shopName = name == null ? "" : name; }
}
