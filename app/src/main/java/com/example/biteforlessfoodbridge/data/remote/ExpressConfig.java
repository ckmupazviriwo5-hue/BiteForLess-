package com.example.biteforlessfoodbridge.data.remote;

import com.example.biteforlessfoodbridge.BuildConfig;

/**
 * Local-first backend configuration (Express + SQLite API, port 5000).
 * The base URL comes from BuildConfig (generated from gitignored
 * local.properties: api.baseUrl). The emulator reaches the host machine via
 * 10.0.2.2; a physical device uses the host's LAN IP instead.
 *
 * This is the ACTIVE backend for the submission. The hosted Supabase project
 * stays configured only as a dormant fallback and is never used for app
 * traffic while this value is set.
 */
public final class ExpressConfig {

    /** Emulator loopback default — override with api.baseUrl in local.properties. */
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:5000";

    private ExpressConfig() {}

    public static String baseUrl() {
        String v = BuildConfig.API_BASE_URL == null ? "" : BuildConfig.API_BASE_URL.trim();
        return v.isEmpty() ? DEFAULT_BASE_URL : v;
    }

    /** API origin for resolving relative resource paths (e.g. /uploads/...). */
    public static String origin() {
        String u = baseUrl();
        // Strip a trailing "/api" so /uploads/... resolves against the server root.
        if (u.endsWith("/api")) u = u.substring(0, u.length() - 4);
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    /** Absolute URL for a server-relative image path; passthrough for http(s). */
    public static String absoluteUrl(String pathOrNull) {
        if (pathOrNull == null) return "";
        String p = pathOrNull.trim();
        if (p.isEmpty()) return "";
        if (p.startsWith("http://") || p.startsWith("https://")) return p;
        return origin() + (p.startsWith("/") ? p : "/" + p);
    }

    public static boolean isConfigured() {
        return !BuildConfig.API_BASE_URL.trim().isEmpty();
    }
}
