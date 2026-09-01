package com.northgate.ratings;

import javax.servlet.http.Cookie;

import com.northgate.ratings.crypto.LegacyDigest;
import com.northgate.ratings.feed.LegacyFeedParser;
import com.northgate.ratings.security.SessionCookieCodec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "northgate.session.secret=dGVzdC1zZXNzaW9uLXNlY3JldC0wMTIzNDU2Nzg5YWJjZGVm",
        "northgate.export.dir=${java.io.tmpdir}/ng-exports-test"
})
class SecurityRemediationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SessionCookieCodec codec;

    @Test
    void searchWithSqlMetaCharactersIsTreatedAsData() throws Exception {
        mvc.perform(get("/api/ratings/search").param("q", "%' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mvc.perform(get("/api/ratings/by-grade").param("grades", "AA-') OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void feedParserRejectsDoctypeDeclarations() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE feed [<!ENTITY xxe SYSTEM \"file:///etc/hostname\">]>"
                + "<feed><issuer><id>&xxe;</id></issuer></feed>";
        LegacyFeedParser parser = new LegacyFeedParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(xxe));
        assertThrows(IllegalArgumentException.class, () -> parser.echoNormalised(xxe));
    }

    @Test
    void sessionCookieIsSignedAndTamperProof() {
        String encoded = codec.encode(new SessionCookieCodec.SessionState("alice", "credit", false));
        assertFalse(codec.decode(encoded).isAdmin());

        String forgedPayload = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(
                "alice|credit|true".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String forged = forgedPayload + encoded.substring(encoded.indexOf('.'));
        assertThrows(IllegalStateException.class, () -> codec.decode(forged));
        assertThrows(IllegalStateException.class, () -> codec.decode("rO0ABXNyABZjb20ubm9ydGhnYXRl"));
    }

    @Test
    void adminHeaderNoLongerGrantsAccess() throws Exception {
        mvc.perform(get("/api/admin/ratings").header("X-Internal-Admin", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/ratings"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedAdminSessionIsAccepted() throws Exception {
        String admin = codec.encode(new SessionCookieCodec.SessionState("ops", "credit", true));
        mvc.perform(get("/api/admin/ratings").cookie(new Cookie(SessionCookieCodec.COOKIE_NAME, admin)))
                .andExpect(status().isOk());
    }

    @Test
    void loginIssuesHardenedCookieWithoutAdminWhenNoOpsHashIsConfigured() throws Exception {
        MvcResult result = mvc.perform(post("/api/session/login").param("user", "alice").param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(SessionCookieCodec.COOKIE_NAME, true))
                .andExpect(cookie().secure(SessionCookieCodec.COOKIE_NAME, true))
                .andReturn();
        Cookie session = result.getResponse().getCookie(SessionCookieCodec.COOKIE_NAME);
        assertFalse(codec.decode(session.getValue()).isAdmin());

        mvc.perform(get("/api/session/whoami").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("alice"))
                .andExpect(jsonPath("$.admin").value(false));
    }

    @Test
    void malformedSessionCookieIsTreatedAsAnonymous() throws Exception {
        mvc.perform(get("/api/session/whoami").cookie(new Cookie(SessionCookieCodec.COOKIE_NAME, "rO0ABXNy")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"user\":null}"));
    }

    @Test
    void passwordHashIsSaltedAndVerifiable() {
        String hash = LegacyDigest.hashPassword("ops-console-secret");
        assertTrue(hash.startsWith("120000$"));
        assertNotEquals(hash, LegacyDigest.hashPassword("ops-console-secret"));
        assertTrue(LegacyDigest.verifyPassword("ops-console-secret", hash));
        assertFalse(LegacyDigest.verifyPassword("other", hash));
        assertFalse(LegacyDigest.verifyPassword("anything", ""));
        assertEquals(32, LegacyDigest.newSessionId().length());
    }

    @Test
    void exportRunRejectsShellMetaCharacters() throws Exception {
        mvc.perform(post("/api/exports/run").param("format", "csv; id"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/exports/run").param("format", "csv").param("desk", "../../tmp/x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportPathsCannotEscapeTheExportDirectory() throws Exception {
        mvc.perform(get("/api/exports/download").param("name", "../../../etc/passwd"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/exports/download").param("name", "..%2F..%2Fetc%2Fpasswd"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/exports/list").param("subdir", "../../.."))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void h2ConsoleAndActuatorEnvAreNotExposed() throws Exception {
        mvc.perform(get("/h2-console")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }
}
