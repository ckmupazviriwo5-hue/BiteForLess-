package com.example.biteforlessfoodbridge.data.remote;

import android.content.Context;

/**
 * Single place that decides WHICH backend the app talks to.
 *
 * Priority: the local Express + SQLite API (api.baseUrl in local.properties)
 * → hosted Supabase (dormant fallback, kept for history) → demo mode.
 * Activities hold {@link SupabaseAuth}-typed references; the Express
 * implementation subclasses it so no UI rewrite is needed.
 */
public final class AuthGateway {

    private AuthGateway() {}

    /** True when a real backend (local API or legacy hosted) is configured. */
    public static boolean isLive(Context context) {
        return ExpressConfig.isConfigured() || SupabaseConfig.isConfigured();
    }

    /** True when the ACTIVE backend is the local Express + SQLite API. */
    public static boolean isExpress(Context context) {
        return ExpressConfig.isConfigured();
    }

    /** Backend-appropriate auth handle (ExpressAuth when the local API is set). */
    public static SupabaseAuth auth(Context context) {
        return ExpressConfig.isConfigured()
                ? new ExpressAuth(context)
                : new SupabaseAuth(context);
    }
}
