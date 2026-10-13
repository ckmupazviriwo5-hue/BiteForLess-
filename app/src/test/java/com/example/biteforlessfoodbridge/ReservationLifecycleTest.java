package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.data.DemoRepository;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/** Reservation lifecycle: create, stock rules, cancel/complete/expiry, history. */
public class ReservationLifecycleTest {

    private static final String USER = "customer@example.com";

    private DemoRepository repo;

    @Before
    public void setUp() {
        repo = DemoRepository.getInstance();
        repo.resetDemoData();
    }

    private FoodListing reservable() {
        long now = System.currentTimeMillis();
        for (FoodListing l : repo.getAll()) {
            if (l.isReservable(now) && l.getAvailableQuantity() >= 3) return l;
        }
        throw new AssertionError("seed lacks a reservable listing with qty >= 3");
    }

    @Test
    public void create_success_reducesStockOnce() {
        FoodListing l = reservable();
        int before = l.getAvailableQuantity();
        Reservation r = repo.create(l.getId(), USER, 2);
        assertNotNull(r.getId());
        assertEquals(ReservationStatus.RESERVED, r.getStatus());
        assertEquals(2, r.getQuantity());
        assertEquals(before - 2, repo.getById(l.getId()).getAvailableQuantity());
    }

    @Test
    public void create_insufficientStock_throwsAndStockUnchanged() {
        FoodListing l = reservable();
        int before = l.getAvailableQuantity();
        try {
            repo.create(l.getId(), USER, before + 1);
            fail("expected insufficient stock");
        } catch (IllegalStateException expected) {
        }
        assertEquals(before, repo.getById(l.getId()).getAvailableQuantity());
        assertTrue(repo.getForUser(USER).isEmpty());
    }

    @Test
    public void create_invalidQuantity_throws() {
        FoodListing l = reservable();
        try {
            repo.create(l.getId(), USER, 0);
            fail("expected invalid quantity");
        } catch (IllegalStateException expected) {
        }
        try {
            repo.create(l.getId(), USER, -2);
            fail("expected invalid quantity");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void create_expiredListing_throws() {
        FoodListing expired = null;
        for (FoodListing l : repo.getAll()) {
            if (l.isExpired(System.currentTimeMillis()) && l.getAvailableQuantity() > 0) {
                expired = l;
            }
        }
        assertNotNull("seed lacks an expired listing with stock", expired);
        try {
            repo.create(expired.getId(), USER, 1);
            fail("expected expired rejection");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void create_inactiveOrSoldOut_throws() {
        boolean coveredInactive = false;
        boolean coveredSoldOut = false;
        for (FoodListing l : repo.getAll()) {
            if (l.getStatus() != ListingStatus.ACTIVE) {
                try {
                    repo.create(l.getId(), USER, 1);
                    fail("expected inactive rejection for " + l.getId());
                } catch (IllegalStateException expected) {
                    coveredInactive = true;
                }
            }
            if (l.getAvailableQuantity() <= 0 && l.getStatus() == ListingStatus.ACTIVE
                    && !l.isExpired(System.currentTimeMillis())) {
                try {
                    repo.create(l.getId(), USER, 1);
                    fail("expected sold-out rejection for " + l.getId());
                } catch (IllegalStateException expected) {
                    coveredSoldOut = true;
                }
            }
        }
        assertTrue("seed lacks inactive listing", coveredInactive);
        assertTrue("seed lacks sold-out listing", coveredSoldOut);
    }

    @Test
    public void create_missingListing_throws() {
        try {
            repo.create("no-such-id", USER, 1);
            fail("expected missing listing");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void cancel_restoresStockExactlyOnce() {
        FoodListing l = reservable();
        int before = l.getAvailableQuantity();
        Reservation r = repo.create(l.getId(), USER, 2);
        repo.cancel(r.getId());
        assertEquals(ReservationStatus.CANCELLED,
                repo.getReservationById(r.getId()).getStatus());
        assertEquals(before, repo.getById(l.getId()).getAvailableQuantity());
    }

    @Test
    public void cancel_repeated_throwsAndDoesNotDoubleRestock() {
        FoodListing l = reservable();
        int before = l.getAvailableQuantity();
        Reservation r = repo.create(l.getId(), USER, 1);
        repo.cancel(r.getId());
        try {
            repo.cancel(r.getId());
            fail("expected repeated-cancel rejection");
        } catch (IllegalStateException expected) {
        }
        assertEquals(before, repo.getById(l.getId()).getAvailableQuantity());
    }

    @Test
    public void complete_doesNotRestoreStock() {
        FoodListing l = reservable();
        int before = l.getAvailableQuantity();
        Reservation r = repo.create(l.getId(), USER, 1);
        repo.complete(r.getId());
        assertEquals(ReservationStatus.COMPLETED,
                repo.getReservationById(r.getId()).getStatus());
        assertEquals(before - 1, repo.getById(l.getId()).getAvailableQuantity());
        try {
            repo.complete(r.getId());
            fail("expected repeated-complete rejection");
        } catch (IllegalStateException expected) {
        }
        try {
            repo.cancel(r.getId());
            fail("completed reservation must not be cancellable");
        } catch (IllegalStateException expected) {
        }
        assertEquals(before - 1, repo.getById(l.getId()).getAvailableQuantity());
    }

    @Test
    public void expiry_transitionsAndRestoresOnce() {
        FoodListing l = reservable();
        int before = l.getAvailableQuantity();
        Reservation r = repo.create(l.getId(), USER, 2);
        // Simulate the collection window passing.
        l.setCollectionDeadlineEpoch(System.currentTimeMillis() - 1000L);
        int transitioned = repo.markExpiredReservations(System.currentTimeMillis());
        assertEquals(1, transitioned);
        assertEquals(ReservationStatus.EXPIRED,
                repo.getReservationById(r.getId()).getStatus());
        assertEquals(before, repo.getById(l.getId()).getAvailableQuantity());
        // Second sweep must be a no-op (restore-once).
        assertEquals(0, repo.markExpiredReservations(System.currentTimeMillis()));
        assertEquals(before, repo.getById(l.getId()).getAvailableQuantity());
    }

    @Test
    public void history_reflectsUserReservations() {
        FoodListing l = reservable();
        assertTrue(repo.getForUser(USER).isEmpty());
        Reservation r1 = repo.create(l.getId(), USER, 1);
        Reservation r2 = repo.create(l.getId(), "other@example.com", 1);
        List<Reservation> mine = repo.getForUser(USER);
        assertEquals(1, mine.size());
        assertEquals(r1.getId(), mine.get(0).getId());
        assertEquals(2, repo.getForListing(l.getId()).size());
        assertEquals(2, repo.getAllReservations().size());
        assertNotNull(repo.getReservationById(r2.getId()));
    }
}
