package com.example.biteforlessfoodbridge.data;

import android.content.Context;

import com.example.biteforlessfoodbridge.data.remote.RemoteRepository;
import com.example.biteforlessfoodbridge.data.remote.SupabaseAuth;
import com.example.biteforlessfoodbridge.data.remote.SupabaseConfig;

/**
 * Chooses the data source. Live backend only when the app is configured AND
 * the user holds a session; otherwise the existing local demo repository.
 * The UI always labels which mode is active — live errors are shown, never
 * silently replaced with demo data.
 */
public final class RepositoryProvider {

    private RepositoryProvider() {}

    /** True only when configured + signed in (live source of truth). */
    public static boolean isLive(Context context) {
        if (!SupabaseConfig.isConfigured()) return false;
        try {
            return new SupabaseAuth(context).hasSession();
        } catch (Exception e) {
            return false;
        }
    }

    public static RemoteRepository remote(Context context) {
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
