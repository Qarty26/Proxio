package com.proxio.backend.controllers;

import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.services.PickupSlotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pickup-slots")
public class PickupSlotController {

    private final PickupSlotService pickupSlotService;

    public PickupSlotController(PickupSlotService pickupSlotService) {
        this.pickupSlotService = pickupSlotService;
    }

    @PostMapping
    public ResponseEntity<PickupSlot> create(@RequestBody PickupSlot pickupSlot, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pickupSlotService.create(pickupSlot, authentication));
    }

    @GetMapping
    public ResponseEntity<List<PickupSlot>> getAll(
            @RequestParam(required = false) Long locationId,
            Authentication authentication) {
        List<PickupSlot> result = locationId != null
                ? pickupSlotService.getByLocation(locationId)
                : pickupSlotService.getAll(authentication);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PickupSlot> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pickupSlotService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PickupSlot> update(@PathVariable Long id,
                                             @RequestBody PickupSlot pickupSlot,
                                             Authentication authentication) {
        return ResponseEntity.ok(pickupSlotService.update(id, pickupSlot, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        pickupSlotService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
