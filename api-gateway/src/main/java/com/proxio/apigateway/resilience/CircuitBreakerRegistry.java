package com.proxio.apigateway.resilience;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CircuitBreakerRegistry {

    private final Map<String, CircuitState> states = new ConcurrentHashMap<>();
    private final int failureThreshold;
    private final Duration openDuration;

    public CircuitBreakerRegistry(@Value("${gateway.resilience.circuit-breaker.failure-threshold:3}") int failureThreshold,
                                  @Value("${gateway.resilience.circuit-breaker.open-seconds:20}") long openSeconds) {
        this.failureThreshold = failureThreshold;
        this.openDuration = Duration.ofSeconds(openSeconds);
    }

    public boolean allowsRequest(String serviceId) {
        CircuitState state = states.get(serviceId);
        if (state == null || !state.open()) {
            return true;
        }
        return Instant.now().isAfter(state.openedAt().plus(openDuration));
    }

    public void recordSuccess(String serviceId) {
        states.remove(serviceId);
    }

    public void recordFailure(String serviceId) {
        states.compute(serviceId, (ignored, current) -> {
            int failures = current == null ? 1 : current.failures() + 1;
            boolean open = failures >= failureThreshold;
            return new CircuitState(failures, open, open ? Instant.now() : null);
        });
    }

    public Map<String, CircuitState> snapshot() {
        return Map.copyOf(states);
    }

    public record CircuitState(int failures, boolean open, Instant openedAt) {
    }
}
