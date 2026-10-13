package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.model.ReviewStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.model.VendorStatus;
import com.example.biteforlessfoodbridge.util.AdminStats;
import com.example.biteforlessfoodbridge.util.ListingQuery;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin metrics, vendor gates, moderation and activity log. */
public class AdminDashboardTest {

    private static final String ACTOR = "admin-demo@test.com";

    private DemoRepository repo;

    @Before
    public void setUp() {
        repo = DemoRepository.getInstance();
        repo.resetDemoData();
    }

    private long future() {
        return System.currentTimeMillis() + 6 * 3_600_000L;
    }

    // ---- metrics ----

    @Test
    public void metrics_matchSeededData() {
        AdminStats.Summary s = AdminStats.compute(repo.getVendors(), repo.getAll(),
                repo.getAllReservations(), repo.getReviews());
        assertEquals(5, s.totalVendors);
        assertEquals(3, s.approvedVendors);
        assertEquals(1, s.pendingVendors);
        assertEquals(1, s.suspendedVendors);
        assertEquals(12, s.totalListings);
        assertTrue(s.activeListings > 0);
        assertEquals(0, s.totalReservations);
        assertEquals(0, s.distinctCustomers);
        assertEquals(2, s.reviewsVisible);
        assertEquals(2, s.reviewsPending);
        assertEquals(0, s.reviewsHidden);
    }

    @Test
    public void metrics_salesExcludeCancelledAndExpired() {
        FoodListing l = firstReservable();
        double unit = l.getDiscountedPrice();
        Reservation keep = repo.create(l.getId(), "a@test.com", 1);
        repo.complete(keep.getId());
        Reservation cancelled = repo.create(l.getId(), "b@test.com", 1);
        repo.cancel(cancelled.getId());
        AdminStats.Summary s = AdminStats.compute(repo.getVendors(), repo.getAll(),
                repo.getAllReservations(), repo.getReviews());
        assertEquals(2, s.totalReservations);
        assertEquals(1, s.completedPickups);
        assertEquals(unit, s.estimatedSales, 0.001);
    }

    // ---- vendor approval ----

    @Test
    public void approvePendingVendor_thenListingCreationAllowed() {
        assertEquals(VendorStatus.PENDING, repo.getVendorById("v4").getStatus());
        try {
            repo.createListing("v4", "Yoghurt", "d", "dairy", 50.0, 25.0, 3,
                    "Claremont", future(), "w", ListingStatus.ACTIVE);
            fail("pending vendor must be blocked");
        } catch (IllegalStateException expected) {
        }
        repo.setVendorStatus("v4", VendorStatus.APPROVED, ACTOR);
        assertEquals(VendorStatus.APPROVED, repo.getVendorById("v4").getStatus());
        FoodListing created = repo.createListing("v4", "Yoghurt", "d", "dairy", 50.0,
                25.0, 3, "Claremont", future(), "w", ListingStatus.ACTIVE);
        assertTrue(created.isReservable(System.currentTimeMillis()));
    }

    @Test
    public void suspendVendor_blocksRestrictedOps_preservesHistoryAndStock() {
        FoodListing l = firstReservableFrom("v2");
        Reservation r = repo.create(l.getId(), "c@test.com", 2);
        int stockBefore = repo.getById(l.getId()).getAvailableQuantity();

        repo.setVendorStatus("v2", VendorStatus.SUSPENDED, ACTOR);
        assertEquals(VendorStatus.SUSPENDED, repo.getVendorById("v2").getStatus());

        // History + stock untouched by suspension.
        assertEquals(ReservationStatus.RESERVED,
                repo.getReservationById(r.getId()).getStatus());
        assertEquals(stockBefore, repo.getById(l.getId()).getAvailableQuantity());

        // Restricted ops rejected through any entry point.
        try {
            repo.createListing("v2", "X", "d", "meals", 50.0, 25.0, 3,
                    "Loc", future(), "w", ListingStatus.ACTIVE);
            fail("suspended create must be blocked");
        } catch (IllegalStateException expected) {
        }
        try {
            repo.updateListing(l.getId(), "v2", "X", "d", l.getCategoryId(),
                    l.getOriginalPrice(), l.getDiscountedPrice(), l.getAvailableQuantity(),
                    l.getLocation(), future(), l.getCollectionWindow());
            fail("suspended update must be blocked");
        } catch (IllegalStateException expected) {
        }
        repo.setListingStatus(l.getId(), ListingStatus.INACTIVE, "v2");
        // Deactivation stays allowed for a suspended vendor (safe direction).
        assertEquals(ListingStatus.INACTIVE, repo.getById(l.getId()).getStatus());
        try {
            repo.setListingStatus(l.getId(), ListingStatus.ACTIVE, "v2");
            fail("suspended reactivation must be blocked");
        } catch (IllegalStateException expected) {
        }
        // Existing reservation still completable (fulfil obligations, no restock).
        repo.completeForVendor(r.getId(), "v2");
        assertEquals(ReservationStatus.COMPLETED,
                repo.getReservationById(r.getId()).getStatus());
        assertEquals(stockBefore, repo.getById(l.getId()).getAvailableQuantity());

        // Customer gate also blocks new reservations on suspended vendor listings.
        repo.setListingStatusByAdmin(l.getId(), ListingStatus.INACTIVE, ACTOR);
        try {
            repo.create(l.getId(), "d@test.com", 1);
            fail("expected rejection");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void setVendorStatus_sameStatus_throws() {
        try {
            repo.setVendorStatus("v1", VendorStatus.APPROVED, ACTOR);
            fail("expected no-op rejection");
        } catch (IllegalStateException expected) {
        }
    }

    // ---- customer directory ----

    @Test
    public void customerSummaries_distinctObservedIds() {
        FoodListing l = firstReservable();
        repo.create(l.getId(), "a@test.com", 1);
        repo.create(l.getId(), "a@test.com", 1);
        repo.complete(repo.create(l.getId(), "b@test.com", 1).getId());
        List<AdminStats.CustomerSummary> rows =
                AdminStats.customerSummaries(repo.getAllReservations());
        assertEquals(2, rows.size());
        for (AdminStats.CustomerSummary c : rows) {
            if ("a@test.com".equals(c.userId)) {
                assertEquals(2, c.reservations);
                assertEquals(0, c.completed);
            } else if ("b@test.com".equals(c.userId)) {
                assertEquals(1, c.reservations);
                assertEquals(1, c.completed);
            } else {
                fail("unexpected user " + c.userId);
            }
            assertTrue(c.lastEpoch > 0);
        }
        AdminStats.Summary s = AdminStats.compute(repo.getVendors(), repo.getAll(),
                repo.getAllReservations(), repo.getReviews());
        assertEquals(2, s.distinctCustomers);
    }

    // ---- listing directory ----

    @Test
    public void adminListingFilter_buckets() {
        Map<String, Vendor> vendors = new HashMap<>();
        for (Vendor v : repo.getVendors()) vendors.put(v.getId(), v);
        long now = System.currentTimeMillis();

        List<FoodListing> expired = ListingQuery.applyForAdmin(repo.getAll(), vendors, "",
                "all", ListingQuery.StatusFilter.EXPIRED, ListingQuery.SortOrder.DEFAULT, now);
        assertFalse(expired.isEmpty());
        for (FoodListing l : expired) assertTrue(l.isExpired(now));

        List<FoodListing> soldOut = ListingQuery.applyForAdmin(repo.getAll(), vendors, "",
                "all", ListingQuery.StatusFilter.SOLD_OUT, ListingQuery.SortOrder.DEFAULT, now);
        assertFalse(soldOut.isEmpty());
        for (FoodListing l : soldOut) assertEquals(0, l.getAvailableQuantity());

        List<FoodListing> paused = ListingQuery.applyForAdmin(repo.getAll(), vendors, "",
                "all", ListingQuery.StatusFilter.PAUSED, ListingQuery.SortOrder.DEFAULT, now);
        assertFalse(paused.isEmpty());

        List<FoodListing> bookable = ListingQuery.applyForAdmin(repo.getAll(), vendors, "",
                "all", ListingQuery.StatusFilter.BOOKABLE, ListingQuery.SortOrder.DEFAULT, now);
        for (FoodListing l : bookable) assertTrue(l.isReservable(now));

        List<FoodListing> bakery = ListingQuery.applyForAdmin(repo.getAll(), vendors, "",
                "bakery", ListingQuery.StatusFilter.ALL, ListingQuery.SortOrder.DEFAULT, now);
        assertFalse(bakery.isEmpty());
        for (FoodListing l : bakery) assertEquals("bakery", l.getCategoryId());
    }

    @Test
    public void adminDeactivateReactivate_rules() {
        FoodListing l = firstReservable();
        repo.setListingStatusByAdmin(l.getId(), ListingStatus.INACTIVE, ACTOR);
        assertFalse(repo.getById(l.getId()).isReservable(System.currentTimeMillis()));
        try {
            repo.create(l.getId(), "x@test.com", 1);
            fail("customer must be blocked after admin deactivation");
        } catch (IllegalStateException expected) {
        }
        repo.setListingStatusByAdmin(l.getId(), ListingStatus.ACTIVE, ACTOR);
        assertTrue(repo.getById(l.getId()).isReservable(System.currentTimeMillis()));

        // Reactivation refused while the vendor is suspended.
        FoodListing v5listing = repo.getListingsForVendor("v5").get(0);
        try {
            repo.setListingStatusByAdmin(v5listing.getId(), ListingStatus.ACTIVE, ACTOR);
            fail("suspended-vendor reactivation must be blocked");
        } catch (IllegalStateException expected) {
        }
        // ...until the vendor is re-approved.
        repo.setVendorStatus("v5", VendorStatus.APPROVED, ACTOR);
        repo.setListingStatusByAdmin(v5listing.getId(), ListingStatus.ACTIVE, ACTOR);
        assertEquals(ListingStatus.ACTIVE, repo.getById(v5listing.getId()).getStatus());
    }

    // ---- reservation overview ----

    @Test
    public void reservationOverview_countsAllStatuses() {
        FoodListing l = firstReservable();
        Reservation r1 = repo.create(l.getId(), "a@test.com", 1);
        Reservation r2 = repo.create(l.getId(), "b@test.com", 1);
        repo.cancel(r2.getId());
        repo.complete(r1.getId());
        assertEquals(2, repo.getAllReservations().size());
        assertTrue(AdminStats.reservationMatches(r1, l.getName(), "Ubuntu", "res-"));
        assertTrue(AdminStats.reservationMatches(r1, l.getName(), "Ubuntu", "ubuntu"));
        assertFalse(AdminStats.reservationMatches(r1, l.getName(), "Ubuntu", "zzz-no-match"));
    }

    // ---- reviews ----

    @Test
    public void reviewModeration_hideShowPreservesRecord() {
        assertEquals(2, repo.getVisibleReviewsForListing("l1").size()
                + repo.getVisibleReviewsForListing("l5").size());
        repo.setReviewStatus("r1", ReviewStatus.HIDDEN, ACTOR);
        assertTrue(repo.getVisibleReviewsForListing("l1").isEmpty());
        assertNotNull(repo.getReviewById("r1")); // preserved
        repo.setReviewStatus("r3", ReviewStatus.VISIBLE, ACTOR);
        assertEquals(1, repo.getVisibleReviewsForListing("l3").size());
    }

    // ---- activity log ----

    @Test
    public void activityLog_recordsRealAdminActions() {
        assertTrue(repo.getActivityLog().isEmpty()); // never pre-seeded
        repo.setVendorStatus("v4", VendorStatus.APPROVED, ACTOR);
        FoodListing l = firstReservable();
        repo.setListingStatusByAdmin(l.getId(), ListingStatus.INACTIVE, ACTOR);
        repo.setReviewStatus("r1", ReviewStatus.HIDDEN, ACTOR);
        assertEquals(3, repo.getActivityLog().size());
        // Newest first, with actor + type recorded.
        assertEquals(DemoRepository.EVENT_REVIEW, repo.getActivityLog().get(0).getType());
        for (int i = 0; i < 3; i++) {
            assertEquals(ACTOR, repo.getActivityLog().get(i).getActor());
            assertNotNull(repo.getActivityLog().get(i).getId());
            assertTrue(repo.getActivityLog().get(i).getAtEpoch() > 0);
        }
        repo.resetDemoData();
        assertTrue(repo.getActivityLog().isEmpty());
    }

    private FoodListing firstReservable() {
        long now = System.currentTimeMillis();
        for (FoodListing l : repo.getAll()) {
            if (l.isReservable(now) && l.getAvailableQuantity() >= 3) return l;
        }
        throw new AssertionError("no reservable seed listing");
    }

    private FoodListing firstReservableFrom(String vendorId) {
        for (FoodListing l : repo.getListingsForVendor(vendorId)) {
            if (l.getStatus() != ListingStatus.ACTIVE) l.setStatus(ListingStatus.ACTIVE);
            if (l.isExpired(System.currentTimeMillis())) {
                l.setCollectionDeadlineEpoch(future());
            }
            if (l.getAvailableQuantity() < 3) l.setAvailableQuantity(5);
            if (l.isReservable(System.currentTimeMillis())) return l;
        }
        throw new AssertionError("no usable listing for " + vendorId);
    }
}
