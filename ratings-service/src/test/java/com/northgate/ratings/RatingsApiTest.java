package com.northgate.ratings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RatingsApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsAKnownIssuer() throws Exception {
        mvc.perform(get("/api/ratings/NG-1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuerName").value("Carrow Bank plc"))
                .andExpect(jsonPath("$.grade").value("AA-"));
    }

    @Test
    void unknownIssuerIsNotFound() throws Exception {
        mvc.perform(get("/api/ratings/NG-9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchMatchesOnPartialName() throws Exception {
        mvc.perform(get("/api/ratings/search").param("q", "Glenmoor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].issuerId").value("NG-1007"));
    }

    /**
     * Contract test for the 2019 desktop client, which reads the four values out of this
     * object positionally. The key set and the key order are both frozen.
     */
    @Test
    void updateGradeResponseShapeIsFrozen() throws Exception {
        mvc.perform(post("/api/ratings/NG-1002/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"BB+\",\"outlook\":\"negative\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"issuerId\":\"NG-1002\",\"grade\":\"BB+\","
                        + "\"outlook\":\"negative\",\"status\":\"OK\"}", true))
                .andExpect(content().string(
                        "{\"issuerId\":\"NG-1002\",\"grade\":\"BB+\",\"outlook\":\"negative\",\"status\":\"OK\"}"));
    }

    @Test
    void updateGradeOnUnknownIssuerIsNotFound() throws Exception {
        mvc.perform(post("/api/ratings/NG-9999/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"BB+\",\"outlook\":\"negative\"}"))
                .andExpect(status().isNotFound());
    }
}
