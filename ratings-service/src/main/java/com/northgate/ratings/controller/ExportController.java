package com.northgate.ratings.controller;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nightly export files are written by a shell helper that predates the service and is
 * still owned by the batch team.
 */
@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private static final Logger LOG = LogManager.getLogger(ExportController.class);

    private final String exportDir;

    public ExportController(@Value("${northgate.export.dir:/var/northgate/exports}") String exportDir) {
        this.exportDir = exportDir;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(@RequestParam("format") String format,
                                                   @RequestParam(value = "desk", defaultValue = "credit") String desk) {
        String command = "/opt/northgate/bin/export.sh --format " + format + " --desk " + desk;
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            result.put("command", command);
            result.put("exitCode", process.waitFor());
            result.put("output", output.toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOG.warn("export run failed: {}", e.getMessage());
            result.put("command", command);
            result.put("error", e.toString());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("name") String name) throws Exception {
        File file = new File(exportDir + "/" + name);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/list")
    public String[] list(@RequestParam(value = "subdir", defaultValue = "") String subdir) {
        File dir = new File(exportDir + "/" + subdir);
        String[] names = dir.list();
        return names == null ? new String[0] : names;
    }
}
