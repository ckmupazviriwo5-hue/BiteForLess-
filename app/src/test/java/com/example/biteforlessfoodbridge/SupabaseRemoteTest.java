package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupabaseAuth;
import com.example.biteforlessfoodbridge.data.remote.SupabaseHttp;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.ReviewStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.model.VendorStatus;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Pure Supabase client logic: PKCE, URLs, mappers, error mapping (no network). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SupabaseRemoteTest {

    @Test
    public void pkce_verifierAndChallenge_shape() {
        String verifier = SupabaseAuth.newVerifier();
        assertTrue(verifier.length() >= 43);
        assertFalse(verifier.contains("+"));
        assertFalse(verifier.contains("/"));
        String c1 = SupabaseAuth.challengeFor(verifier);
        String c2 = SupabaseAuth.challengeFor(verifier);
        assertEquals(c1, c2); // deterministic
        assertFalse(c1.equals(verifier));
        assertFalse(c1.contains("+"));
    }

    @Test
    public void providerUrl_containsPkceAndRedirect() {
        String url = SupabaseAuth.providerUrl("google", "test-verifier-12345678901234567890");
        assertTrue(url.contains("/auth/v1/authorize"));
        assertTrue(url.contains("provider=google"));
        assertTrue(url.contains("code_challenge="));
        assertTrue(url.contains("code_challenge_method=s256"));
        assertTrue(url.contains("redirect_to="));
    }

    @Test
    public void queryParam_parsesCallback() {
        String url = "biteforless://auth-callback?code=abc123&extra=1";
        assertEquals("abc123", SupabaseAuth.queryParam(url, "code"));
        assertEquals("", SupabaseAuth.queryParam(url, "missing"));
        assertEquals("", SupabaseAuth.queryParam("biteforless://auth-callback", "code"));
        assertEquals("access_denied",
                SupabaseAuth.queryParam("biteforless://auth-callback?error=access_denied", "error"));
        assertEquals("a b", SupabaseAuth.queryParam("x?e=a%20b", "e").replace("%20", " "));
    }

    @Test
    public void parseTime_isoVariants() {
        assertTrue(RemoteRepository.parseTime("2026-10-13T12:00:00+00:00") > 0);
        assertTrue(RemoteRepository.parseTime("2026-10-13T12:00:00.123Z") > 0);
        assertTrue(RemoteRepository.parseTime("2026-10-13T12:00:00Z") > 0);
        assertEquals(0, RemoteRepository.parseTime(""));
        assertEquals(0, RemoteRepository.parseTime(null));
        assertEquals(0, RemoteRepository.parseTime("not-a-date"));
    }

    @Test
    public void parseListing_mapsAllFields() throws Exception {
        JSONObject o = new JSONObject()
                .put("id", "l1").put("vendor_id", "v1").put("name", "Bread")
                .put("description", "d").put("category_id", "bakery")
                .put("original_price", 60.0).put("discounted_price", 29.0)
                .put("stock", 8).put("location", "Woodstock")
                .put("deadline", "2026-10-14T12:00:00+00:00")
                .put("collection_window", "16:00-18:00").put("status", "active");
        FoodListing l = RemoteRepository.parseListing(o);
        assertEquals("l1", l.getId());
        assertEquals(8, l.getAvailableQuantity());
        assertEquals(ListingStatus.ACTIVE, l.getStatus());
        assertEquals(52, l.getDiscountPercent());
        assertTrue(l.getCollectionDeadlineEpoch() > 0);
        o.put("status", "inactive");
        assertEquals(ListingStatus.INACTIVE, RemoteRepository.parseListing(o).getStatus());
    }

    @Test
    public void parseReservation_mapsStatus() throws Exception {
        JSONObject o = new JSONObject()
                .put("id", "RES-1").put("listing_id", "l1").put("customer_id", "u1")
                .put("quantity", 2).put("created_at", "2026-10-13T12:00:00Z")
                .put("status", "completed").put("pickup_code", "DEMO-1");
        Reservation r = RemoteRepository.parseReservation(o);
        assertEquals(ReservationStatus.COMPLETED, r.getStatus());
        assertEquals(2, r.getQuantity());
        assertEquals(VendorStatus.APPROVED,
                RemoteRepository.parseVendorStatus("approved"));
        assertEquals(VendorStatus.SUSPENDED,
                RemoteRepository.parseVendorStatus("suspended"));
        assertEquals(VendorStatus.PENDING,
                RemoteRepository.parseVendorStatus("pending"));
    }

    @Test
    public void parseReview_clampsRatingAndStatus() throws Exception {
        JSONObject o = new JSONObject()
                .put("id", "r1").put("listing_id", "l1").put("rating", 9)
                .put("body", "x").put("status", "hidden");
        Review r = RemoteRepository.parseReview(o);
        assertEquals(5, r.getRating());
        assertEquals(ReviewStatus.HIDDEN, r.getStatus());
    }

    @Test
    public void parseVendor_roundTripsStatus() {
        assertEquals("approved",
                RemoteRepository.vendorStatusToApi(VendorStatus.APPROVED));
        Vendor v = RemoteRepository.parseVendor(
                safeObj("{\"id\":\"v9\",\"name\":\"N\",\"location\":\"L\",\"status\":\"suspended\"}"));
        assertEquals(VendorStatus.SUSPENDED, v.getStatus());
    }

    @Test
    public void errorMapping_friendlyMessages() {
        assertEquals("Wrong email or password",
                SupabaseHttp.authError("{\"msg\":\"Invalid login credentials\"}", 400));
        assertEquals("Session expired — please sign in again",
                SupabaseHttp.apiError("{}", 401));
        assertEquals("Session expired — please sign in again",
                SupabaseHttp.apiError("{}", 403));
        assertTrue(SupabaseHttp.apiError("{}", 500).contains("Server error"));
        assertEquals("This email is already registered — try signing in",
                SupabaseHttp.authError("{\"msg\":\"User already registered\"}", 422));
    }

    private static JSONObject safeObj(String json) {
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
