package com.proxio.backend.controllers;

import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.services.PickupSlotService;
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
@RequestMapping("/api/pickup-slots")
public class PickupSlotController {

    private final PickupSlotService pickupSlotService;

    public PickupSlotController(PickupSlotService pickupSlotService) {
        this.pickupSlotService = pickupSlotService;
    }

    @PostMapping
    public ResponseEntity<PickupSlot> create(@RequestBody PickupSlot pickupSlot) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pickupSlotService.create(pickupSlot));
    }

    @GetMapping
    public ResponseEntity<List<PickupSlot>> getAll() {
        return ResponseEntity.ok(pickupSlotService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PickupSlot> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pickupSlotService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PickupSlot> update(@PathVariable Long id, @RequestBody PickupSlot pickupSlot) {
        return ResponseEntity.ok(pickupSlotService.update(id, pickupSlot));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pickupSlotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
