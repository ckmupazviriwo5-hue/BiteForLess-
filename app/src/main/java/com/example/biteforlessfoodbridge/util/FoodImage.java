package com.example.biteforlessfoodbridge.util;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.biteforlessfoodbridge.R;
import com.example.biteforlessfoodbridge.model.FoodListing;

import java.util.Locale;

/**
 * Listing artwork with reliable fallbacks (no separate live dataset):
 * remote photo when the record carries one, otherwise a per-category
 * vector that always works offline. Never the app logo as a dish photo.
 */
public final class FoodImage {

    private FoodImage() {}

    /** Local fallback per category (matches the website category map; case-insensitive). */
    public static int fallbackFor(String categoryId) {
        String c = categoryId == null ? "" : categoryId.toLowerCase(Locale.US);
        if (c.isEmpty()) return R.drawable.ic_food_meals;
        switch (c) {
            case "bakery": return R.drawable.ic_food_bakery;
            case "produce": case "fruits & vegetables": case "vegetables":
            case "fruits-vegetables":
                return R.drawable.ic_food_produce;
            case "dairy": return R.drawable.ic_food_dairy;
            case "meals": case "restaurant": return R.drawable.ic_food_meals;
            case "beverages": case "drinks": return R.drawable.ic_food_beverages;
            case "snacks": return R.drawable.ic_food_snacks;
            default: return R.drawable.ic_food_meals;
        }
    }

    /** Binds remote art (when present) or the category fallback. */
    public static void load(ImageView view, FoodListing listing, String imageUrlOrNull) {
        int fallback = fallbackFor(listing == null ? null : listing.getCategoryId());
        if (imageUrlOrNull != null && imageUrlOrNull.startsWith("http")) {
            Glide.with(view)
                    .load(imageUrlOrNull)
                    .placeholder(fallback)
                    .error(fallback)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(view);
        } else {
            Glide.with(view).clear(view);
            view.setImageResource(fallback);
        }
    }
}
