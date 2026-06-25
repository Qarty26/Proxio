package com.proxio.backend.controllers;

import com.proxio.backend.models.WeeklyOffer;
import com.proxio.backend.services.WeeklyOfferService;
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

import java.util.List;

@RestController
@RequestMapping("/api/weekly-offers")
public class WeeklyOfferController {

    private final WeeklyOfferService weeklyOfferService;

    public WeeklyOfferController(WeeklyOfferService weeklyOfferService) {
        this.weeklyOfferService = weeklyOfferService;
    }

    @PostMapping
    public ResponseEntity<WeeklyOffer> create(@RequestBody WeeklyOffer weeklyOffer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(weeklyOfferService.create(weeklyOffer));
    }

    @GetMapping
    public ResponseEntity<List<WeeklyOffer>> getAll() {
        return ResponseEntity.ok(weeklyOfferService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklyOffer> getById(@PathVariable Long id) {
        return ResponseEntity.ok(weeklyOfferService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeeklyOffer> update(@PathVariable Long id, @RequestBody WeeklyOffer weeklyOffer) {
        return ResponseEntity.ok(weeklyOfferService.update(id, weeklyOffer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        weeklyOfferService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
