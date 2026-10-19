package com.example.biteforlessfoodbridge.data;

import android.content.Context;

import com.example.biteforlessfoodbridge.data.remote.AuthGateway;
import com.example.biteforlessfoodbridge.data.remote.ExpressAuth;
import com.example.biteforlessfoodbridge.data.remote.ExpressRepository;
import com.example.biteforlessfoodbridge.data.remote.ExpressConfig;
import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupabaseAuth;
import com.example.biteforlessfoodbridge.data.remote.SupabaseConfig;

/**
 * Chooses the data source. The LOCAL Express + SQLite API is the active
 * backend whenever api.baseUrl is configured (priority over the dormant
 * hosted Supabase fallback); otherwise the existing local demo repository.
 * The UI always labels which mode is active — live errors are shown, never
 * silently replaced with demo data.
 */
public final class RepositoryProvider {

    private RepositoryProvider() {}

    /** True only when a backend is configured AND the user holds a session. */
    public static boolean isLive(Context context) {
        if (ExpressConfig.isConfigured()) {
            return new ExpressAuth(context).hasSession();
        }
        if (SupabaseConfig.isConfigured()) {
            try {
                return new SupabaseAuth(context).hasSession();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /** Live repository — Express-backed when configured (RemoteRepository API). */
    public static RemoteRepository remote(Context context) {
        if (ExpressConfig.isConfigured()) {
            return new ExpressRepository(context);
        }
        return new RemoteRepository(context);
    }

    public static DemoRepository demo() {
        return DemoRepository.getInstance();
    }

    /** Demo user id for the offline path (session email). */
    public static String demoUserId(Context context) {
        try {
            return new SessionManager(context).getEmail();
        } catch (Exception e) {
            return "";
        }
    }
}
