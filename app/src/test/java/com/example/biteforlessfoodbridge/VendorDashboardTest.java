package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.util.VendorStats;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/** Vendor ownership, listing CRUD guards, pickup flow and metrics. */
public class VendorDashboardTest {

    private static final String VENDOR = "v1"; // Mama Chanda's Bakery (demo vendor)
    private static final String OTHER = "v2";
    private static final String USER = "customer@example.com";

    private DemoRepository repo;

    @Before
    public void setUp() {
        repo = DemoRepository.getInstance();
        repo.resetDemoData();
    }

    private long future() {
        return System.currentTimeMillis() + 6 * 3_600_000L;
    }

    // ---- ownership ----

    @Test
    public void vendorListings_containOnlyOwnListings() {
        for (FoodListing l : repo.getListingsForVendor(VENDOR)) {
            assertEquals(VENDOR, l.getVendorId());
        }
        assertTrue(repo.getListingsForVendor(VENDOR).size() >= 2); // l1, l2, l8 seeded
    }

    @Test
    public void vendorReservations_onlyCoverOwnListings() {
        FoodListing own = repo.getListingsForVendor(VENDOR).get(0);
        FoodListing foreign = repo.getListingsForVendor(OTHER).get(0);
        // Ensure both reservable for the test.
        own.setStatus(ListingStatus.ACTIVE);
        if (own.isExpired(System.currentTimeMillis())) {
            own.setCollectionDeadlineEpoch(future());
        }
        foreign.setStatus(ListingStatus.ACTIVE);
        if (foreign.isExpired(System.currentTimeMillis())) {
            foreign.setCollectionDeadlineEpoch(future());
        }
        if (own.getAvailableQuantity() < 1) own.setAvailableQuantity(3);
        if (foreign.getAvailableQuantity() < 1) foreign.setAvailableQuantity(3);
        repo.create(own.getId(), USER, 1);
        repo.create(foreign.getId(), USER, 1);
        List<Reservation> mine = repo.getReservationsForVendor(VENDOR);
        assertEquals(1, mine.size());
        assertEquals(own.getId(), mine.get(0).getListingId());
    }

    // ---- create ----

    @Test
    public void createListing_valid_isVisibleToCustomers() {
        int before = repo.getAll().size();
        FoodListing created = repo.createListing(VENDOR, "Demo Soup", "Tasty demo soup",
                "meals", 60.0, 30.0, 5, "Woodstock, Cape Town", future(),
                "Today 16:00-18:00", ListingStatus.ACTIVE);
        assertEquals(before + 1, repo.getAll().size());
        assertTrue(created.isReservable(System.currentTimeMillis()));
        assertFalse(repo.search("Demo Soup").isEmpty());
        Reservation r = repo.create(created.getId(), USER, 1);
        assertEquals(ReservationStatus.RESERVED, r.getStatus());
    }

    @Test
    public void createListing_invalidFields_throw() {
        assertThrowsMsg(() -> repo.createListing(VENDOR, "  ", "d", "meals",
                60.0, 30.0, 5, "Loc", future(), "w", ListingStatus.ACTIVE));
        assertThrowsMsg(() -> repo.createListing(VENDOR, "Soup", "d", "meals",
                30.0, 30.0, 5, "Loc", future(), "w", ListingStatus.ACTIVE)); // not below
        assertThrowsMsg(() -> repo.createListing(VENDOR, "Soup", "d", "meals",
                30.0, 40.0, 5, "Loc", future(), "w", ListingStatus.ACTIVE)); // above
        assertThrowsMsg(() -> repo.createListing(VENDOR, "Soup", "d", "meals",
                60.0, 30.0, -1, "Loc", future(), "w", ListingStatus.ACTIVE));
        assertThrowsMsg(() -> repo.createListing(VENDOR, "Soup", "d", "meals",
                60.0, 30.0, 0, "Loc", future(), "w", ListingStatus.ACTIVE)); // active + 0
        assertThrowsMsg(() -> repo.createListing(VENDOR, "Soup", "d", "nope",
                60.0, 30.0, 5, "Loc", future(), "w", ListingStatus.ACTIVE));
        assertThrowsMsg(() -> repo.createListing(VENDOR, "Soup", "d", "meals",
                60.0, 30.0, 5, "  ", future(), "w", ListingStatus.ACTIVE));
        assertThrowsMsg(() -> repo.createListing("unknown-vendor", "Soup", "d", "meals",
                60.0, 30.0, 5, "Loc", future(), "w", ListingStatus.ACTIVE));
        assertThrowsMsg(() -> repo.createListing(VENDOR, "Soup", "d", "meals",
                60.0, 30.0, 5, "Loc", System.currentTimeMillis() - 1000L, "w",
                ListingStatus.ACTIVE)); // past deadline
    }

    private void assertThrowsMsg(ThrowingCall call) {
        try {
            call.run();
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private interface ThrowingCall {
        void run();
    }

    // ---- edit / deactivate ----

    @Test
    public void updateListing_editsSameRecordWithoutDuplicates() {
        int before = repo.getAll().size();
        FoodListing l = repo.getListingsForVendor(VENDOR).get(0);
        repo.updateListing(l.getId(), VENDOR, "Renamed (demo)", "New desc", "bakery",
                70.0, 35.0, l.getAvailableQuantity(), "New location", future(), "New window");
        assertEquals(before, repo.getAll().size());
        FoodListing updated = repo.getById(l.getId());
        assertEquals("Renamed (demo)", updated.getName());
        assertEquals(70.0, updated.getOriginalPrice(), 0.001);
        assertEquals("New location", updated.getLocation());
        assertFalse(repo.search("Renamed").isEmpty()); // customer-visible
    }

    @Test
    public void deactivate_blocksNewReservationsButKeepsHistory() {
        FoodListing l = reservableVendorListing();
        Reservation r = repo.create(l.getId(), USER, 1);
        repo.setListingStatus(l.getId(), ListingStatus.INACTIVE, VENDOR);
        assertFalse(repo.getById(l.getId()).isReservable(System.currentTimeMillis()));
        try {
            repo.create(l.getId(), USER, 1);
            fail("expected inactive rejection");
        } catch (IllegalStateException expected) {
        }
        // Existing reservation untouched and still visible in history.
        assertEquals(ReservationStatus.RESERVED,
                repo.getReservationById(r.getId()).getStatus());
        assertEquals(1, repo.getForUser(USER).size());
        // Reactivate restores reservability.
        repo.setListingStatus(l.getId(), ListingStatus.ACTIVE, VENDOR);
        assertTrue(repo.getById(l.getId()).isReservable(System.currentTimeMillis()));
    }

    @Test
    public void updateListing_belowOutstandingReserved_throws() {
        FoodListing l = reservableVendorListing();
        repo.create(l.getId(), USER, 2);
        int outstanding = repo.getOutstandingReserved(l.getId());
        assertEquals(2, outstanding);
        int stockBefore = repo.getById(l.getId()).getAvailableQuantity();
        try {
            repo.updateListing(l.getId(), VENDOR, l.getName(), "d", l.getCategoryId(),
                    l.getOriginalPrice(), l.getDiscountedPrice(), outstanding - 1,
                    l.getLocation(), future(), l.getCollectionWindow());
            fail("expected committed-stock rejection");
        } catch (IllegalStateException expected) {
        }
        assertEquals(stockBefore, repo.getById(l.getId()).getAvailableQuantity());
        // Setting exactly outstanding is allowed.
        repo.updateListing(l.getId(), VENDOR, l.getName(), "d", l.getCategoryId(),
                l.getOriginalPrice(), l.getDiscountedPrice(), outstanding,
                l.getLocation(), future(), l.getCollectionWindow());
        assertEquals(outstanding, repo.getById(l.getId()).getAvailableQuantity());
    }

    // ---- cross-vendor guards ----

    @Test
    public void crossVendorManagement_rejected() {
        FoodListing foreign = repo.getListingsForVendor(OTHER).get(0);
        try {
            repo.updateListing(foreign.getId(), VENDOR, "Hijacked", "d",
                    foreign.getCategoryId(), foreign.getOriginalPrice(),
                    foreign.getDiscountedPrice(), foreign.getAvailableQuantity(),
                    foreign.getLocation(), future(), foreign.getCollectionWindow());
            fail("expected ownership rejection");
        } catch (IllegalStateException expected) {
        }
        try {
            repo.setListingStatus(foreign.getId(), ListingStatus.INACTIVE, VENDOR);
            fail("expected ownership rejection");
        } catch (IllegalStateException expected) {
        }
        assertTrue(repo.search("Hijacked").isEmpty());

        // Reservation completion on another vendor's listing is also rejected.
        foreign.setStatus(ListingStatus.ACTIVE);
        if (foreign.isExpired(System.currentTimeMillis())) {
            foreign.setCollectionDeadlineEpoch(future());
        }
        if (foreign.getAvailableQuantity() < 1) foreign.setAvailableQuantity(3);
        Reservation r = repo.create(foreign.getId(), USER, 1);
        try {
            repo.completeForVendor(r.getId(), VENDOR);
            fail("expected ownership rejection");
        } catch (IllegalStateException expected) {
        }
        assertEquals(ReservationStatus.RESERVED,
                repo.getReservationById(r.getId()).getStatus());
    }

    // ---- pickup completion + metrics ----

    @Test
    public void completeForVendor_success_noRestock_historyUpdated() {
        FoodListing l = reservableVendorListing();
        double unit = l.getDiscountedPrice();
        int before = l.getAvailableQuantity();
        Reservation r = repo.create(l.getId(), USER, 2);
        repo.completeForVendor(r.getId(), VENDOR);
        assertEquals(ReservationStatus.COMPLETED,
                repo.getReservationById(r.getId()).getStatus());
        assertEquals(before - 2, repo.getById(l.getId()).getAvailableQuantity());
        // History reflects completion.
        assertEquals(ReservationStatus.COMPLETED, repo.getForUser(USER).get(0).getStatus());
        // Metrics.
        VendorStats.Summary s = VendorStats.compute(
                repo.getAll(), repo.getAllReservations(), VENDOR);
        assertEquals(1, s.completedPickups);
        assertEquals(2 * unit, s.estimatedSales, 0.001);
        assertEquals(2, s.rescuedItems);
        // Repeated completion rejected, sales not double-counted.
        try {
            repo.completeForVendor(r.getId(), VENDOR);
            fail("expected repeated-completion rejection");
        } catch (IllegalStateException expected) {
        }
        VendorStats.Summary again = VendorStats.compute(
                repo.getAll(), repo.getAllReservations(), VENDOR);
        assertEquals(2 * unit, again.estimatedSales, 0.001);
    }

    @Test
    public void metrics_reflectRealData() {
        VendorStats.Summary empty = VendorStats.compute(
                repo.getAll(), repo.getAllReservations(), VENDOR);
        assertEquals(0, empty.completedPickups);
        assertEquals(0.0, empty.estimatedSales, 0.001);
        assertTrue(empty.totalListings >= 2);
        assertTrue(empty.availableStock > 0);

        FoodListing l = reservableVendorListing();
        repo.completeForVendor(repo.create(l.getId(), USER, 1).getId(), VENDOR);
        VendorStats.Summary s = VendorStats.compute(
                repo.getAll(), repo.getAllReservations(), VENDOR);
        assertEquals(1, s.completedPickups);
        assertEquals(1, s.totalReservations);
        assertEquals(l.getDiscountedPrice(), s.estimatedSales, 0.001);
        assertEquals(l.getSavings(), s.customerSavings, 0.001);
    }

    private FoodListing reservableVendorListing() {
        long now = System.currentTimeMillis();
        for (FoodListing l : repo.getListingsForVendor(VENDOR)) {
            if (l.getStatus() != ListingStatus.ACTIVE) l.setStatus(ListingStatus.ACTIVE);
            if (l.isExpired(now)) l.setCollectionDeadlineEpoch(future());
            if (l.getAvailableQuantity() < 3) l.setAvailableQuantity(5);
            if (l.isReservable(System.currentTimeMillis())) return l;
        }
        throw new AssertionError("no usable vendor listing");
    }
}
