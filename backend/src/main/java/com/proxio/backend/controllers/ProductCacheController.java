package com.proxio.backend.controllers;

import com.proxio.backend.services.ProductCacheService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductCacheController {

    private final ProductCacheService productCacheService;

    public ProductCacheController(ProductCacheService productCacheService) {
        this.productCacheService = productCacheService;
    }

    @GetMapping("/summary")
    public ResponseEntity<String> summary() {
        ProductCacheService.CachedSummary summary = productCacheService.productsSummary();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Cache", summary.cacheHit() ? "HIT" : "MISS")
                .body(summary.json());
    }
}
