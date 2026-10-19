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

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Live repository against the LOCAL Express + SQLite API (the frozen contract
 * in the website project's docs/LOCAL_API_CONTRACT.md v1). Extends
 * {@link RemoteRepository} so every existing screen keeps compiling; every
 * public method is overridden to hit the local API and map its JSON onto the
 * same app models. The hosted Supabase path underneath is never invoked.
 *
 * Field mapping (Express → app model):
 *   deals.food_name→name, shop_id→vendorId, category→categoryId (display name),
 *   quantity_remaining→availableQuantity, expires_at→collectionDeadline,
 *   area/city→location, image_url (absolutized)→imageUrl.
 *   reservations.deal_id→listingId, user_id→userId, reservation_code→pickupCode,
 *   pickup_pin→pickupPin, collected→COMPLETED.
 *   reviews.deal_id→listingId, comment→text, published→VISIBLE, hidden→HIDDEN.
 *   shops (vendors): area/city→location.
 */
public class ExpressRepository extends RemoteRepository {

    private final ExpressAuth auth;
    private final ExpressHttp http = ExpressHttp.get();

    public ExpressRepository(Context context) {
        super(context);
        this.auth = new ExpressAuth(context);
    }

    @Override public SupabaseAuth auth() {
        return auth;
    }

    private String token() {
        return auth.accessToken();
    }

    // ---------- timestamp helper (pure) ----------

    /** Parses Express ISO timestamps AND SQLite "YYYY-MM-DD HH:MM:SS" (UTC). */
    public static long parseTimeEx(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        String s = iso.trim().replaceAll("\\.\\d+", ""); // strip fractional seconds
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ssXX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
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

    // ---------- mappers (pure) ----------

    /**
     * Chips and filters match on a STABLE slug ("Bakery" → bakery) instead of
     * the database's UUID ids, so listings join to categories reliably.
     */
    public static Category parseCategory(JSONObject o) {
        String name = o.optString("name", "");
        return new Category(slug(name.isEmpty() ? o.optString("id", "") : name), name);
    }

    /** Lowercase display name → stable url-safe id (pure). */
    public static String slug(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    /**
     * Nullable column guard: SQLite NULLs arrive as JSONObject.NULL, and
     * optString() would coerce those to the literal text "null" — treat them
     * as empty strings instead.
     */
    public static String str(JSONObject o, String key) {
        return o.isNull(key) ? "" : o.optString(key, "");
    }

    public static Vendor parseShop(JSONObject o) {
        String area = str(o, "area");
        String city = str(o, "city");
        String location = !area.isEmpty() ? area : city;
        return new Vendor(str(o, "id"),
                str(o, "name"),
                location,
                parseVendorStatus(o.optString("status", "pending")));
    }

    public static FoodListing parseDeal(JSONObject o) {
        String status = o.optString("status", "active");
        String area = str(o, "area");
        String city = str(o, "city");
        FoodListing l = new FoodListing(
                str(o, "id"),
                str(o, "shop_id"),
                str(o, "food_name"),
                str(o, "description"),
                slug(str(o, "category")),
                o.optDouble("original_price", 0),
                o.optDouble("discounted_price", 0),
                o.optInt("quantity_remaining", 0),
                !area.isEmpty() ? area : city,
                parseTimeEx(str(o, "expires_at")),
                windowOf(o),
                "active".equalsIgnoreCase(status) ? ListingStatus.ACTIVE : ListingStatus.INACTIVE);
        l.setImageUrl(ExpressConfig.absoluteUrl(str(o, "image_url")));
        return l;
    }

    private static String windowOf(JSONObject o) {
        String start = str(o, "pickup_window_start");
        String end = str(o, "pickup_window_end");
        if (start.isEmpty() && end.isEmpty()) return "";
        if (end.isEmpty()) return start;
        if (start.isEmpty()) return end;
        return start + "–" + end;
    }

    public static Reservation parseReservation(JSONObject o) {
        Reservation r = new Reservation(
                str(o, "id"),
                str(o, "deal_id"),
                str(o, "user_id"),
                Math.max(1, o.optInt("quantity", 1)),
                parseTimeEx(str(o, "created_at")),
                parseResStatus(o.optString("status", "reserved")),
                str(o, "reservation_code"));
        r.setPickupPin(str(o, "pickup_pin"));
        r.setCustomerName(str(o, "customer_name"));
        r.setFoodName(str(o, "food_name"));
        r.setShopName(str(o, "shop_name"));
        return r;
    }

    public static Review parseReview(JSONObject o) {
        String status = str(o, "moderation_status");
        if (status.isEmpty()) status = "published";
        ReviewStatus rs = "hidden".equalsIgnoreCase(status) ? ReviewStatus.HIDDEN
                : ReviewStatus.VISIBLE;
        Review review = new Review(str(o, "id"),
                str(o, "deal_id"),
                o.isNull("user_id") ? "" : str(o, "user_id"),
                Math.max(1, Math.min(5, o.optInt("rating", 3))),
                str(o, "comment"),
                parseTimeEx(str(o, "created_at")),
                rs);
        review.setUserName(str(o, "user_name"));
        return review;
    }

    public static ActivityEvent parseEvent(JSONObject o) {
        String action = str(o, "action");
        String targetType = str(o, "target_type");
        String text = targetType.isEmpty() ? action : action + " · " + targetType;
        String actor = str(o, "actor_name");
        if (actor.isEmpty()) actor = str(o, "actor_email");
        if (actor.isEmpty()) actor = o.optString("actor_role", "system");
        return new ActivityEvent(str(o, "id"),
                action.isEmpty() ? "admin" : action,
                text,
                parseTimeEx(str(o, "created_at")),
                actor);
    }

    private static ReservationStatus parseResStatus(String s) {
        if ("collected".equalsIgnoreCase(s)) return ReservationStatus.COMPLETED;
        if ("cancelled".equalsIgnoreCase(s)) return ReservationStatus.CANCELLED;
        if ("expired".equalsIgnoreCase(s)) return ReservationStatus.EXPIRED;
        return ReservationStatus.RESERVED;
    }

    // ---------- reads ----------

    @Override
    public void getCategories(SupaResult<List<Category>> cb) {
        http.get("/api/categories", "", token(), mapList(cb, "categories", ExpressRepository::parseCategory));
    }

    @Override
    public void getVendors(String statusEqualsOrNull, SupaResult<List<Vendor>> cb) {
        boolean admin = "admin".equalsIgnoreCase(auth.role());
        String path = admin ? "/api/admin/vendors" : "/api/shops";
        String query = statusEqualsOrNull == null ? "" : "status=" + enc(statusEqualsOrNull);
        http.get(path, query, token(), mapList(cb, admin ? "vendors" : "shops", ExpressRepository::parseShop));
    }

    /** Storefront: the local catalog is already active+approved+future; stock filter added. */
    @Override
    public void getStorefrontListings(SupaResult<List<FoodListing>> cb) {
        http.get("/api/deals", "limit=60&available=1&sort=newest", token(),
                mapList(cb, "deals", ExpressRepository::parseDeal));
    }

    @Override
    public void getAllListings(SupaResult<List<FoodListing>> cb) {
        String role = auth.role();
        if ("admin".equalsIgnoreCase(role)) {
            http.get("/api/admin/deals", "", token(), mapList(cb, "deals", ExpressRepository::parseDeal));
        } else if ("shop_owner".equalsIgnoreCase(role) || "vendor".equalsIgnoreCase(role)) {
            http.get("/api/deals/mine", "", token(), mapList(cb, "deals", ExpressRepository::parseDeal));
        } else {
            http.get("/api/deals", "limit=60", token(), mapList(cb, "deals", ExpressRepository::parseDeal));
        }
    }

    @Override
    public void getListing(String id, SupaResult<FoodListing> cb) {
        http.get("/api/deals/" + enc(id), "", token(),
                mapObject(cb, "deal", ExpressRepository::parseDeal));
    }

    @Override
    public void getVendor(String id, SupaResult<Vendor> cb) {
        // Direct fetch: GET /api/shops/:id resolves shops of ANY status.
        http.get("/api/shops/" + enc(id), "", token(),
                mapObject(cb, "shop", ExpressRepository::parseShop));
    }

    @Override
    public void getMyVendor(SupaResult<Vendor> cb) {
        http.get("/api/shops/me", "", token(), mapObject(cb, "shop", ExpressRepository::parseShop));
    }

    @Override
    public void getCategoryName(String id, SupaResult<String> cb) {
        // Resolve a slug (or raw id / display name) to the category display name.
        getCategories(new SupaResult<List<Category>>() {
            @Override public void onSuccess(List<Category> cats) {
                for (Category c : cats) {
                    if (c.getId().equals(id) || c.getName().equalsIgnoreCase(id)
                            || slug(c.getName()).equals(id)) {
                        cb.onSuccess(c.getName());
                        return;
                    }
                }
                cb.onSuccess(id == null ? "" : id);
            }
            @Override public void onError(String m) { cb.onSuccess(id == null ? "" : id); }
        });
    }

    @Override
    public void getMyReservations(SupaResult<List<Reservation>> cb) {
        http.get("/api/reservations", "limit=200", token(),
                mapList(cb, "reservations", ExpressRepository::parseReservation));
    }

    @Override
    public void getAllReservations(SupaResult<List<Reservation>> cb) {
        if ("admin".equalsIgnoreCase(auth.role())) {
            http.get("/api/admin/reservations", "limit=200", token(),
                    mapList(cb, "reservations", ExpressRepository::parseReservation));
        } else {
            getMyReservations(cb);
        }
    }

    @Override
    public void getVendorReservations(String vendorId, SupaResult<List<Reservation>> cb) {
        // Auth-scoped on the server; vendorId is implied by the session.
        http.get("/api/shops/me/reservations", "limit=200", token(),
                mapList(cb, "reservations", ExpressRepository::parseReservation));
    }

    @Override
    public void getVisibleReviews(String listingId, SupaResult<List<Review>> cb) {
        http.get("/api/reviews", "deal_id=" + enc(listingId) + "&limit=50", token(),
                mapList(cb, "reviews", ExpressRepository::parseReview));
    }

    @Override
    public void getAllReviews(SupaResult<List<Review>> cb) {
        // ?all=1 includes hidden rows for moderation when the token is an admin.
        http.get("/api/reviews", "all=1&limit=100", token(),
                mapList(cb, "reviews", ExpressRepository::parseReview));
    }

    @Override
    public void getActivityLog(SupaResult<List<ActivityEvent>> cb) {
        http.get("/api/admin/activity", "limit=100", token(),
                mapList(cb, "activity", ExpressRepository::parseEvent));
    }

    // ---------- writes ----------

    @Override
    public void rpcCreateReservation(String listingId, int qty, SupaResult<Reservation> cb) {
        try {
            JSONObject body = new JSONObject()
                    .put("deal_id", listingId)
                    .put("quantity", Math.max(1, qty));
            http.send("POST", "/api/reservations", body, token(),
                    mapObject(cb, "reservation", ExpressRepository::parseReservation));
        } catch (JSONException e) {
            cb.onError("Could not place reservation");
        }
    }

    @Override
    public void rpcCancel(String reservationId, SupaResult<String> cb) {
        http.send("POST", "/api/reservations/" + enc(reservationId) + "/cancel", null, token(),
                ok(cb));
    }

    /** Pickup verification with the customer's PIN (contract §6). */
    @Override
    public void verifyPickup(String reservationId, String pickupPin, SupaResult<String> cb) {
        try {
            JSONObject body = new JSONObject().put("pickup_pin",
                    pickupPin == null ? "" : pickupPin.trim());
            http.send("POST", "/api/reservations/" + enc(reservationId) + "/verify", body, token(),
                    ok(cb));
        } catch (JSONException e) {
            cb.onError("Could not verify pickup");
        }
    }

    /** Local API verifies with a PIN — a PIN-less complete is not possible. */
    @Override
    public void rpcComplete(String reservationId, SupaResult<String> cb) {
        cb.onError("Enter the customer's 4-digit pickup PIN to verify");
    }

    @Override
    public void rpcSetVendorStatus(String vendorId, VendorStatus status, SupaResult<String> cb) {
        try {
            JSONObject body = new JSONObject().put("status", vendorStatusToApi(status));
            http.send("PATCH", "/api/admin/vendors/" + enc(vendorId) + "/status", body, token(),
                    ok(cb));
        } catch (JSONException e) {
            cb.onError("Could not update vendor");
        }
    }

    @Override
    public void rpcSetListingStatus(String listingId, ListingStatus status,
                                    SupaResult<String> cb) {
        try {
            // Owner-or-admin endpoint; "paused" is the local API's pause state.
            JSONObject body = new JSONObject()
                    .put("status", status == ListingStatus.ACTIVE ? "active" : "paused");
            http.send("PATCH", "/api/deals/" + enc(listingId) + "/status", body, token(),
                    ok(cb));
        } catch (JSONException e) {
            cb.onError("Could not update listing");
        }
    }

    @Override
    public void rpcModerateReview(String reviewId, ReviewStatus status,
                                  SupaResult<String> cb) {
        String s = status == ReviewStatus.HIDDEN ? "hidden" : "published";
        try {
            JSONObject body = new JSONObject().put("status", s);
            http.send("PATCH", "/api/reviews/" + enc(reviewId) + "/status", body, token(),
                    ok(cb));
        } catch (JSONException e) {
            cb.onError("Could not moderate review");
        }
    }

    // ---------- vendor listing CRUD (multipart, contract §5) ----------

    @Override
    public void createListing(JSONObject fields, SupaResult<FoodListing> cb) {
        String wantedStatus = fields == null ? "active" : fields.optString("status", "active");
        http.sendMultipart("POST", "/api/deals", toApiFields(fields), null, token(),
                new SupaResult<JSONObject>() {
                    @Override public void onSuccess(JSONObject env) {
                        JSONObject deal = env.optJSONObject("deal");
                        if (deal == null) {
                            cb.onError("Bad server response");
                            return;
                        }
                        FoodListing created = parseDeal(deal);
                        if ("active".equalsIgnoreCase(wantedStatus)) {
                            cb.onSuccess(created);
                            return;
                        }
                        // The form asked for inactive — pause right after create.
                        rpcSetListingStatus(created.getId(), ListingStatus.INACTIVE,
                                new SupaResult<String>() {
                                    @Override public void onSuccess(String s) {
                                        created.setStatus(ListingStatus.INACTIVE);
                                        cb.onSuccess(created);
                                    }
                                    @Override public void onError(String m) {
                                        cb.onSuccess(created); // created; pause failed — editable later
                                    }
                                });
                    }
                    @Override public void onError(String m) { cb.onError(m); }
                });
    }

    /**
     * Text fields only — the server's PUT /api/deals/:id is JSON (no multipart
     * parser), so an edit MUST go as JSON or every field would be silently
     * dropped. Photo replacement is POST /api/deals/:id/images.
     */
    @Override
    public void updateListing(String id, JSONObject fields, SupaResult<FoodListing> cb) {
        http.send("PUT", "/api/deals/" + enc(id), toApiFields(fields), token(),
                mapObject(cb, "deal", ExpressRepository::parseDeal));
    }

    /** Replaces the listing photo (multipart, contract §5) and returns the deal. */
    public void saveListingImage(String dealIdOrNull, JSONObject fields, File image,
                                 SupaResult<FoodListing> cb) {
        if (dealIdOrNull == null || image == null) {
            createListing(fields, cb); // create path keeps its single multipart call
            return;
        }
        http.sendMultipart("POST", "/api/deals/" + enc(dealIdOrNull) + "/images",
                null, image, token(),
                new SupaResult<JSONObject>() {
                    @Override public void onSuccess(JSONObject env) {
                        getListing(dealIdOrNull, cb);
                    }
                    @Override public void onError(String m) { cb.onError(m); }
                });
    }

    /** Translates the form's Supabase-style column keys to the local contract. */
    private static JSONObject toApiFields(JSONObject fields) {
        JSONObject out = new JSONObject();
        if (fields == null) return out;
        java.util.Iterator<String> keys = fields.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            Object v = fields.opt(k);
            try {
                switch (k) {
                    case "name": out.put("food_name", v); break;
                    case "category_id": out.put("category", v); break;
                    case "stock": out.put("quantity", v); break;
                    case "deadline": out.put("expires_at", v); break;
                    // The form collects one free-text pickup window; the API
                    // stores start/end — keep the vendor's text as the start.
                    case "collection_window": out.put("pickup_window_start", v); break;
                    case "vendor_id":
                    case "location":   // derived from the shop's area/city server-side
                    case "currency":   // always ZMW
                    case "id":         // server-generated
                        break;
                    default: out.put(k, v); // description, original_price, discounted_price…
                }
            } catch (JSONException ignored) {
                // non-JSON-serializable value — skip the field
            }
        }
        return out;
    }

    @Override
    public void createReview(String listingId, int rating, String body, SupaResult<Review> cb) {
        try {
            JSONObject fields = new JSONObject()
                    .put("deal_id", listingId)
                    .put("rating", rating)
                    .put("comment", body == null ? "" : body);
            http.send("POST", "/api/reviews", fields, token(),
                    new SupaResult<JSONObject>() {
                        @Override public void onSuccess(JSONObject env) {
                            // The API acknowledges with { success, message } only —
                            // build the submitted row locally for the caller.
                            Review r = new Review("", listingId, auth.userId(),
                                    Math.max(1, Math.min(5, rating)),
                                    body == null ? "" : body,
                                    System.currentTimeMillis(), ReviewStatus.VISIBLE);
                            r.setUserName(auth.displayName());
                            cb.onSuccess(r);
                        }
                        @Override public void onError(String message) { cb.onError(message); }
                    });
        } catch (JSONException e) {
            cb.onError("Could not submit review");
        }
    }

    /** Admin customer directory labels (id → name/email) from GET /api/admin/users. */
    public void getAdminUserLabels(SupaResult<java.util.Map<String, String>> cb) {
        http.get("/api/admin/users", "role=customer", token(),
                new SupaResult<JSONObject>() {
                    @Override public void onSuccess(JSONObject env) {
                        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
                        JSONArray arr = env.optJSONArray("users");
                        try {
                            if (arr != null) {
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject u = arr.getJSONObject(i);
                                    String name = str(u, "name");
                                    if (name.isEmpty()) name = str(u, "email");
                                    out.put(str(u, "id"), name);
                                }
                            }
                            cb.onSuccess(out);
                        } catch (JSONException e) {
                            cb.onError("Bad server response");
                        }
                    }
                    @Override public void onError(String m) { cb.onError(m); }
                });
    }

    @Override
    public void onboardVendor(String name, String location, SupaResult<Vendor> cb) {
        // The local contract's vendor onboarding is a full account+shop wizard
        // (POST /api/vendors/register) collected on the website; the app has no
        // equivalent UI in this release.
        cb.onError("Vendor onboarding runs on the website in this release");
    }

    // ---------- callback adapters ----------

    private interface Mapper<T> {
        T map(JSONObject o) throws JSONException;
    }

    /** Maps an envelope { success, <key>: [...] } into a typed list. */
    private <T> SupaResult<JSONObject> mapList(SupaResult<List<T>> cb, String key, Mapper<T> m) {
        return new SupaResult<JSONObject>() {
            @Override public void onSuccess(JSONObject env) {
                try {
                    JSONArray arr = env.optJSONArray(key);
                    List<T> out = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) out.add(m.map(arr.getJSONObject(i)));
                    }
                    cb.onSuccess(out);
                } catch (JSONException e) {
                    cb.onError("Bad server response");
                }
            }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    /** Maps an envelope { success, <key>: {...} } into a typed object. */
    private <T> SupaResult<JSONObject> mapObject(SupaResult<T> cb, String key, Mapper<T> m) {
        return new SupaResult<JSONObject>() {
            @Override public void onSuccess(JSONObject env) {
                try {
                    JSONObject o = env.optJSONObject(key);
                    if (o == null) {
                        cb.onError("Bad server response");
                        return;
                    }
                    cb.onSuccess(m.map(o));
                } catch (JSONException e) {
                    cb.onError("Bad server response");
                }
            }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    private SupaResult<JSONObject> ok(final SupaResult<String> cb) {
        return new SupaResult<JSONObject>() {
            @Override public void onSuccess(JSONObject env) { cb.onSuccess("ok"); }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    private static String enc(String s) {
        if (s == null) return "";
        try {
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s.replace(" ", "%20");
        }
    }
}
