package com.northgate.ratings.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.northgate.ratings.report.ReportQueryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * The jobs the scheduler triggers overnight. Nothing here takes a value from a request:
 * the command is a constant, the snapshot path is a server generated UUID under a fixed
 * directory, and the only value that reaches the log is the name of an enum constant.
 */
@Service
public class NightlyJobRunner {

    private static final Logger LOG = LogManager.getLogger(NightlyJobRunner.class);

    private static final String SNAPSHOT_DIR = "/var/northgate/snapshots";

    private final JdbcTemplate jdbc;

    public NightlyJobRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int refreshWarehouseCache() throws Exception {
        Process process = Runtime.getRuntime().exec(new String[] {"/opt/northgate/bin/refresh-cache.sh"});
        return process.waitFor();
    }

    public String writeSnapshot(ReportQueryBuilder.Dimension dimension) throws Exception {
        LOG.info("nightly snapshot starting for dimension " + dimension.name());
        List<Map<String, Object>> rows = jdbc.queryForList(
                ReportQueryBuilder.rollup(dimension, ReportQueryBuilder.Window.LAST_30_DAYS));
        File target = new File(SNAPSHOT_DIR, UUID.randomUUID().toString() + ".txt");
        StringBuilder body = new StringBuilder();
        for (Map<String, Object> row : rows) {
            body.append(row.toString()).append('\n');
        }
        Files.write(target.toPath(), body.toString().getBytes(StandardCharsets.UTF_8));
        return target.getAbsolutePath();
    }
}
