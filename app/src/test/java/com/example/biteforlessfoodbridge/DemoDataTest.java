package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.model.Category;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.Vendor;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Demo seed integrity: realistic mix, valid prices, unique ids, state examples. */
public class DemoDataTest {

    private DemoRepository repo;

    @Before
    public void setUp() {
        repo = DemoRepository.getInstance();
        repo.resetDemoData();
    }

    @Test
    public void seed_hasCategoriesVendorsAndListings() {
        List<Category> categories = repo.getCategories();
        List<Vendor> vendors = repo.getVendors();
        List<FoodListing> listings = repo.getAll();
        assertTrue("expected several categories", categories.size() >= 4);
        assertTrue("expected several vendors", vendors.size() >= 3);
        assertTrue("expected ~12 listings", listings.size() >= 8);
    }

    @Test
    public void seed_idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (FoodListing l : repo.getAll()) {
            assertTrue("duplicate listing id: " + l.getId(), ids.add(l.getId()));
        }
    }

    @Test
    public void seed_allPricesValid() {
        for (FoodListing l : repo.getAll()) {
            assertTrue("original must be > 0 for " + l.getId(), l.getOriginalPrice() > 0);
            assertTrue("discounted must be below original for " + l.getId(),
                    l.getDiscountedPrice() >= 0 && l.getDiscountedPrice() < l.getOriginalPrice());
        }
    }

    @Test
    public void seed_coversAvailabilitySpectrum() {
        long now = System.currentTimeMillis();
        boolean reservable = false;
        boolean lowStock = false;
        boolean zeroStock = false;
        boolean expired = false;
        boolean inactive = false;
        for (FoodListing l : repo.getAll()) {
            if (l.isReservable(now)) reservable = true;
            if (l.isReservable(now) && l.getAvailableQuantity() <= 2) lowStock = true;
            if (l.getAvailableQuantity() <= 0) zeroStock = true;
            if (l.isExpired(now)) expired = true;
            if (l.getStatus() != com.example.biteforlessfoodbridge.model.ListingStatus.ACTIVE) inactive = true;
        }
        assertTrue("expected reservable listings", reservable);
        assertTrue("expected low-stock example", lowStock);
        assertTrue("expected out-of-stock example", zeroStock);
        assertTrue("expected expired example", expired);
        assertTrue("expected inactive example", inactive);
    }

    @Test
    public void reset_restoresOriginalStock() {
        FoodListing first = repo.getAll().get(0);
        int before = first.getAvailableQuantity();
        repo.create(first.getId(), "tester@example.com", 1);
        assertEquals(before - 1, repo.getById(first.getId()).getAvailableQuantity());
        repo.resetDemoData();
        assertEquals(before, repo.getById(first.getId()).getAvailableQuantity());
        assertTrue(repo.getAllReservations().isEmpty());
    }
}
