package com.example.biteforlessfoodbridge.data;

import com.example.biteforlessfoodbridge.model.ActivityEvent;
import com.example.biteforlessfoodbridge.model.Category;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.ReviewStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.model.VendorStatus;
import com.example.biteforlessfoodbridge.repository.ListingRepository;
import com.example.biteforlessfoodbridge.repository.ReservationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Shared in-memory demo dataset for Phase 3 (single source of truth).
 *
 * <p>All customer screens must use {@link #getInstance()} so reservations,
 * stock levels and history stay consistent. Business rules live here — never
 * in Activities, Fragments or adapters.
 *
 * <p>TEMPORARY LIMITATION: state lives in process memory and is lost on
 * process restart; there is no cross-device sync and no real concurrency
 * protection. A Room-backed implementation replaces this in Phase 6 without
 * changing screen code (same interfaces).
 */
public class DemoRepository implements ListingRepository, ReservationRepository {

    /** Activity event types recorded by {@link #logEvent}. */
    public static final String EVENT_VENDOR_STATUS = "vendor_status";
    public static final String EVENT_LISTING_STATUS = "listing_status";
    public static final String EVENT_REVIEW = "review";

    private static DemoRepository instance;

    public static synchronized DemoRepository getInstance() {
        if (instance == null) {
            instance = new DemoRepository();
        }
        return instance;
    }

    private final List<Category> categories = new ArrayList<>();
    private final List<Vendor> vendors = new ArrayList<>();
    private final List<FoodListing> listings = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();
    private final List<ActivityEvent> activityLog = new ArrayList<>();
    private int reservationSeq;
    private int listingSeq = 100;
    private int eventSeq;
    private final Random random = new Random();

    private DemoRepository() {
        resetDemoData();
    }

    /** Clears all state and reseeds the known demo dataset. */
    public synchronized void resetDemoData() {
        categories.clear();
        vendors.clear();
        listings.clear();
        reservations.clear();
        reviews.clear();
        activityLog.clear();
        reservationSeq = 0;
        eventSeq = 0;
        categories.addAll(DemoData.categories());
        vendors.addAll(DemoData.vendors());
        listings.addAll(DemoData.listings(System.currentTimeMillis()));
        reviews.addAll(DemoData.reviews(System.currentTimeMillis()));
    }

    /** Records an admin action. Never pre-seeded; newest entries come first. */
    public synchronized ActivityEvent logEvent(String type, String text, String actor) {
        eventSeq++;
        ActivityEvent event = new ActivityEvent("EVT-" + eventSeq,
                type == null ? "admin" : type,
                text == null ? "" : text,
                System.currentTimeMillis(),
                actor == null ? "admin (demo)" : actor);
        activityLog.add(0, event);
        return event;
    }

    public synchronized List<ActivityEvent> getActivityLog() {
        return new ArrayList<>(activityLog);
    }

    // ---- read access for screens ----

    public synchronized List<Category> getCategories() {
        return new ArrayList<>(categories);
    }

    public synchronized List<Vendor> getVendors() {
        return new ArrayList<>(vendors);
    }

    public synchronized Vendor getVendorById(String id) {
        if (id == null) return null;
        for (Vendor v : vendors) {
            if (id.equals(v.getId())) return v;
        }
        return null;
    }

    public synchronized String getVendorName(String vendorId) {
        Vendor v = getVendorById(vendorId);
        return v == null ? "Unknown vendor" : v.getName();
    }

    public synchronized String getCategoryName(String categoryId) {
        if (categoryId == null) return "";
        for (Category c : categories) {
            if (categoryId.equals(c.getId())) return c.getName();
        }
        return categoryId;
    }

    /**
     * Changes the canonical vendor record (approve / suspend / reactivate).
     * Suspension never cancels reservations or touches stock — existing
     * obligations stay visible; only new business is blocked by the gates.
     */
    public synchronized void setVendorStatus(String vendorId, VendorStatus status, String actor) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null) {
            throw new IllegalStateException("Vendor not found");
        }
        if (status == null) {
            throw new IllegalStateException("Invalid vendor status");
        }
        if (vendor.getStatus() == status) {
            throw new IllegalStateException("Vendor is already " + status.name());
        }
        vendor.setStatus(status);
        logEvent(EVENT_VENDOR_STATUS,
                vendor.getName() + " → " + status.name(), actor);
    }

    /** Number of listings owned by a vendor (for the directory). */
    public synchronized int getListingCountForVendor(String vendorId) {
        return getListingsForVendor(vendorId).size();
    }

    // ---- ListingRepository ----

    @Override
    public synchronized List<FoodListing> getAll() {
        return new ArrayList<>(listings);
    }

    @Override
    public synchronized FoodListing getById(String id) {
        if (id == null) return null;
        for (FoodListing l : listings) {
            if (id.equals(l.getId())) return l;
        }
        return null;
    }

    @Override
    public synchronized List<FoodListing> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        List<FoodListing> out = new ArrayList<>();
        for (FoodListing l : listings) {
            if (q.isEmpty() || matches(l, q)) out.add(l);
        }
        return out;
    }

    private boolean matches(FoodListing l, String q) {
        return l.getName().toLowerCase(Locale.US).contains(q)
                || getVendorName(l.getVendorId()).toLowerCase(Locale.US).contains(q);
    }

    @Override
    public synchronized void save(FoodListing listing) {
        if (listing == null || listing.getId() == null) {
            throw new IllegalStateException("Listing is invalid");
        }
        for (int i = 0; i < listings.size(); i++) {
            if (listing.getId().equals(listings.get(i).getId())) {
                listings.set(i, listing);
                return;
            }
        }
        listings.add(listing);
    }

    @Override
    public synchronized void delete(String id) {
        for (int i = 0; i < listings.size(); i++) {
            if (listings.get(i).getId().equals(id)) {
                listings.remove(i);
                return;
            }
        }
    }

    // Interface names (renamed in Phase 3 to avoid erasure clashes with ListingRepository).
    @Override
    public synchronized List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations);
    }

    // ---- ReservationRepository ----

    @Override
    public synchronized List<Reservation> getForUser(String userId) {
        List<Reservation> out = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.getUserId() != null && r.getUserId().equals(userId)) out.add(r);
        }
        return out;
    }

    @Override
    public synchronized List<Reservation> getForListing(String listingId) {
        List<Reservation> out = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.getListingId() != null && r.getListingId().equals(listingId)) out.add(r);
        }
        return out;
    }

    @Override
    public synchronized Reservation getReservationById(String id) {
        if (id == null) return null;
        for (Reservation r : reservations) {
            if (id.equals(r.getId())) return r;
        }
        return null;
    }

    @Override
    public synchronized Reservation create(String listingId, String userId, int quantity) {
        long now = System.currentTimeMillis();
        if (listingId == null || userId == null) {
            throw new IllegalStateException("Listing not found");
        }
        if (quantity < 1) {
            throw new IllegalStateException("Quantity must be at least 1");
        }
        FoodListing listing = getById(listingId);
        if (listing == null) {
            throw new IllegalStateException("Listing not found");
        }
        Vendor vendor = getVendorById(listing.getVendorId());
        if (vendor != null && vendor.getStatus() != VendorStatus.APPROVED) {
            throw new IllegalStateException("This vendor is not taking orders right now (demo)");
        }
        if (!listing.isReservable(now)) {
            throw new IllegalStateException("This deal is no longer available");
        }
        if (quantity > listing.getAvailableQuantity()) {
            throw new IllegalStateException(
                    "Only " + listing.getAvailableQuantity() + " left in stock");
        }
        // Stock rechecked above at operation time; decrement exactly once.
        listing.setAvailableQuantity(listing.getAvailableQuantity() - quantity);
        reservationSeq++;
        String resId = String.format(Locale.US, "RES-%04d", reservationSeq);
        String pickupCode = String.format(Locale.US, "DEMO-%04d", random.nextInt(10000));
        Reservation reservation = new Reservation(resId, listingId, userId, quantity,
                now, ReservationStatus.RESERVED, pickupCode);
        reservations.add(0, reservation); // newest first
        return reservation;
    }

    @Override
    public synchronized void cancel(String reservationId) {
        Reservation r = getReservationById(reservationId);
        if (r == null) {
            throw new IllegalStateException("Reservation not found");
        }
        if (r.getStatus() != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only active reservations can be cancelled");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        FoodListing listing = getById(r.getListingId());
        if (listing != null) {
            listing.setAvailableQuantity(listing.getAvailableQuantity() + r.getQuantity());
        }
    }

    @Override
    public synchronized void complete(String reservationId) {
        Reservation r = getReservationById(reservationId);
        if (r == null) {
            throw new IllegalStateException("Reservation not found");
        }
        if (r.getStatus() != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only active reservations can be completed");
        }
        // Completing a pickup never restores stock.
        r.setStatus(ReservationStatus.COMPLETED);
    }

    // ---- Vendor-scoped operations (Phase 4) ----
    //
    // A stable demo vendor identity is mapped in the vendor UI (v1 / Mama
    // Chanda's Bakery in the offline dataset). Every mutating call takes
    // that vendorId and refuses to touch another vendor's listings or
    // reservations. Demo roles are UI routing only — this ownership check
    // is a data-integrity guard, not security.

    /** Listings owned by one vendor (canonical records, not copies). */
    public synchronized List<FoodListing> getListingsForVendor(String vendorId) {
        List<FoodListing> out = new ArrayList<>();
        for (FoodListing l : listings) {
            if (l.getVendorId() != null && l.getVendorId().equals(vendorId)) out.add(l);
        }
        return out;
    }

    /** Reservations placed on one vendor's listings, newest first. */
    public synchronized List<Reservation> getReservationsForVendor(String vendorId) {
        List<Reservation> out = new ArrayList<>();
        for (Reservation r : reservations) {
            FoodListing l = getById(r.getListingId());
            if (l != null && vendorId != null && vendorId.equals(l.getVendorId())) out.add(r);
        }
        return out;
    }

    /** Quantity already committed to outstanding (RESERVED) reservations. */
    public synchronized int getOutstandingReserved(String listingId) {
        int total = 0;
        for (Reservation r : reservations) {
            if (r.getListingId() != null && r.getListingId().equals(listingId)
                    && r.getStatus() == ReservationStatus.RESERVED) {
                total += r.getQuantity();
            }
        }
        return total;
    }

    /**
     * Creates a listing for {@code vendorId}. The new record is immediately
     * visible to customers through the same shared list.
     */
    public synchronized FoodListing createListing(String vendorId, String name, String description,
                                                 String categoryId, double originalPrice,
                                                 double discountedPrice, int quantity,
                                                 String location, long deadlineEpoch,
                                                 String collectionWindow, ListingStatus status) {
        long now = System.currentTimeMillis();
        if (getVendorById(vendorId) == null) {
            throw new IllegalStateException("Unknown vendor");
        }
        requireVendorApproved(vendorId, "create listings");
        validateListingFields(name, categoryId, originalPrice, discountedPrice,
                quantity, location, deadlineEpoch, status, now, true);
        listingSeq++;
        FoodListing listing = new FoodListing("vl-" + listingSeq, vendorId, name.trim(),
                description == null ? "" : description.trim(), categoryId, originalPrice,
                discountedPrice, quantity, location.trim(), deadlineEpoch,
                collectionWindow == null ? "" : collectionWindow.trim(),
                status == null ? ListingStatus.ACTIVE : status);
        listings.add(0, listing); // newest first for vendor + customer lists
        return listing;
    }

    /**
     * Edits a listing in place (same record — no duplicates). Rechecks every
     * condition at save time, including committed stock.
     */
    public synchronized void updateListing(String listingId, String vendorId, String name,
                                          String description, String categoryId,
                                          double originalPrice, double discountedPrice,
                                          int newQuantity, String location, long deadlineEpoch,
                                          String collectionWindow) {
        FoodListing listing = requireOwnedListing(listingId, vendorId);
        requireVendorApproved(vendorId, "edit listings");
        validateListingFields(name, categoryId, originalPrice, discountedPrice,
                newQuantity, location, deadlineEpoch, listing.getStatus(),
                System.currentTimeMillis(), false);
        int outstanding = getOutstandingReserved(listingId);
        if (newQuantity < outstanding) {
            throw new IllegalStateException("Cannot set stock below "
                    + outstanding + " already reserved");
        }
        listing.setName(name.trim());
        listing.setDescription(description == null ? "" : description.trim());
        listing.setCategoryId(categoryId);
        listing.setOriginalPrice(originalPrice);
        listing.setDiscountedPrice(discountedPrice);
        listing.setAvailableQuantity(newQuantity);
        listing.setLocation(location.trim());
        listing.setCollectionDeadlineEpoch(deadlineEpoch);
        listing.setCollectionWindow(collectionWindow == null ? "" : collectionWindow.trim());
    }

    /**
     * Activates/deactivates a listing. Deactivation blocks new reservations
     * via {@code isReservable}; existing reservations are untouched.
     */
    public synchronized void setListingStatus(String listingId, ListingStatus status, String vendorId) {
        FoodListing listing = requireOwnedListing(listingId, vendorId);
        if (status == null) {
            throw new IllegalStateException("Invalid status");
        }
        if (status == ListingStatus.ACTIVE) {
            requireVendorApproved(vendorId, "activate listings");
        }
        listing.setStatus(status);
    }

    /**
     * Admin deactivation/reactivation without ownership checks. Activation is
     * refused while the owning vendor is not approved; deactivation is always
     * allowed. Never touches stock or reservations.
     */
    public synchronized void setListingStatusByAdmin(String listingId, ListingStatus status,
                                                    String actor) {
        FoodListing listing = getById(listingId);
        if (listing == null) {
            throw new IllegalStateException("Listing not found");
        }
        if (status == null) {
            throw new IllegalStateException("Invalid status");
        }
        if (status == ListingStatus.ACTIVE) {
            Vendor vendor = getVendorById(listing.getVendorId());
            if (vendor == null || vendor.getStatus() != VendorStatus.APPROVED) {
                throw new IllegalStateException("Approve the vendor before reactivating");
            }
        }
        listing.setStatus(status);
        logEvent(EVENT_LISTING_STATUS,
                listing.getName() + " → " + status.name(), actor);
    }

    private synchronized void requireVendorApproved(String vendorId, String action) {
        Vendor vendor = getVendorById(vendorId);
        if (vendor == null || vendor.getStatus() != VendorStatus.APPROVED) {
            throw new IllegalStateException("Vendor account is not approved to " + action
                    + " (demo)");
        }
    }

    /** Vendor-side pickup completion (simulated, demo only). */
    public synchronized void completeForVendor(String reservationId, String vendorId) {
        Reservation r = getReservationById(reservationId);
        if (r == null) {
            throw new IllegalStateException("Reservation not found");
        }
        FoodListing listing = getById(r.getListingId());
        if (listing == null || vendorId == null || !vendorId.equals(listing.getVendorId())) {
            throw new IllegalStateException("This reservation belongs to another vendor");
        }
        complete(reservationId); // enforces RESERVED-only, never restores stock
    }

    private synchronized FoodListing requireOwnedListing(String listingId, String vendorId) {
        FoodListing listing = getById(listingId);
        if (listing == null) {
            throw new IllegalStateException("Listing not found");
        }
        if (vendorId == null || !vendorId.equals(listing.getVendorId())) {
            throw new IllegalStateException("This listing belongs to another vendor");
        }
        return listing;
    }

    private void validateListingFields(String name, String categoryId, double originalPrice,
                                       double discountedPrice, int quantity, String location,
                                       long deadlineEpoch, ListingStatus status,
                                       long nowEpoch, boolean isCreate) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Food name is required");
        }
        if (categoryId == null || !hasCategory(categoryId)) {
            throw new IllegalStateException("Choose a valid category");
        }
        if (originalPrice <= 0) {
            throw new IllegalStateException("Original price must be above K0");
        }
        if (discountedPrice < 0 || discountedPrice >= originalPrice) {
            throw new IllegalStateException("Discounted price must be below the original price");
        }
        if (quantity < 0) {
            throw new IllegalStateException("Quantity cannot be negative");
        }
        if (status == ListingStatus.ACTIVE && quantity < 1 && isCreate) {
            throw new IllegalStateException("Active listings need at least 1 in stock");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalStateException("Collection location is required");
        }
        if (deadlineEpoch <= 0) {
            throw new IllegalStateException("Collection deadline is required");
        }
        if (isCreate && deadlineEpoch <= nowEpoch) {
            throw new IllegalStateException("Collection deadline must be in the future");
        }
    }

    private boolean hasCategory(String categoryId) {
        for (Category c : categories) {
            if (c.getId().equals(categoryId)) return true;
        }
        return false;
    }

    // ---- Reviews (Phase 5) ----

    public synchronized List<Review> getReviews() {
        return new ArrayList<>(reviews);
    }

    public synchronized Review getReviewById(String id) {
        if (id == null) return null;
        for (Review r : reviews) {
            if (id.equals(r.getId())) return r;
        }
        return null;
    }

    /** Customer-safe view: only VISIBLE reviews (never verified purchases). */
    public synchronized List<Review> getVisibleReviewsForListing(String listingId) {
        List<Review> out = new ArrayList<>();
        for (Review r : reviews) {
            if (r.getListingId() != null && r.getListingId().equals(listingId)
                    && r.getStatus() == ReviewStatus.VISIBLE) {
                out.add(r);
            }
        }
        return out;
    }

    /** Moderation preserves the record; hiding never deletes. */
    public synchronized void setReviewStatus(String reviewId, ReviewStatus status, String actor) {
        Review review = getReviewById(reviewId);
        if (review == null) {
            throw new IllegalStateException("Review not found");
        }
        if (status == null) {
            throw new IllegalStateException("Invalid review status");
        }
        review.setStatus(status);
        logEvent(EVENT_REVIEW, "Review " + review.getId() + " → " + status.name(), actor);
    }

    /**
     * Marks RESERVED reservations whose listing has expired as EXPIRED,
     * restoring stock exactly once per reservation.
     *
     * @return number of reservations transitioned.
     */
    public synchronized int markExpiredReservations(long nowEpoch) {
        int count = 0;
        for (Reservation r : reservations) {
            if (r.getStatus() == ReservationStatus.RESERVED) {
                FoodListing listing = getById(r.getListingId());
                if (listing != null && listing.isExpired(nowEpoch)) {
                    r.setStatus(ReservationStatus.EXPIRED);
                    listing.setAvailableQuantity(listing.getAvailableQuantity() + r.getQuantity());
                    count++;
                }
            }
        }
        return count;
    }
}
