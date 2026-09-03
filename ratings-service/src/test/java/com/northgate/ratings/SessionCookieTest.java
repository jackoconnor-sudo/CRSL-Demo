package com.northgate.ratings;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.servlet.http.Cookie;

import com.northgate.ratings.security.SessionCookieCodec;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionCookieTest {

    @Autowired
    private MockMvc mvc;

    /** Anything Serializable stands in for a gadget chain. */
    static class Hostile implements Serializable {
        private static final long serialVersionUID = 1L;
        boolean admin = true;
    }

    @Test
    void loginRoundTripsThroughWhoami() throws Exception {
        MvcResult login = mvc.perform(post("/api/session/login")
                        .param("user", "alice").param("password", "pw").param("desk", "rates"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = login.getResponse().getCookie(SessionCookieCodec.COOKIE_NAME);
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        mvc.perform(get("/api/session/whoami").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("alice"))
                .andExpect(jsonPath("$.desk").value("rates"))
                .andExpect(jsonPath("$.admin").value(false));
    }

    @Test
    void serializedJavaObjectInCookieIsRejected() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new Hostile());
        }
        String forged = Base64.encodeBase64String(bytes.toByteArray());
        mvc.perform(get("/api/session/whoami").cookie(new Cookie(SessionCookieCodec.COOKIE_NAME, forged)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void flippingTheAdminFlagBreaksTheSignature() throws Exception {
        MvcResult login = mvc.perform(post("/api/session/login")
                        .param("user", "bob").param("password", "pw"))
                .andReturn();
        String value = login.getResponse().getCookie(SessionCookieCodec.COOKIE_NAME).getValue();
        String[] parts = value.split("\\.");
        parts[2] = "1";
        String tampered = String.join(".", parts);
        mvc.perform(get("/api/session/whoami").cookie(new Cookie(SessionCookieCodec.COOKIE_NAME, tampered)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void whoamiWithoutCookieIsAnonymous() throws Exception {
        mvc.perform(get("/api/session/whoami"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").isEmpty());
    }
}
