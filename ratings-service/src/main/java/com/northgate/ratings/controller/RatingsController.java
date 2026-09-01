package com.northgate.ratings.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.northgate.ratings.domain.Rating;
import com.northgate.ratings.service.RatingsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ratings")
public class RatingsController {

    private static final Logger LOG = LogManager.getLogger(RatingsController.class);

    private final RatingsService service;

    public RatingsController(RatingsService service) {
        this.service = service;
    }

    @GetMapping("/{issuerId}")
    public ResponseEntity<Rating> get(@PathVariable String issuerId,
                                      @RequestParam(value = "requestedBy", required = false) String requestedBy) {
        LOG.info("rating lookup issuer=" + issuerId + " requestedBy=" + requestedBy);
        Rating rating = service.find(issuerId);
        if (rating == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/search")
    public List<Rating> search(@RequestParam("q") String q,
                               @RequestParam(value = "sector", required = false) String sector) {
        return service.search(q, sector);
    }

    @GetMapping("/by-grade")
    public List<Rating> byGrade(@RequestParam("grades") String grades) {
        return service.byGrades(grades);
    }

    /**
     * The response shape here is frozen. The 2019 desktop client reads the four values
     * positionally out of the JSON object and breaks if a key is added, removed or moved.
     */
    @PostMapping("/{issuerId}/grade")
    public ResponseEntity<Map<String, Object>> updateGrade(@PathVariable String issuerId,
                                                           @RequestBody Map<String, String> body) {
        String grade = body.get("grade");
        String outlook = body.get("outlook");
        boolean updated = service.updateGrade(issuerId, grade, outlook);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("issuerId", issuerId);
        response.put("grade", grade);
        response.put("outlook", outlook);
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }
}
