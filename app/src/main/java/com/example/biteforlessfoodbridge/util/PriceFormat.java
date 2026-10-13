package com.example.biteforlessfoodbridge.util;

import java.text.NumberFormat;
import java.util.Locale;

/** ZMW (Zambian kwacha) price formatting + discount helpers (pure logic, unit-tested). */
public final class PriceFormat {

    private static final Locale ZMW = new Locale("en", "ZM");

    private PriceFormat() {}

    public static String format(double amount) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(ZMW);
        fmt.setMaximumFractionDigits(2);
        fmt.setMinimumFractionDigits(2);
        return fmt.format(amount);
    }

    public static int discountPercent(double original, double discounted) {
        if (original <= 0 || discounted < 0 || discounted >= original) return 0;
        return (int) Math.round((1.0 - discounted / original) * 100.0);
    }

    public static String discountBadge(double original, double discounted) {
        int pct = discountPercent(original, discounted);
        return pct > 0 ? "-" + pct + "%" : "";
    }

    public static String savings(double original, double discounted) {
        double s = original - discounted;
        return format(Math.max(0, s)) + " saved";
    }
}
