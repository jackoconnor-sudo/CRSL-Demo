package com.northgate.ratings;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.Cookie;

import com.northgate.ratings.security.SessionCookieCodec;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfiguredCredentialsTest {

    private static final String FABRICATED_TOKEN = "ngw_live_8f3c21bb94d24e7ba0c15ee7d3f10a92";

    @Test
    void noCredentialLiteralsRemainInSourceOrImage() throws Exception {
        for (String relative : new String[] {
                "src/main/resources/application.yml",
                "src/main/java/com/northgate/ratings/integration/WarehouseClient.java",
                "src/main/java/com/northgate/ratings/controller/SessionController.java",
                "Dockerfile"}) {
            String text = new String(Files.readAllBytes(Paths.get(relative)), StandardCharsets.UTF_8);
            assertFalse(text.contains(FABRICATED_TOKEN), relative + " still contains the warehouse token");
            assertFalse(text.contains("Wareh0use-2019!"), relative + " still contains the warehouse password");
            assertFalse(text.contains("9f5c9912e7cbc0a4e2a0a2d1b8b2d2b2"), relative + " still contains the ops hash");
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    class WithoutOpsHash {
        @Autowired
        private MockMvc mvc;

        @Test
        void nobodyIsAdminWhenNoHashIsConfigured() throws Exception {
            assertFalse(adminAfterLogin(mvc, "ops-pw"));
        }
    }

    @Nested
    @SpringBootTest(properties = "northgate.ops.console-password-hash=0de3bf8e97918ae65f81219cd41e92e4")
    @AutoConfigureMockMvc
    class WithOpsHash {
        @Autowired
        private MockMvc mvc;

        @Test
        void configuredHashGrantsAdmin() throws Exception {
            assertTrue(adminAfterLogin(mvc, "ops-pw"));
            assertFalse(adminAfterLogin(mvc, "wrong"));
        }
    }

    private static boolean adminAfterLogin(MockMvc mvc, String password) throws Exception {
        Cookie cookie = mvc.perform(post("/api/session/login").param("user", "ops").param("password", password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SessionCookieCodec.COOKIE_NAME);
        String body = mvc.perform(get("/api/session/whoami").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.contains("\"admin\":true");
    }
}
