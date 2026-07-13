package com.afra7kom.backend.controller;

import com.afra7kom.backend.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final AvailabilityService availabilityService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Pong!");
        response.put("timestamp", new java.util.Date());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fix-stock/{materielId}")
    public ResponseEntity<Map<String, Object>> fixStock(@PathVariable Long materielId) {
        try {
            Map<String, Object> result = availabilityService.fixMaterielStock(materielId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
