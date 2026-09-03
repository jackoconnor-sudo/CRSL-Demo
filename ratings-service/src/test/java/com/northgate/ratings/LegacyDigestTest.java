package com.northgate.ratings;

import java.util.HashSet;
import java.util.Set;

import com.northgate.ratings.crypto.LegacyDigest;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyDigestTest {

    @AfterEach
    void clearKey() {
        System.clearProperty(LegacyDigest.FIELD_KEY_ENV);
    }

    @Test
    void passwordHashIsSaltedPbkdf2NotMd5() {
        String hash = LegacyDigest.hashPassword("pw", "alice");
        assertEquals(64, hash.length());
        assertEquals(hash, LegacyDigest.hashPassword("pw", "alice"));
        assertNotEquals(hash, LegacyDigest.hashPassword("pw", "bob"));
        assertNotEquals("9003d1df22eb4d3820015070385194c8", hash); // md5("pw")
    }

    @Test
    void fingerprintIsSha256() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                LegacyDigest.fingerprint("hello"));
    }

    @Test
    void fieldEncryptionRoundTripsWithFreshIvAndDetectsTampering() {
        System.setProperty(LegacyDigest.FIELD_KEY_ENV, Base64.encodeBase64String(new byte[32]));
        String a = LegacyDigest.encryptField("IBAN GB00");
        String b = LegacyDigest.encryptField("IBAN GB00");
        assertNotEquals(a, b);
        assertEquals("IBAN GB00", LegacyDigest.decryptField(a));
        byte[] raw = Base64.decodeBase64(a);
        raw[raw.length - 1] ^= 1;
        String tampered = Base64.encodeBase64String(raw);
        assertThrows(IllegalStateException.class, () -> LegacyDigest.decryptField(tampered));
    }

    @Test
    void fieldEncryptionRefusesToRunWithoutAKey() {
        assertThrows(IllegalStateException.class, () -> LegacyDigest.encryptField("x"));
    }

    @Test
    void sessionIdsAre128BitAndUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String id = LegacyDigest.newSessionId();
            assertEquals(32, id.length());
            seen.add(id);
        }
        assertEquals(1000, seen.size());
    }
}
