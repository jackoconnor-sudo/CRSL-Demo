package com.northgate.ratings;

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
class ErrorDisclosureTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void parseFailureDoesNotLeakInternals() throws Exception {
        mvc.perform(post("/api/feed/xml").contentType(MediaType.APPLICATION_XML).content("<feed><unclosed>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/feed/xml"))
                .andExpect(jsonPath("$.message").value("bad request"))
                .andExpect(jsonPath("$.errorId").isString())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.northgate"))))
                .andExpect(content().string(not(containsString("\tat "))));
    }
}
