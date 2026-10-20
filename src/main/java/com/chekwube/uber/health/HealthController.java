package com.chekwube.uber.health;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "uber-backend", "timestamp", Instant.now().toString());
    }
}