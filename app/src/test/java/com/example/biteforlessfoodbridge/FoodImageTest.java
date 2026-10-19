package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.util.FoodImage;
import com.example.biteforlessfoodbridge.R;

import org.junit.Test;

/** Category artwork fallback mapping (pure logic, offline-safe). */
public class FoodImageTest {

    @Test
    public void fallback_coversAllKnownCategories() {
        assertEquals(R.drawable.ic_food_bakery, FoodImage.fallbackFor("bakery"));
        assertEquals(R.drawable.ic_food_produce, FoodImage.fallbackFor("produce"));
        assertEquals(R.drawable.ic_food_dairy, FoodImage.fallbackFor("dairy"));
        assertEquals(R.drawable.ic_food_meals, FoodImage.fallbackFor("meals"));
        assertEquals(R.drawable.ic_food_beverages, FoodImage.fallbackFor("beverages"));
        assertEquals(R.drawable.ic_food_snacks, FoodImage.fallbackFor("snacks"));
    }

    @Test
    public void fallback_unknownOrNull_neverZero() {
        assertNotEquals(0, FoodImage.fallbackFor("mystery"));
        assertNotEquals(0, FoodImage.fallbackFor(null));
        assertNotEquals(0, FoodImage.fallbackFor(""));
    }
}
