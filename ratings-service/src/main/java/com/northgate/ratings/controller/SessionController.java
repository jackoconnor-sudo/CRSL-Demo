package com.northgate.ratings.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.northgate.ratings.crypto.LegacyDigest;
import com.northgate.ratings.security.SessionCookieCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionCookieCodec codec;
    private final String opsConsolePasswordHash;

    public SessionController(SessionCookieCodec codec,
                             @Value("${northgate.ops-console.password-hash:}") String opsConsolePasswordHash) {
        this.codec = codec;
        this.opsConsolePasswordHash = opsConsolePasswordHash;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam("user") String user,
                                                     @RequestParam("password") String password,
                                                     @RequestParam(value = "desk", defaultValue = "credit") String desk,
                                                     HttpServletResponse response) {
        boolean admin = LegacyDigest.verifyPassword(password, opsConsolePasswordHash);

        SessionCookieCodec.SessionState state = new SessionCookieCodec.SessionState(user, desk, admin);
        Cookie cookie = new Cookie(SessionCookieCodec.COOKIE_NAME, codec.encode(state));
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 12);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("desk", desk);
        body.put("sessionId", LegacyDigest.newSessionId());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/whoami")
    public Map<String, Object> whoami(@CookieValue(value = SessionCookieCodec.COOKIE_NAME, required = false) String cookie) {
        Map<String, Object> body = new LinkedHashMap<>();
        SessionCookieCodec.SessionState state = null;
        if (cookie != null) {
            try {
                state = codec.decode(cookie);
            } catch (IllegalStateException e) {
                state = null;
            }
        }
        if (state == null) {
            body.put("user", null);
            return body;
        }
        body.put("user", state.getUser());
        body.put("desk", state.getDesk());
        body.put("admin", state.isAdmin());
        return body;
    }
}
