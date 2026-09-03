package com.northgate.ratings.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Clients get a fixed message and a correlation id; the exception itself, with its stack
 * trace, goes to the server log under that id.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(body("bad request", e, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> failure(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(500).body(body("internal error", e, request));
    }

    private Map<String, Object> body(String message, Exception e, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        LOG.error("request {} {} failed, errorId={}", request.getMethod(), request.getRequestURI(), errorId, e);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("path", request.getRequestURI());
        body.put("message", message);
        body.put("errorId", errorId);
        return body;
    }
}
