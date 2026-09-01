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
 *
 * The cookie is {@code base64url(user|desk|admin) + "." + base64url(hmac)}. Every replica
 * must share {@code northgate.session.secret} or sessions issued by one will be rejected
 * by the others.
 */
@Component
public class SessionCookieCodec {

    private static final Logger LOG = LogManager.getLogger(SessionCookieCodec.class);

    public static final String COOKIE_NAME = "ng_session";

    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final char FIELD_SEPARATOR = '|';

    private final byte[] key;

    public SessionCookieCodec(@Value("${northgate.session.secret:}") String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            LOG.warn("northgate.session.secret is not set; using a per-instance key, sessions will not survive a "
                    + "restart or span replicas");
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            this.key = generated;
        } else {
            this.key = Base64.decodeBase64(secret.trim());
        }
    }

    public String encode(SessionState state) {
        String payload = field(state.getUser()) + FIELD_SEPARATOR + field(state.getDesk()) + FIELD_SEPARATOR
                + state.isAdmin();
        String encodedPayload = Base64.encodeBase64URLSafeString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + Base64.encodeBase64URLSafeString(sign(encodedPayload));
    }

    public SessionState decode(String cookieValue) {
        try {
            int dot = cookieValue.indexOf('.');
            if (dot <= 0 || dot == cookieValue.length() - 1) {
                throw new IllegalArgumentException("malformed session");
            }
            String encodedPayload = cookieValue.substring(0, dot);
            byte[] presented = Base64.decodeBase64(cookieValue.substring(dot + 1));
            if (!MessageDigest.isEqual(sign(encodedPayload), presented)) {
                throw new IllegalArgumentException("session signature mismatch");
            }
            String payload = new String(Base64.decodeBase64(encodedPayload), StandardCharsets.UTF_8);
            String[] fields = payload.split("\\" + FIELD_SEPARATOR, -1);
            if (fields.length != 3) {
                throw new IllegalArgumentException("malformed session");
            }
            return new SessionState(fields[0], fields[1], Boolean.parseBoolean(fields[2]));
        } catch (Exception e) {
            throw new IllegalStateException("session decode failed", e);
        }
    }

    private static String field(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(FIELD_SEPARATOR) >= 0) {
            throw new IllegalArgumentException("session field may not contain '" + FIELD_SEPARATOR + "'");
        }
        return value;
    }

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, MAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("session signing failed", e);
        }
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
