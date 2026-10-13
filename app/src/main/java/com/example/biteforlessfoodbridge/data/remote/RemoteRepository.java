package com.example.biteforlessfoodbridge.data.remote;

import android.content.Context;

import com.example.biteforlessfoodbridge.model.ActivityEvent;
import com.example.biteforlessfoodbridge.model.Category;
import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.model.Reservation;
import com.example.biteforlessfoodbridge.model.ReservationStatus;
import com.example.biteforlessfoodbridge.model.Review;
import com.example.biteforlessfoodbridge.model.ReviewStatus;
import com.example.biteforlessfoodbridge.model.Vendor;
import com.example.biteforlessfoodbridge.model.VendorStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Live backend repository over PostgREST + secure RPC. All calls are async;
 * the hosted database is the source of truth (no silent demo fallback —
 * errors surface to the UI). Pure JSON mappers are unit-tested.
 */
public class RemoteRepository {

    private final SupabaseAuth auth;
    private final SupabaseHttp http = SupabaseHttp.get();

    public RemoteRepository(Context context) {
        this.auth = new SupabaseAuth(context);
    }

    public SupabaseAuth auth() {
        return auth;
    }

    private String token() {
        return auth.accessToken();
    }

    // ---------- timestamp helpers (pure, tested) ----------

    /** Normalises Supabase timestamptz to epoch millis (0 on unparseable). */
    public static long parseTime(String iso) {        if (iso == null || iso.isEmpty()) return 0;
        String s = iso.trim().replaceAll("\\.\\d+", ""); // strip fractional
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ssXX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss",
        };
        if (s.endsWith("Z")) s = s.substring(0, s.length() - 1) + "+00:00";
        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                f.setLenient(false);
                return f.parse(s).getTime();
            } catch (ParseException ignored) {
            }
        }
        return 0;
    }

    /** Formats epoch millis as UTC ISO-8601 for PostgREST writes (pure, tested). */
    public static String isoFor(long epochMillis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new java.util.Date(epochMillis));
    }

    // ---------- mappers (pure, tested) ----------

    public static Category parseCategory(JSONObject o) {
        return new Category(o.optString("id", ""),
                o.optString("name", ""));
    }

    public static VendorStatus parseVendorStatus(String s) {
        if ("approved".equalsIgnoreCase(s)) return VendorStatus.APPROVED;
        if ("suspended".equalsIgnoreCase(s)) return VendorStatus.SUSPENDED;
        return VendorStatus.PENDING;
    }

    public static String vendorStatusToApi(VendorStatus s) {
        if (s == VendorStatus.APPROVED) return "approved";
        if (s == VendorStatus.SUSPENDED) return "suspended";
        return "pending";
    }

    public static Vendor parseVendor(JSONObject o) {
        return new Vendor(o.optString("id", ""),
                o.optString("name", ""),
                o.optString("location", ""),
                parseVendorStatus(o.optString("status", "pending")));
    }

    public static FoodListing parseListing(JSONObject o) {
        String status = o.optString("status", "active");
        return new FoodListing(
                o.optString("id", ""),
                o.optString("vendor_id", ""),
                o.optString("name", ""),
                o.optString("description", ""),
                o.optString("category_id", ""),
                o.optDouble("original_price", 0),
                o.optDouble("discounted_price", 0),
                o.optInt("stock", 0),
                o.optString("location", ""),
                parseTime(o.optString("deadline", "")),
                o.optString("collection_window", ""),
                "active".equalsIgnoreCase(status) ? ListingStatus.ACTIVE : ListingStatus.INACTIVE);
    }

    public static ReservationStatus parseReservationStatus(String s) {
        if ("completed".equalsIgnoreCase(s)) return ReservationStatus.COMPLETED;
        if ("cancelled".equalsIgnoreCase(s)) return ReservationStatus.CANCELLED;
        if ("expired".equalsIgnoreCase(s)) return ReservationStatus.EXPIRED;
        return ReservationStatus.RESERVED;
    }

    public static Reservation parseReservation(JSONObject o) {
        return new Reservation(
                o.optString("id", ""),
                o.optString("listing_id", ""),
                o.optString("customer_id", ""),
                Math.max(1, o.optInt("quantity", 1)),
                parseTime(o.optString("created_at", "")),
                parseReservationStatus(o.optString("status", "reserved")),
                o.optString("pickup_code", ""));
    }

    public static Review parseReview(JSONObject o) {
        String status = o.optString("status", "pending");
        ReviewStatus rs = "visible".equalsIgnoreCase(status) ? ReviewStatus.VISIBLE
                : "hidden".equalsIgnoreCase(status) ? ReviewStatus.HIDDEN
                : ReviewStatus.PENDING;
        String label = o.optString("reviewer_label", "");
        if (label.isEmpty() && !o.isNull("reviewer_id")) {
            label = o.optString("reviewer_id", "");
        }
        return new Review(o.optString("id", ""),
                o.optString("listing_id", ""),
                o.isNull("reviewer_id") ? "" : o.optString("reviewer_id", ""),
                Math.max(1, Math.min(5, o.optInt("rating", 3))),
                o.optString("body", ""),
                parseTime(o.optString("created_at", "")),
                rs);
    }

    public static ActivityEvent parseEvent(JSONObject o) {
        return new ActivityEvent(o.optString("id", ""),
                o.optString("type", "admin"),
                o.optString("text", ""),
                parseTime(o.optString("created_at", "")),
                o.optString("actor_label", ""));
    }

    // ---------- reads ----------

    public void getCategories(SupaResult<List<Category>> cb) {
        http.get("/rest/v1/categories", "select=*&is_active=eq.true&order=name", token(),
                mapArray(cb, RemoteRepository::parseCategory));
    }

    public void getVendors(String statusEqualsOrNull, SupaResult<List<Vendor>> cb) {
        String q = "select=*&order=name";
        if (statusEqualsOrNull != null) q += "&status=eq." + statusEqualsOrNull;
        http.get("/rest/v1/vendors", q, token(), mapArray(cb, RemoteRepository::parseVendor));
    }

    /** Storefront: active + in-stock rows with vendor state; UI applies final eligibility. */
    public void getStorefrontListings(SupaResult<List<FoodListing>> cb) {
        http.get("/rest/v1/listings",
                "select=*,vendors(status)&status=eq.active&stock=gt.0&order=created_at.desc",
                token(), mapArray(cb, RemoteRepository::parseListing));
    }

    public void getAllListings(SupaResult<List<FoodListing>> cb) {
        http.get("/rest/v1/listings", "select=*&order=created_at.desc", token(),
                mapArray(cb, RemoteRepository::parseListing));
    }

    public void getListing(String id, SupaResult<FoodListing> cb) {
        http.getObject("/rest/v1/listings", "id=eq." + encode(id) + "&select=*", token(),
                mapObject(cb, RemoteRepository::parseListing));
    }

    public void getVendor(String id, SupaResult<Vendor> cb) {
        http.getObject("/rest/v1/vendors", "id=eq." + encode(id) + "&select=*", token(),
                mapObject(cb, RemoteRepository::parseVendor));
    }

    /** Vendor business owned by the signed-in user (null-owner rows excluded). */
    public void getMyVendor(SupaResult<Vendor> cb) {
        http.get("/rest/v1/vendors", "select=*", token(),
                new SupaResult<JSONArray>() {
                    @Override public void onSuccess(JSONArray arr) {
                        try {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                if (auth.userId().equals(o.optString("owner_id", ""))) {
                                    cb.onSuccess(parseVendor(o));
                                    return;
                                }
                            }
                            cb.onError("No vendor business linked to this account");
                        } catch (JSONException e) {
                            cb.onError("Bad server response");
                        }
                    }
                    @Override public void onError(String m) { cb.onError(m); }
                });
    }

    public void getCategoryName(String id, SupaResult<String> cb) {
        getCategories(new SupaResult<List<Category>>() {
            @Override public void onSuccess(List<Category> cats) {
                for (Category c : cats) {
                    if (c.getId().equals(id)) {
                        cb.onSuccess(c.getName());
                        return;
                    }
                }
                cb.onSuccess(id);
            }
            @Override public void onError(String m) { cb.onError(m); }
        });
    }

    public void getMyReservations(SupaResult<List<Reservation>> cb) {
        http.get("/rest/v1/reservations",
                "select=*&customer_id=eq." + encode(auth.userId()) + "&order=created_at.desc",
                token(), mapArray(cb, RemoteRepository::parseReservation));
    }

    public void getAllReservations(SupaResult<List<Reservation>> cb) {
        http.get("/rest/v1/reservations", "select=*&order=created_at.desc", token(),
                mapArray(cb, RemoteRepository::parseReservation));
    }

    public void getVendorReservations(String vendorId, SupaResult<List<Reservation>> cb) {
        http.get("/rest/v1/reservations",
                "select=*&vendor_id=eq." + encode(vendorId) + "&order=created_at.desc",
                token(), mapArray(cb, RemoteRepository::parseReservation));
    }

    public void getVisibleReviews(String listingId, SupaResult<List<Review>> cb) {
        http.get("/rest/v1/reviews",
                "select=*&listing_id=eq." + encode(listingId) + "&status=eq.visible"
                        + "&order=created_at.desc",
                token(), mapArray(cb, RemoteRepository::parseReview));
    }

    public void getAllReviews(SupaResult<List<Review>> cb) {
        http.get("/rest/v1/reviews", "select=*&order=created_at.desc", token(),
                mapArray(cb, RemoteRepository::parseReview));
    }

    public void getActivityLog(SupaResult<List<ActivityEvent>> cb) {
        http.get("/rest/v1/activity_events",
                "select=*&order=created_at.desc&limit=100", token(),
                mapArray(cb, RemoteRepository::parseEvent));
    }

    // ---------- secure writes (RPC) ----------

    public void rpcCreateReservation(String listingId, int qty,
                                     SupaResult<Reservation> cb) {
        try {
            JSONObject body = new JSONObject()
                    .put("p_listing_id", listingId)
                    .put("p_quantity", qty);
            http.post("/rest/v1/rpc/create_reservation", body, token(), true,
                    new SupaResult<JSONArray>() {
                        @Override public void onSuccess(JSONArray arr) {
                            try {
                                JSONObject o = arr.getJSONObject(0);
                                Reservation r = new Reservation(
                                        o.optString("id", ""),
                                        listingId, auth.userId(), qty,
                                        System.currentTimeMillis(),
                                        ReservationStatus.RESERVED,
                                        o.optString("pickup_code", ""));
                                cb.onSuccess(r);
                            } catch (Exception e) {
                                cb.onError("Bad server response");
                            }
                        }
                        @Override public void onError(String m) { cb.onError(m); }
                    });
        } catch (JSONException e) {
            cb.onError("Could not place reservation");
        }
    }

    public void rpcCancel(String reservationId, SupaResult<String> cb) {
        rpcSimple("cancel_reservation", reservationId, cb);
    }

    public void rpcComplete(String reservationId, SupaResult<String> cb) {
        rpcSimple("complete_pickup", reservationId, cb);
    }

    public void rpcSetVendorStatus(String vendorId, VendorStatus status,
                                   SupaResult<String> cb) {
        try {
            JSONObject body = new JSONObject()
                    .put("p_vendor_id", vendorId)
                    .put("p_status", vendorStatusToApi(status));
            http.post("/rest/v1/rpc/set_vendor_status", body, token(), true,
                    stringResult(cb));
        } catch (JSONException e) {
            cb.onError("Could not update vendor");
        }
    }

    public void rpcSetListingStatus(String listingId, ListingStatus status,
                                    SupaResult<String> cb) {
        try {
            JSONObject body = new JSONObject()
                    .put("p_listing_id", listingId)
                    .put("p_status", status == ListingStatus.ACTIVE ? "active" : "inactive");
            http.post("/rest/v1/rpc/set_listing_status", body, token(), true,
                    stringResult(cb));
        } catch (JSONException e) {
            cb.onError("Could not update listing");
        }
    }

    public void rpcModerateReview(String reviewId, ReviewStatus status,
                                  SupaResult<String> cb) {
        String s = status == ReviewStatus.VISIBLE ? "visible"
                : status == ReviewStatus.HIDDEN ? "hidden" : "pending";
        try {
            JSONObject body = new JSONObject()
                    .put("p_review_id", reviewId)
                    .put("p_status", s);
            http.post("/rest/v1/rpc/moderate_review", body, token(), true,
                    stringResult(cb));
        } catch (JSONException e) {
            cb.onError("Could not moderate review");
        }
    }

    private void rpcSimple(String fn, String id, SupaResult<String> cb) {
        try {
            String key = fn.startsWith("cancel") || fn.startsWith("complete")
                    ? "p_reservation_id" : "p_id";
            JSONObject body = new JSONObject().put(key, id);
            http.post("/rest/v1/rpc/" + fn, body, token(), true, stringResult(cb));
        } catch (JSONException e) {
            cb.onError("Request failed");
        }
    }

    // ---------- vendor listing CRUD (owner RLS) ----------

    public void createListing(JSONObject fields, SupaResult<FoodListing> cb) {
        http.post("/rest/v1/listings", fields, token(), true,
                firstObject(cb, RemoteRepository::parseListing, "Could not create listing"));
    }

    public void updateListing(String id, JSONObject fields, SupaResult<FoodListing> cb) {
        http.patch("/rest/v1/listings", "id=eq." + encode(id), fields, token(),
                firstObject(cb, RemoteRepository::parseListing, "Could not update listing"));
    }

    public void createReview(String listingId, int rating, String body,
                             SupaResult<Review> cb) {
        try {
            JSONObject fields = new JSONObject()
                    .put("id", "rev-" + System.currentTimeMillis())
                    .put("listing_id", listingId)
                    .put("rating", rating)
                    .put("body", body == null ? "" : body)
                    .put("status", "pending");
            http.post("/rest/v1/reviews", fields, token(), true,
                    firstObject(cb, RemoteRepository::parseReview, "Could not submit review"));
        } catch (JSONException e) {
            cb.onError("Could not submit review");
        }
    }

    public void onboardVendor(String name, String location, SupaResult<Vendor> cb) {
        try {
            JSONObject fields = new JSONObject()
                    .put("id", "v-" + System.currentTimeMillis())
                    .put("name", name)
                    .put("location", location == null ? "" : location)
                    .put("status", "pending");
            http.post("/rest/v1/vendors", fields, token(), true,
                    firstObject(cb, RemoteRepository::parseVendor, "Could not register vendor"));
        } catch (JSONException e) {
            cb.onError("Could not register vendor");
        }
    }

    // ---------- callback adapters ----------

    private interface Mapper<T> {
        T map(JSONObject o) throws JSONException;
    }

    private <T> SupaResult<JSONArray> mapArray(SupaResult<List<T>> cb, Mapper<T> m) {
        return new SupaResult<JSONArray>() {
            @Override public void onSuccess(JSONArray arr) {
                try {
                    List<T> out = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) out.add(m.map(arr.getJSONObject(i)));
                    cb.onSuccess(out);
                } catch (JSONException e) {
                    cb.onError("Bad server response");
                }
            }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    private <T> SupaResult<JSONObject> mapObject(SupaResult<T> cb, Mapper<T> m) {
        return new SupaResult<JSONObject>() {
            @Override public void onSuccess(JSONObject o) {
                try {
                    cb.onSuccess(m.map(o));
                } catch (JSONException e) {
                    cb.onError("Bad server response");
                }
            }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    private <T> SupaResult<JSONArray> firstObject(SupaResult<T> cb, Mapper<T> m,
                                                 String emptyMessage) {
        return new SupaResult<JSONArray>() {
            @Override public void onSuccess(JSONArray arr) {
                try {
                    if (arr.length() == 0) {
                        cb.onError(emptyMessage);
                        return;
                    }
                    cb.onSuccess(m.map(arr.getJSONObject(0)));
                } catch (JSONException e) {
                    cb.onError("Bad server response");
                }
            }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    private SupaResult<JSONArray> stringResult(SupaResult<String> cb) {
        return new SupaResult<JSONArray>() {
            @Override public void onSuccess(JSONArray arr) { cb.onSuccess("ok"); }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    private static String encode(String s) {
        return s == null ? "" : s.replace(" ", "%20").replace("'", "%27");
    }
}
