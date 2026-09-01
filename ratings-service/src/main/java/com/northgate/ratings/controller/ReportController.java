package com.northgate.ratings.controller;

import java.util.List;
import java.util.Map;

import com.northgate.ratings.report.ReportQueryBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final JdbcTemplate jdbc;

    public ReportController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/rollup")
    public List<Map<String, Object>> rollup(@RequestParam(value = "by", defaultValue = "SECTOR") String by,
                                            @RequestParam(value = "window", defaultValue = "LAST_30_DAYS") String window) {
        ReportQueryBuilder.Dimension dimension = ReportQueryBuilder.Dimension.valueOf(by.toUpperCase());
        ReportQueryBuilder.Window w = ReportQueryBuilder.Window.valueOf(window.toUpperCase());
        return jdbc.queryForList(ReportQueryBuilder.rollup(dimension, w));
    }

    @GetMapping("/projection")
    public List<Map<String, Object>> projection(@RequestParam("columns") String columns,
                                                @RequestParam(value = "window", defaultValue = "ALL_TIME") String window) {
        ReportQueryBuilder.Window w = ReportQueryBuilder.Window.valueOf(window.toUpperCase());
        return jdbc.queryForList(ReportQueryBuilder.projection(columns.split(","), w));
    }

    @GetMapping("/coverage")
    public List<Map<String, Object>> coverage(@RequestParam(value = "by", defaultValue = "GRADE") String by) {
        ReportQueryBuilder.Dimension dimension = ReportQueryBuilder.Dimension.valueOf(by.toUpperCase());
        return jdbc.queryForList(ReportQueryBuilder.coverage(dimension));
    }
}
