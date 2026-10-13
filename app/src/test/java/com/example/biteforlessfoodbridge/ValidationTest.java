package com.example.biteforlessfoodbridge;

import static org.junit.Assert.*;

import com.example.biteforlessfoodbridge.util.Validation;

import org.junit.Test;

public class ValidationTest {

    @Test
    public void email_validAddresses_pass() {
        assertTrue(Validation.isEmail("user@example.com"));
        assertTrue(Validation.isEmail("a.b@mail.co.za"));
    }

    @Test
    public void email_invalidAddresses_fail() {
        assertFalse(Validation.isEmail(""));
        assertFalse(Validation.isEmail("not-an-email"));
        assertFalse(Validation.isEmail("user@"));
        assertNotNull(Validation.emailError("bad"));
        assertNull(Validation.emailError("user@example.com"));
    }

    @Test
    public void password_minLength_enforced() {
        assertTrue(Validation.isPasswordLengthOk("123456", 6));
        assertFalse(Validation.isPasswordLengthOk("12345", 6));
        assertFalse(Validation.isPasswordLengthOk(null, 6));
    }

    @Test
    public void pricePair_discountMustBeBelowOriginal() {
        assertTrue(Validation.isPricePairValid(50.0, 25.0));
        assertFalse(Validation.isPricePairValid(0.0, 0.0));
        assertFalse(Validation.isPricePairValid(50.0, 50.0));
        assertFalse(Validation.isPricePairValid(50.0, 60.0));
        assertFalse(Validation.isPricePairValid(50.0, -1.0));
    }

    @Test
    public void quantity_mustBeWithinStock() {
        assertTrue(Validation.isQuantityValid(1, 5));
        assertTrue(Validation.isQuantityValid(5, 5));
        assertFalse(Validation.isQuantityValid(0, 5));
        assertFalse(Validation.isQuantityValid(6, 5));
        assertFalse(Validation.isQuantityValid(1, 0));
    }

    @Test
    public void rating_betweenOneAndFive() {
        assertTrue(Validation.isRatingValid(1));
        assertTrue(Validation.isRatingValid(5));
        assertFalse(Validation.isRatingValid(0));
        assertFalse(Validation.isRatingValid(6));
    }
}
