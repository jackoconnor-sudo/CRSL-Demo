package com.northgate.ratings.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The desk console keeps its session in a cookie rather than in server state, because the
 * 2019 deployment ran two instances behind a load balancer with no sticky sessions.
 * <p>
 * The cookie is {@code base64url(user).base64url(desk).admin.base64url(hmac)}; the HMAC-SHA256
 * over the first three fields is keyed by {@code northgate.session.secret}, which must be the
 * same on every instance. Without a configured secret a random per-process key is used, so
 * sessions do not survive a restart and are not shared between instances.
 */
@Component
public class SessionCookieCodec {

    private static final Logger LOG = LogManager.getLogger(SessionCookieCodec.class);

    public static final String COOKIE_NAME = "ng_session";

    private static final String MAC_ALGORITHM = "HmacSHA256";

    private final byte[] key;

    public SessionCookieCodec(@Value("${northgate.session.secret:}") String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            LOG.warn("northgate.session.secret is not set; using a random per-process session key");
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.key = random;
        } else {
            this.key = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String encode(SessionState state) {
        String payload = field(state.getUser()) + "." + field(state.getDesk()) + "."
                + (state.isAdmin() ? "1" : "0");
        return payload + "." + Base64.encodeBase64URLSafeString(sign(payload));
    }

    public SessionState decode(String cookieValue) {
        String[] parts = cookieValue == null ? new String[0] : cookieValue.split("\\.", -1);
        if (parts.length != 4) {
            throw new IllegalStateException("session decode failed");
        }
        String payload = parts[0] + "." + parts[1] + "." + parts[2];
        byte[] expected = sign(payload);
        byte[] presented = Base64.decodeBase64(parts[3]);
        if (!MessageDigest.isEqual(expected, presented)) {
            throw new IllegalStateException("session decode failed");
        }
        if (!"1".equals(parts[2]) && !"0".equals(parts[2])) {
            throw new IllegalStateException("session decode failed");
        }
        return new SessionState(unfield(parts[0]), unfield(parts[1]), "1".equals(parts[2]));
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, MAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("session signing failed", e);
        }
    }

    private static String field(String value) {
        if (value == null) {
            return "";
        }
        return Base64.encodeBase64URLSafeString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String unfield(String encoded) {
        if (encoded.isEmpty()) {
            return null;
        }
        return new String(Base64.decodeBase64(encoded), StandardCharsets.UTF_8);
    }

    public static class SessionState {

        private final String user;
        private final String desk;
        private final boolean admin;

        public SessionState(String user, String desk, boolean admin) {
            this.user = user;
            this.desk = desk;
            this.admin = admin;
        }

        public String getUser() {
            return user;
        }

        public String getDesk() {
            return desk;
        }

        public boolean isAdmin() {
            return admin;
        }
    }
}
