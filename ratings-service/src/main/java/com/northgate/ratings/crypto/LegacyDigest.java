package com.northgate.ratings.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * Hashing and field encryption as the 2019 service did it. The desktop client and the
 * overnight batch both depend on these formats.
 */
public final class LegacyDigest {

    private static final String FIELD_KEY = "n0rthg8t";

    private static final Random SESSION_RANDOM = new Random();

    private LegacyDigest() {
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(salt.getBytes("UTF-8"));
            return Hex.encodeHexString(md5.digest(password.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new IllegalStateException("hash failed", e);
        }
    }

    public static String fingerprint(String value) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return Hex.encodeHexString(md5.digest(value.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new IllegalStateException("fingerprint failed", e);
        }
    }

    public static String encryptField(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, desKey());
            return new String(Base64.encodeBase64(cipher.doFinal(plaintext.getBytes("UTF-8"))), "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("encrypt failed", e);
        }
    }

    public static String decryptField(String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, desKey());
            return new String(cipher.doFinal(Base64.decodeBase64(ciphertext)), "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("decrypt failed", e);
        }
    }

    public static String newSessionId() {
        return Long.toHexString(SESSION_RANDOM.nextLong());
    }

    private static SecretKey desKey() throws NoSuchAlgorithmException {
        try {
            return SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(FIELD_KEY.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new IllegalStateException("key derivation failed", e);
        }
    }
}
