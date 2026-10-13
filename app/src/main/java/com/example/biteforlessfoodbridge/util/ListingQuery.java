package com.example.biteforlessfoodbridge.util;

import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Vendor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure search / filter / sort over listings. No Android dependencies so it is
 * unit-testable. Never mutates the source list.
 */
public final class ListingQuery {

    public enum SortOrder {
        DEFAULT,
        PRICE_ASC,
        PRICE_DESC,
        DISCOUNT_DESC,
        DEADLINE_ASC
    }

    /**
     * Admin availability buckets, evaluated in priority order so every listing
     * lands in exactly one bucket: EXPIRED first, then SOLD_OUT (active but
     * zero stock), then PAUSED (inactive), otherwise BOOKABLE means reservable.
     */
    public enum StatusFilter {
        ALL,
        BOOKABLE,
        PAUSED,
        SOLD_OUT,
        EXPIRED
    }

    private ListingQuery() {}

    /**
     * @param vendorsById vendor lookup for vendor-name search; may be null.
     * @param categoryId  null or "all" disables the category filter.
     * @param maxPrice    null disables the price filter (compares discounted price).
     * @param minDiscountPct null or 0 disables the discount filter.
     */
    public static List<FoodListing> apply(List<FoodListing> source,
                                         Map<String, Vendor> vendorsById,
                                         String query,
                                         String categoryId,
                                         Double maxPrice,
                                         Integer minDiscountPct,
                                         boolean availableOnly,
                                         SortOrder sort,
                                         long nowEpoch) {
        return applyFull(source, vendorsById, query, categoryId, maxPrice,
                minDiscountPct, availableOnly, StatusFilter.ALL, sort, nowEpoch);
    }

    /** Admin variant with an availability-bucket filter instead of available-only. */
    public static List<FoodListing> applyForAdmin(List<FoodListing> source,
                                                 Map<String, Vendor> vendorsById,
                                                 String query,
                                                 String categoryId,
                                                 StatusFilter statusFilter,
                                                 SortOrder sort,
                                                 long nowEpoch) {
        return applyFull(source, vendorsById, query, categoryId, null, 0,
                false, statusFilter == null ? StatusFilter.ALL : statusFilter,
                sort, nowEpoch);
    }

    private static List<FoodListing> applyFull(List<FoodListing> source,
                                              Map<String, Vendor> vendorsById,
                                              String query,
                                              String categoryId,
                                              Double maxPrice,
                                              Integer minDiscountPct,
                                              boolean availableOnly,
                                              StatusFilter statusFilter,
                                              SortOrder sort,
                                              long nowEpoch) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        List<FoodListing> out = new ArrayList<>();
        if (source == null) return out;
        for (FoodListing l : source) {
            if (l == null) continue;
            if (!q.isEmpty() && !matchesQuery(l, vendorName(vendorsById, l.getVendorId()), q)) continue;
            if (categoryId != null && !"all".equalsIgnoreCase(categoryId)
                    && !categoryId.equals(l.getCategoryId())) continue;
            if (maxPrice != null && l.getDiscountedPrice() > maxPrice) continue;
            if (minDiscountPct != null && minDiscountPct > 0
                    && l.getDiscountPercent() < minDiscountPct) continue;
            if (availableOnly && !l.isReservable(nowEpoch)) continue;
            if (!matchesStatus(l, statusFilter, nowEpoch)) continue;
            out.add(l);
        }
        if (sort != null) {
            switch (sort) {
                case PRICE_ASC:
                    Collections.sort(out, Comparator.comparingDouble(FoodListing::getDiscountedPrice));
                    break;
                case PRICE_DESC:
                    Collections.sort(out, (a, b) -> Double.compare(
                            b.getDiscountedPrice(), a.getDiscountedPrice()));
                    break;
                case DISCOUNT_DESC:
                    Collections.sort(out, (a, b) -> Integer.compare(
                            b.getDiscountPercent(), a.getDiscountPercent()));
                    break;
                case DEADLINE_ASC:
                    Collections.sort(out, Comparator.comparingLong(FoodListing::getCollectionDeadlineEpoch));
                    break;
                case DEFAULT:
                default:
                    break; // keep canonical repository ordering
            }
        }
        return out;
    }

    private static String vendorName(Map<String, Vendor> vendorsById, String vendorId) {
        if (vendorsById == null || vendorId == null) return "";
        Vendor v = vendorsById.get(vendorId);
        return v == null ? "" : v.getName();
    }

    /** Single-bucket classification; see {@link StatusFilter}. */
    public static boolean matchesStatus(FoodListing l, StatusFilter filter, long nowEpoch) {
        if (filter == null || filter == StatusFilter.ALL) return true;
        boolean expired = l.isExpired(nowEpoch);
        switch (filter) {
            case EXPIRED:
                return expired;
            case SOLD_OUT:
                return !expired && l.getAvailableQuantity() <= 0
                        && l.getStatus() == ListingStatus.ACTIVE;
            case PAUSED:
                return !expired && l.getStatus() == ListingStatus.INACTIVE;
            case BOOKABLE:
            default:
                return l.isReservable(nowEpoch);
        }
    }

    private static boolean matchesQuery(FoodListing l, String vendorName, String q) {
        return l.getName().toLowerCase(Locale.US).contains(q)
                || vendorName.toLowerCase(Locale.US).contains(q);
    }
}
