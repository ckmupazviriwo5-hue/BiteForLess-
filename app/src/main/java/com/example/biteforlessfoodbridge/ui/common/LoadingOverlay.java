package com.example.biteforlessfoodbridge.ui.common;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.android.material.snackbar.Snackbar;

/**
 * Full-screen loading spinner attached to the activity root (no layout edits
 * needed) + error Snackbar with retry. Used by live-backend loads only.
 */
public final class LoadingOverlay {

    private LoadingOverlay() {}

    public static View show(Activity activity) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        ProgressBar bar = new ProgressBar(activity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        FrameLayout wrapper = new FrameLayout(activity);
        wrapper.setTag("biteforless_loading");
        wrapper.addView(bar, params);
        root.addView(wrapper, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return wrapper;
    }

    public static void hide(View overlay) {
        if (overlay == null) return;
        try {
            ViewGroup parent = (ViewGroup) overlay.getParent();
            if (parent != null) parent.removeView(overlay);
        } catch (Exception ignored) {
        }
    }

    public static void error(Activity activity, String message, Runnable retryOrNull) {
        View root = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(root,
                message == null ? "Request failed" : message,
                Snackbar.LENGTH_LONG);
        if (retryOrNull != null) {
            snackbar.setAction("Retry", v -> retryOrNull.run());
        }
        snackbar.show();
    }
}
