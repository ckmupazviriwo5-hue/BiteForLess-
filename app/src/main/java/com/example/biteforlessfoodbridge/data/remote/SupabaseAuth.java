package com.example.biteforlessfoodbridge.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * GoTrue authentication: email/password + Google OAuth (PKCE authorization
 * code flow). Sessions persist in EncryptedSharedPreferences with a plain
 * fallback; passwords are never stored. Designed so more providers can be
 * added via {@link #providerUrl(String, String, String)}.
 */
public class SupabaseAuth {

    private static final String PREFS = "supabase_session";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PKCE = "pkce_verifier";

    private final Context app;
    private SharedPreferences secure;

    public SupabaseAuth(Context context) {
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

    public boolean hasSession() {
        return !accessToken().isEmpty() && !userId().isEmpty();
    }

    public String accessToken() {
        return prefs().getString(KEY_ACCESS, "");
    }

    public String userId() {
        return prefs().getString(KEY_USER_ID, "");
    }

    public String email() {
        return prefs().getString(KEY_EMAIL, "");
    }

    public void signOut() {
        prefs().edit().clear().apply();
    }

    private void saveSession(JSONObject payload) throws JSONException {
        String access = payload.optString("access_token", "");
        String refresh = payload.optString("refresh_token", "");
        JSONObject user = payload.optJSONObject("user");
        String uid = user == null ? "" : user.optString("id", "");
        String mail = user == null ? "" : user.optString("email", "");
        prefs().edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .putString(KEY_USER_ID, uid)
                .putString(KEY_EMAIL, mail)
                .apply();
    }

    // ---- email/password ----

    public void signUp(String name, String email, String password, SupaResult<String> cb) {
        try {
            JSONObject body = new JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("data", new JSONObject().put("name", name))
                    .put("gotrue_meta_security", new JSONObject());
            SupabaseHttp.get().authPost("/auth/v1/signup", body, wrapSession(cb));
        } catch (JSONException e) {
            cb.onError("Could not start sign-up");
        }
    }

    public void signIn(String email, String password, SupaResult<String> cb) {
        try {
            JSONObject body = new JSONObject()
                    .put("email", email)
                    .put("password", password);
            SupabaseHttp.get().authPost("/auth/v1/token?grant_type=password", body,
                    wrapSession(cb));
        } catch (JSONException e) {
            cb.onError("Could not start sign-in");
        }
    }

    /** Restores/refreshes the session on app restart. */
    public void restore(SupaResult<Boolean> cb) {
        String refresh = prefs().getString(KEY_REFRESH, "");
        if (refresh.isEmpty() || !hasSession()) {
            cb.onSuccess(false);
            return;
        }
        // Validate cheaply: fetch own profile; on 401, try one refresh.
        fetchRole(new SupaResult<String>() {
            @Override public void onSuccess(String role) { cb.onSuccess(true); }
            @Override public void onError(String message) {
                refresh(new SupaResult<String>() {
                    @Override public void onSuccess(String v) { cb.onSuccess(true); }
                    @Override public void onError(String m) {
                        signOut();
                        cb.onSuccess(false);
                    }
                });
            }
        });
    }

    public void refresh(SupaResult<String> cb) {
        String refresh = prefs().getString(KEY_REFRESH, "");
        if (refresh.isEmpty()) {
            cb.onError("Session expired — please sign in again");
            return;
        }
        try {
            JSONObject body = new JSONObject().put("refresh_token", refresh);
            SupabaseHttp.get().authPost("/auth/v1/token?grant_type=refresh_token", body,
                    wrapSession(cb));
        } catch (JSONException e) {
            cb.onError("Session expired — please sign in again");
        }
    }

    private SupaResult<JSONObject> wrapSession(SupaResult<String> cb) {
        return new SupaResult<JSONObject>() {
            @Override public void onSuccess(JSONObject payload) {
                try {
                    saveSession(payload);
                    cb.onSuccess(email());
                } catch (JSONException e) {
                    cb.onError("Bad auth response");
                }
            }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    /** Server-authoritative role from profiles (never trusted from the client). */
    public void fetchRole(SupaResult<String> cb) {
        String uid = userId();
        if (uid.isEmpty()) {
            cb.onError("Not signed in");
            return;
        }
        SupabaseHttp.get().getObject("/rest/v1/profiles",
                "id=eq." + SupabaseAuth.enc(uid) + "&select=role", accessToken(),
                new SupaResult<JSONObject>() {
                    @Override public void onSuccess(JSONObject o) {
                        cb.onSuccess(o.optString("role", "customer"));
                    }
                    @Override public void onError(String message) { cb.onError(message); }
                });
    }

    // ---- Google OAuth (PKCE) ----

    /** PKCE verifier (43–128 chars, stored for the callback exchange). */
    public static String newVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes,
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    /** S256 code challenge for a verifier (unit-tested, pure). */
    public static String challengeFor(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.encodeToString(digest,
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Builds the Supabase authorize URL for a provider. Additional providers
     * reuse this with a different {@code provider} name.
     */
    public static String providerUrl(String provider, String verifier) {
        return SupabaseConfig.url() + "/auth/v1/authorize?provider" + "="
                + enc(provider)
                + "&redirect_to=" + enc(SupabaseConfig.OAUTH_REDIRECT_URI)
                + "&code_challenge=" + enc(challengeFor(verifier))
                + "&code_challenge_method=s256";
    }

    /** Pure-Java percent-encoding for query values (unit-tested). */
    public static String enc(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /** Pure query-param parser for callback URLs (unit-tested, no Android Uri). */
    public static String queryParam(String url, String key) {
        if (url == null || key == null) return "";
        int q = url.indexOf('?');
        if (q < 0) return "";
        String query = url.substring(q + 1);
        int hash = query.indexOf('#');
        if (hash >= 0) query = query.substring(0, hash);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            if (k.equals(key)) {
                return eq < 0 ? "" : pair.substring(eq + 1);
            }
        }
        return "";
    }

    public void beginGoogle(SupaResult<String> cb) {
        String verifier = newVerifier();
        prefs().edit().putString(KEY_PKCE, verifier).apply();
        cb.onSuccess(providerUrl("google", verifier));
    }

    /** Exchanges the callback code for a session (PKCE, no client secret). */
    public void exchangeCode(String code, SupaResult<String> cb) {
        String verifier = prefs().getString(KEY_PKCE, "");
        prefs().edit().remove(KEY_PKCE).apply();
        if (code == null || code.isEmpty() || verifier.isEmpty()) {
            cb.onError("Google sign-in was not completed");
            return;
        }
        try {
            JSONObject body = new JSONObject()
                    .put("auth_code", code)
                    .put("code_verifier", verifier);
            SupabaseHttp.get().authPost("/auth/v1/token?grant_type=pkce", body,
                    wrapSession(cb));
        } catch (JSONException e) {
            cb.onError("Google sign-in failed");
        }
    }

    /** Extracts ?code= / ?error= from the deep-link callback (pure helper). */
    public static String callbackCode(Uri callback) {
        return callback == null ? "" : queryParam(callback.toString(), "code");
    }

    public static String callbackError(Uri callback) {
        if (callback == null) return "";
        String err = queryParam(callback.toString(), "error_description");
        if (err.isEmpty()) err = queryParam(callback.toString(), "error");
        return err;
    }
}
