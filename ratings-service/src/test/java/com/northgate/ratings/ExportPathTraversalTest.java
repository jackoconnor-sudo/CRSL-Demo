package com.northgate.ratings;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "northgate.export.dir=${java.io.tmpdir}/ng-export-test/exports")
@AutoConfigureMockMvc
class ExportPathTraversalTest {

    private static final Path BASE = Paths.get(System.getProperty("java.io.tmpdir"), "ng-export-test");

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    static void layOutFiles() throws Exception {
        Files.createDirectories(BASE.resolve("exports/credit"));
        Files.write(BASE.resolve("exports/credit-20260101.csv"), "a,b\n".getBytes(StandardCharsets.UTF_8));
        Files.write(BASE.resolve("outside.txt"), "secret".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void downloadsAFileInsideTheExportDirectory() throws Exception {
        mvc.perform(get("/api/exports/download").param("name", "credit-20260101.csv"))
                .andExpect(status().isOk())
                .andExpect(content().string("a,b\n"));
    }

    @Test
    void dotDotDownloadIsNotFound() throws Exception {
        mvc.perform(get("/api/exports/download").param("name", "../outside.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void absolutePathDownloadIsNotFound() throws Exception {
        mvc.perform(get("/api/exports/download").param("name", BASE.resolve("outside.txt").toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listStaysInsideTheExportDirectory() throws Exception {
        mvc.perform(get("/api/exports/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("credit-20260101.csv")));
        mvc.perform(get("/api/exports/list").param("subdir", "credit"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/exports/list").param("subdir", ".."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
