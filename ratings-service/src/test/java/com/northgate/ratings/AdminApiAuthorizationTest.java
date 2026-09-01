package com.northgate.ratings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminApiAuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void adminListingWithoutTheHeaderIsForbidden() throws Exception {
        mvc.perform(get("/api/admin/ratings"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListingWithAFalseHeaderIsForbidden() throws Exception {
        mvc.perform(get("/api/admin/ratings").header("X-Internal-Admin", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListingWithTheHeaderIsAllowed() throws Exception {
        mvc.perform(get("/api/admin/ratings").header("X-Internal-Admin", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void cacheFlushWithoutTheHeaderIsForbidden() throws Exception {
        mvc.perform(delete("/api/admin/cache"))
                .andExpect(status().isForbidden());
    }

    @Test
    void gradeOverrideWithoutTheHeaderIsForbiddenAndLeavesTheGradeAlone() throws Exception {
        mvc.perform(post("/api/admin/ratings/NG-1003/override").param("grade", "D"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/ratings/NG-1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value("AA-"));
    }

    @Test
    void publicRatingsLookupIsUnaffected() throws Exception {
        mvc.perform(get("/api/ratings/NG-1003"))
                .andExpect(status().isOk());
    }
}
