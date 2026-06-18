package com.proxio.backend.services;

import com.proxio.backend.models.Product;
import com.proxio.backend.models.enums.ProductCategory;
import com.proxio.backend.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductCacheService {

    private static final String PRODUCTS_SUMMARY_KEY = "proxio:products:summary";

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public ProductCacheService(ProductRepository productRepository,
                               StringRedisTemplate redisTemplate,
                               @Value("${proxio.cache.products-summary-ttl-seconds:60}") long ttlSeconds) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public CachedSummary productsSummary() {
        try {
            String cached = redisTemplate.opsForValue().get(PRODUCTS_SUMMARY_KEY);
            if (cached != null) {
                return new CachedSummary(cached, true);
            }
        } catch (Exception ignored) {
            // The database path remains available when Redis is down.
        }

        String summary = buildSummary(productRepository.findAll());
        try {
            redisTemplate.opsForValue().set(PRODUCTS_SUMMARY_KEY, summary, ttl);
        } catch (Exception ignored) {
            // Cache writes are best-effort for the demo layer.
        }
        return new CachedSummary(summary, false);
    }

    public void evictProductsSummary() {
        try {
            redisTemplate.delete(PRODUCTS_SUMMARY_KEY);
        } catch (Exception ignored) {
            // Mutations should not fail if Redis is temporarily unavailable.
        }
    }

    private String buildSummary(List<Product> products) {
        Map<ProductCategory, Long> byCategory = products.stream()
                .collect(Collectors.groupingBy(Product::getCategory,
                        () -> new EnumMap<>(ProductCategory.class),
                        Collectors.counting()));
        String categorySummary = byCategory.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .sorted()
                .collect(Collectors.joining(","));

        return """
                {"totalProducts":%d,"byCategory":"%s"}
                """.formatted(products.size(), categorySummary);
    }

    public record CachedSummary(String json, boolean cacheHit) {
    }
}
