package com.example.biteforlessfoodbridge.data;

import com.example.biteforlessfoodbridge.model.Category;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.ReviewStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.model.VendorStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Clearly-marked DEMONSTRATION seed data (no backend).
 * Seeded once per repository init; covers available, low-stock,
 * out-of-stock, inactive and expired examples.
 */
public final class DemoData {

    private DemoData() {}

    public static List<Category> categories() {
        List<Category> out = new ArrayList<>();
        out.add(new Category("bakery", "Bakery"));
        out.add(new Category("produce", "Produce"));
        out.add(new Category("dairy", "Dairy"));
        out.add(new Category("meals", "Meals"));
        out.add(new Category("beverages", "Beverages"));
        out.add(new Category("snacks", "Snacks"));
        return out;
    }

    public static List<Vendor> vendors() {
        List<Vendor> out = new ArrayList<>();
        out.add(new Vendor("v1", "Mama Chanda's Bakery", "Matero, Lusaka", VendorStatus.APPROVED));
        out.add(new Vendor("v2", "Soweto Fresh Produce", "Soweto Market, Lusaka", VendorStatus.APPROVED));
        out.add(new Vendor("v3", "Kapiri Kitchen", "Kalingalinga, Lusaka", VendorStatus.APPROVED));
        out.add(new Vendor("v4", "Lusaka Dairy Corner", "Kabulonga, Lusaka", VendorStatus.PENDING));
        out.add(new Vendor("v5", "Cairo Road Cafe", "Cairo Road, Lusaka", VendorStatus.SUSPENDED));
        return out;
    }

    /** Deadlines are relative to {@code nowEpoch} so the demo is always coherent. */
    public static List<FoodListing> listings(long nowEpoch) {
        long h = 3_600_000L;
        List<FoodListing> out = new ArrayList<>();
        out.add(new FoodListing("l1", "v1", "Sourdough Loaf Bundle",
                "Demo: two day-old sourdough loaves, still fresh for toast.",
                "bakery", 60.0, 29.0, 8, "Matero, Lusaka",
                nowEpoch + 6 * h, "Today 16:00-18:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l2", "v1", "Butter Croissants (4-pack)",
                "Demo: morning-batch croissants, flaky and buttery.",
                "bakery", 80.0, 35.0, 2, "Matero, Lusaka",
                nowEpoch + 4 * h, "Today 15:00-17:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l3", "v2", "Seasonal Veggie Box",
                "Demo: mixed surplus vegetables, great for a week of cooking.",
                "produce", 120.0, 59.0, 10, "Soweto Market, Lusaka",
                nowEpoch + 8 * h, "Today 14:00-19:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l4", "v2", "Fruit Salad Cups (2-pack)",
                "Demo: pre-cut fruit cups from this morning.",
                "produce", 45.0, 20.0, 0, "Soweto Market, Lusaka",
                nowEpoch + 5 * h, "Today 14:00-18:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l5", "v3", "Chicken Curry + Rice",
                "Demo: hearty lunch portions, cooked fresh at midday.",
                "meals", 95.0, 45.0, 6, "Kalingalinga, Lusaka",
                nowEpoch + 3 * h, "Today 12:00-15:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l6", "v3", "Veg Bean Stew",
                "Demo: slow-cooked bean stew, vegan friendly.",
                "meals", 70.0, 30.0, 12, "Kalingalinga, Lusaka",
                nowEpoch + 3 * h, "Today 12:00-15:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l7", "v5", "Cafe Muffin Box (4-pack)",
                "Demo: assorted muffins. Vendor account suspended, listing paused.",
                "snacks", 55.0, 25.0, 3, "Cairo Road, Lusaka",
                nowEpoch + 5 * h, "Today 13:00-17:00", ListingStatus.INACTIVE));
        out.add(new FoodListing("l8", "v1", "Cinnamon Rolls (6-pack)",
                "Demo: yesterday's cinnamon rolls, still soft inside.",
                "bakery", 90.0, 40.0, 5, "Matero, Lusaka",
                nowEpoch - 2 * h, "Was 08:00-10:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l9", "v2", "Fresh Milk 2L",
                "Demo: two 2L bottles nearing best-before, keep refrigerated.",
                "dairy", 70.0, 55.0, 9, "Soweto Market, Lusaka",
                nowEpoch + 24 * h, "Tomorrow 09:00-18:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l10", "v3", "Beef Samosas (6-pack)",
                "Demo: crispy samosas, perfect for sharing.",
                "snacks", 60.0, 27.0, 1, "Kalingalinga, Lusaka",
                nowEpoch + 2 * h, "Today 12:00-14:00", ListingStatus.ACTIVE));
        out.add(new FoodListing("l11", "v4", "Drinking Yoghurt (4-pack)",
                "Demo: mixed-flavour yoghurts. Vendor pending approval.",
                "dairy", 85.0, 38.0, 7, "Kabulonga, Lusaka",
                nowEpoch + 20 * h, "Tomorrow 09:00-17:00", ListingStatus.INACTIVE));
        out.add(new FoodListing("l12", "v2", "Fresh Orange Juice 1L",
                "Demo: squeezed this morning, no added sugar.",
                "beverages", 50.0, 22.0, 15, "Soweto Market, Lusaka",
                nowEpoch + 10 * h, "Today 09:00-19:00", ListingStatus.ACTIVE));
        return out;
    }

    /**
     * Clearly-marked SAMPLE reviews for moderation demo. Not verified
     * purchases; userIds are demo identifiers only.
     */
    public static List<Review> reviews(long nowEpoch) {
        List<Review> out = new ArrayList<>();
        out.add(new Review("r1", "l1", "demo-customer-01", 5,
                "Demo review: crusty and fresh, great value.",
                nowEpoch - 5 * 3_600_000L, ReviewStatus.VISIBLE));
        out.add(new Review("r2", "l5", "demo-customer-02", 4,
                "Demo review: generous portion, pickup was quick.",
                nowEpoch - 3 * 3_600_000L, ReviewStatus.VISIBLE));
        out.add(new Review("r3", "l3", "demo-customer-01", 3,
                "Demo review: box was good but heavy on potatoes.",
                nowEpoch - 2 * 3_600_000L, ReviewStatus.PENDING));
        out.add(new Review("r4", "l10", "demo-customer-03", 2,
                "Demo review: portion smaller than expected.",
                nowEpoch - 3_600_000L, ReviewStatus.PENDING));
        return out;
    }
}
