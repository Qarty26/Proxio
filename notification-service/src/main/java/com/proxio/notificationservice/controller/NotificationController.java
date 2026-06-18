package com.proxio.notificationservice.controller;

import com.proxio.notificationservice.model.NotificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, Object>>> templates() {
        return ResponseEntity.ok(List.of(
                Map.of("id", "welcome", "channel", "email", "enabled", true),
                Map.of("id", "weekly-offer", "channel", "push", "enabled", true),
                Map.of("id", "pickup-reminder", "channel", "sms", "enabled", false)
        ));
    }

    @GetMapping("/ops/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "notification-service",
                "status", "ready",
                "checkedAt", Instant.now().toString()
        ));
    }

    @PostMapping("/send-test")
    public ResponseEntity<Map<String, Object>> sendTest(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "queued",
                "recipient", request.recipient(),
                "channel", request.channel(),
                "message", request.message(),
                "queuedAt", Instant.now().toString()
        ));
    }
}
