package com.example.biteforlessfoodbridge.data.remote;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Minimal OkHttp wrapper for Supabase PostgREST / GoTrue / RPC.
 * All callbacks land on the main thread. No blocking calls on callers.
 */
public class SupabaseHttp {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static SupabaseHttp instance;

    public static synchronized SupabaseHttp get() {
        if (instance == null) instance = new SupabaseHttp();
        return instance;
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
    private final Handler main = new Handler(Looper.getMainLooper());

    private SupabaseHttp() {}

    private Request.Builder base(String path, String accessToken) {
        Request.Builder b = new Request.Builder()
                .url(SupabaseConfig.url() + path)
                .header("apikey", SupabaseConfig.anonKey());
        if (accessToken != null && !accessToken.isEmpty()) {
            b.header("Authorization", "Bearer " + accessToken);
        } else {
            b.header("Authorization", "Bearer " + SupabaseConfig.anonKey());
        }
        return b;
    }

    public void get(String path, String query, String accessToken, SupaResult<JSONArray> cb) {
        String full = path + (query == null || query.isEmpty() ? "" : "?" + query);
        Request req = base(full, accessToken)
                .header("Accept", "application/json")
                .get().build();
        enqueue(req, new SupaResult<String>() {
            @Override public void onSuccess(String body) {
                try {
                    String t = body.trim();
                    if (t.startsWith("[")) cb.onSuccess(new JSONArray(t));
                    else if (t.isEmpty()) cb.onSuccess(new JSONArray());
                    else cb.onSuccess(new JSONArray().put(new JSONObject(t)));
                } catch (JSONException e) {
                    fail(cb, "Bad server response");
                }
            }
            @Override public void onError(String message) { fail(cb, message); }
        });
    }

    public void getObject(String path, String query, String accessToken, SupaResult<JSONObject> cb) {
        String full = path + (query == null || query.isEmpty() ? "" : "?" + query);
        Request req = base(full, accessToken)
                .header("Accept", "application/vnd.pgrst.object+json")
                .get().build();
        enqueue(req, new SupaResult<String>() {
            @Override public void onSuccess(String body) {
                try {
                    cb.onSuccess(new JSONObject(body));
                } catch (JSONException e) {
                    fail(cb, "Bad server response");
                }
            }
            @Override public void onError(String message) { fail(cb, message); }
        });
    }

    public void post(String path, JSONObject body, String accessToken,
                     boolean returnRepresentation, SupaResult<JSONArray> cb) {
        Request req = base(path, accessToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Prefer", returnRepresentation ? "return=representation"
                        : "return=minimal")
                .post(RequestBody.create(body == null ? "{}" : body.toString(), JSON))
                .build();
        enqueue(req, new SupaResult<String>() {
            @Override public void onSuccess(String responseBody) {
                try {
                    String t = responseBody.trim();
                    if (t.isEmpty()) cb.onSuccess(new JSONArray());
                    else if (t.startsWith("[")) cb.onSuccess(new JSONArray(t));
                    else cb.onSuccess(new JSONArray().put(new JSONObject(t)));
                } catch (JSONException e) {
                    fail(cb, "Bad server response");
                }
            }
            @Override public void onError(String message) { fail(cb, message); }
        });
    }

    public void patch(String path, String query, JSONObject body, String accessToken,
                      SupaResult<JSONArray> cb) {
        String full = path + (query == null || query.isEmpty() ? "" : "?" + query);
        Request req = base(full, accessToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();
        enqueue(req, new SupaResult<String>() {
            @Override public void onSuccess(String responseBody) {
                try {
                    String t = responseBody.trim();
                    cb.onSuccess(t.isEmpty() ? new JSONArray() : new JSONArray(t));
                } catch (JSONException e) {
                    fail(cb, "Bad server response");
                }
            }
            @Override public void onError(String message) { fail(cb, message); }
        });
    }

    /** Raw POST for GoTrue endpoints (different error shape, no apikey override). */
    public void authPost(String path, JSONObject body, SupaResult<JSONObject> cb) {
        Request req = new Request.Builder()
                .url(SupabaseConfig.url() + path)
                .header("apikey", SupabaseConfig.anonKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fail(cb, networkMessage(e));
            }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    String text = rb == null ? "" : rb.string();
                    if (!response.isSuccessful()) {
                        fail(cb, authError(text, response.code()));
                        return;
                    }
                    try {
                        succeed(cb, new JSONObject(text));
                    } catch (JSONException e) {
                        fail(cb, "Bad auth response");
                    }
                } catch (IOException e) {
                    fail(cb, networkMessage(e));
                }
            }
        });
    }

    private void enqueue(Request req, SupaResult<String> cb) {
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fail(cb, networkMessage(e));
            }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    String text = rb == null ? "" : rb.string();
                    if (!response.isSuccessful()) {
                        fail(cb, apiError(text, response.code()));
                        return;
                    }
                    succeed(cb, text);
                } catch (IOException e) {
                    fail(cb, networkMessage(e));
                }
            }
        });
    }

    public static String networkMessage(IOException e) {
        String m = e.getMessage();
        if (m != null && m.toLowerCase().contains("unable to resolve host")) {
            return "No connection — check your internet and retry";
        }
        return "Network error — please retry";
    }

    public static String apiError(String body, int code) {
        try {
            JSONObject o = new JSONObject(body);
            if (o.has("message")) return friendly(o.optString("message", ""), code);
            if (o.has("msg")) return friendly(o.optString("msg", ""), code);
            if (o.has("error_description")) {
                return friendly(o.optString("error_description", ""), code);
            }
        } catch (JSONException ignored) {
        }
        if (code == 401 || code == 403) return "Session expired — please sign in again";
        if (code == 404) return "Not found on the server";
        if (code >= 500) return "Server error — please retry later";
        return "Request failed (" + code + ")";
    }

    public static String authError(String body, int code) {
        try {
            JSONObject o = new JSONObject(body);
            if (o.has("msg")) return friendly(o.optString("msg", ""), code);
            if (o.has("error_description")) {
                return friendly(o.optString("error_description", ""), code);
            }
            if (o.has("message")) return friendly(o.optString("message", ""), code);
        } catch (JSONException ignored) {
        }
        return "Sign-in failed (" + code + ")";
    }

    private static String friendly(String server, int code) {
        if (server == null || server.isEmpty()) {
            if (code == 401 || code == 403) return "Session expired — please sign in again";
            return "Request failed (" + code + ")";
        }
        String low = server.toLowerCase();
        if (low.contains("invalid login credentials") || low.contains("invalid_grant")) {
            return "Wrong email or password";
        }
        if (low.contains("user already registered") || low.contains("already exists")) {
            return "This email is already registered — try signing in";
        }
        if (low.contains("jwt") && (low.contains("expired") || code == 401)) {
            return "Session expired — please sign in again";
        }
        return server;
    }

    private <T> void succeed(SupaResult<T> cb, T value) {
        main.post(() -> cb.onSuccess(value));
    }

    private <T> void fail(SupaResult<T> cb, String message) {
        main.post(() -> cb.onError(message));
    }
}
