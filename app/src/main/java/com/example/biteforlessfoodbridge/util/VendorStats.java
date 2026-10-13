package com.example.biteforlessfoodbridge.util;

import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vendor dashboard metrics computed from real local data (no hard-coding).
 * Pure logic — no Android dependencies, unit-tested.
 *
 * <ul>
 *   <li>Estimated sales = sum over COMPLETED reservations of qty × discounted price.</li>
 *   <li>Customer savings = sum over COMPLETED of qty × (original − discounted).</li>
 *   <li>Meals rescued = total qty in COMPLETED reservations.</li>
 *   <li>Available stock = current qty across the vendor's listings.</li>
 * </ul>
 */
public final class VendorStats {

    private VendorStats() {}

    public static class Summary {
        public int totalListings;
        public int activeListings;
        public int totalReservations;
        public int reservedCount;
        public int completedPickups;
        public int cancelledCount;
        public int expiredCount;
        public double estimatedSales;
        public double customerSavings;
        public int rescuedItems;
        public int availableStock;
    }

    public static Summary compute(List<FoodListing> allListings,
                                  List<Reservation> allReservations,
                                  String vendorId) {
        Summary s = new Summary();
        if (vendorId == null) return s;
        Map<String, FoodListing> byId = new HashMap<>();
        if (allListings != null) {
            for (FoodListing l : allListings) {
                if (l == null) continue;
                byId.put(l.getId(), l);
                if (vendorId.equals(l.getVendorId())) {
                    s.totalListings++;
                    if (l.getStatus() == ListingStatus.ACTIVE) s.activeListings++;
                    s.availableStock += Math.max(0, l.getAvailableQuantity());
                }
            }
        }
        if (allReservations != null) {
            for (Reservation r : allReservations) {
                if (r == null) continue;
                FoodListing l = byId.get(r.getListingId());
                if (l == null || !vendorId.equals(l.getVendorId())) continue;
                s.totalReservations++;
                if (r.getStatus() == ReservationStatus.RESERVED) {
                    s.reservedCount++;
                } else if (r.getStatus() == ReservationStatus.COMPLETED) {
                    s.completedPickups++;
                    s.estimatedSales += r.getQuantity() * l.getDiscountedPrice();
                    s.customerSavings += r.getQuantity() * l.getSavings();
                    s.rescuedItems += r.getQuantity();
                } else if (r.getStatus() == ReservationStatus.CANCELLED) {
                    s.cancelledCount++;
                } else if (r.getStatus() == ReservationStatus.EXPIRED) {
                    s.expiredCount++;
                }
            }
        }
        return s;
    }
}
