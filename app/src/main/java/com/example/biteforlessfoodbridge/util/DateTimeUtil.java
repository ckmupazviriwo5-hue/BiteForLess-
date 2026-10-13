package com.example.biteforlessfoodbridge.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Date/time helpers for collection windows and deadlines. */
public final class DateTimeUtil {

    private DateTimeUtil() {}

    public static String formatDeadline(long epochMillis) {
        if (epochMillis <= 0) return "No deadline";
        SimpleDateFormat fmt = new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());
        return "Collect by " + fmt.format(new Date(epochMillis));
    }

    public static String relativeToNow(long epochMillis, long nowMillis) {
        long diff = epochMillis - nowMillis;
        if (diff <= 0) return "Expired";
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours < 1) {
            long mins = TimeUnit.MILLISECONDS.toMinutes(diff);
            return "Expires in " + Math.max(1, mins) + " min";
        }
        if (hours < 24) return "Expires in " + hours + "h";
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return "Expires in " + days + "d";
    }

    public static boolean isExpired(long deadlineEpochMillis, long nowMillis) {
        return deadlineEpochMillis > 0 && nowMillis > deadlineEpochMillis;
    }
}
