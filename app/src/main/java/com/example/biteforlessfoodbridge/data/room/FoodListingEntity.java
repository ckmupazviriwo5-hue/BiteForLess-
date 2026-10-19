package com.example.biteforlessfoodbridge.data.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "listings")
public class FoodListingEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String vendorId;
    private String name;
    private String description;
    private String categoryId;
    private double originalPrice;
    private double discountedPrice;
    private int availableQuantity;
    private String location;
    private long collectionDeadlineEpoch;
    private String collectionWindow;
    private String status; // ACTIVE, PAUSED, SOLD_OUT, DEACTIVATED
    private String imageUrl;

    public FoodListingEntity(@NonNull String id, String vendorId, String name, String description,
                             String categoryId, double originalPrice, double discountedPrice,
                             int availableQuantity, String location, long collectionDeadlineEpoch,
                             String collectionWindow, String status, String imageUrl) {
        this.id = id;
        this.vendorId = vendorId;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.availableQuantity = availableQuantity;
        this.location = location;
        this.collectionDeadlineEpoch = collectionDeadlineEpoch;
        this.collectionWindow = collectionWindow;
        this.status = status;
        this.imageUrl = imageUrl;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }
    public double getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(double discountedPrice) { this.discountedPrice = discountedPrice; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public long getCollectionDeadlineEpoch() { return collectionDeadlineEpoch; }
    public void setCollectionDeadlineEpoch(long collectionDeadlineEpoch) { this.collectionDeadlineEpoch = collectionDeadlineEpoch; }
    public String getCollectionWindow() { return collectionWindow; }
    public void setCollectionWindow(String collectionWindow) { this.collectionWindow = collectionWindow; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
