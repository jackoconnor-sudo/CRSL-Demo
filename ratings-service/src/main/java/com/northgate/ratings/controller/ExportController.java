package com.northgate.ratings.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            LOG.warn("export run failed: " + e.getMessage());
            result.put("command", command);
            result.put("error", e.toString());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("name") String name) throws Exception {
        Path file = resolveInsideExportDir(name);
        if (file == null || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName().toString().replace("\"", "") + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/list")
    public String[] list(@RequestParam(value = "subdir", defaultValue = "") String subdir) {
        Path dir = resolveInsideExportDir(subdir);
        if (dir == null) {
            return new String[0];
        }
        String[] names = dir.toFile().list();
        return names == null ? new String[0] : names;
    }

    /**
     * Resolves a caller supplied relative path against the export directory and returns null
     * when the normalised result would escape it.
     */
    private Path resolveInsideExportDir(String relative) {
        if (relative == null || relative.indexOf('\0') >= 0) {
            return null;
        }
        Path root = Paths.get(exportDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            return null;
        }
        try {
            if (Files.exists(resolved)) {
                Path real = resolved.toRealPath();
                Path realRoot = root.toRealPath();
                if (!real.startsWith(realRoot)) {
                    return null;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return resolved;
    }
}
