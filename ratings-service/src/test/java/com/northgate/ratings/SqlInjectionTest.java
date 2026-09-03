package com.northgate.ratings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SqlInjectionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void searchTautologyDoesNotDumpTheTable() throws Exception {
        mvc.perform(get("/api/ratings/search").param("q", "zzz%' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sectorTautologyDoesNotWidenTheSearch() throws Exception {
        mvc.perform(get("/api/ratings/search").param("q", "Carrow").param("sector", "x' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void issuerIdTautologyIsNotFound() throws Exception {
        mvc.perform(get("/api/ratings/NG-9999' OR '1'='1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void gradeListTautologyIsTreatedAsLiteralGrades() throws Exception {
        mvc.perform(get("/api/ratings/by-grade").param("grades", "ZZZ') OR ('1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void gradeListStillMatchesRealGrades() throws Exception {
        mvc.perform(get("/api/ratings/by-grade").param("grades", "AA-, A-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void updateGradeWithInjectedIssuerTouchesNoOtherRows() throws Exception {
        mvc.perform(post("/api/ratings/NG-9998' OR '1'='1/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"D\",\"outlook\":\"negative\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/ratings/NG-1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value("AA-"));
    }
}
