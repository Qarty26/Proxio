package com.proxio.configservice.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class ConfigCatalogService {

    private static final List<String> KNOWN_CONFIGS = List.of(
            "backend-dev",
            "api-gateway-dev",
            "notification-service-dev"
    );

    public Map<String, Object> config(String application, String profile) {
        String key = application + "-" + profile;
        if (!KNOWN_CONFIGS.contains(key)) {
            return Map.of(
                    "application", application,
                    "profile", profile,
                    "found", false,
                    "message", "No centralized config was registered for this application/profile."
            );
        }

        try {
            ClassPathResource resource = new ClassPathResource("config/" + key + ".yaml");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return Map.of(
                    "application", application,
                    "profile", profile,
                    "found", true,
                    "source", "classpath:/config/" + key + ".yaml",
                    "content", content
            );
        } catch (Exception exception) {
            return Map.of(
                    "application", application,
                    "profile", profile,
                    "found", false,
                    "message", exception.getMessage()
            );
        }
    }

    public List<String> knownConfigs() {
        return KNOWN_CONFIGS;
    }
}
