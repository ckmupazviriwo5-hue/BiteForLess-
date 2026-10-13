package com.example.biteforlessfoodbridge.data.remote;

import com.example.biteforlessfoodbridge.BuildConfig;

/**
 * Hosted backend configuration. Values come from BuildConfig (generated from
 * gitignored local.properties: supabase.url / supabase.anonKey).
 * Empty = local demo mode with the existing in-memory repository.
 */
public final class SupabaseConfig {

    private SupabaseConfig() {}

    public static String url() {
        return BuildConfig.SUPABASE_URL == null ? "" : BuildConfig.SUPABASE_URL.trim();
    }

    public static String anonKey() {
        return BuildConfig.SUPABASE_ANON_KEY == null ? "" : BuildConfig.SUPABASE_ANON_KEY.trim();
    }

    public static boolean isConfigured() {
        String u = url();
        return !u.isEmpty() && u.startsWith("http") && !anonKey().isEmpty();
    }

    /** Deep-link callback registered in the manifest for OAuth (PKCE). */
    public static final String OAUTH_REDIRECT_SCHEME = "biteforless";
    public static final String OAUTH_REDIRECT_HOST = "auth-callback";
    public static final String OAUTH_REDIRECT_URI =
            OAUTH_REDIRECT_SCHEME + "://" + OAUTH_REDIRECT_HOST;
}
