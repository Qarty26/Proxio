package com.proxio.backend.controllers;

import com.proxio.backend.models.UserRating;
import com.proxio.backend.services.UserRatingService;
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
@RequestMapping("/api/user-ratings")
public class UserRatingController {

    private final UserRatingService userRatingService;

    public UserRatingController(UserRatingService userRatingService) {
        this.userRatingService = userRatingService;
    }

    @PostMapping
    public ResponseEntity<UserRating> create(@Valid @RequestBody UserRating userRating) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userRatingService.create(userRating));
    }

    @GetMapping
    public ResponseEntity<List<UserRating>> getAll() {
        return ResponseEntity.ok(userRatingService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRating> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userRatingService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserRating> update(@PathVariable Long id, @Valid @RequestBody UserRating userRating) {
        return ResponseEntity.ok(userRatingService.update(id, userRating));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userRatingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
