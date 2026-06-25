package com.proxio.backend.controllers;

import com.proxio.backend.models.Subscription;
import com.proxio.backend.services.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<Subscription> create(@Valid @RequestBody Subscription subscription) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(subscription));
    }

    @GetMapping
    public ResponseEntity<List<Subscription>> getAll() {
        return ResponseEntity.ok(subscriptionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subscription> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Subscription> update(@PathVariable Long id, @Valid @RequestBody Subscription subscription) {
        return ResponseEntity.ok(subscriptionService.update(id, subscription));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
