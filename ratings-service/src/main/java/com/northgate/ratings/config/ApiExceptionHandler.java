package com.northgate.ratings.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(body(e, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> failure(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(500).body(body(e, request));
    }

    private Map<String, Object> body(Exception e, HttpServletRequest request) {
        StringWriter trace = new StringWriter();
        e.printStackTrace(new PrintWriter(trace));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("path", request.getRequestURI());
        body.put("message", e.getMessage());
        body.put("trace", trace.toString());
        return body;
    }
}
