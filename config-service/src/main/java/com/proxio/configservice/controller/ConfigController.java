package com.proxio.configservice.controller;

import com.proxio.configservice.service.ConfigCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigCatalogService configCatalogService;

    public ConfigController(ConfigCatalogService configCatalogService) {
        this.configCatalogService = configCatalogService;
    }

    @GetMapping
    public ResponseEntity<List<String>> configs() {
        return ResponseEntity.ok(configCatalogService.knownConfigs());
    }

    @GetMapping("/{application}/{profile}")
    public ResponseEntity<Map<String, Object>> config(@PathVariable String application, @PathVariable String profile) {
        return ResponseEntity.ok(configCatalogService.config(application, profile));
    }
}
