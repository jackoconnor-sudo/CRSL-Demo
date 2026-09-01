package com.northgate.ratings.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    private static final String EXPORT_SCRIPT = "/opt/northgate/bin/export.sh";
    private static final Set<String> FORMATS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("csv", "json", "xml")));
    private static final Pattern DESK = Pattern.compile("[a-z][a-z0-9_-]{0,31}");
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Pattern SUBDIR = Pattern.compile("(?:[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*)?");

    private final Path exportDir;

    public ExportController(@Value("${northgate.export.dir:/var/northgate/exports}") String exportDir) {
        this.exportDir = Paths.get(exportDir).toAbsolutePath().normalize();
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(@RequestParam("format") String format,
                                                   @RequestParam(value = "desk", defaultValue = "credit") String desk) {
        if (!FORMATS.contains(format)) {
            throw new IllegalArgumentException("unsupported format");
        }
        if (!DESK.matcher(desk).matches()) {
            throw new IllegalArgumentException("invalid desk");
        }
        String[] argv = {EXPORT_SCRIPT, "--format", format, "--desk", desk};
        String command = String.join(" ", argv);
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Process process = new ProcessBuilder(argv).redirectErrorStream(true).start();
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
            result.put("error", "export helper failed");
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("name") String name) throws Exception {
        if (!FILE_NAME.matcher(name).matches()) {
            return ResponseEntity.notFound().build();
        }
        Path file = resolveInsideExportDir(name);
        if (file == null || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/list")
    public String[] list(@RequestParam(value = "subdir", defaultValue = "") String subdir) {
        if (!SUBDIR.matcher(subdir).matches()) {
            return new String[0];
        }
        Path dir = resolveInsideExportDir(subdir);
        if (dir == null) {
            return new String[0];
        }
        String[] names = dir.toFile().list();
        return names == null ? new String[0] : names;
    }

    private Path resolveInsideExportDir(String relative) {
        Path resolved = exportDir.resolve(relative).normalize();
        if (!resolved.startsWith(exportDir)) {
            return null;
        }
        try {
            if (Files.exists(resolved) && !resolved.toRealPath().startsWith(exportDir.toRealPath())) {
                return null;
            }
        } catch (java.io.IOException e) {
            return null;
        }
        return resolved;
    }
}
