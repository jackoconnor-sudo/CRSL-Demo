package com.northgate.ratings.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

/**
 * The desk console keeps its session in a cookie rather than in server state, because the
 * 2019 deployment ran two instances behind a load balancer with no sticky sessions.
 */
@Component
public class SessionCookieCodec {

    public static final String COOKIE_NAME = "ng_session";

    public String encode(SessionState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(state);
            out.close();
            return new String(Base64.encodeBase64(bytes.toByteArray()), "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("session encode failed", e);
        }
    }

    public SessionState decode(String cookieValue) {
        try {
            byte[] raw = Base64.decodeBase64(cookieValue);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(raw));
            return (SessionState) in.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("session decode failed", e);
        }
    }

    public static class SessionState implements Serializable {

        private static final long serialVersionUID = 20190716L;

        private String user;
        private String desk;
        private boolean admin;

        public SessionState() {
        }

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
