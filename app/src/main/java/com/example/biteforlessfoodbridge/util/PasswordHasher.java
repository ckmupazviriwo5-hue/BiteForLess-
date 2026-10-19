package com.example.biteforlessfoodbridge.util;

import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Secure password hashing utility using PBKDF2-HMAC-SHA256 with unique random salt.
 * Complies with professional security requirements (no plaintext or reversible encryption).
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordHasher() {}

    public static class HashResult {
        public final String hash;
        public final String salt;

        public HashResult(String hash, String salt) {
            this.hash = hash;
            this.salt = salt;
        }
    }

    public static HashResult hashPassword(String password) {
        try {
            byte[] saltBytes = new byte[16];
            new SecureRandom().nextBytes(saltBytes);
            String salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
            String hash = pbkdf2(password, saltBytes);
            return new HashResult(hash, salt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    public static boolean verify(String password, String expectedHash, String saltStr) {
        try {
            byte[] saltBytes = Base64.decode(saltStr, Base64.NO_WRAP);
            String computed = pbkdf2(password, saltBytes);
            return computed.equals(expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static String pbkdf2(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }
}
