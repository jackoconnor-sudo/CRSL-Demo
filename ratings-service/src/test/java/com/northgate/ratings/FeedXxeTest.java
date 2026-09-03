package com.northgate.ratings;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeedXxeTest {

    private static final String FEED = "<feed><issuer><id>NG-9</id><name>Acme</name><grade>A</grade>"
            + "<outlook>stable</outlook><sector>Utilities</sector><reviewed>2026-01-01</reviewed></issuer></feed>";

    @Autowired
    private MockMvc mvc;

    @Test
    void plainFeedStillIngests() throws Exception {
        mvc.perform(post("/api/feed/xml").contentType(MediaType.APPLICATION_XML).content(FEED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].issuerId").value("NG-9"));
    }

    @Test
    void externalEntityInIngestIsRejected() throws Exception {
        Path secret = Files.createTempFile("ng-xxe", ".txt");
        Files.write(secret, "TOP-SECRET".getBytes(StandardCharsets.UTF_8));
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE feed [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>"
                + "<feed><issuer><id>&xxe;</id></issuer></feed>";
        mvc.perform(post("/api/feed/xml").contentType(MediaType.APPLICATION_XML).content(xxe))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("TOP-SECRET"))));
    }

    @Test
    void externalEntityInNormaliseIsRejected() throws Exception {
        Path secret = Files.createTempFile("ng-xxe", ".txt");
        Files.write(secret, "TOP-SECRET".getBytes(StandardCharsets.UTF_8));
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE feed [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>"
                + "<feed>&xxe;</feed>";
        mvc.perform(post("/api/feed/xml/normalise").contentType(MediaType.APPLICATION_XML).content(xxe))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("TOP-SECRET"))));
    }
}
