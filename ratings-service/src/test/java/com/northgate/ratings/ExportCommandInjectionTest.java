package com.northgate.ratings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExportCommandInjectionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void shellMetacharactersInFormatAreRejectedAndNotExecuted() throws Exception {
        Path marker = Files.createTempDirectory("ng-cmdi").resolve("pwned");
        mvc.perform(post("/api/exports/run")
                        .param("format", "csv; touch " + marker)
                        .param("desk", "credit"))
                .andExpect(status().isBadRequest());
        assertFalse(new File(marker.toString()).exists(), "injected command was executed");
    }

    @Test
    void shellMetacharactersInDeskAreRejectedAndNotExecuted() throws Exception {
        Path marker = Files.createTempDirectory("ng-cmdi").resolve("pwned");
        mvc.perform(post("/api/exports/run")
                        .param("format", "csv")
                        .param("desk", "credit$(touch " + marker + ")"))
                .andExpect(status().isBadRequest());
        assertFalse(new File(marker.toString()).exists(), "injected command was executed");
    }

    @Test
    void unknownFormatIsRejected() throws Exception {
        mvc.perform(post("/api/exports/run").param("format", "exe"))
                .andExpect(status().isBadRequest());
    }
}
