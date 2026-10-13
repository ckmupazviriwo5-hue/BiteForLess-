package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.util.ListingQuery;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Search, combined filters and sorting against the shared dataset. */
public class ListingQueryTest {

    private List<FoodListing> source;
    private Map<String, Vendor> vendors;
    private long now;

    @Before
    public void setUp() {
        DemoRepository repo = DemoRepository.getInstance();
        repo.resetDemoData();
        source = repo.getAll();
        vendors = new HashMap<>();
        for (Vendor v : repo.getVendors()) vendors.put(v.getId(), v);
        now = System.currentTimeMillis();
    }

    @Test
    public void search_matchesNameCaseInsensitive() {
        List<FoodListing> result = ListingQuery.apply(source, vendors, "SOURDOUGH",
                "all", null, 0, false, ListingQuery.SortOrder.DEFAULT, now);
        assertFalse(result.isEmpty());
        for (FoodListing l : result) {
            assertTrue(l.getName().toLowerCase().contains("sourdough"));
        }
    }

    @Test
    public void search_matchesVendor() {
        List<FoodListing> result = ListingQuery.apply(source, vendors, "soweto",
                "all", null, 0, false, ListingQuery.SortOrder.DEFAULT, now);
        assertFalse(result.isEmpty());
        for (FoodListing l : result) {
            assertEquals("v2", l.getVendorId());
        }
    }

    @Test
    public void filter_categoryAndMaxPriceAndDiscount_combined() {
        List<FoodListing> result = ListingQuery.apply(source, vendors, "",
                "bakery", 40.0, 50, false, ListingQuery.SortOrder.DEFAULT, now);
        assertFalse(result.isEmpty());
        for (FoodListing l : result) {
            assertEquals("bakery", l.getCategoryId());
            assertTrue(l.getDiscountedPrice() <= 40.0);
            assertTrue(l.getDiscountPercent() >= 50);
        }
    }

    @Test
    public void filter_availableOnly_excludesExpiredAndSoldOut() {
        List<FoodListing> result = ListingQuery.apply(source, vendors, "",
                "all", null, 0, true, ListingQuery.SortOrder.DEFAULT, now);
        assertFalse(result.isEmpty());
        for (FoodListing l : result) {
            assertTrue("should be reservable: " + l.getId(), l.isReservable(now));
        }
    }

    @Test
    public void filter_clearingRestoresFullSet() {
        List<FoodListing> filtered = ListingQuery.apply(source, vendors, "sourdough",
                "bakery", 10.0, 90, true, ListingQuery.SortOrder.PRICE_ASC, now);
        List<FoodListing> cleared = ListingQuery.apply(source, vendors, "",
                "all", null, 0, false, ListingQuery.SortOrder.DEFAULT, now);
        assertEquals(source.size(), cleared.size());
        assertTrue(filtered.size() < cleared.size());
    }

    @Test
    public void sort_priceAscending_ordersCorrectly() {
        List<FoodListing> result = ListingQuery.apply(source, vendors, "",
                "all", null, 0, false, ListingQuery.SortOrder.PRICE_ASC, now);
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).getDiscountedPrice()
                    <= result.get(i).getDiscountedPrice());
        }
    }

    @Test
    public void sort_discountDescending_ordersCorrectly() {
        List<FoodListing> result = ListingQuery.apply(source, vendors, "",
                "all", null, 0, false, ListingQuery.SortOrder.DISCOUNT_DESC, now);
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).getDiscountPercent()
                    >= result.get(i).getDiscountPercent());
        }
    }

    @Test
    public void sort_doesNotMutateSource() {
        List<String> before = new ArrayList<>();
        for (FoodListing l : source) before.add(l.getId());
        ListingQuery.apply(source, vendors, "", "all", null, 0, false,
                ListingQuery.SortOrder.PRICE_DESC, now);
        for (int i = 0; i < source.size(); i++) {
            assertEquals(before.get(i), source.get(i).getId());
        }
    }

    @Test
    public void sort_deadlineAscending_ordersCorrectly() {
        List<FoodListing> result = ListingQuery.apply(source, vendors, "",
                "all", null, 0, false, ListingQuery.SortOrder.DEADLINE_ASC, now);
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).getCollectionDeadlineEpoch()
                    <= result.get(i).getCollectionDeadlineEpoch());
        }
    }
}
