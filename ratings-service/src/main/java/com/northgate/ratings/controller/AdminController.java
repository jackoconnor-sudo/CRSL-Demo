package com.northgate.ratings.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.northgate.ratings.domain.Rating;
import com.northgate.ratings.integration.WarehouseClient;
import com.northgate.ratings.service.RatingsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal surface. Everything under /api/admin is expected to be reachable only from the
 * ops console; the check itself lives in
 * {@link com.northgate.ratings.config.AdminApiFilter}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final RatingsService service;
    private final WarehouseClient warehouse;

    public AdminController(RatingsService service, WarehouseClient warehouse) {
        this.service = service;
        this.warehouse = warehouse;
    }

    @GetMapping("/ratings")
    public List<Rating> all(@RequestParam(value = "q", defaultValue = "") String q) {
        return service.search(q, null);
    }

    @PostMapping("/ratings/{issuerId}/override")
    public Map<String, Object> override(@PathVariable String issuerId,
                                        @RequestParam("grade") String grade,
                                        @RequestParam(value = "outlook", defaultValue = "stable") String outlook) {
        boolean updated = service.updateGrade(issuerId, grade, outlook);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("issuerId", issuerId);
        body.put("updated", updated);
        return body;
    }

    @GetMapping("/warehouse/{issuerId}")
    public String dossier(@PathVariable String issuerId) {
        return warehouse.fetchIssuerDossier(issuerId);
    }

    @DeleteMapping("/cache")
    public Map<String, Object> flushCache() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("flushed", true);
        return body;
    }
}
