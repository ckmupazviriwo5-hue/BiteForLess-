package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.data.remote.ExpressAuth;
import com.example.biteforlessfoodbridge.data.remote.ExpressConfig;
import com.example.biteforlessfoodbridge.data.remote.ExpressRepository;
import com.example.biteforlessfoodbridge.model.ActivityEvent;
import com.example.biteforlessfoodbridge.model.Category;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.ReviewStatus;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Pure mapping logic of the LOCAL Express + SQLite backend client
 * (docs/LOCAL_API_CONTRACT.md v1): slugs, DTO → model mapping, timestamps,
 * image URL resolution and role mapping. No network.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ExpressMappingTest {

    // ---- category slugs (chips/filters join on these, never DB UUIDs) ----

    @Test
    public void slug_lowercasesAndDashifies() {
        assertEquals("bakery", ExpressRepository.slug("Bakery"));
        assertEquals("fruits-vegetables", ExpressRepository.slug("Fruits & Vegetables"));
        assertEquals("meals", ExpressRepository.slug("  Meals "));
        assertEquals("", ExpressRepository.slug(null));
        assertEquals("", ExpressRepository.slug(" & "));
    }

    @Test
    public void parseCategory_idIsStableSlug() throws Exception {
        Category c = ExpressRepository.parseCategory(new JSONObject()
                .put("id", "cat_8e5d3c01-d33a")
                .put("name", "Bakery"));
        assertEquals("bakery", c.getId());
        assertEquals("Bakery", c.getName());
    }

    // ---- deal row (contract §5) → FoodListing ----

    @Test
    public void parseDeal_mapsContractFields() throws Exception {
        JSONObject d = new JSONObject()
                .put("id", "deal_abc")
                .put("shop_id", "shop_1")
                .put("food_name", "Butter Croissants")
                .put("description", "Flaky and fresh")
                .put("category", "Bakery")
                .put("original_price", 28)
                .put("discounted_price", 14)
                .put("quantity", 6)
                .put("quantity_remaining", 4)
                .put("status", "active")
                .put("expires_at", "2026-10-19T18:00:00.000Z")
                .put("pickup_window_start", "15:00")
                .put("pickup_window_end", "18:30")
                .put("area", "Kabwata")
                .put("city", "Lusaka")
                .put("image_url", "/uploads/demo/croissants.jpg");
        FoodListing l = ExpressRepository.parseDeal(d);
        assertEquals("deal_abc", l.getId());
        assertEquals("shop_1", l.getVendorId());
        assertEquals("Butter Croissants", l.getName());
        assertEquals("bakery", l.getCategoryId());          // slug, not UUID/name
        assertEquals(4, l.getAvailableQuantity());          // remaining stock
        assertEquals("Kabwata", l.getLocation());            // area beats city
        assertEquals("15:00–18:30", l.getCollectionWindow());
        assertEquals(ListingStatus.ACTIVE, l.getStatus());
        assertEquals(50, l.getDiscountPercent());
        assertTrue(l.getCollectionDeadlineEpoch() > 0);
        // image resolved against the configured API origin, never a CDN
        assertTrue(l.getImageUrl().endsWith("/uploads/demo/croissants.jpg"));
        assertTrue(l.getImageUrl().startsWith("http"));
    }

    @Test
    public void parseDeal_nonActiveStatusesBecomeInactive() throws Exception {
        JSONObject d = new JSONObject().put("status", "paused").put("food_name", "X");
        assertEquals(ListingStatus.INACTIVE, ExpressRepository.parseDeal(d).getStatus());
    }

    @Test
    public void parseTimeEx_handlesIsoAndSqliteFormats() {
        assertTrue(ExpressRepository.parseTimeEx("2026-10-19T12:00:00.000Z") > 0);
        assertTrue(ExpressRepository.parseTimeEx("2026-10-19T12:00:00+02:00") > 0);
        // SQLite stores "YYYY-MM-DD HH:MM:SS" (UTC) — must parse too.
        assertTrue(ExpressRepository.parseTimeEx("2026-10-19 12:00:00") > 0);
        assertEquals(0, ExpressRepository.parseTimeEx(""));
        assertEquals(0, ExpressRepository.parseTimeEx(null));
        assertEquals(0, ExpressRepository.parseTimeEx("not-a-date"));
    }

    // ---- reservation row (contract §6) → Reservation ----

    @Test
    public void parseReservation_mapsCodesPinsAndStatuses() throws Exception {
        Reservation r = ExpressRepository.parseReservation(new JSONObject()
                .put("id", "reservation_1")
                .put("deal_id", "deal_abc")
                .put("user_id", "user_1")
                .put("quantity", 2)
                .put("created_at", "2026-10-19T10:00:00Z")
                .put("status", "collected")
                .put("reservation_code", "BFL-1234ABCD")
                .put("pickup_pin", "4821")
                .put("customer_name", "Nchimunya"));
        assertEquals(ReservationStatus.COMPLETED, r.getStatus());
        assertEquals("deal_abc", r.getListingId());
        assertEquals("BFL-1234ABCD", r.getPickupCode());
        assertEquals("4821", r.getPickupPin());
        assertEquals("Nchimunya", r.getCustomerName());

        assertEquals(ReservationStatus.CANCELLED, ExpressRepository.parseReservation(
                new JSONObject().put("status", "cancelled")).getStatus());
        assertEquals(ReservationStatus.EXPIRED, ExpressRepository.parseReservation(
                new JSONObject().put("status", "expired")).getStatus());
        assertEquals(ReservationStatus.RESERVED, ExpressRepository.parseReservation(
                new JSONObject().put("status", "reserved")).getStatus());
    }

    // ---- review row (contract §8: moderation_status/comment) → Review ----

    @Test
    public void parseReview_moderationStatusAndComment() throws Exception {
        Review hidden = ExpressRepository.parseReview(new JSONObject()
                .put("id", "rev_1")
                .put("deal_id", "deal_abc")
                .put("rating", 9)                       // clamped to 5
                .put("comment", "Great food")
                .put("moderation_status", "hidden")
                .put("created_at", "2026-10-19T09:00:00Z"));
        assertEquals(ReviewStatus.HIDDEN, hidden.getStatus());
        assertEquals("Great food", hidden.getText());
        assertEquals(5, hidden.getRating());

        Review published = ExpressRepository.parseReview(
                new JSONObject().put("moderation_status", "published"));
        assertEquals(ReviewStatus.VISIBLE, published.getStatus());
    }

    // ---- activity row (contract §7) → ActivityEvent ----

    @Test
    public void parseEvent_usesActionAndActorName() throws Exception {
        ActivityEvent e = ExpressRepository.parseEvent(new JSONObject()
                .put("id", "log_1")
                .put("action", "reservation.create")
                .put("target_type", "reservation")
                .put("actor_name", "Admin")
                .put("created_at", "2026-10-19T08:00:00Z"));
        assertEquals("reservation.create", e.getType());
        assertEquals("reservation.create · reservation", e.getText());
        assertEquals("Admin", e.getActor());
        assertTrue(e.getAtEpoch() > 0);
    }

    // ---- image URL resolution (contract §5: API origin, never a CDN) ----

    @Test
    public void absoluteUrl_resolvesAgainstServerRoot() {
        assertTrue(ExpressConfig.absoluteUrl("/uploads/products/a.jpg")
                .endsWith("/uploads/products/a.jpg"));
        assertFalse(ExpressConfig.absoluteUrl("/uploads/x.jpg").contains("/api/"));
        assertEquals("https://cdn.example/img.png",
                ExpressConfig.absoluteUrl("https://cdn.example/img.png"));
        assertEquals("", ExpressConfig.absoluteUrl(null));
        assertEquals("", ExpressConfig.absoluteUrl("  "));
    }

    // ---- server-assigned roles → app roles (routing only; authz is server-side) ----

    @Test
    public void mapRole_serverVocabularyToAppRoles() {
        assertEquals("admin", ExpressAuth.mapRole("admin"));
        assertEquals("vendor", ExpressAuth.mapRole("shop_owner"));
        assertEquals("customer", ExpressAuth.mapRole("customer"));
        assertEquals("customer", ExpressAuth.mapRole(""));
    }
}
