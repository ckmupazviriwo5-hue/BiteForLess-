package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.model.FoodListing;
import com.example.biteforlessfoodbridge.model.ListingStatus;
import com.example.biteforlessfoodbridge.util.DateTimeUtil;
import com.example.biteforlessfoodbridge.util.PriceFormat;

import org.junit.Test;

public class MarketplaceRulesTest {

    @Test
    public void currency_isZMW() {
        assertEquals("ZMW", java.util.Currency.getInstance(
                new java.util.Locale("en", "ZM")).getCurrencyCode());
        String formatted = PriceFormat.format(29.0);
        assertTrue("expected amount, got: " + formatted, formatted.contains("29.00"));
        assertFalse("must not render as Rand, got: " + formatted,
                formatted.startsWith("R"));
        // Discount math is currency-independent and stays consistent.
        assertEquals("-52%", PriceFormat.discountBadge(60.0, 29.0));
        assertTrue(PriceFormat.savings(60.0, 29.0).contains("31.00"));
    }

    @Test
    public void discountPercent_halfPrice_fifty() {
        assertEquals(50, PriceFormat.discountPercent(50.0, 25.0));
        assertEquals("", PriceFormat.discountBadge(50.0, 50.0));
        assertEquals("-50%", PriceFormat.discountBadge(50.0, 25.0));
    }

    @Test
    public void discountPercent_invalidPrices_zero() {
        assertEquals(0, PriceFormat.discountPercent(0.0, 0.0));
        assertEquals(0, PriceFormat.discountPercent(50.0, 60.0));
    }

    @Test
    public void listing_reservable_onlyWhenActiveWithStockAndUnexpired() {
        long now = System.currentTimeMillis();
        long future = now + 3_600_000L;
        FoodListing ok = new FoodListing("1", "v1", "Bread", "Day-old bread",
                "bakery", 50.0, 25.0, 5, "Cape Town", future, "16:00-18:00",
                ListingStatus.ACTIVE);
        assertTrue(ok.isReservable(now));
        assertEquals(50, ok.getDiscountPercent());

        FoodListing noStock = new FoodListing("2", "v1", "Bread", "d",
                "bakery", 50.0, 25.0, 0, "Cape Town", future, "16:00-18:00",
                ListingStatus.ACTIVE);
        assertFalse(noStock.isReservable(now));

        FoodListing inactive = new FoodListing("3", "v1", "Bread", "d",
                "bakery", 50.0, 25.0, 5, "Cape Town", future, "16:00-18:00",
                ListingStatus.INACTIVE);
        assertFalse(inactive.isReservable(now));

        FoodListing expired = new FoodListing("4", "v1", "Bread", "d",
                "bakery", 50.0, 25.0, 5, "Cape Town", now - 1000L, "16:00-18:00",
                ListingStatus.ACTIVE);
        assertFalse(expired.isReservable(now));
        assertTrue(DateTimeUtil.isExpired(expired.getCollectionDeadlineEpoch(), now));
    }
}
