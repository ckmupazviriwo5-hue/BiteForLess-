package com.example.biteforlessfoodbridge.data.remote;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Minimal OkHttp wrapper for the local Express + SQLite API (see
 * docs/LOCAL_API_CONTRACT.md in the website project). Every endpoint answers
 * with the envelope { success, message?, ...resource }; errors carry a
 * human-readable "message". All callbacks land on the main thread.
 */
public class ExpressHttp {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static ExpressHttp instance;

    public static synchronized ExpressHttp get() {
        if (instance == null) instance = new ExpressHttp();
        return instance;
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS) // image uploads
            .build();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ExpressHttp() {}

    private Request.Builder base(String path, String accessToken) {
        Request.Builder b = new Request.Builder().url(ExpressConfig.baseUrl() + path);
        if (accessToken != null && !accessToken.isEmpty()) {
            b.header("Authorization", "Bearer " + accessToken);
        }
        return b;
    }

    /** GET returning the parsed envelope object. */
    public void get(String path, String query, String accessToken, SupaResult<JSONObject> cb) {
        String full = path + (query == null || query.isEmpty() ? "" : "?" + query);
        Request req = base(full, accessToken).get().build();
        enqueueEnvelope(req, cb);
    }

    /** POST/PUT/PATCH/DELETE with a JSON body, returning the parsed envelope. */
    public void send(String method, String path, JSONObject body, String accessToken,
                     SupaResult<JSONObject> cb) {
        RequestBody rb = RequestBody.create(body == null ? "{}" : body.toString(), JSON);
        Request req = base(path, accessToken)
                .header("Content-Type", "application/json")
                .method(method, rb)
                .build();
        enqueueEnvelope(req, cb);
    }

    /** Multipart POST/PUT (listing create/edit with an optional photo). */
    public void sendMultipart(String method, String path, JSONObject fields,
                              File imageOrNull, String accessToken, SupaResult<JSONObject> cb) {
        MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (fields != null) {
            java.util.Iterator<String> keys = fields.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                mb.addFormDataPart(k, fields.optString(k, ""));
            }
        }
        if (imageOrNull != null && imageOrNull.exists()) {
            mb.addFormDataPart("image", imageOrNull.getName(),
                    RequestBody.create(imageOrNull, MediaType.get("image/*")));
        }
        Request req = base(path, accessToken).method(method, mb.build()).build();
        enqueueEnvelope(req, cb);
    }

    private void enqueueEnvelope(Request req, SupaResult<JSONObject> cb) {
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
                    try {
                        JSONObject env = text.trim().isEmpty()
                                ? new JSONObject().put("success", true)
                                : new JSONObject(text);
                        succeed(cb, env);
                    } catch (JSONException e) {
                        fail(cb, "Bad server response");
                    }
                } catch (IOException e) {
                    fail(cb, networkMessage(e));
                }
            }
        });
    }

    public static String networkMessage(IOException e) {
        String m = e.getMessage();
        if (m != null && (m.toLowerCase().contains("unable to resolve host")
                || m.toLowerCase().contains("failed to connect")
                || m.toLowerCase().contains("connection refused"))) {
            return "Cannot reach the local server — start it on the computer "
                    + "(start.bat) and check the API address";
        }
        return "Network error — please retry";
    }

    /** Express errors are { success:false, message } — pass the message through. */
    public static String apiError(String body, int code) {
        try {
            JSONObject o = new JSONObject(body);
            String msg = o.optString("message", "");
            if (!msg.isEmpty()) return msg;
        } catch (JSONException ignored) {
        }
        if (code == 401) return "Session expired — please sign in again";
        if (code == 403) return "You do not have access to that";
        if (code == 404) return "Not found on the server";
        if (code == 429) return "Too many requests — slow down and retry";
        if (code >= 500) return "Server error — please retry later";
        return "Request failed (" + code + ")";
    }

    private <T> void succeed(SupaResult<T> cb, T value) {
        main.post(() -> cb.onSuccess(value));
    }

    private <T> void fail(SupaResult<T> cb, String message) {
        main.post(() -> cb.onError(message));
    }
}
