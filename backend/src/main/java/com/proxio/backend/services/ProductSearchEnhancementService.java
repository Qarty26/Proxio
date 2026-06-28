package com.proxio.backend.services;

import com.proxio.backend.models.Product;
import com.proxio.backend.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductSearchEnhancementService {

    private final ProductRepository productRepository;

    public ProductSearchEnhancementService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<SearchResult> enhancedSearch(String query, int limit) {
        Set<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        return productRepository.findAll().stream()
                .map(product -> score(product, tokens))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(SearchResult::score).reversed()
                        .thenComparing(result -> result.product().getName()))
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
    }

    private SearchResult score(Product product, Set<String> tokens) {
        String name = normalize(product.getName());
        String description = normalize(product.getDescription());
        String category = product.getCategory() == null ? "" : normalize(product.getCategory().name());
        int score = 0;

        for (String token : tokens) {
            if (name.contains(token)) {
                score += 5;
            }
            if (category.contains(token)) {
                score += 3;
            }
            if (description.contains(token)) {
                score += 2;
            }
        }

        return new SearchResult(product, score, score >= 5 ? "high" : "medium");
    }

    private Set<String> tokenize(String query) {
        return Arrays.stream(normalize(query).split("\\s+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record SearchResult(Product product, int score, String confidence) {
    }
}
