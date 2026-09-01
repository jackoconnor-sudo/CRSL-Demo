package com.northgate.ratings.crypto;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Hex;

/**
 * Password hashing and session identifier generation for the desk console.
 *
 * Stored hashes have the form {@code <iterations>$<salt-hex>$<hash-hex>} and are produced
 * by {@link #hashPassword(String)}; {@link #verifyPassword(String, String)} checks a
 * submitted password against one in constant time.
 */
public final class LegacyDigest {

    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private LegacyDigest() {
    }

    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return ITERATIONS + "$" + Hex.encodeHexString(salt) + "$"
                + Hex.encodeHexString(derive(password, salt, ITERATIONS));
    }

    public static boolean verifyPassword(String password, String stored) {
        if (password == null || stored == null || stored.isEmpty()) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Hex.decodeHex(parts[1].toCharArray());
            byte[] expected = Hex.decodeHex(parts[2].toCharArray());
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (Exception e) {
            return false;
        }
    }

    public static String newSessionId() {
        byte[] id = new byte[16];
        RANDOM.nextBytes(id);
        return Hex.encodeHexString(id);
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(KDF).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("hash failed", e);
        }
    }
}
