package com.example.biteforlessfoodbridge.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

/**
 * Authentication against the local Express + SQLite API (frozen contract §2).
 * Mirrors the {@link SupabaseAuth} surface so existing activities keep
 * compiling while the active backend is the local server.
 *
 * Contract notes honoured here:
 * - Login is unified (one endpoint, any account type); the ROLE comes from
 *   the server response and /api/auth/me — never from client input.
 * - The API role "shop_owner" is mapped to the app's "vendor" role for UI
 *   routing only; authorization itself stays server-side on every request.
 * - Sessions are JWT bearer tokens; restore() validates against /api/auth/me,
 *   keeps the session through network failures or transient server errors,
 *   and clears it only when the server actually rejects the credentials.
 */
public class ExpressAuth extends SupabaseAuth {

    private static final String PREFS = "express_session";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_STATUS = "status";

    private final Context app;
    private SharedPreferences secure;

    public ExpressAuth(Context context) {
        super(context);
        this.app = context.getApplicationContext();
    }

    private synchronized SharedPreferences prefs() {
        if (secure != null) return secure;
        try {
            MasterKey masterKey = new MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            secure = EncryptedSharedPreferences.create(app, PREFS, masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            secure = app.getSharedPreferences(PREFS + "_plain", Context.MODE_PRIVATE);
        }
        return secure;
    }

    // ---- session ----

    @Override public boolean hasSession() {
        return !accessToken().isEmpty() && !userId().isEmpty();
    }

    @Override public String accessToken() {
        return prefs().getString(KEY_ACCESS, "");
    }

    @Override public String userId() {
        return prefs().getString(KEY_USER_ID, "");
    }

    @Override public String email() {
        return prefs().getString(KEY_EMAIL, "");
    }

    /** Display name of the signed-in user ("" when unknown). */
    @Override public String displayName() {
        return prefs().getString(KEY_NAME, "");
    }

    public String status() {
        return prefs().getString(KEY_STATUS, "");
    }

    /** Server-assigned role of the signed-in user (api vocabulary). */
    public String role() {
        return prefs().getString(KEY_ROLE, "customer");
    }

    @Override public void signOut() {
        // Best-effort server logout (symmetry only; JWT is cleared client-side).
        final String token = accessToken();
        prefs().edit().clear().apply();
        if (!token.isEmpty()) {
            ExpressHttp.get().send("POST", "/api/auth/logout", null, token,
                    new SupaResult<JSONObject>() {
                        @Override public void onSuccess(JSONObject env) { }
                        @Override public void onError(String message) { }
                    });
        }
    }

    private void saveSession(JSONObject user, String token) {
        prefs().edit()
                .putString(KEY_ACCESS, token == null ? "" : token)
                .putString(KEY_USER_ID, user.optString("id", ""))
                .putString(KEY_EMAIL, user.optString("email", ""))
                .putString(KEY_NAME, user.optString("name", ""))
                .putString(KEY_ROLE, user.optString("role", "customer"))
                .putString(KEY_STATUS, user.optString("status", ""))
                .apply();
    }

    // ---- email/password ----

    @Override
    public void signUp(String name, String email, String password, SupaResult<String> cb) {
        // The local contract requires a phone number; the sign-up form collects it.
        signUp(name, email, "", password, cb);
    }

    /** Real sign-up: POST /api/auth/register { name, email, phone, password }. */
    public void signUp(String name, String email, String phone, String password,
                       SupaResult<String> cb) {
        try {
            JSONObject body = new JSONObject()
                    .put("name", name == null ? "" : name.trim())
                    .put("email", email == null ? "" : email.trim())
                    .put("phone", phone == null ? "" : phone.trim())
                    .put("password", password == null ? "" : password);
            ExpressHttp.get().send("POST", "/api/auth/register", body, null,
                    envelope(cb));
        } catch (Exception e) {
            cb.onError("Could not start sign-up");
        }
    }

    @Override
    public void signIn(String email, String password, SupaResult<String> cb) {
        try {
            JSONObject body = new JSONObject()
                    .put("email", email == null ? "" : email.trim())
                    .put("password", password == null ? "" : password);
            ExpressHttp.get().send("POST", "/api/auth/login", body, null,
                    new SupaResult<JSONObject>() {
                        @Override public void onSuccess(JSONObject env) {
                            try {
                                JSONObject user = env.optJSONObject("user");
                                String token = env.optString("token", "");
                                if (user == null || token.isEmpty()) {
                                    cb.onError("Bad auth response");
                                    return;
                                }
                                saveSession(user, token);
                                cb.onSuccess(email());
                            } catch (Exception e) {
                                cb.onError("Bad auth response");
                            }
                        }
                        @Override public void onError(String message) { cb.onError(message); }
                    });
        } catch (Exception e) {
            cb.onError("Could not start sign-in");
        }
    }

    /** Validates the stored JWT against /api/auth/me. */
    @Override
    public void restore(SupaResult<Boolean> cb) {
        if (!hasSession()) {
            cb.onSuccess(false);
            return;
        }
        ExpressHttp.get().get("/api/auth/me", "", accessToken(),
                new SupaResult<JSONObject>() {
                    @Override public void onSuccess(JSONObject env) {
                        JSONObject user = env.optJSONObject("user");
                        if (user != null) {
                            saveSession(user, accessToken()); // refresh cached profile
                            cb.onSuccess(true);
                        } else {
                            signOut();
                            cb.onSuccess(false);
                        }
                    }
                    @Override public void onError(String message) {
                        // Keep the session through connectivity problems and
                        // transient server errors; end it only when the server
                        // actually rejects the stored credentials.
                        if (isAuthRejection(message)) {
                            signOut();
                            cb.onSuccess(false);
                        } else {
                            cb.onSuccess(true);
                        }
                    }
                });
    }

    /** ExpressHttp's distinct prefixes for transport failures. */
    private static boolean isNetworkError(String message) {
        return message != null && (message.startsWith("Network error")
                || message.startsWith("Cannot reach"));
    }

    /** Server messages that mean this stored session is no longer valid. */
    private static boolean isAuthRejection(String message) {
        if (isNetworkError(message) || message == null) return false;
        String m = message.toLowerCase(java.util.Locale.US);
        return m.contains("expired") || m.contains("sign in")
                || m.contains("authentication required") || m.contains("token")
                || m.contains("user not found");
    }

    /** Server-authoritative role, mapped to the app's role vocabulary. */
    @Override
    public void fetchRole(SupaResult<String> cb) {
        ExpressHttp.get().get("/api/auth/me", "", accessToken(),
                new SupaResult<JSONObject>() {
                    @Override public void onSuccess(JSONObject env) {
                        JSONObject user = env.optJSONObject("user");
                        String role = user == null ? "customer" : user.optString("role", "customer");
                        cb.onSuccess(mapRole(role));
                    }
                    @Override public void onError(String message) { cb.onError(message); }
                });
    }

    /** API role → app role: admin→admin, shop_owner→vendor, else customer. */
    public static String mapRole(String apiRole) {
        if ("admin".equalsIgnoreCase(apiRole)) return "admin";
        if ("shop_owner".equalsIgnoreCase(apiRole)) return "vendor";
        return "customer";
    }

    private SupaResult<JSONObject> envelope(final SupaResult<String> cb) {
        return new SupaResult<JSONObject>() {
            @Override public void onSuccess(JSONObject env) {
                JSONObject user = env.optJSONObject("user");
                String token = env.optString("token", "");
                if (user == null || token.isEmpty()) {
                    cb.onError("Bad auth response");
                    return;
                }
                saveSession(user, token);
                cb.onSuccess(email());
            }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }
}
