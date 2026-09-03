package com.northgate.ratings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "northgate.admin.token=test-admin-token")
@AutoConfigureMockMvc
class AdminAuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void adminListingWithoutHeaderIsForbidden() throws Exception {
        mvc.perform(get("/api/admin/ratings"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminHeaderTrueIsNotEnoughWhenTokenConfigured() throws Exception {
        mvc.perform(get("/api/admin/ratings").header("X-Internal-Admin", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cacheFlushWithWrongTokenIsForbidden() throws Exception {
        mvc.perform(delete("/api/admin/cache").header("X-Internal-Admin", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListingWithTokenIsAllowed() throws Exception {
        mvc.perform(get("/api/admin/ratings").header("X-Internal-Admin", "test-admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void publicRatingsAreUnaffected() throws Exception {
        mvc.perform(get("/api/ratings/NG-1003"))
                .andExpect(status().isOk());
    }
}
