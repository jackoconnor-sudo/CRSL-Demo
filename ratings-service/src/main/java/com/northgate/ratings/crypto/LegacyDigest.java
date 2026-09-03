package com.northgate.ratings.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * Hashing and field encryption. Password hashes are PBKDF2-HMAC-SHA256, fingerprints are
 * SHA-256, field encryption is AES-256-GCM keyed from NORTHGATE_FIELD_KEY (base64, 32 bytes)
 * and session identifiers come from SecureRandom.
 */
public final class LegacyDigest {

    public static final String FIELD_KEY_ENV = "NORTHGATE_FIELD_KEY";

    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private LegacyDigest() {
    }

    public static String hashPassword(String password, String salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8),
                    PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("hash failed", e);
        }
    }

    public static String fingerprint(String value) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return Hex.encodeHexString(sha256.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("fingerprint failed", e);
        }
    }

    /** Returns base64(iv || ciphertext || tag); a fresh IV is drawn for every call. */
    public static String encryptField(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, fieldKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + sealed.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(sealed, 0, out, iv.length, sealed.length);
            return Base64.encodeBase64String(out);
        } catch (Exception e) {
            throw new IllegalStateException("encrypt failed", e);
        }
    }

    public static String decryptField(String ciphertext) {
        try {
            byte[] raw = Base64.decodeBase64(ciphertext);
            if (raw.length <= GCM_IV_BYTES) {
                throw new IllegalArgumentException("ciphertext too short");
            }
            byte[] iv = Arrays.copyOfRange(raw, 0, GCM_IV_BYTES);
            byte[] sealed = Arrays.copyOfRange(raw, GCM_IV_BYTES, raw.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, fieldKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(sealed), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decrypt failed", e);
        }
    }

    public static String newSessionId() {
        byte[] id = new byte[16];
        RANDOM.nextBytes(id);
        return Hex.encodeHexString(id);
    }

    private static SecretKey fieldKey() {
        String encoded = System.getProperty(FIELD_KEY_ENV, System.getenv(FIELD_KEY_ENV));
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalStateException(FIELD_KEY_ENV + " is not configured");
        }
        byte[] key = Base64.decodeBase64(encoded);
        if (key.length != 32) {
            throw new IllegalStateException(FIELD_KEY_ENV + " must decode to 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }
}
