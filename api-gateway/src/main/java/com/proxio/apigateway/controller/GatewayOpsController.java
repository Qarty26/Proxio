package com.proxio.apigateway.controller;

import com.proxio.apigateway.resilience.CircuitBreakerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GatewayOpsController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public GatewayOpsController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/gateway/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "api-gateway"));
    }

    @GetMapping("/gateway/circuit-breakers")
    public ResponseEntity<Map<String, CircuitBreakerRegistry.CircuitState>> circuitBreakers() {
        return ResponseEntity.ok(circuitBreakerRegistry.snapshot());
    }
}
