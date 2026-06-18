package com.proxio.backend.controllers;

import com.proxio.backend.services.ProductSearchEnhancementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiSearchController {

    private final ProductSearchEnhancementService productSearchEnhancementService;

    public AiSearchController(ProductSearchEnhancementService productSearchEnhancementService) {
        this.productSearchEnhancementService = productSearchEnhancementService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductSearchEnhancementService.SearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(productSearchEnhancementService.enhancedSearch(query, limit));
    }
}
