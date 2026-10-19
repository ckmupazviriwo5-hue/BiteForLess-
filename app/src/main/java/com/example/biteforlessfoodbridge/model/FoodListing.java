package com.example.biteforlessfoodbridge.model;

/**
 * Surplus food listing. Plain POJO for Phase 2; becomes a Room @Entity in Phase 6.
 * Prices are in ZMW.
 */
public class FoodListing {
    private final String id;
    private final String vendorId;
    private String name;
    private String description;
    private String categoryId;
    private double originalPrice;
    private double discountedPrice;
    private int availableQuantity;
    private String location;
    private long collectionDeadlineEpoch;
    private String collectionWindow;
    private ListingStatus status;

    public FoodListing(String id, String vendorId, String name, String description,
                       String categoryId, double originalPrice, double discountedPrice,
                       int availableQuantity, String location, long collectionDeadlineEpoch,
                       String collectionWindow, ListingStatus status) {
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
    }

    public String getId() { return id; }
    public String getVendorId() { return vendorId; }
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
    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }

    /** Discount percentage, 0 when original price is invalid. */
    public int getDiscountPercent() {
        if (originalPrice <= 0 || discountedPrice < 0 || discountedPrice >= originalPrice) return 0;
        return (int) Math.round((1.0 - discountedPrice / originalPrice) * 100.0);
    }

    public double getSavings() {
        double savings = originalPrice - discountedPrice;
        return savings > 0 ? savings : 0;
    }

    public boolean isExpired(long nowEpoch) {
        return collectionDeadlineEpoch > 0 && nowEpoch > collectionDeadlineEpoch;
    }

    /** Customer-visible availability (ignores vendor state; repository combines both). */
    public boolean isReservable(long nowEpoch) {
        return status == ListingStatus.ACTIVE && availableQuantity > 0 && !isExpired(nowEpoch);
    }

    /** Absolute URL of the listing photo ("" when the record has none). */
    private String imageUrl = "";

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl == null ? "" : imageUrl;
    }
}
