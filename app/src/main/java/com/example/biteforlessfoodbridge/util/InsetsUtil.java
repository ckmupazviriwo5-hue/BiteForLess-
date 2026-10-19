package com.example.biteforlessfoodbridge.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge padding helpers. Call once per screen on the root view (and
 * optionally extra bottom padding for scroll content above the nav bar).
 */
public final class InsetsUtil {

    private InsetsUtil() {}

    /** Pads a view by the status + navigation bar insets (accumulating). */
    public static void applySystemBarPadding(View view) {
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseLeft + bars.left, baseTop + bars.top,
                    baseRight + bars.right, baseBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
