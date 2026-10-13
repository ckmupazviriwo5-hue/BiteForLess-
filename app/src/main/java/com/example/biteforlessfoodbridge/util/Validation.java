package com.example.biteforlessfoodbridge.util;

import java.util.regex.Pattern;

/** Shared form + domain validation (pure logic, unit-tested). */
public final class Validation {

    private Validation() {}

    /** Simple email pattern (pure-Java so local unit tests can run without the Android framework). */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static boolean isRequired(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isEmail(String value) {
        return isRequired(value) && EMAIL_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isPasswordLengthOk(String password, int minLen) {
        return password != null && password.length() >= minLen;
    }

    /** Listing prices: original > 0, discounted >= 0 and strictly below original. */
    public static boolean isPricePairValid(double original, double discounted) {
        return original > 0 && discounted >= 0 && discounted < original;
    }

    /** Reservation quantity: 1..available. */
    public static boolean isQuantityValid(int requested, int available) {
        return requested >= 1 && requested <= available;
    }

    public static boolean isRatingValid(int rating) {
        return rating >= 1 && rating <= 5;
    }

    public static String requiredErrorIfEmpty(String value) {
        return isRequired(value) ? null : "This field is required";
    }

    public static String emailError(String value) {
        if (!isRequired(value)) return "Email is required";
        if (!isEmail(value)) return "Enter a valid email address";
        return null;
    }
}
