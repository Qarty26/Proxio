package com.proxio.backend.controllers;

import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.services.VendorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    public ResponseEntity<Vendor> create(@Valid @RequestBody VendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.create(toVendor(request)));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Vendor>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "farmName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(vendorService.getAllPaged(pageable));
    }

    @GetMapping
    public ResponseEntity<List<Vendor>> getAll() {
        return ResponseEntity.ok(vendorService.getAll());
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Vendor> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(vendorService.getByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vendor> update(@PathVariable Long id, @Valid @RequestBody VendorRequest request) {
        return ResponseEntity.ok(vendorService.update(id, toVendor(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Vendor toVendor(VendorRequest request) {
        Vendor vendor = new Vendor();
        vendor.setFarmName(request.farmName());
        vendor.setDescription(request.description());
        vendor.setProfileImageUrl(request.profileImageUrl());

        User user = new User();
        user.setId(request.user().id());
        vendor.setUser(user);

        return vendor;
    }

    public record VendorRequest(
            @NotNull(message = "User is required")
            IdReference user,

            @NotBlank(message = "Farm name is required")
            @Size(min = 2, max = 120, message = "Farm name must be between 2 and 120 characters")
            String farmName,

            @Size(max = 500, message = "Description must be at most 500 characters")
            String description,

            String profileImageUrl
    ) {
    }

    public record IdReference(@NotNull(message = "Id is required") Long id) {
    }
}
