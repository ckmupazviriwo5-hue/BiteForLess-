package com.example.biteforlessfoodbridge.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.biteforlessfoodbridge.model.UserRole;

/**
 * Lightweight demo session (SharedPreferences only).
 * Stores the SIMULATED role + profile; never real passwords or auth tokens.
 */
public class SessionManager {

    private static final String PREFS = "biteforless_session";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_SIGNED_IN = "signed_in";
    private static final String KEY_VENDOR_ID = "vendor_id";

    /** Stable demo vendor for the vendor role (Mama Chanda's Bakery, seeded as "v1"). */
    public static final String DEMO_VENDOR_ID = "v1";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void signIn(String name, String email, UserRole role) {
        prefs.edit()
                .putBoolean(KEY_SIGNED_IN, true)
                .putString(KEY_NAME, name == null ? "" : name)
                .putString(KEY_EMAIL, email == null ? "" : email)
                .putString(KEY_ROLE, role == null ? UserRole.CUSTOMER.name() : role.name())
                .apply();
    }

    public void signOut() {
        prefs.edit().putBoolean(KEY_SIGNED_IN, false).apply();
    }

    public boolean isSignedIn() {
        return prefs.getBoolean(KEY_SIGNED_IN, false);
    }

    public UserRole getRole() {
        try {
            return UserRole.valueOf(prefs.getString(KEY_ROLE, UserRole.CUSTOMER.name()));
        } catch (IllegalArgumentException e) {
            return UserRole.CUSTOMER;
        }
    }

    public void setRole(UserRole role) {
        prefs.edit().putString(KEY_ROLE, role.name()).apply();
    }

    public String getName() { return prefs.getString(KEY_NAME, ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }

    /**
     * Explicit mapping from the simulated vendor role to a seeded vendor
     * record. Demo roles are UI routing only, never a security boundary.
     */
    public String getVendorId() { return prefs.getString(KEY_VENDOR_ID, ""); }

    public void setVendorId(String vendorId) {
        prefs.edit().putString(KEY_VENDOR_ID, vendorId == null ? "" : vendorId).apply();
    }

    /** Returns the mapped vendor, defaulting a vendor-role session to the demo vendor. */
    public String requireDemoVendorId() {
        String id = getVendorId();
        if (id == null || id.isEmpty()) {
            id = DEMO_VENDOR_ID;
            setVendorId(id);
        }
        return id;
    }

    public void setProfile(String name, String email) {
        prefs.edit().putString(KEY_NAME, name).putString(KEY_EMAIL, email).apply();
    }
}
