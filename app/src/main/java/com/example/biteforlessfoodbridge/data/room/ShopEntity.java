package com.example.biteforlessfoodbridge.data.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shops")
public class ShopEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String ownerId;
    private String name;
    private String category;
    private String whatsapp;
    private String location;
    private String status; // APPROVED, PENDING, SUSPENDED
    private double rating;
    private int reviewCount;
    private String imageUrl;

    public ShopEntity(@NonNull String id, String ownerId, String name, String category,
                      String whatsapp, String location, String status, double rating,
                      int reviewCount, String imageUrl) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.category = category;
        this.whatsapp = whatsapp;
        this.location = location;
        this.status = status;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.imageUrl = imageUrl;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
