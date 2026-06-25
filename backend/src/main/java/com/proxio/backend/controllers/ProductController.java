package com.proxio.backend.controllers;

import com.proxio.backend.models.Product;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.enums.ProductCategory;
import com.proxio.backend.services.ProductService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(toProduct(request), authentication));
    }

    // SPECIFIC endpoints FIRST - before the wildcard {id}
    @GetMapping("/paged")
    public ResponseEntity<Page<Product>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) ProductCategory category,
            Authentication authentication
    ) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Product> productPage;
        if (category != null) {
            productPage = productService.getByCategoryPaged(category, pageable, authentication);
        } else {
            productPage = productService.getAllPaged(pageable, authentication);
        }

        return ResponseEntity.ok(productPage);
    }

    @GetMapping("/all")
    public ResponseEntity<java.util.List<Product>> getAll(Authentication authentication) {
        return ResponseEntity.ok(productService.getAll(authentication));
    }

    // WILDCARD endpoint LAST
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request, Authentication authentication) {
        return ResponseEntity.ok(productService.update(id, toProduct(request), authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        productService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }

    private Product toProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setUnit(request.unit());
        product.setImageUrl(request.imageUrl());
        product.setCategory(request.category());

        Vendor vendor = new Vendor();
        vendor.setId(request.vendor().id());
        product.setVendor(vendor);

        return product;
    }

    public record ProductRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
            String name,

            @NotBlank(message = "Description is required")
            @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
            String description,

            @NotBlank(message = "Unit is required")
            @Size(max = 40, message = "Unit must be at most 40 characters")
            String unit,

            String imageUrl,

            @NotNull(message = "Category is required")
            ProductCategory category,

            @NotNull(message = "Vendor is required")
            IdReference vendor
    ) {
    }

    public record IdReference(@NotNull(message = "Id is required") Long id) {
    }
}
