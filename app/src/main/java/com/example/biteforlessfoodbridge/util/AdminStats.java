package com.example.biteforlessfoodbridge.util;

import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.ReviewStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.model.VendorStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Platform-wide admin metrics from real local data. Pure logic — no Android
 * dependencies, unit-tested. Sales count COMPLETED reservations only.
 */
public final class AdminStats {

    private AdminStats() {}

    public static class Summary {
        public int totalVendors;
        public int approvedVendors;
        public int pendingVendors;
        public int rejectedVendors;
        public int suspendedVendors;
        public int totalListings;
        public int activeListings;
        public int totalReservations;
        public int reservedQtyOutstanding;
        public int completedPickups;
        public double estimatedSales;
        public int distinctCustomers;
        public int reviewsVisible;
        public int reviewsPending;
        public int reviewsHidden;
    }

    /** A customer account, joined with its observed reservation activity. */
    public static class CustomerSummary {
        public final String userId;
        /** Display name/email from the user directory when available. */
        public String label = "";
        public int reservations;
        public int completed;
        public long lastEpoch;

        public CustomerSummary(String userId) {
            this.userId = userId;
        }
    }

    public static Summary compute(List<Vendor> vendors,
                                  List<FoodListing> listings,
                                  List<Reservation> reservations,
                                  List<Review> reviews) {
        Summary s = new Summary();
        if (vendors != null) {
            s.totalVendors = vendors.size();
            for (Vendor v : vendors) {
                if (v == null || v.getStatus() == null) continue;
                if (v.getStatus() == VendorStatus.APPROVED) s.approvedVendors++;
                else if (v.getStatus() == VendorStatus.PENDING) s.pendingVendors++;
                else if (v.getStatus() == VendorStatus.REJECTED) s.rejectedVendors++;
                else if (v.getStatus() == VendorStatus.SUSPENDED) s.suspendedVendors++;
            }
        }
        Map<String, FoodListing> byId = new HashMap<>();
        if (listings != null) {
            s.totalListings = listings.size();
            for (FoodListing l : listings) {
                if (l == null) continue;
                byId.put(l.getId(), l);
                if (l.getStatus() == ListingStatus.ACTIVE) s.activeListings++;
            }
        }
        Map<String, Boolean> seenCustomers = new HashMap<>();
        if (reservations != null) {
            s.totalReservations = reservations.size();
            for (Reservation r : reservations) {
                if (r == null) continue;
                if (r.getUserId() != null) seenCustomers.put(r.getUserId(), true);
                if (r.getStatus() == ReservationStatus.RESERVED) {
                    s.reservedQtyOutstanding += r.getQuantity();
                } else if (r.getStatus() == ReservationStatus.COMPLETED) {
                    s.completedPickups++;
                    FoodListing l = byId.get(r.getListingId());
                    if (l != null) s.estimatedSales += r.getQuantity() * l.getDiscountedPrice();
                }
            }
        }
        s.distinctCustomers = seenCustomers.size();
        if (reviews != null) {
            for (Review r : reviews) {
                if (r == null || r.getStatus() == null) continue;
                if (r.getStatus() == ReviewStatus.VISIBLE) s.reviewsVisible++;
                else if (r.getStatus() == ReviewStatus.PENDING) s.reviewsPending++;
                else if (r.getStatus() == ReviewStatus.HIDDEN) s.reviewsHidden++;
            }
        }
        return s;
    }

    /** Distinct demo customers observed in reservation records (honest directory). */
    public static List<CustomerSummary> customerSummaries(List<Reservation> reservations) {
        Map<String, CustomerSummary> map = new HashMap<>();
        if (reservations != null) {
            for (Reservation r : reservations) {
                if (r == null || r.getUserId() == null) continue;
                CustomerSummary c = map.get(r.getUserId());
                if (c == null) {
                    c = new CustomerSummary(r.getUserId());
                    map.put(r.getUserId(), c);
                }
                c.reservations++;
                if (r.getStatus() == ReservationStatus.COMPLETED) c.completed++;
                if (r.getCreatedAtEpoch() > c.lastEpoch) c.lastEpoch = r.getCreatedAtEpoch();
            }
        }
        return new ArrayList<>(map.values());
    }

    /** Overview search: reservation ref, listing name or vendor name (case-insensitive). */
    public static boolean reservationMatches(Reservation r, String listingName,
                                             String vendorName, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        if (q.isEmpty()) return true;
        if (r.getId() != null && r.getId().toLowerCase(Locale.US).contains(q)) return true;
        if (listingName != null && listingName.toLowerCase(Locale.US).contains(q)) return true;
        return vendorName != null && vendorName.toLowerCase(Locale.US).contains(q);
    }
}
